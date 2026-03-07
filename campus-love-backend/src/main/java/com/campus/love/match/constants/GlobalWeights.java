package com.campus.love.match.constants;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局匹配权重常量（V2.0）
 *
 * 包含默认权重、权重边界、以及各种阈值配置
 * 用于匹配系统的动态权重管理
 */
public final class GlobalWeights {

    private GlobalWeights() {}

    // ==================== 全局默认权重 ====================

    /**
     * 全局默认权重（新用户冷启动使用）
     * 所有权重之和必须为 1.0
     */
    public static final Map<String, Double> DEFAULT_WEIGHTS = new HashMap<>() {{
        put("interest", 0.30);
        put("mbti",     0.25);
        put("zodiac",   0.15);
        put("bazi",     0.15);
        put("major",    0.10);
        put("age",      0.05);
    }};

    // ==================== 权重边界配置 ====================

    /**
     * 各维度权重边界（防止个性化权重过度偏移）
     * 每个维度：[最小值, 最大值]
     */
    public static final Map<String, double[]> WEIGHT_BOUNDS = new HashMap<>() {{
        put("interest", new double[]{0.10, 0.50});
        put("mbti",     new double[]{0.10, 0.40});
        put("zodiac",   new double[]{0.02, 0.25});
        put("bazi",     new double[]{0.02, 0.25});
        put("major",    new double[]{0.03, 0.20});
        put("age",      new double[]{0.01, 0.15});
    }};

    // ==================== 匹配阈值配置 ====================

    /**
     * 年龄差最大值（岁）
     * 超过此值年龄匹配分为最低分
     */
    public static final int MAX_AGE_DIFF_YEARS = 5;

    /**
     * 冷启动行为数阈值
     * 低于此值使用全局默认权重
     * 高于或等于此值使用个性化权重
     */
    public static final int COLD_START_THRESHOLD = 30;

    // ==================== 动态权重算法参数 ====================

    /**
     * EMA（指数移动平均）平滑系数
     * 值越大，权重学习越快；值越小，权重越稳定
     * 范围：[0.001, 0.30]，推荐值：0.15（平衡策略）
     * 提高到0.15让权重变化更明显，用户行为能更快影响推荐
     */
    public static final double EMA_ALPHA = 0.15;

    /**
     * 权重周衰减率
     * 每周权重向默认值靠拢的百分比
     * 防止早期行为永久影响权重
     */
    public static final double WEEKLY_DECAY_RATE = 0.05;

    // ==================== 推荐系统配置 ====================

    /**
     * 每日推荐用户数量上限
     */
    public static final int DAILY_RECOMMEND_LIMIT = 50;

    /**
     * 候选池最大数量
     * 批量计算匹配度时的最大用户数
     */
    public static final int CANDIDATE_POOL_LIMIT = 500;

    /**
     * 曝光次数上限
     * 用户被推荐但未被关注的最大次数
     * 超过后暂时降低推荐优先级
     */
    public static final int MAX_EXPOSURE_COUNT = 3;

    // ==================== 匹配分档配置 ====================

    /**
     * 高匹配分阈值
     * 分数 >= 此值视为高匹配
     */
    public static final int HIGH_MATCH_THRESHOLD = 75;

    /**
     * 低匹配分阈值
     * 分数 < 此值视为低匹配
     */
    public static final int LOW_MATCH_THRESHOLD = 50;

    // ==================== 辅助方法 ====================

    /**
     * 获取默认权重值
     */
    public static double getDefaultWeight(String dimension) {
        return DEFAULT_WEIGHTS.getOrDefault(dimension, 0.0);
    }

    /**
     * 获取权重边界
     */
    public static double[] getWeightBounds(String dimension) {
        return WEIGHT_BOUNDS.getOrDefault(dimension, new double[]{0.0, 1.0});
    }

    /**
     * 检查权重值是否在边界内
     */
    public static boolean isWeightInBounds(String dimension, double weight) {
        double[] bounds = getWeightBounds(dimension);
        return weight >= bounds[0] && weight <= bounds[1];
    }

    /**
     * 裁剪权重到边界内
     */
    public static double clipWeightToBounds(String dimension, double weight) {
        double[] bounds = getWeightBounds(dimension);
        return Math.max(bounds[0], Math.min(bounds[1], weight));
    }

    /**
     * 归一化权重（确保和为1.0）
     */
    public static Map<String, Double> normalizeWeights(Map<String, Double> weights) {
        double sum = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sum == 0) return new HashMap<>(DEFAULT_WEIGHTS);

        Map<String, Double> normalized = new HashMap<>();
        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            normalized.put(entry.getKey(), entry.getValue() / sum);
        }
        return normalized;
    }

    /**
     * 获取所有维度名称
     */
    public static String[] getAllDimensions() {
        return new String[]{"interest", "mbti", "zodiac", "bazi", "major", "age"};
    }
}
