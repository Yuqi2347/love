package com.campus.love.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MsgTypeEnum {

    TEXT(1, "文字"),
    IMAGE(2, "图片"),
    EMOJI(3, "表情");

    private final Integer code;
    private final String label;
}
