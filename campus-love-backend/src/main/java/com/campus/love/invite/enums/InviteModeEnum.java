package com.campus.love.invite.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 邀约模式枚举
 */
@Getter
@AllArgsConstructor
public enum InviteModeEnum {

    PUBLIC("公开"),
    PRIVATE("一对一");

    private final String description;
}
