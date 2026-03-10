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
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class YuanFenService {

    private final FollowService followService;
    private final MatchService matchService;
    private final UserMapper userMapper;
    private final YuanFenAnalysisSkill yuanFenSkill;
    private final YuanFenAnalysisLogMapper logMapper;
    private final ObjectMapper objectMapper;
    private final AiConfig aiConfig;

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

        // 4. 全局冷却检查：1小时内只能与一个用户分析
        //    如果在冷却期内已经和其他用户分析过，则阻止（但允许查看与同一用户的缓存结果）
        if (cooldownHours > 0) {
            try {
                LocalDateTime cooldownSince = LocalDateTime.now().minusHours(cooldownHours);
                // 查找当前用户最近的分析记录（作为A或B）
                YuanFenAnalysisLog recentLog = logMapper.selectOne(
                        new LambdaQueryWrapper<YuanFenAnalysisLog>()
                                .and(w -> w.eq(YuanFenAnalysisLog::getUserIdA, userId)
                                        .or().eq(YuanFenAnalysisLog::getUserIdB, userId))
                                .gt(YuanFenAnalysisLog::getCreatedAt, cooldownSince)
                                .orderByDesc(YuanFenAnalysisLog::getCreatedAt)
                                .last("LIMIT 1"));
                if (recentLog != null) {
                    // 如果最近分析的是不同的用户对，则阻止
                    boolean isSamePair = (recentLog.getUserIdA() == minId && recentLog.getUserIdB() == maxId);
                    if (!isSamePair) {
                        long remaining = Duration.between(LocalDateTime.now(),
                                recentLog.getCreatedAt().plusHours(cooldownHours)).getSeconds();
                        if (remaining > 0) {
                            throw new BusinessException(ResultCode.BAD_REQUEST,
                                    String.format("冷却中，%d分钟后可与新用户缘分分析", (remaining + 59) / 60));
                        }
                    }
                }
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.warn("查询全局冷却失败，继续处理", e);
            }
        }

        // 5. 检查缓存：-1=永久缓存，>0=冷却期内缓存，0=不缓存（每次调用AI）
        if (cooldownHours == 0) {
            log.info("YuanFen CACHE SKIP (cooldown=0, always call AI) userId={} targetUserId={}", userId, targetUserId);
        }
        if (cooldownHours != 0) {
            try {
                LambdaQueryWrapper<YuanFenAnalysisLog> wrapper = new LambdaQueryWrapper<YuanFenAnalysisLog>()
                        .eq(YuanFenAnalysisLog::getUserIdA, minId)
                        .eq(YuanFenAnalysisLog::getUserIdB, maxId)
                        .orderByDesc(YuanFenAnalysisLog::getCreatedAt)
                        .last("LIMIT 1");
                // 永久缓存(-1)：不限制时间；限时冷却(>0)：只查冷却期内的记录
                if (cooldownHours > 0) {
                    LocalDateTime cooldownSince = LocalDateTime.now().minusHours(cooldownHours);
                    wrapper.gt(YuanFenAnalysisLog::getCreatedAt, cooldownSince);
                }
                YuanFenAnalysisLog cachedLog = logMapper.selectOne(wrapper);

                if (cachedLog != null && cachedLog.getAiResult() != null) {
                    try {
                        YuanFenAnalysisResponse cached = objectMapper.readValue(
                                cachedLog.getAiResult(), YuanFenAnalysisResponse.class);
                        LocalDateTime nextAvailable = cooldownHours > 0
                                ? cachedLog.getCreatedAt().plusHours(cooldownHours)
                                : cachedLog.getCreatedAt(); // 永久缓存时 nextAvailable 无意义
                        cached.setNextAvailableAt(nextAvailable.toString());
                        cached.setGeneratedAt(cachedLog.getCreatedAt().toString());
                        log.info("YuanFen CACHE HIT userId={} targetUserId={}", userId, targetUserId);
                        return cached;
                    } catch (Exception e) {
                        log.warn("解析缓存的缘分分析结果失败，将重新生成", e);
                    }
                }
            } catch (Exception e) {
                log.warn("查询缘分解析缓存失败，继续生成新结果", e);
            }
        }

        // 5. 获取双方信息 + 匹配详情
        log.info("YuanFen NO CACHE, calling AI userId={} targetUserId={}", userId, targetUserId);
        User userA = userMapper.selectById(userId);
        User userB = userMapper.selectById(targetUserId);
        if (userA == null || userB == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        MatchResultResponse matchResult = matchService.getMatchDetail(targetUserId);

        // 6. 调用 AI（失败时自动降级为本地生成结果）
        var analysisResult = yuanFenSkill.analyze(userA, userB, matchResult);
        YuanFenAnalysisResponse result = analysisResult.getResponse();

        // 7. 保存日志（含 Token 消耗）
        try {
            YuanFenAnalysisLog logEntry = new YuanFenAnalysisLog();
            logEntry.setUserIdA(minId);
            logEntry.setUserIdB(maxId);
            logEntry.setTotalScore(matchResult.getMatchScore());
            logEntry.setAiResult(objectMapper.writeValueAsString(result));
            logEntry.setTokensUsed(analysisResult.getTokensUsed());
            logEntry.setCreatedAt(LocalDateTime.now());
            logMapper.insert(logEntry);
        } catch (Exception e) {
            log.warn("保存缘分解析日志失败（不影响返回结果）", e);
        }

        return result;
    }

    /**
     * 获取冷却剩余秒数（用于前端倒计时展示）
     * 检查全局冷却：1小时内只能与一个用户分析
     */
    public long getCooldownRemaining(Long targetUserId) {
        long cooldownHours = aiConfig.getYuanfenCooldownHours();
        if (cooldownHours <= 0) return 0;

        Long userId = CurrentUser.getId();
        long minId = Math.min(userId, targetUserId);
        long maxId = Math.max(userId, targetUserId);

        try {
            LocalDateTime cooldownSince = LocalDateTime.now().minusHours(cooldownHours);

            // 查找当前用户最近的分析记录（全局冷却）
            YuanFenAnalysisLog recentLog = logMapper.selectOne(
                    new LambdaQueryWrapper<YuanFenAnalysisLog>()
                            .and(w -> w.eq(YuanFenAnalysisLog::getUserIdA, userId)
                                    .or().eq(YuanFenAnalysisLog::getUserIdB, userId))
                            .gt(YuanFenAnalysisLog::getCreatedAt, cooldownSince)
                            .orderByDesc(YuanFenAnalysisLog::getCreatedAt)
                            .last("LIMIT 1"));

            if (recentLog == null) return 0;

            // 如果最近分析的就是同一对用户，返回该对的冷却时间
            boolean isSamePair = (recentLog.getUserIdA() == minId && recentLog.getUserIdB() == maxId);
            LocalDateTime nextAvailable = recentLog.getCreatedAt().plusHours(cooldownHours);
            long remaining = Duration.between(LocalDateTime.now(), nextAvailable).getSeconds();

            if (isSamePair) {
                // 同一对用户：返回缓存刷新剩余时间
                return Math.max(0, remaining);
            } else {
                // 不同用户：返回全局冷却剩余时间
                return Math.max(0, remaining);
            }
        } catch (Exception e) {
            log.warn("查询冷却状态失败", e);
            return 0;
        }
    }
}
