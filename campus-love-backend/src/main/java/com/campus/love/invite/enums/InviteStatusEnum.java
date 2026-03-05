package com.campus.love.invite.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 邀约状态枚举
 */
@Getter
@AllArgsConstructor
public enum InviteStatusEnum {

    RECRUITING("招募中"),
    FULL("已满员"),
    CONFIRMED("已确认"),
    IN_PROGRESS("进行中"),
    ENDED("已结束"),
    CANCELLED("已取消");

    private final String description;
}
