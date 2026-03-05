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
            // 表不存在或其他数据库错误，返回临时默认权重
            log.warn("Failed to query user weights, using in-memory default weights for user: {}", userId, e);
            return UserWeights.defaultWeights(userId);
        }
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
            // 发生任何错误时，返回默认权重
            log.warn("Failed to get effective weights for user: {}, using default weights", userId, e);
            return new HashMap<>(GlobalWeights.DEFAULT_WEIGHTS);
        }
    }

    /**
     * 用户触发行为后更新其个性化权重
     *
     * 保守策略说明（V2.1）：
     * 1. 只有"关注"行为才更新权重（明确的正向信号）
     * 2. "忽略"行为不更新权重（原因不明，可能是长相、距离等非匹配因素）
     * 3. 关注时：根据被关注用户的各维度得分，提高相应维度权重
     * 4. 每次调整极其微小（0.001级别），需要大量行为才能看到明显变化
     *
     * 设计理念：
     * - 用户忽略一个人可能是因为长相、距离、个人简介等非匹配因素
     * - 用户关注一个人说明整体画像有吸引力，但不知道具体是哪方面吸引
     * - 因此关注时均匀提高所有维度的权重，让系统更多样化推荐
     *
     * @param userId       操作用户
     * @param targetUserId 被操作的目标用户
     * @param actionType   行为类型
     * @param signalStrength 信号强度
     */
    @Transactional
    public void updateWeightsOnAction(Long userId, Long targetUserId, String actionType, int signalStrength) {
        // 保守策略：只有关注行为才更新权重
        if (!"FOLLOW".equals(actionType)) {
            // 其他行为（忽略、查看等）不更新权重
            return;
        }

        // 读取当前用户权重
        UserWeights weights = getUserWeights(userId);

        // 计算各维度得分（用于分析）
        Map<String, Integer> dimensionScores = calculateDimensionScores(userId, targetUserId);
        if (dimensionScores.isEmpty()) {
            log.warn("Failed to calculate dimension scores for users: {} -> {}", userId, targetUserId);
            return;
        }

        // 计算综合匹配分（用于判断推荐质量）
        double avgScore = dimensionScores.values().stream().mapToInt(Integer::intValue).average().orElse(50.0);

        // 保守的权重更新策略
        Map<String, Double> newWeights = new HashMap<>(weights.getWeightMap());

        // 如果关注的是高质量匹配（综合分>60），说明用户认可系统的推荐逻辑
        // 轻微提高所有维度权重，让系统继续按现有逻辑推荐
        if (avgScore > 60) {
            for (String dim : GlobalWeights.getAllDimensions()) {
                double currentWeight = newWeights.get(dim);
                double dimScore = dimensionScores.getOrDefault(dim, 50);

                // 该维度得分越高，权重增加越多（但总量很小）
                double scoreFactor = dimScore / 100.0; // 0.0~1.0
                double adjustment = 0.001 * scoreFactor * GlobalWeights.EMA_ALPHA;

                newWeights.put(dim, GlobalWeights.clipWeightToBounds(dim, currentWeight + adjustment));
            }
        }

        // 归一化，确保所有权重之和 = 1.0
        newWeights = GlobalWeights.normalizeWeights(newWeights);

        // 保存更新后的权重
        weights.updateWeights(newWeights);
        weights.incrementActionCount();
        try {
            userWeightsMapper.updateById(weights);
            log.info("User {} followed target {} (avg score: {}), slight weight adjustment applied",
                    userId, targetUserId, String.format("%.1f", avgScore));
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
            Map<String, Double> decayed = new HashMap<>();
            Map<String, Double> defaultW = GlobalWeights.DEFAULT_WEIGHTS;

            for (String dim : GlobalWeights.getAllDimensions()) {
                double current = uw.getWeight(dim);
                double def = defaultW.get(dim);
                // 衰减公式：w_new = w_default × decay + w_current × (1 - decay)
                double decayedWeight = def * GlobalWeights.WEEKLY_DECAY_RATE +
                                       current * (1 - GlobalWeights.WEEKLY_DECAY_RATE);
                decayed.put(dim, decayedWeight);
            }

            // 归一化
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
}
