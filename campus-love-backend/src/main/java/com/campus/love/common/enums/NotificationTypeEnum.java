package com.campus.love.common.enums;

import lombok.Getter;

/**
 * 站内通知类型枚举
 */
@Getter
public enum NotificationTypeEnum {

    /**
     * 报名成功（发送给参与者自己）
     */
    INVITE_JOIN_SUCCESS("报名成功"),

    /**
     * 有人加入邀约（发送给发起人）
     */
    INVITE_NEW_PARTICIPANT("有人加入你的邀约"),

    /**
     * 邀约被取消（发送给所有参与者）
     */
    INVITE_CANCELLED("邀约被取消"),

    /**
     * 参与者退出邀约（发送给发起人）
     */
    INVITE_PARTICIPANT_LEAVE("参与者退出邀约"),

    /**
     * 被发起人踢出邀约（发送给被踢用户）
     */
    INVITE_PARTICIPANT_KICKED("被踢出邀约"),

    /**
     * 匹配到合适邀约（发送给等待邀约用户）
     */
    INVITE_MATCH_FOUND("找到匹配邀约"),

    /**
     * 活动开始前1天提醒
     */
    INVITE_REMIND_1D("活动开始前1天提醒"),

    /**
     * 活动开始前1小时提醒
     */
    INVITE_REMIND_1H("活动开始前1小时提醒"),

    /**
     * 有人申请再次加入邀约（发送给发起人，需同意后该用户才能重新加入）
     */
    INVITE_REJOIN_REQUEST("申请再次加入邀约"),

    /**
     * 新举报（发送给管理员）
     */
    REPORT("新举报"),

    /**
     * 有人回复了你的评论（发送给被回复用户）
     */
    COMMENT_REPLY("回复了你的评论"),

    /**
     * 有人评论了你的动态（发送给动态作者）
     */
    COMMENT_ON_POST("评论了你的动态"),

    /**
     * 有人赞了你的动态（发送给动态作者）
     */
    POST_LIKE("赞了你的动态"),

    /**
     * 有人赞了你的评论（发送给评论作者）
     */
    COMMENT_LIKE("赞了你的评论");

    private final String description;

    NotificationTypeEnum(String description) {
        this.description = description;
    }
}

