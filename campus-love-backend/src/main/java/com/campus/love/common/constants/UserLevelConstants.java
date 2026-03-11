package com.campus.love.common.constants;

/**
 * 用户等级配置常量
 */
public final class UserLevelConstants {

    private UserLevelConstants() {}

    /**
     * 各等级所需活跃度
     */
    public static final int[] LEVEL_THRESHOLDS = {
        0,      // Lv1: 0分（初始等级）
        50,     // Lv2: 50分
        150,    // Lv3: 150分
        300,    // Lv4: 300分
        500,    // Lv5: 500分
        800,    // Lv6: 800分
        1200,   // Lv7: 1200分
        1700,   // Lv8: 1700分
        2300,   // Lv9: 2300分
        3000    // Lv10: 3000分
    };

    private static final int MIN_LEVEL = 1;

    /** 发布帖子所需的最低等级（默认 1） */
    public static int getMinLevel() {
        return MIN_LEVEL;
    }

    /**
     * 发布发现模块帖子所需的最低等级
     */
    public static final int POST_FEED_MIN_LEVEL = 3;

    /**
     * 最大等级
     */
    public static final int MAX_LEVEL = LEVEL_THRESHOLDS.length;

    /**
     * 根据活跃度获取用户等级
     */
    public static int getLevelByScore(int score) {
        for (int i = LEVEL_THRESHOLDS.length - 1; i >= 0; i--) {
            if (score >= LEVEL_THRESHOLDS[i]) {
                return i + 1;
            }
        }
        return 1;
    }

    /**
     * 获取当前等级的进度百分比
     */
    public static int getLevelProgress(int score) {
        int currentLevel = getLevelByScore(score);
        if (currentLevel >= MAX_LEVEL) {
            return 100;
        }

        int currentLevelThreshold = LEVEL_THRESHOLDS[currentLevel - 1];
        int nextLevelThreshold = LEVEL_THRESHOLDS[currentLevel];

        if (nextLevelThreshold == currentLevelThreshold) {
            return 100;
        }

        return (int) ((score - currentLevelThreshold) * 100.0 / (nextLevelThreshold - currentLevelThreshold));
    }

    /**
     * 获取升级所需剩余积分
     */
    public static int getScoreToNextLevel(int score) {
        int currentLevel = getLevelByScore(score);
        if (currentLevel >= MAX_LEVEL) {
            return 0;
        }
        return LEVEL_THRESHOLDS[currentLevel] - score;
    }
}
