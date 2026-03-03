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

    // 用户模块 1xxx
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户已注册"),
    INVALID_CAMPUS_EMAIL(1003, "请使用校园邮箱注册(.edu.cn)"),
    INVALID_CREDENTIALS(1004, "邮箱或密码错误"),
    PROFILE_INCOMPLETE(1005, "请先完善个人资料"),

    // 关注模块 2xxx
    ALREADY_FOLLOWED(2001, "已关注该用户"),
    CANNOT_FOLLOW_SELF(2002, "不能关注自己"),
    NOT_FOLLOWED(2003, "尚未关注该用户"),

    // 聊天模块 3xxx
    CHAT_LIMIT_EXCEEDED(3001, "今日单向聊天次数已达上限"),
    MESSAGE_SEND_FAILED(3002, "消息发送失败"),

    // 朋友圈模块 4xxx
    FEED_NOT_FOUND(4001, "动态不存在"),
    ALREADY_LIKED(4002, "已点赞"),

    // 匹配模块 5xxx
    MATCH_CALCULATION_FAILED(5001, "匹配计算失败"),

    // 系统错误 9xxx
    INTERNAL_ERROR(9999, "系统内部错误");

    private final Integer code;
    private final String message;
}
