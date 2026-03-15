package com.campus.love.match.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.campus.love.match.constants.GlobalWeights;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户个性化匹配权重实体（V2.0）
 *
 * 每个用户拥有独立的匹配权重配置
 * 权重随用户行为（关注/忽略/聊天等）动态调整
 *
 * @author Campus Love Team
 * @version 2.0
 */
@Data
@TableName("t_user_match_weights")
public class UserWeights {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 兴趣权重
     */
    private BigDecimal weightInterest;

    /**
     * OCEAN权重
     */
    private BigDecimal weightOcean;

    /**
     * 星座权重
     */
    private BigDecimal weightZodiac;

    /**
     * 价值观权重
     */
    private BigDecimal weightValues;

    /**
     * 专业权重
     */
    private BigDecimal weightMajor;

    /**
     * 年龄/年级权重
     */
    private BigDecimal weightAgeGrade;

    /**
     * 累计行为次数
     * < 30次使用全局默认权重（冷启动）
     * >= 30次使用个性化权重
     */
    private Integer actionCount;

    /**
     * 最后更新时间
     */
    private LocalDateTime lastUpdated;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 创建默认权重对象
     */
    public static UserWeights defaultWeights(Long userId, boolean hasRealOcean) {
        UserWeights weights = new UserWeights();
        weights.setUserId(userId);
        Map<String, Double> defaults = hasRealOcean ? GlobalWeights.DEFAULT_WEIGHTS : GlobalWeights.COLD_START_WEIGHTS;
        weights.setWeightOcean(BigDecimal.valueOf(defaults.getOrDefault("ocean", 0.0)));
        weights.setWeightInterest(BigDecimal.valueOf(defaults.getOrDefault("interest", 0.0)));
        weights.setWeightValues(BigDecimal.valueOf(defaults.getOrDefault("values", 0.0)));
        weights.setWeightAgeGrade(BigDecimal.valueOf(defaults.getOrDefault("age_grade", 0.0)));
        weights.setWeightMajor(BigDecimal.valueOf(defaults.getOrDefault("major", 0.0)));
        weights.setWeightZodiac(BigDecimal.valueOf(defaults.getOrDefault("zodiac", 0.0)));
        weights.setActionCount(0);
        return weights;
    }

    /**
     * 获取权重Map
     */
    public Map<String, Double> getWeightMap() {
        Map<String, Double> map = new HashMap<>();
        map.put("ocean", weightOcean != null ? weightOcean.doubleValue() : GlobalWeights.getDefaultWeight("ocean"));
        map.put("interest", weightInterest != null ? weightInterest.doubleValue() : GlobalWeights.getDefaultWeight("interest"));
        map.put("values", weightValues != null ? weightValues.doubleValue() : GlobalWeights.getDefaultWeight("values"));
        map.put("age_grade", weightAgeGrade != null ? weightAgeGrade.doubleValue() : GlobalWeights.getDefaultWeight("age_grade"));
        map.put("major", weightMajor != null ? weightMajor.doubleValue() : GlobalWeights.getDefaultWeight("major"));
        map.put("zodiac", weightZodiac != null ? weightZodiac.doubleValue() : GlobalWeights.getDefaultWeight("zodiac"));
        return map;
    }

    /**
     * 更新权重（从Map）
     */
    public void updateWeights(Map<String, Double> newWeights) {
        this.weightOcean = BigDecimal.valueOf(newWeights.getOrDefault("ocean", GlobalWeights.getDefaultWeight("ocean")));
        this.weightInterest = BigDecimal.valueOf(newWeights.getOrDefault("interest", GlobalWeights.getDefaultWeight("interest")));
        this.weightValues = BigDecimal.valueOf(newWeights.getOrDefault("values", GlobalWeights.getDefaultWeight("values")));
        this.weightAgeGrade = BigDecimal.valueOf(newWeights.getOrDefault("age_grade", GlobalWeights.getDefaultWeight("age_grade")));
        this.weightZodiac = BigDecimal.valueOf(newWeights.getOrDefault("zodiac", GlobalWeights.getDefaultWeight("zodiac")));
        this.weightMajor = BigDecimal.valueOf(newWeights.getOrDefault("major", GlobalWeights.getDefaultWeight("major")));
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * 增加行为计数
     */
    public void incrementActionCount() {
        this.actionCount = (this.actionCount == null ? 0 : this.actionCount) + 1;
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * 判断是否可以使用个性化权重
     */
    public boolean canUsePersonalizedWeights() {
        return actionCount != null && actionCount >= GlobalWeights.COLD_START_THRESHOLD;
    }

    /**
     * 获取指定维度的权重
     */
    public Double getWeight(String dimension) {
        return switch (dimension) {
            case "ocean" -> weightOcean != null ? weightOcean.doubleValue() : GlobalWeights.getDefaultWeight("ocean");
            case "interest" -> weightInterest != null ? weightInterest.doubleValue() : GlobalWeights.getDefaultWeight("interest");
            case "values" -> weightValues != null ? weightValues.doubleValue() : GlobalWeights.getDefaultWeight("values");
            case "age_grade" -> weightAgeGrade != null ? weightAgeGrade.doubleValue() : GlobalWeights.getDefaultWeight("age_grade");
            case "zodiac" -> weightZodiac != null ? weightZodiac.doubleValue() : GlobalWeights.getDefaultWeight("zodiac");
            case "major" -> weightMajor != null ? weightMajor.doubleValue() : GlobalWeights.getDefaultWeight("major");
            default -> GlobalWeights.getDefaultWeight(dimension);
        };
    }

    /**
     * 设置指定维度的权重
     */
    public void setWeight(String dimension, Double value) {
        switch (dimension) {
            case "ocean" -> this.weightOcean = BigDecimal.valueOf(value);
            case "interest" -> this.weightInterest = BigDecimal.valueOf(value);
            case "values" -> this.weightValues = BigDecimal.valueOf(value);
            case "age_grade" -> this.weightAgeGrade = BigDecimal.valueOf(value);
            case "zodiac" -> this.weightZodiac = BigDecimal.valueOf(value);
            case "major" -> this.weightMajor = BigDecimal.valueOf(value);
        }
    }

}
