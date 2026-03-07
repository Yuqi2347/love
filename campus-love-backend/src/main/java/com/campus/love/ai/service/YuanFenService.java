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

        // 3. 对称 Key：保证 (A,B) 和 (B,A) 共享冷却
        long minId = Math.min(userId, targetUserId);
        long maxId = Math.max(userId, targetUserId);

        // 4. 检查冷却 + 缓存（冷却时间为0时跳过）
        if (cooldownHours > 0) {
            try {
                LocalDateTime cooldownSince = LocalDateTime.now().minusHours(cooldownHours);
                YuanFenAnalysisLog cachedLog = logMapper.selectOne(
                        new LambdaQueryWrapper<YuanFenAnalysisLog>()
                                .eq(YuanFenAnalysisLog::getUserIdA, minId)
                                .eq(YuanFenAnalysisLog::getUserIdB, maxId)
                                .gt(YuanFenAnalysisLog::getCreatedAt, cooldownSince)
                                .orderByDesc(YuanFenAnalysisLog::getCreatedAt)
                                .last("LIMIT 1")
                );

                if (cachedLog != null && cachedLog.getAiResult() != null) {
                    try {
                        YuanFenAnalysisResponse cached = objectMapper.readValue(
                                cachedLog.getAiResult(), YuanFenAnalysisResponse.class);
                        LocalDateTime nextAvailable = cachedLog.getCreatedAt().plusHours(cooldownHours);
                        cached.setNextAvailableAt(nextAvailable.toString());
                        cached.setGeneratedAt(cachedLog.getCreatedAt().toString());
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
        User userA = userMapper.selectById(userId);
        User userB = userMapper.selectById(targetUserId);
        if (userA == null || userB == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        MatchResultResponse matchResult = matchService.getMatchDetail(targetUserId);

        // 6. 调用 AI（失败时自动降级为本地生成结果）
        YuanFenAnalysisResponse result = yuanFenSkill.analyze(userA, userB, matchResult);

        // 7. 保存日志
        try {
            YuanFenAnalysisLog logEntry = new YuanFenAnalysisLog();
            logEntry.setUserIdA(minId);
            logEntry.setUserIdB(maxId);
            logEntry.setTotalScore(matchResult.getMatchScore());
            logEntry.setAiResult(objectMapper.writeValueAsString(result));
            logMapper.insert(logEntry);
        } catch (Exception e) {
            log.warn("保存缘分解析日志失败（不影响返回结果）", e);
        }

        return result;
    }

    /**
     * 获取冷却剩余秒数（用于前端倒计时展示）
     */
    public long getCooldownRemaining(Long targetUserId) {
        long cooldownHours = aiConfig.getYuanfenCooldownHours();
        if (cooldownHours <= 0) return 0;

        Long userId = CurrentUser.getId();
        long minId = Math.min(userId, targetUserId);
        long maxId = Math.max(userId, targetUserId);

        try {
            LocalDateTime cooldownSince = LocalDateTime.now().minusHours(cooldownHours);
            YuanFenAnalysisLog last = logMapper.selectOne(
                    new LambdaQueryWrapper<YuanFenAnalysisLog>()
                            .eq(YuanFenAnalysisLog::getUserIdA, minId)
                            .eq(YuanFenAnalysisLog::getUserIdB, maxId)
                            .gt(YuanFenAnalysisLog::getCreatedAt, cooldownSince)
                            .orderByDesc(YuanFenAnalysisLog::getCreatedAt)
                            .last("LIMIT 1")
            );

            if (last == null) return 0;

            LocalDateTime nextAvailable = last.getCreatedAt().plusHours(cooldownHours);
            long remaining = Duration.between(LocalDateTime.now(), nextAvailable).getSeconds();
            return Math.max(0, remaining);
        } catch (Exception e) {
            log.warn("查询冷却状态失败", e);
            return 0;
        }
    }
}
