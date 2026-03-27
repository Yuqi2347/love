package com.campus.love.common.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 邀约等级限制：等级 → 每日发起次数 / 同时参与上限（公共/私密分开）。
 */
@Getter
@AllArgsConstructor
public enum InviteLevelLimit {

    LEVEL_1(1, 2, 1, 1),
    LEVEL_4(4, 4, 2, 2),
    LEVEL_6_PLUS(6, 6, 3, 3);

    private final int level;
    private final int dailyLimit;
    private final int publicConcurrentLimit;
    private final int privateConcurrentLimit;

    /** 总并发上限（兼容旧字段使用）。 */
    public int getConcurrentLimit() {
        return publicConcurrentLimit + privateConcurrentLimit;
    }

    /**
     * 根据用户等级获取每日发起次数与同时参与上限。
     * level <= 3 -> 每日2，公共1 + 私密1
     * level >= 4 && level <= 5 -> 每日4，公共2 + 私密2
     * level >= 6 -> 每日6，公共3 + 私密3
     */
    public static InviteLevelLimit fromLevel(int level) {
        if (level <= 3) {
            return LEVEL_1;
        }
        if (level <= 5) {
            return LEVEL_4;
        }
        return LEVEL_6_PLUS;
    }
}
