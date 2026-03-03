package com.campus.love.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GenderEnum {

    UNKNOWN(0, "未知"),
    MALE(1, "男"),
    FEMALE(2, "女");

    private final Integer code;
    private final String label;

    public static GenderEnum fromCode(Integer code) {
        for (GenderEnum g : values()) {
            if (g.code.equals(code)) return g;
        }
        return UNKNOWN;
    }
}
