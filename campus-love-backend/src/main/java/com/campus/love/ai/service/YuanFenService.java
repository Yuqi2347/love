package com.campus.love.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.ai.config.AiConfig;
import com.campus.love.ai.dto.YuanFenAnalysisResponse;
import com.campus.love.ai.entity.YuanFenAnalysisLog;
import com.campus.love.ai.mapper.YuanFenAnalysisLogMapper;
import com.campus.love.ai.skill.YuanFenAnalysisSkill;
import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.follow.service.FollowService;
import com.campus.love.match.dto.MatchResultResponse;
import com.campus.love.match.service.MatchService;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonProcessingException;

@Slf4j
@Service
@RequiredArgsConstructor
public class YuanFenService {

    private static final String REDIS_INFLIGHT = "campus:love:yuanfen:inflight:";
    private static final String REDIS_BURST = "campus:love:yuanfen:burst:";
    private static final String REDIS_GLOBAL_AI_HOUR = "campus:love:yuanfen:ai:global:hour:";
    private static final DateTimeFormatter YUANFEN_HOUR_BUCKET = DateTimeFormatter.ofPattern("yyyyMMddHH");

    /**
     * 与 application.yml 中 spring.jackson.time-zone、JDBC serverTimezone 一致。
     * Docker/云主机若 JVM 默认 UTC 而库按上海时间存 DATETIME，用 now() 算冷却窗会导致永远查不到「窗内」记录。
     */
    private static final ZoneId YUANFEN_ZONE = ZoneId.of("Asia/Shanghai");

    private final FollowService followService;
    private final MatchService matchService;
    private final UserMapper userMapper;
    private final YuanFenAnalysisSkill yuanFenSkill;
    private final YuanFenAnalysisLogMapper logMapper;
    private final ObjectMapper objectMapper;
    private final AiConfig aiConfig;
    private final StringRedisTemplate stringRedisTemplate;

    public YuanFenAnalysisResponse getAnalysis(Long targetUserId) {
        Long userId = CurrentUser.getId();
        long cooldownHours = aiConfig.getYuanfenCooldownHours();

        // 1. 不能对自己分析
        if (userId.equals(targetUserId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }

        // 2. 校验互相关注
        if (!followService.isMutual(userId, targetUserId)) {
            throw new BusinessException(ResultCode.YUANFEN_NOT_MUTUAL);
        }

        // 3. 对称 Key：保证 (A,B) 和 (B,A) 共享缓存和冷却
        long minId = Math.min(userId, targetUserId);
        long maxId = Math.max(userId, targetUserId);

        // 4. 检查缓存：按用户对独立冷却，冷却期内返回上次缓存结果
        if (cooldownHours == 0) {
            log.info("YuanFen CACHE SKIP (cooldown=0, always call AI) userId={} targetUserId={}", userId, targetUserId);
        }
        if (cooldownHours != 0) {
            try {
                LocalDateTime cooldownSince = cooldownHours > 0 ? nowForYuanfen().minusHours(cooldownHours) : null;
                YuanFenAnalysisLog cachedLog = selectLatestLogForPair(minId, maxId, cooldownSince);

                if (cachedLog == null) {
                    log.info(
                            "YuanFen CACHE MISS 无记录: userId={} targetUserId={} pair=({}, {}) cooldownHours={} windowSince={}",
                            userId,
                            targetUserId,
                            minId,
                            maxId,
                            cooldownHours,
                            cooldownSince);
                } else if (cachedLog.getAiResult() == null || cachedLog.getAiResult().isBlank()) {
                    log.warn(
                            "YuanFen CACHE MISS ai_result 为空: logId={} pair=({}, {})",
                            cachedLog.getId(),
                            minId,
                            maxId);
                } else {
                    try {
                        YuanFenAnalysisResponse cached = parseCachedAiJson(cachedLog.getAiResult());
                        cached.setGeneratedAt(cachedLog.getCreatedAt().toString());
                        cached.setNextAvailableAt(null);
                        cached.setFromCache(Boolean.TRUE);
                        log.info("YuanFen CACHE HIT userId={} targetUserId={} logId={}", userId, targetUserId, cachedLog.getId());
                        return cached;
                    } catch (Exception e) {
                        String preview = cachedLog.getAiResult().length() > 240
                                ? cachedLog.getAiResult().substring(0, 240) + "..."
                                : cachedLog.getAiResult();
                        log.warn(
                                "YuanFen CACHE MISS JSON 解析失败，将重新调 AI: logId={} preview={}",
                                cachedLog.getId(),
                                preview,
                                e);
                    }
                }
            } catch (Exception e) {
                log.error(
                        "YuanFen 查询缓存异常（将尝试调 AI）: userId={} targetUserId={} pair=({}, {})",
                        userId,
                        targetUserId,
                        minId,
                        maxId,
                        e);
            }
        }

        // 4b. 限时冷却：一小时内若已与其他好友做过解析，则不能再对新一对调 AI（本对已有缓存的已在 4 步返回）
        if (cooldownHours > 0
                && findLatestBlockingOtherPairLog(
                                userId, minId, maxId, nowForYuanfen().minusHours(cooldownHours))
                        != null) {
            throw new BusinessException(ResultCode.YUANFEN_COOLDOWN);
        }

        // 5～7. 同对互斥 + 发起频控，再拉数据、调 AI、落库（中断未落库不算「完成一次」，但会计入 burst 防刷）
        int yfTimeout = aiConfig.getYuanfenTimeoutSeconds() > 0 ? aiConfig.getYuanfenTimeoutSeconds() : 60;
        int inflightTtl = Math.max(aiConfig.getYuanfenInflightTtlSeconds(), yfTimeout + 25);
        boolean inflightHeld = false;
        try {
            if (!tryAcquireInflightLock(minId, maxId, inflightTtl)) {
                throw new BusinessException(ResultCode.YUANFEN_IN_PROGRESS);
            }
            inflightHeld = true;
            recordStartBurstOrThrow(userId);

            log.info("YuanFen NO CACHE, calling AI userId={} targetUserId={}", userId, targetUserId);
            User userA = userMapper.selectById(userId);
            User userB = userMapper.selectById(targetUserId);
            if (userA == null || userB == null) {
                throw new BusinessException(ResultCode.USER_NOT_FOUND);
            }

            MatchResultResponse matchResult = matchService.getMatchDetail(targetUserId);

            if (!tryReserveGlobalHourlyAiSlot()) {
                throw new BusinessException(ResultCode.YUANFEN_AI_GLOBAL_BUSY);
            }

            var analysisResult = yuanFenSkill.analyze(userA, userB, matchResult);
            YuanFenAnalysisResponse result = analysisResult.getResponse();
            result.setFromCache(Boolean.FALSE);

            persistAnalysisLogWithRetry(minId, maxId, matchResult.getMatchScore(), result, analysisResult.getTokensUsed());

            result.setNextAvailableAt(null);
            return result;
        } finally {
            if (inflightHeld) {
                releaseInflightLock(minId, maxId);
            }
        }
    }

    private LocalDateTime nowForYuanfen() {
        return LocalDateTime.now(YUANFEN_ZONE);
    }

    /**
     * 全站当前自然小时（上海时区）内限制「真实调 AI」次数；缓存命中路径不会调用本方法。
     * 无法读 Redis 时不拦截，避免误杀。
     */
    private boolean tryReserveGlobalHourlyAiSlot() {
        int max = aiConfig.getYuanfenGlobalAiHourlyMax();
        if (max <= 0) {
            return true;
        }
        String bucket = nowForYuanfen().format(YUANFEN_HOUR_BUCKET);
        String key = REDIS_GLOBAL_AI_HOUR + bucket;
        try {
            Long n = stringRedisTemplate.opsForValue().increment(key);
            if (n != null && n == 1L) {
                long sec = secondsUntilEndOfHourShanghai();
                if (sec > 0) {
                    stringRedisTemplate.expire(key, sec, TimeUnit.SECONDS);
                }
            }
            if (n != null && n > max) {
                stringRedisTemplate.opsForValue().decrement(key);
                log.warn("YuanFen 全站本小时 AI 次数已达上限 {}/{}", max, bucket);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("YuanFen 全站小时计数 Redis 异常，放行: {}", e.getMessage());
            return true;
        }
    }

    private long secondsUntilEndOfHourShanghai() {
        ZonedDateTime now = ZonedDateTime.now(YUANFEN_ZONE);
        ZonedDateTime end = now.withMinute(0).withSecond(0).withNano(0).plusHours(1);
        return Math.max(60L, Duration.between(now, end).getSeconds());
    }

    /** 与 YuanFenAnalysisSkill 一致：去 markdown 壳、去弯引号，避免库里文本与 readValue 不一致 */
    private YuanFenAnalysisResponse parseCachedAiJson(String raw) throws JsonProcessingException {
        String json = raw.trim();
        if (json.startsWith("```")) {
            json = json.replaceFirst("```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }
        json = json.replace("「", "").replace("」", "").replace("『", "").replace("』", "")
                .replace("\u201C", "").replace("\u201D", "");
        return objectMapper.readValue(json, YuanFenAnalysisResponse.class);
    }

    /**
     * 取该对最新一条日志；限时窗内 {@code windowSince != null} 时仅保留 created_at &gt;= windowSince。
     * 条件必须在 orderBy/last 之前追加，否则 MP 生成 SQL 可能错误导致永远查不到行。
     */
    private YuanFenAnalysisLog selectLatestLogForPair(long minId, long maxId, LocalDateTime windowSince) {
        LambdaQueryWrapper<YuanFenAnalysisLog> wrapper = new LambdaQueryWrapper<YuanFenAnalysisLog>()
                .eq(YuanFenAnalysisLog::getUserIdA, minId)
                .eq(YuanFenAnalysisLog::getUserIdB, maxId);
        if (windowSince != null) {
            wrapper.ge(YuanFenAnalysisLog::getCreatedAt, windowSince);
        }
        wrapper.orderByDesc(YuanFenAnalysisLog::getCreatedAt).last("LIMIT 1");
        List<YuanFenAnalysisLog> list = logMapper.selectList(wrapper);
        return list.isEmpty() ? null : list.get(0);
    }

    private void persistAnalysisLogWithRetry(
            long minId,
            long maxId,
            Integer totalScore,
            YuanFenAnalysisResponse result,
            Integer tokensUsed) {
        String json;
        try {
            json = objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("缘分解析结果序列化失败，无法落库: minId={} maxId={}", minId, maxId, e);
            return;
        }
        Exception last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                YuanFenAnalysisLog logEntry = new YuanFenAnalysisLog();
                logEntry.setUserIdA(minId);
                logEntry.setUserIdB(maxId);
                logEntry.setTotalScore(totalScore);
                logEntry.setAiResult(json);
                logEntry.setTokensUsed(tokensUsed);
                logEntry.setCreatedAt(nowForYuanfen());
                int rows = logMapper.insert(logEntry);
                if (rows >= 1) {
                    if (attempt > 1) {
                        log.info("缘分解析日志第 {} 次重试后写入成功: minId={} maxId={}", attempt, minId, maxId);
                    }
                    return;
                }
            } catch (Exception e) {
                last = e;
                log.warn("保存缘分解析日志失败 (尝试 {}/3): minId={} maxId={} — {}", attempt, minId, maxId, e.getMessage());
                try {
                    Thread.sleep(80L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        log.error(
                "保存缘分解析日志最终失败，退出再登将无法命中缓存。请查: 1) Flyway 是否执行 2) total_score/ai_result 列类型 3) 库连接。minId={} maxId={}",
                minId,
                maxId,
                last);
    }

    /** 同对用户仅允许一个 AI 请求在途 */
    private boolean tryAcquireInflightLock(long minId, long maxId, int ttlSeconds) {
        try {
            String key = REDIS_INFLIGHT + minId + ":" + maxId;
            Boolean ok = stringRedisTemplate.opsForValue()
                    .setIfAbsent(key, "1", Duration.ofSeconds(Math.max(30, ttlSeconds)));
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            log.warn("YuanFen inflight lock redis error, allow request: {}", e.getMessage());
            return true;
        }
    }

    private void releaseInflightLock(long minId, long maxId) {
        try {
            stringRedisTemplate.delete(REDIS_INFLIGHT + minId + ":" + maxId);
        } catch (Exception e) {
            log.warn("YuanFen inflight release: {}", e.getMessage());
        }
    }

    /**
     * 缓存命中不会调用本方法。每次真正进入 AI 流程计 1 次，用于限制「取消再点」等刷接口行为。
     */
    private void recordStartBurstOrThrow(Long userId) {
        int max = aiConfig.getYuanfenStartBurstMax();
        if (max <= 0) {
            return;
        }
        int window = Math.max(60, aiConfig.getYuanfenStartBurstWindowSeconds());
        try {
            String key = REDIS_BURST + userId;
            Long c = stringRedisTemplate.opsForValue().increment(key);
            if (c != null && c == 1L) {
                stringRedisTemplate.expire(key, window, TimeUnit.SECONDS);
            }
            if (c != null && c > max) {
                stringRedisTemplate.opsForValue().decrement(key);
                throw new BusinessException(ResultCode.YUANFEN_START_BURST);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("YuanFen burst counter redis error, skip: {}", e.getMessage());
        }
    }

    /**
     * 获取缘分分析缓存（仅读取，不触发 AI）
     * 供破冰、心动时刻等 Agent 复用缘分分析结果
     */
    public Optional<YuanFenAnalysisResponse> getCachedAnalysis(Long userAId, Long userBId) {
        if (userAId == null || userBId == null || userAId.equals(userBId)) return Optional.empty();
        long minId = Math.min(userAId, userBId);
        long maxId = Math.max(userAId, userBId);
        try {
            YuanFenAnalysisLog cachedLog = selectLatestLogForPair(minId, maxId, null);
            if (cachedLog != null && cachedLog.getAiResult() != null && !cachedLog.getAiResult().isBlank()) {
                YuanFenAnalysisResponse r = parseCachedAiJson(cachedLog.getAiResult());
                return Optional.of(r);
            }
        } catch (Exception e) {
            log.warn("getCachedAnalysis failed: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * 获取冷却剩余秒数（用于前端倒计时展示）
     * <ul>
     *   <li>本对在当前冷却窗口内已有结果：返回 0（可点开弹窗，后端走缓存、不调 AI）</li>
     *   <li>本对尚无结果，但本小时已与其他好友做过解析：返回距离「可解析新好友」的剩余秒数</li>
     * </ul>
     */
    public long getCooldownRemaining(Long targetUserId) {
        long cooldownHours = aiConfig.getYuanfenCooldownHours();
        if (cooldownHours <= 0) return 0;

        Long userId = CurrentUser.getId();
        long minId = Math.min(userId, targetUserId);
        long maxId = Math.max(userId, targetUserId);
        LocalDateTime now = nowForYuanfen();
        LocalDateTime since = now.minusHours(cooldownHours);

        try {
            YuanFenAnalysisLog pairInWindow = selectLatestLogForPair(minId, maxId, since);
            if (pairInWindow != null) {
                return 0;
            }

            YuanFenAnalysisLog blocking = findLatestBlockingOtherPairLog(userId, minId, maxId, since);
            if (blocking == null || blocking.getCreatedAt() == null) {
                return 0;
            }
            LocalDateTime nextAvailable = blocking.getCreatedAt().plusHours(cooldownHours);
            return Math.max(0, Duration.between(now, nextAvailable).getSeconds());
        } catch (Exception e) {
            log.warn("查询冷却状态失败", e);
            return 0;
        }
    }

    /**
     * 当前用户在 since 之后参与的、且不是 (minId,maxId) 这一对的、最近一条解析（用于全局一小时锁）
     */
    private YuanFenAnalysisLog findLatestBlockingOtherPairLog(
            Long userId, long minId, long maxId, LocalDateTime since) {
        List<YuanFenAnalysisLog> list = logMapper.selectList(
                new LambdaQueryWrapper<YuanFenAnalysisLog>()
                        .nested(w -> w.eq(YuanFenAnalysisLog::getUserIdA, userId)
                                .or()
                                .eq(YuanFenAnalysisLog::getUserIdB, userId))
                        .ge(YuanFenAnalysisLog::getCreatedAt, since)
                        .and(w -> w.ne(YuanFenAnalysisLog::getUserIdA, minId)
                                .or()
                                .ne(YuanFenAnalysisLog::getUserIdB, maxId))
                        .orderByDesc(YuanFenAnalysisLog::getCreatedAt)
                        .last("LIMIT 1"));
        return list.isEmpty() ? null : list.get(0);
    }
}
