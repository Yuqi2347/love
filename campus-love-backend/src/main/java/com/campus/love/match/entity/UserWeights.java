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
     * MBTI权重
     */
    private BigDecimal weightMbti;

    /**
     * 星座权重
     */
    private BigDecimal weightZodiac;

    /**
     * 八字权重
     */
    private BigDecimal weightBazi;

    /**
     * 专业权重
     */
    private BigDecimal weightMajor;

    /**
     * 年龄权重
     */
    private BigDecimal weightAge;

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
    public static UserWeights defaultWeights(Long userId) {
        UserWeights weights = new UserWeights();
        weights.setUserId(userId);
        weights.setWeightInterest(BigDecimal.valueOf(GlobalWeights.getDefaultWeight("interest")));
        weights.setWeightMbti(BigDecimal.valueOf(GlobalWeights.getDefaultWeight("mbti")));
        weights.setWeightZodiac(BigDecimal.valueOf(GlobalWeights.getDefaultWeight("zodiac")));
        weights.setWeightBazi(BigDecimal.valueOf(GlobalWeights.getDefaultWeight("bazi")));
        weights.setWeightMajor(BigDecimal.valueOf(GlobalWeights.getDefaultWeight("major")));
        weights.setWeightAge(BigDecimal.valueOf(GlobalWeights.getDefaultWeight("age")));
        weights.setActionCount(0);
        return weights;
    }

    /**
     * 获取权重Map
     */
    public Map<String, Double> getWeightMap() {
        Map<String, Double> map = new HashMap<>();
        map.put("interest", weightInterest != null ? weightInterest.doubleValue() : GlobalWeights.getDefaultWeight("interest"));
        map.put("mbti", weightMbti != null ? weightMbti.doubleValue() : GlobalWeights.getDefaultWeight("mbti"));
        map.put("zodiac", weightZodiac != null ? weightZodiac.doubleValue() : GlobalWeights.getDefaultWeight("zodiac"));
        map.put("bazi", weightBazi != null ? weightBazi.doubleValue() : GlobalWeights.getDefaultWeight("bazi"));
        map.put("major", weightMajor != null ? weightMajor.doubleValue() : GlobalWeights.getDefaultWeight("major"));
        map.put("age", weightAge != null ? weightAge.doubleValue() : GlobalWeights.getDefaultWeight("age"));
        return map;
    }

    /**
     * 更新权重（从Map）
     */
    public void updateWeights(Map<String, Double> newWeights) {
        this.weightInterest = BigDecimal.valueOf(newWeights.getOrDefault("interest", GlobalWeights.getDefaultWeight("interest")));
        this.weightMbti = BigDecimal.valueOf(newWeights.getOrDefault("mbti", GlobalWeights.getDefaultWeight("mbti")));
        this.weightZodiac = BigDecimal.valueOf(newWeights.getOrDefault("zodiac", GlobalWeights.getDefaultWeight("zodiac")));
        this.weightBazi = BigDecimal.valueOf(newWeights.getOrDefault("bazi", GlobalWeights.getDefaultWeight("bazi")));
        this.weightMajor = BigDecimal.valueOf(newWeights.getOrDefault("major", GlobalWeights.getDefaultWeight("major")));
        this.weightAge = BigDecimal.valueOf(newWeights.getOrDefault("age", GlobalWeights.getDefaultWeight("age")));
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
            case "interest" -> weightInterest != null ? weightInterest.doubleValue() : GlobalWeights.getDefaultWeight("interest");
            case "mbti" -> weightMbti != null ? weightMbti.doubleValue() : GlobalWeights.getDefaultWeight("mbti");
            case "zodiac" -> weightZodiac != null ? weightZodiac.doubleValue() : GlobalWeights.getDefaultWeight("zodiac");
            case "bazi" -> weightBazi != null ? weightBazi.doubleValue() : GlobalWeights.getDefaultWeight("bazi");
            case "major" -> weightMajor != null ? weightMajor.doubleValue() : GlobalWeights.getDefaultWeight("major");
            case "age" -> weightAge != null ? weightAge.doubleValue() : GlobalWeights.getDefaultWeight("age");
            default -> GlobalWeights.getDefaultWeight(dimension);
        };
    }

    /**
     * 设置指定维度的权重
     */
    public void setWeight(String dimension, Double value) {
        switch (dimension) {
            case "interest" -> this.weightInterest = BigDecimal.valueOf(value);
            case "mbti" -> this.weightMbti = BigDecimal.valueOf(value);
            case "zodiac" -> this.weightZodiac = BigDecimal.valueOf(value);
            case "bazi" -> this.weightBazi = BigDecimal.valueOf(value);
            case "major" -> this.weightMajor = BigDecimal.valueOf(value);
            case "age" -> this.weightAge = BigDecimal.valueOf(value);
        }
    }

}
