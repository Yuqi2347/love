package com.campus.love.common.enums;

import lombok.Getter;

/**
 * 五行枚举
 * 用于八字合婚算法中的五行相生相克判断
 */
@Getter
public enum WuXing {
    WOOD("木"),
    FIRE("火"),
    EARTH("土"),
    METAL("金"),
    WATER("水");

    private final String chineseName;

    WuXing(String chineseName) {
        this.chineseName = chineseName;
    }

    /**
     * 判断五行相生关系（木生火、火生土、土生金、金生水、水生木）
     */
    public static boolean isGenerating(WuXing a, WuXing b) {
        if (a == null || b == null) return false;
        return switch (a) {
            case WOOD  -> b == FIRE;
            case FIRE  -> b == EARTH;
            case EARTH -> b == METAL;
            case METAL -> b == WATER;
            case WATER -> b == WOOD;
        };
    }

    /**
     * 判断五行相克关系（木克土、土克水、水克火、火克金、金克木）
     */
    public static boolean isOvercoming(WuXing a, WuXing b) {
        if (a == null || b == null) return false;
        return switch (a) {
            case WOOD  -> b == EARTH;
            case FIRE  -> b == METAL;
            case EARTH -> b == WATER;
            case METAL -> b == WOOD;
            case WATER -> b == FIRE;
        };
    }

    /**
     * 获取生我的五行（用神分析用）
     */
    public WuXing getGenerating() {
        return switch (this) {
            case WOOD  -> WATER;   // 水生木
            case FIRE  -> WOOD;    // 木生火
            case EARTH -> FIRE;    // 火生土
            case METAL -> EARTH;   // 土生金
            case WATER -> METAL;   // 金生水
        };
    }

    /**
     * 获取我生的五行
     */
    public WuXing getGenerated() {
        return switch (this) {
            case WOOD  -> FIRE;    // 木生火
            case FIRE  -> EARTH;   // 火生土
            case EARTH -> METAL;   // 土生金
            case METAL -> WATER;   // 金生水
            case WATER -> WOOD;    // 水生木
        };
    }

    /**
     * 获取克我的五行
     */
    public WuXing getOvercoming() {
        return switch (this) {
            case WOOD  -> METAL;   // 金克木
            case FIRE  -> WATER;   // 水克火
            case EARTH -> WOOD;    // 木克土
            case METAL -> FIRE;    // 火克金
            case WATER -> EARTH;   // 土克水
        };
    }

    /**
     * 获取我克的五行
     */
    public WuXing getOvercame() {
        return switch (this) {
            case WOOD  -> EARTH;   // 木克土
            case FIRE  -> METAL;   // 火克金
            case EARTH -> WATER;   // 土克水
            case METAL -> WOOD;    // 金克木
            case WATER -> FIRE;    // 水克火
        };
    }
}
