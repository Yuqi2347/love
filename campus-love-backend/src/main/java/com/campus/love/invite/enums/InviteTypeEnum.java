package com.campus.love.invite.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 邀约类型枚举
 */
@Getter
@AllArgsConstructor
public enum InviteTypeEnum {

    DINNER("约饭"),
    SPORT("运动"),
    STUDY("自习"),
    DRAMA("追剧"),
    OTHER("其他");

    private final String description;
}
