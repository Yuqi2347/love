package com.campus.love.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证或Token已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),

    // 用户模块 1xxx
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户已注册"),
    INVALID_CAMPUS_EMAIL(1003, "请使用支持的学校邮箱注册"),
    INVALID_CREDENTIALS(1004, "邮箱或密码错误"),
    VERIFY_CODE_INVALID(1006, "验证码错误或已过期"),
    VERIFY_CODE_COOLDOWN(1007, "请60秒后再发送验证码"),
    PROFILE_INCOMPLETE(1005, "请先完善个人资料"),

    // 关注模块 2xxx
    ALREADY_FOLLOWED(2001, "已关注该用户"),
    CANNOT_FOLLOW_SELF(2002, "不能关注自己"),
    NOT_FOLLOWED(2003, "尚未关注该用户"),

    // 聊天模块 3xxx
    CHAT_LIMIT_EXCEEDED(3001, "未互关时，对方回复前仅可发送一条消息"),
    MESSAGE_SEND_FAILED(3002, "消息发送失败"),

    // 朋友圈模块 4xxx
    FEED_NOT_FOUND(4001, "动态不存在"),
    ALREADY_LIKED(4002, "已点赞"),
    ALREADY_LIKED_COMMENT(4004, "已点赞该评论"),
    INSUFFICIENT_LEVEL(4003, "等级不足，需要达到Lv3才能发布动态"),

    // 匹配模块 5xxx
    MATCH_CALCULATION_FAILED(5001, "匹配计算失败"),

    // 邀约模块 6xxx
    INVITE_NOT_FOUND(6001, "邀约不存在"),
    INVITE_FULL(6002, "邀约已满员"),
    ALREADY_JOINED(6003, "已加入该邀约"),
    NOT_MUTUAL_FOLLOW(6004, "只能邀约互相关注的用户"),
    INVITE_LIMIT_EXCEEDED(6005, "今日邀约次数已达上限"),
    CREDIT_TOO_LOW(6006, "信用分过低"),
    INVITE_TIME_PASSED(6007, "邀约时间已过"),
    CANCEL_TOO_CLOSE(6008, "活动即将开始，无法取消"),
    ALREADY_RATED(6009, "已评价过该邀约"),
    PARTICIPATE_LIMIT_EXCEEDED(6010, "同时参与的邀约已达上限"),

    // AI 模块 7xxx
    YUANFEN_NOT_MUTUAL(7001, "需要互相关注才能使用缘分解析"),
    YUANFEN_COOLDOWN(7002, "缘分解析冷却中，请稍后再试"),
    AI_SERVICE_UNAVAILABLE(7003, "AI 服务暂时不可用，请稍后重试"),

    // 心动时刻模块 8xxx
    MOMENT_ALREADY_ENROLLED(8001, "本周已报名，无需重复报名"),
    MOMENT_BANNED(8002, "您已被禁止参加心动时刻活动"),
    MOMENT_NOT_ENROLLED(8003, "本周未报名心动时刻"),
    MOMENT_NO_RESULT(8004, "本周匹配结果尚未揭晓"),
    MOMENT_PROFILE_INCOMPLETE(8005, "请先完成心动时刻问卷"),
    MOMENT_ENROLLMENT_CLOSED(8006, "本周报名已截止，匹配已触发"),

    // 系统错误 9xxx
    INTERNAL_ERROR(9999, "系统内部错误");

    private final Integer code;
    private final String message;
}
