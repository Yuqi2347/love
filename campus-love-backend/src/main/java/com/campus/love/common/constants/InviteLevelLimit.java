package com.campus.love.common.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 邀约等级限制：等级 → 每日发起次数 / 同时进行数。
 */
@Getter
@AllArgsConstructor
public enum InviteLevelLimit {

    LEVEL_1(1, 3, 2),
    LEVEL_2(2, 5, 3),
    LEVEL_3_PLUS(3, 10, 5);

    private final int level;
    private final int dailyLimit;
    private final int concurrentLimit;

    /**
     * 根据用户等级获取每日发起次数与同时进行数限制。
     * level <= 1 -> 3/2, level == 2 -> 5/3, level >= 3 -> 10/5
     */
    public static InviteLevelLimit fromLevel(int level) {
        if (level <= 1) {
            return LEVEL_1;
        }
        if (level == 2) {
            return LEVEL_2;
        }
        return LEVEL_3_PLUS;
    }
}
