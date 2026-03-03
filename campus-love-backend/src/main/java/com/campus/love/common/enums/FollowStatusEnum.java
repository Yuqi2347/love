package com.campus.love.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FollowStatusEnum {

    NONE(0, "未关注"),
    ONE_WAY(1, "单向关注"),
    MUTUAL(2, "互相关注");

    private final Integer code;
    private final String label;
}
