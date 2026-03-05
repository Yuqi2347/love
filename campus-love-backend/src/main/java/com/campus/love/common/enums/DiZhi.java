package com.campus.love.common.enums;

import lombok.Getter;

/**
 * 地支枚举（十二地支：子丑寅卯辰巳午未申酉戌亥）
 * 用于八字排盘和合婚算法
 */
@Getter
public enum DiZhi {
    ZI("子", WuXing.WATER, YinYang.YANG, new TianGan[]{TianGan.GUI}),
    CHOU("丑", WuXing.EARTH, YinYang.YIN, new TianGan[]{TianGan.JI, TianGan.GUI, TianGan.XIN}),
    YIN("寅", WuXing.WOOD, YinYang.YANG, new TianGan[]{TianGan.JIA, TianGan.BING, TianGan.WU}),
    MAO("卯", WuXing.WOOD, YinYang.YIN, new TianGan[]{TianGan.YI}),
    CHEN("辰", WuXing.EARTH, YinYang.YANG, new TianGan[]{TianGan.WU, TianGan.YI, TianGan.GUI}),
    SI("巳", WuXing.FIRE, YinYang.YIN, new TianGan[]{TianGan.BING, TianGan.WU, TianGan.GENG}),
    WU("午", WuXing.FIRE, YinYang.YANG, new TianGan[]{TianGan.DING, TianGan.JI}),
    WEI("未", WuXing.EARTH, YinYang.YIN, new TianGan[]{TianGan.JI, TianGan.DING, TianGan.YI}),
    SHEN("申", WuXing.METAL, YinYang.YANG, new TianGan[]{TianGan.GENG, TianGan.REN, TianGan.WU}),
    YOU("酉", WuXing.METAL, YinYang.YIN, new TianGan[]{TianGan.XIN}),
    XU("戌", WuXing.EARTH, YinYang.YANG, new TianGan[]{TianGan.WU, TianGan.XIN, TianGan.DING}),
    HAI("亥", WuXing.WATER, YinYang.YIN, new TianGan[]{TianGan.REN, TianGan.JIA});

    private final String chineseChar;
    private final WuXing wuXing;
    private final YinYang yinYang;
    /**
     * 藏干（地支中隐藏的天干）
     */
    private final TianGan[] hiddenGans;

    DiZhi(String chineseChar, WuXing wuXing, YinYang yinYang, TianGan[] hiddenGans) {
        this.chineseChar = chineseChar;
        this.wuXing = wuXing;
        this.yinYang = yinYang;
        this.hiddenGans = hiddenGans;
    }

    /**
     * 根据索引获取地支（0=子, 1=丑, ..., 11=亥）
     */
    public static DiZhi of(int index) {
        return values()[((index % 12) + 12) % 12];
    }

    /**
     * 根据中文字符获取地支
     */
    public static DiZhi fromChar(String chineseChar) {
        for (DiZhi zhi : values()) {
            if (zhi.chineseChar.equals(chineseChar)) {
                return zhi;
            }
        }
        return null;
    }

    /**
     * 判断两个地支是否六合（子丑合、寅亥合、卯戌合、辰酉合、巳申合、午未合）
     */
    public static boolean isLiuHe(DiZhi a, DiZhi b) {
        if (a == null || b == null) return false;
        return switch (a) {
            case ZI  -> b == CHOU;   // 子丑合
            case CHOU-> b == ZI;     // 丑子合
            case YIN -> b == HAI;    // 寅亥合
            case HAI -> b == YIN;    // 亥寅合
            case MAO -> b == XU;     // 卯戌合
            case XU  -> b == MAO;    // 戌卯合
            case CHEN-> b == YOU;    // 辰酉合
            case YOU -> b == CHEN;   // 酉辰合
            case SI  -> b == SHEN;   // 巳申合
            case SHEN-> b == SI;     // 申巳合
            case WU  -> b == WEI;    // 午未合
            case WEI -> b == WU;     // 未午合
        };
    }

    /**
     * 判断两个地支是否六冲（子午冲、丑未冲、寅申冲、卯酉冲、辰戌冲、巳亥冲）
     */
    public static boolean isLiuChong(DiZhi a, DiZhi b) {
        if (a == null || b == null) return false;
        return switch (a) {
            case ZI  -> b == WU;     // 子午冲
            case WU  -> b == ZI;     // 午子冲
            case CHOU-> b == WEI;    // 丑未冲
            case WEI -> b == CHOU;   // 未丑冲
            case YIN -> b == SHEN;   // 寅申冲
            case SHEN-> b == YIN;    // 申寅冲
            case MAO -> b == YOU;    // 卯酉冲
            case YOU -> b == MAO;    // 酉卯冲
            case CHEN-> b == XU;     // 辰戌冲
            case XU  -> b == CHEN;   // 戌辰冲
            case SI  -> b == HAI;    // 巳亥冲
            case HAI -> b == SI;     // 亥巳冲
        };
    }

    /**
     * 判断两个地支是否相刑
     * 寅刑巳、巳刑申、申刑寅（无礼之刑）
     * 丑刑戌、戌刑未、未刑丑（持势之刑）
     * 子刑卯、卯刑子（无礼之刑）
     */
    public static boolean isXing(DiZhi a, DiZhi b) {
        if (a == null || b == null) return false;
        // 三刑组1：寅巳申互刑
        if ((a == YIN && b == SI) || (a == SI && b == SHEN) || (a == SHEN && b == YIN)) return true;
        // 三刑组2：丑戌未互刑
        if ((a == CHOU && b == XU) || (a == XU && b == WEI) || (a == WEI && b == CHOU)) return true;
        // 子卯相刑
        return (a == ZI && b == MAO) || (a == MAO && b == ZI);
    }

    /**
     * 判断两个地支是否三合（能够组成三合局）
     * 申子辰（水局）、寅午戌（火局）、亥卯未（木局）、巳酉丑（金局）
     */
    public static boolean isSanHe(DiZhi a, DiZhi b) {
        if (a == null || b == null) return false;
        return switch (a) {
            case SHEN-> b == ZI || b == CHEN;   // 申子辰水局
            case ZI  -> b == SHEN || b == CHEN;
            case CHEN-> b == SHEN || b == ZI;
            case YIN -> b == WU || b == XU;      // 寅午戌火局
            case WU  -> b == YIN || b == XU;
            case XU  -> b == YIN || b == WU;
            case HAI -> b == MAO || b == WEI;    // 亥卯未木局
            case MAO -> b == HAI || b == WEI;
            case WEI -> b == HAI || b == MAO;
            case SI  -> b == YOU || b == CHOU;   // 巳酉丑金局
            case YOU -> b == SI || b == CHOU;
            case CHOU-> b == SI || b == YOU;
            default -> false;
        };
    }
}
