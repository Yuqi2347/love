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
     * @param userId       操作用户
     * @param targetUserId 被操作的目标用户
     * @param actionType   行为类型
     * @param signalStrength 信号强度（+1关注, -1忽略, +2聊天, -3拉黑, 0查看）
     */
    @Transactional
    public void updateWeightsOnAction(Long userId, Long targetUserId, String actionType, int signalStrength) {
        // 1. 读取当前用户权重
        UserWeights weights = getUserWeights(userId);

        // 2. 计算各维度得分
        Map<String, Integer> dimensionScores = calculateDimensionScores(userId, targetUserId);
        if (dimensionScores.isEmpty()) {
            log.warn("Failed to calculate dimension scores for users: {} -> {}", userId, targetUserId);
            return;
        }

        // 3. 归一化维度得分到[-1, 1]
        Map<String, Double> normalizedScores = normalizeScores(dimensionScores);

        // 4. EMA更新各维度权重
        Map<String, Double> newWeights = new HashMap<>(weights.getWeightMap());

        for (String dim : GlobalWeights.getAllDimensions()) {
            double currentWeight = newWeights.get(dim);
            double dimScore = normalizedScores.getOrDefault(dim, 0.0);

            // 权重调整量 = EMA_ALPHA × 信号强度 × 维度归一化得分
            double adjustment = GlobalWeights.EMA_ALPHA * signalStrength * dimScore * 0.01;
            double newWeight = currentWeight + adjustment;

            // 限制在边界内
            newWeights.put(dim, GlobalWeights.clipWeightToBounds(dim, newWeight));
        }

        // 5. 归一化，确保所有权重之和 = 1.0
        newWeights = GlobalWeights.normalizeWeights(newWeights);

        // 6. 保存更新后的权重
        weights.updateWeights(newWeights);
        weights.incrementActionCount();
        userWeightsMapper.updateById(weights);

        log.debug("Updated weights for user {} after {} action: new weights = {}",
                userId, actionType, newWeights);
    }

    /**
     * 权重时间衰减（每周执行一次）
     * 防止早期行为永久影响权重
     */
    @Scheduled(cron = "0 0 3 * * MON")
    @Transactional
    public void decayWeightsTowardDefault() {
        List<UserWeights> allWeights = userWeightsMapper.selectList(null);

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
        for (UserWeights uw : allWeights) {
            userWeightsMapper.updateById(uw);
        }
        log.info("Decayed weights for {} users toward default", allWeights.size());
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
        userWeightsMapper.updateById(weights);
        log.info("Reset weights for user {} to default", userId);
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
