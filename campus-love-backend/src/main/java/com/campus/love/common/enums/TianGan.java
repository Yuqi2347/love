package com.campus.love.common.enums;

import lombok.Getter;

/**
 * 天干枚举（十天干：甲乙丙丁戊己庚辛壬癸）
 * 用于八字排盘和合婚算法
 */
@Getter
public enum TianGan {
    JIA("甲", WuXing.WOOD, YinYang.YANG),
    YI("乙", WuXing.WOOD, YinYang.YIN),
    BING("丙", WuXing.FIRE, YinYang.YANG),
    DING("丁", WuXing.FIRE, YinYang.YIN),
    WU("戊", WuXing.EARTH, YinYang.YANG),
    JI("己", WuXing.EARTH, YinYang.YIN),
    GENG("庚", WuXing.METAL, YinYang.YANG),
    XIN("辛", WuXing.METAL, YinYang.YIN),
    REN("壬", WuXing.WATER, YinYang.YANG),
    GUI("癸", WuXing.WATER, YinYang.YIN);

    private final String chineseChar;
    private final WuXing wuXing;
    private final YinYang yinYang;

    TianGan(String chineseChar, WuXing wuXing, YinYang yinYang) {
        this.chineseChar = chineseChar;
        this.wuXing = wuXing;
        this.yinYang = yinYang;
    }

    /**
     * 根据索引获取天干（0=甲, 1=乙, ..., 9=癸）
     */
    public static TianGan of(int index) {
        return values()[((index % 10) + 10) % 10];
    }

    /**
     * 根据中文字符获取天干
     */
    public static TianGan fromChar(String chineseChar) {
        for (TianGan gan : values()) {
            if (gan.chineseChar.equals(chineseChar)) {
                return gan;
            }
        }
        return null;
    }

    /**
     * 判断两个天干是否相合（甲己合、乙庚合、丙辛合、丁壬合、戊癸合）
     */
    public static boolean isHe(TianGan a, TianGan b) {
        if (a == null || b == null) return false;
        return switch (a) {
            case JIA -> b == JI;     // 甲己合
            case YI  -> b == GENG;   // 乙庚合
            case BING-> b == XIN;    // 丙辛合
            case DING-> b == REN;    // 丁壬合
            case WU  -> b == GUI;    // 戊癸合
            case JI  -> b == JIA;    // 己甲合
            case GENG-> b == YI;     // 庚乙合
            case XIN -> b == BING;   // 辛丙合
            case REN -> b == DING;   // 壬丁合
            case GUI -> b == WU;     // 癸戊合
        };
    }

    /**
     * 判断两个天干是否相冲（甲庚冲、乙辛冲、丙壬冲、丁癸冲）
     */
    public static boolean isChong(TianGan a, TianGan b) {
        if (a == null || b == null) return false;
        return switch (a) {
            case JIA -> b == GENG;   // 甲庚冲
            case YI  -> b == XIN;    // 乙辛冲
            case BING-> b == REN;    // 丙壬冲
            case DING-> b == GUI;    // 丁癸冲
            case GENG-> b == JIA;    // 庚甲冲
            case XIN -> b == YI;     // 辛乙冲
            case REN -> b == BING;   // 壬丙冲
            case GUI -> b == DING;   // 癸丁冲
            default -> false;
        };
    }
}
