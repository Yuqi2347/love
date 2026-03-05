package com.campus.love.invite.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 邀约周期枚举
 */
@Getter
@AllArgsConstructor
public enum InvitePeriodEnum {

    ONCE("单次"),
    WEEKLY("每周"),
    MONTHLY("每月");

    private final String description;
}
