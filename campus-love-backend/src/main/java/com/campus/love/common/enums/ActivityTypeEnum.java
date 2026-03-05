package com.campus.love.common.enums;

import lombok.Getter;

/**
 * 活跃度活动类型枚举
 */
@Getter
public enum ActivityTypeEnum {

    /**
     * 关注用户
     */
    FOLLOW("关注用户", 5),

    /**
     * 浏览帖子/用户
     */
    VIEW("浏览", 1),

    /**
     * 点赞帖子
     */
    LIKE("点赞", 3),

    /**
     * 收藏帖子
     */
    COLLECT("收藏", 3),

    /**
     * 发布帖子
     */
    POST("发布动态", 10),

    /**
     * 发表评论
     */
    COMMENT("发表评论", 5),

    /**
     * 发起邀约
     */
    INVITE_CREATE("发起邀约", 10),

    /**
     * 加入邀约
     */
    INVITE_JOIN("加入邀约", 5),

    /**
     * 完成邀约
     */
    INVITE_COMPLETE("完成邀约", 5);

    /**
     * 活动描述
     */
    private final String description;

    /**
     * 活跃度积分
     */
    private final int score;

    ActivityTypeEnum(String description, int score) {
        this.description = description;
        this.score = score;
    }
}
