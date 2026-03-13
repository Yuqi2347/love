package com.campus.love.match.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.common.utils.BaziCalculator;
import com.campus.love.match.constants.GlobalWeights;
import com.campus.love.match.constants.MbtiCompatibilityMatrix;
import com.campus.love.match.constants.ZodiacCompatibilityTable;
import com.campus.love.match.entity.UserWeights;
import com.campus.love.match.mapper.UserWeightsMapper;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户个性化权重服务（V2.0）
 *
 * 功能：
 * 1. getUserWeights() - 获取用户权重
 * 2. updateWeightsOnAction() - 行为触发权重更新（EMA算法）
 * 3. decayWeightsTowardDefault() - 权重时间衰减
 *
 * @author Campus Love Team
 * @version 2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserWeightService {

    private final UserWeightsMapper userWeightsMapper;
    private final UserMapper userMapper;
    private final InterestMatcher interestMatcher;
    private final MajorCategoryMatcher majorCategoryMatcher;

    /**
     * 获取用户权重
     * 如果不存在则创建默认权重
     */
    public UserWeights getUserWeights(Long userId) {
        try {
            UserWeights weights = userWeightsMapper.selectOne(
                    new LambdaQueryWrapper<UserWeights>()
                            .eq(UserWeights::getUserId, userId)
            );

            if (weights == null) {
                weights = UserWeights.defaultWeights(userId);
                try {
                    userWeightsMapper.insert(weights);
                    log.debug("Created default weights for user: {}", userId);
                } catch (Exception e) {
                    log.warn("Table t_user_match_weights may not exist, using in-memory default weights for user: {}", userId);
                }
            }

            return weights;
        } catch (Exception e) {
            if (isTableOrConfigException(e)) {
                log.warn("Table t_user_match_weights may not exist or config not ready, using in-memory default weights for user: {}", userId);
            } else {
                log.error("Failed to query user weights for user: {}", userId, e);
            }
            return UserWeights.defaultWeights(userId);
        }
    }

    /** 是否为表不存在/配置未就绪类异常（可接受降级），否则视为数据异常需打 error */
    private static boolean isTableOrConfigException(Throwable e) {
        String msg = e.getMessage();
        if (msg == null) {
            return false;
        }
        String lower = msg.toLowerCase();
        return lower.contains("doesn't exist") || lower.contains("unknown table")
                || lower.contains("exist") && (lower.contains("table") || lower.contains("database"));
    }

    /**
     * 获取用户的有效权重Map
     * 如果行为次数不足冷启动阈值，返回全局默认权重
     */
    public Map<String, Double> getEffectiveWeights(Long userId) {
        try {
            UserWeights weights = getUserWeights(userId);

            if (!weights.canUsePersonalizedWeights()) {
                log.debug("User {} is in cold start (count={}), using default weights",
                        userId, weights.getActionCount());
                return new HashMap<>(GlobalWeights.DEFAULT_WEIGHTS);
            }

            return weights.getWeightMap();
        } catch (Exception e) {
            if (isTableOrConfigException(e)) {
                log.warn("Weights table/config not ready for user: {}, using default weights", userId);
            } else {
                log.error("Failed to get effective weights for user: {}", userId, e);
            }
            return new HashMap<>(GlobalWeights.DEFAULT_WEIGHTS);
        }
    }

    /**
     * 用户触发行为后更新其个性化权重
     *
     * 平衡策略说明（V2.1）：
     * 1. "关注"行为：提高被关注用户优势维度的权重（正向信号）
     * 2. "忽略"行为：降低被忽略用户低分维度的权重（负向信号）
     * 3. 权重调整幅度适中，用户行为能较快影响推荐
     *
     * @param userId       操作用户
     * @param targetUserId 被操作的目标用户
     * @param actionType   行为类型
     * @param signalStrength 信号强度
     */
    @Transactional
    public void updateWeightsOnAction(Long userId, Long targetUserId, String actionType, int signalStrength) {
        // 只处理关注和忽略行为
        if (!"FOLLOW".equals(actionType) && !"IGNORE".equals(actionType)) {
            return;
        }

        // 读取当前用户权重
        UserWeights weights = getUserWeights(userId);

        // 计算各维度得分
        Map<String, Integer> dimensionScores = calculateDimensionScores(userId, targetUserId);
        if (dimensionScores.isEmpty()) {
            log.warn("Failed to calculate dimension scores for users: {} -> {}", userId, targetUserId);
            return;
        }

        // 计算综合匹配分
        double avgScore = dimensionScores.values().stream().mapToInt(Integer::intValue).average().orElse(50.0);

        Map<String, Double> newWeights = new HashMap<>(weights.getWeightMap());

        if ("FOLLOW".equals(actionType)) {
            // 关注行为：提高被关注用户优势维度的权重
            // 如果关注的是高质量匹配（综合分>55），提高所有维度权重
            if (avgScore > 55) {
                for (String dim : GlobalWeights.getAllDimensions()) {
                    double currentWeight = newWeights.get(dim);
                    double dimScore = dimensionScores.getOrDefault(dim, 50);

                    // 该维度得分越高，权重增加越多
                    double scoreFactor = dimScore / 100.0;
                    // 增大调整幅度：0.001 → 0.005
                    double adjustment = 0.005 * scoreFactor * GlobalWeights.EMA_ALPHA;

                    newWeights.put(dim, GlobalWeights.clipWeightToBounds(dim, currentWeight + adjustment));
                }
            }
        } else if ("IGNORE".equals(actionType)) {
            // 忽略行为：降低低分维度的权重
            // 如果忽略的是低质量匹配（综合分<50），降低其低分维度权重
            if (avgScore < 50) {
                for (String dim : GlobalWeights.getAllDimensions()) {
                    double currentWeight = newWeights.get(dim);
                    double dimScore = dimensionScores.getOrDefault(dim, 50);

                    // 该维度得分越低，权重降低越多
                    double scoreFactor = (50 - dimScore) / 50.0; // 0~1，得分越低因子越大
                    if (scoreFactor > 0) {
                        // 负向调整，但幅度稍微小一些
                        double adjustment = 0.003 * scoreFactor * GlobalWeights.EMA_ALPHA;

                        newWeights.put(dim, GlobalWeights.clipWeightToBounds(dim, currentWeight - adjustment));
                    }
                }
            }
        }

        // bazi_unknown 时八字权重锁定为 0（技术文档 V1.1.0）
        User user = userMapper.selectById(userId);
        if (user != null && Boolean.TRUE.equals(user.getBaziUnknown())) {
            newWeights.put("bazi", 0.0);
        }
        newWeights = GlobalWeights.normalizeWeights(newWeights);

        // 保存更新后的权重
        weights.updateWeights(newWeights);
        weights.incrementActionCount();
        try {
            userWeightsMapper.updateById(weights);
            log.info("User {} {} target {} (avg score: {}), weight adjustment applied",
                    userId, actionType.toLowerCase(), targetUserId, String.format("%.1f", avgScore));
        } catch (Exception e) {
            log.warn("Failed to save weights to database (table may not exist), using in-memory weights for user: {}", userId, e);
        }
    }

    /**
     * 权重时间衰减（每周执行一次）
     * 防止早期行为永久影响权重
     */
    @Scheduled(cron = "0 0 3 * * MON")
    @Transactional
    public void decayWeightsTowardDefault() {
        List<UserWeights> allWeights;
        try {
            allWeights = userWeightsMapper.selectList(null);
        } catch (Exception e) {
            log.warn("Failed to query weights for decay (table may not exist), skipping decay: {}", e.getMessage());
            return;
        }

        if (allWeights.isEmpty()) {
            log.debug("No weights to decay");
            return;
        }

        for (UserWeights uw : allWeights) {
            User user = userMapper.selectById(uw.getUserId());
            Map<String, Double> defaultW = (user != null && Boolean.TRUE.equals(user.getBaziUnknown()))
                    ? GlobalWeights.WITHOUT_BAZI : GlobalWeights.DEFAULT_WEIGHTS;

            Map<String, Double> decayed = new HashMap<>();
            for (String dim : GlobalWeights.getAllDimensions()) {
                double current = uw.getWeight(dim);
                double def = defaultW.get(dim);
                double decayedWeight = def * GlobalWeights.WEEKLY_DECAY_RATE +
                                       current * (1 - GlobalWeights.WEEKLY_DECAY_RATE);
                decayed.put(dim, decayedWeight);
            }
            if (user != null && Boolean.TRUE.equals(user.getBaziUnknown())) {
                decayed.put("bazi", 0.0);
            }
            decayed = GlobalWeights.normalizeWeights(decayed);
            uw.updateWeights(decayed);
        }

        // 批量更新
        int successCount = 0;
        for (UserWeights uw : allWeights) {
            try {
                userWeightsMapper.updateById(uw);
                successCount++;
            } catch (Exception e) {
                log.warn("Failed to update decayed weights for user {}: {}", uw.getUserId(), e.getMessage());
            }
        }
        log.info("Decayed weights for {} / {} users toward default", successCount, allWeights.size());
    }

    /**
     * 计算两个用户在各维度的得分
     */
    private Map<String, Integer> calculateDimensionScores(Long userId1, Long userId2) {
        User user1 = userMapper.selectById(userId1);
        User user2 = userMapper.selectById(userId2);

        if (user1 == null || user2 == null) {
            return Collections.emptyMap();
        }

        Map<String, Integer> scores = new HashMap<>();

        // 兴趣匹配
        scores.put("interest", interestMatcher.calculateInterestScore(
                user1.getInterests(), user2.getInterests()));

        // MBTI匹配
        scores.put("mbti", MbtiCompatibilityMatrix.getCompatibility(
                user1.getMbti(), user2.getMbti()));

        // 星座匹配
        scores.put("zodiac", ZodiacCompatibilityTable.getCompatibility(
                user1.getZodiac(), user2.getZodiac()));

        // 八字匹配
        LocalDateTime birth1 = user1.getBirthDate() != null ?
                LocalDateTime.of(user1.getBirthDate(),
                        user1.getBirthTime() != null ? user1.getBirthTime() : java.time.LocalTime.NOON) :
                null;
        LocalDateTime birth2 = user2.getBirthDate() != null ?
                LocalDateTime.of(user2.getBirthDate(),
                        user2.getBirthTime() != null ? user2.getBirthTime() : java.time.LocalTime.NOON) :
                null;
        scores.put("bazi", BaziCalculator.calculateHunYinScore(birth1, birth2));

        // 专业匹配
        scores.put("major", majorCategoryMatcher.calculateMajorScore(
                user1.getMajor(), user2.getMajor()));

        // 年龄匹配
        scores.put("age", calculateAgeScore(user1.getBirthDate(), user2.getBirthDate()));

        return scores;
    }

    /**
     * 归一化维度得分到[-1, 1]
     * 以50为中心，高于50为正，低于50为负
     */
    private Map<String, Double> normalizeScores(Map<String, Integer> scores) {
        Map<String, Double> normalized = new HashMap<>();

        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            double normalizedValue = (entry.getValue() - 50.0) / 50.0;
            normalized.put(entry.getKey(), Math.max(-1.0, Math.min(1.0, normalizedValue)));
        }

        return normalized;
    }

    /**
     * 计算年龄匹配分
     */
    private int calculateAgeScore(LocalDate date1, LocalDate date2) {
        if (date1 == null || date2 == null) return 50;
        int diff = Math.abs(Period.between(date1, date2).getYears());
        if (diff == 0) return 100;
        if (diff >= GlobalWeights.MAX_AGE_DIFF_YEARS) return 20;
        return 100 - (diff * 80 / GlobalWeights.MAX_AGE_DIFF_YEARS);
    }

    /**
     * 批量预热用户权重（用于系统初始化）
     */
    @Transactional
    public void warmUpUserWeights(List<Long> userIds) {
        for (Long userId : userIds) {
            getUserWeights(userId);
        }
        log.info("Warmed up weights for {} users", userIds.size());
    }

    /**
     * 重置用户权重为默认值（管理员功能）
     */
    @Transactional
    public void resetUserWeights(Long userId) {
        UserWeights weights = getUserWeights(userId);
        weights.updateWeights(GlobalWeights.DEFAULT_WEIGHTS);
        weights.setActionCount(0);
        try {
            userWeightsMapper.updateById(weights);
            log.info("Reset weights for user {} to default", userId);
        } catch (Exception e) {
            log.warn("Failed to save reset weights to database (table may not exist) for user: {}", userId, e);
            log.info("Reset in-memory weights for user {} to default", userId);
        }
    }

    /**
     * 获取权重使用统计
     */
    public Map<String, Object> getWeightStats(Long userId) {
        UserWeights weights = getUserWeights(userId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("userId", userId);
        stats.put("actionCount", weights.getActionCount());
        stats.put("canUsePersonalizedWeights", weights.canUsePersonalizedWeights());
        stats.put("weights", weights.getWeightMap());
        stats.put("lastUpdated", weights.getLastUpdated());

        return stats;
    }

    /**
     * 用户手动设置权重偏好（高/中/低）
     * high=0.25, medium=0.15, low=0.05，然后归一化确保总和为1
     */
    public void setWeightPreferences(Long userId, Map<String, String> preferences) {
        Map<String, Double> rawWeights = new HashMap<>();
        for (String dim : GlobalWeights.getAllDimensions()) {
            String level = preferences.getOrDefault(dim, "medium");
            double w;
            switch (level.toLowerCase()) {
                case "high": w = 0.25; break;
                case "low": w = 0.05; break;
                default: w = 0.15; break;
            }
            rawWeights.put(dim, w);
        }
        // 归一化并裁剪到边界
        Map<String, Double> normalized = GlobalWeights.normalizeWeights(rawWeights);
        for (Map.Entry<String, Double> entry : normalized.entrySet()) {
            entry.setValue(GlobalWeights.clipWeightToBounds(entry.getKey(), entry.getValue()));
        }
        normalized = GlobalWeights.normalizeWeights(normalized);

        UserWeights weights = getUserWeights(userId);
        weights.updateWeights(normalized);
        weights.setActionCount(Math.max(weights.getActionCount() != null ? weights.getActionCount() : 0,
                GlobalWeights.COLD_START_THRESHOLD));
        try {
            userWeightsMapper.updateById(weights);
            log.info("Updated weight preferences for user {}: {}", userId, normalized);
        } catch (Exception e) {
            log.warn("Failed to save weight preferences to database for user: {}", userId, e);
        }
    }
}
