package com.campus.love.common.enums;

import lombok.Getter;

@Getter
public enum GenderEnum {

    UNKNOWN(0, "未知"),
    MALE(1, "男"),
    FEMALE(2, "女");

    private final Integer code;
    private final String label;

    GenderEnum(Integer code, String label) {
        this.code = code;
        this.label = label;
    }

    public static GenderEnum fromCode(Integer code) {
        for (GenderEnum g : values()) {
            if (g.code.equals(code)) return g;
        }
        return UNKNOWN;
    }
}
