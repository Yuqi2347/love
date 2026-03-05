package com.campus.love.common.enums;

import lombok.Getter;

/**
 * 阴阳枚举
 * 用于天干地支的阴阳属性判断
 */
@Getter
public enum YinYang {
    YANG("阳"),
    YIN("阴");

    private final String chineseName;

    YinYang(String chineseName) {
        this.chineseName = chineseName;
    }

    /**
     * 判断两个属性是否相同（阴阳同属性较和谐）
     */
    public static boolean isSame(YinYang a, YinYang b) {
        if (a == null || b == null) return false;
        return a == b;
    }

    /**
     * 判断两个属性是否相反（阴阳互补）
     */
    public static boolean isOpposite(YinYang a, YinYang b) {
        if (a == null || b == null) return false;
        return a != b;
    }
}
