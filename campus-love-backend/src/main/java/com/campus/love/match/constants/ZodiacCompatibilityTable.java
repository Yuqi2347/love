package com.campus.love.match.constants;

import java.util.Map;

/**
 * 12星座相性表。
 * 得分范围 0-100。
 * 排列顺序：白羊 金牛 双子 巨蟹 狮子 处女 天秤 天蝎 射手 摩羯 水瓶 双鱼
 */
public final class ZodiacCompatibilityTable {

    private ZodiacCompatibilityTable() {}

    private static final String[] ZODIAC = {
            "白羊座", "金牛座", "双子座", "巨蟹座", "狮子座", "处女座",
            "天秤座", "天蝎座", "射手座", "摩羯座", "水瓶座", "双鱼座"
    };

    private static final int[][] SCORES = {
        //  白羊 金牛 双子 巨蟹 狮子 处女 天秤 天蝎 射手 摩羯 水瓶 双鱼
            { 75,  55,  80,  50,  90,  60,  85,  50,  95,  45,  80,  65 }, // 白羊
            { 55,  80,  60,  85,  55,  90,  65,  80,  50,  90,  55,  75 }, // 金牛
            { 80,  60,  75,  55,  80,  60,  90,  50,  80,  55,  95,  65 }, // 双子
            { 50,  85,  55,  80,  60,  75,  55,  90,  45,  75,  55,  95 }, // 巨蟹
            { 90,  55,  80,  60,  75,  55,  80,  65,  90,  50,  70,  55 }, // 狮子
            { 60,  90,  60,  75,  55,  80,  60,  75,  55,  90,  60,  70 }, // 处女
            { 85,  65,  90,  55,  80,  60,  75,  60,  80,  55,  85,  55 }, // 天秤
            { 50,  80,  50,  90,  65,  75,  60,  80,  55,  75,  55,  90 }, // 天蝎
            { 95,  50,  80,  45,  90,  55,  80,  55,  75,  50,  85,  60 }, // 射手
            { 45,  90,  55,  75,  50,  90,  55,  75,  50,  80,  60,  70 }, // 摩羯
            { 80,  55,  95,  55,  70,  60,  85,  55,  85,  60,  75,  60 }, // 水瓶
            { 65,  75,  65,  95,  55,  70,  55,  90,  60,  70,  60,  80 }, // 双鱼
    };

    private static final Map<String, Integer> ZODIAC_INDEX;

    static {
        ZODIAC_INDEX = new java.util.HashMap<>();
        for (int i = 0; i < ZODIAC.length; i++) {
            ZODIAC_INDEX.put(ZODIAC[i], i);
        }
    }

    public static int getCompatibility(String zodiac1, String zodiac2) {
        if (zodiac1 == null || zodiac2 == null) return 50;
        Integer idx1 = ZODIAC_INDEX.get(zodiac1);
        Integer idx2 = ZODIAC_INDEX.get(zodiac2);
        if (idx1 == null || idx2 == null) return 50;
        return SCORES[idx1][idx2];
    }
}
