package com.campus.love.common.utils;

import com.campus.love.common.enums.WuXing;

import java.util.*;

/**
 * 八字合婚算法常量：天干地支六合/三合/冲/刑、纳音等。
 */
public final class BaziConstants {

    private BaziConstants() {}

    /** 天干六合得分 */
    public static final Map<String, Integer> TIAN_GAN_HE_SCORES = new HashMap<>() {{
        put("甲己", 15); put("己甲", 15);
        put("乙庚", 15); put("庚乙", 15);
        put("丙辛", 15); put("辛丙", 15);
        put("丁壬", 15); put("壬丁", 15);
        put("戊癸", 15); put("癸戊", 15);
    }};

    /** 天干相冲得分 */
    public static final Map<String, Integer> TIAN_GAN_CHONG_SCORES = new HashMap<>() {{
        put("甲庚", -10); put("庚甲", -10);
        put("乙辛", -10); put("辛乙", -10);
        put("丙壬", -10); put("壬丙", -10);
        put("丁癸", -10); put("癸丁", -10);
    }};

    /** 地支六合得分 */
    public static final Map<String, Integer> DI_ZHI_LIU_HE_SCORES = new HashMap<>() {{
        put("子丑", 12); put("丑子", 12);
        put("寅亥", 12); put("亥寅", 12);
        put("卯戌", 12); put("戌卯", 12);
        put("辰酉", 12); put("酉辰", 12);
        put("巳申", 12); put("申巳", 12);
        put("午未", 12); put("未午", 12);
    }};

    /** 地支六冲得分 */
    public static final Map<String, Integer> DI_ZHI_CHONG_SCORES = new HashMap<>() {{
        put("子午", -15); put("午子", -15);
        put("丑未", -12); put("未丑", -12);
        put("寅申", -12); put("申寅", -12);
        put("卯酉", -12); put("酉卯", -12);
        put("辰戌", -10); put("戌辰", -10);
        put("巳亥", -10); put("亥巳", -10);
    }};

    /** 地支相刑得分 */
    public static final Map<String, Integer> DI_ZHI_XING_SCORES = new HashMap<>() {{
        put("寅巳", -8); put("巳申", -8); put("申寅", -8);
        put("丑戌", -6); put("戌未", -6); put("未丑", -6);
        put("子卯", -5); put("卯子", -5);
    }};

    /** 地支三合局组合 */
    public static final List<Set<String>> DI_ZHI_SAN_HE_GROUPS = List.of(
            Set.of("申", "子", "辰"),
            Set.of("寅", "午", "戌"),
            Set.of("亥", "卯", "未"),
            Set.of("巳", "酉", "丑")
    );

    /** 纳音五行表（60甲子对应30组纳音） */
    public static final Map<String, String> NAYIN_MAP = new LinkedHashMap<>() {{
        put("甲子", "海中金"); put("乙丑", "海中金");
        put("丙寅", "炉中火"); put("丁卯", "炉中火");
        put("戊辰", "大林木"); put("己巳", "大林木");
        put("庚午", "路旁土"); put("辛未", "路旁土");
        put("壬申", "剑锋金"); put("癸酉", "剑锋金");
        put("甲戌", "山头火"); put("乙亥", "山头火");
        put("丙子", "涧下水"); put("丁丑", "涧下水");
        put("戊寅", "城头土"); put("己卯", "城头土");
        put("庚辰", "白蜡金"); put("辛巳", "白蜡金");
        put("壬午", "杨柳木"); put("癸未", "杨柳木");
        put("甲申", "泉中水"); put("乙酉", "泉中水");
        put("丙戌", "屋上土"); put("丁亥", "屋上土");
        put("戊子", "霹雳火"); put("己丑", "霹雳火");
        put("庚寅", "松柏木"); put("辛卯", "松柏木");
        put("壬辰", "长流水"); put("癸巳", "长流水");
        put("甲午", "砂中金"); put("乙未", "砂中金");
        put("丙申", "山下火"); put("丁酉", "山下火");
        put("戊戌", "平地木"); put("己亥", "平地木");
        put("庚子", "壁上土"); put("辛丑", "壁上土");
        put("壬寅", "金箔金"); put("癸卯", "金箔金");
        put("甲辰", "覆灯火"); put("乙巳", "覆灯火");
        put("丙午", "天河水"); put("丁未", "天河水");
        put("戊申", "大驿土"); put("己酉", "大驿土");
        put("庚戌", "钗钏金"); put("辛亥", "钗钏金");
        put("壬子", "桑柘木"); put("癸丑", "桑柘木");
        put("甲寅", "大溪水"); put("乙卯", "大溪水");
        put("丙辰", "沙中土"); put("丁巳", "沙中土");
        put("戊午", "天上火"); put("己未", "天上火");
        put("庚申", "石榴木"); put("辛酉", "石榴木");
        put("壬戌", "大海水"); put("癸亥", "大海水");
    }};

    /** 纳音五行映射 */
    public static final Map<String, WuXing> NAYIN_WUXING_MAP = new HashMap<>() {{
        put("海中金", WuXing.METAL); put("剑锋金", WuXing.METAL); put("白蜡金", WuXing.METAL);
        put("砂中金", WuXing.METAL); put("金箔金", WuXing.METAL); put("钗钏金", WuXing.METAL);
        put("炉中火", WuXing.FIRE); put("山头火", WuXing.FIRE); put("霹雳火", WuXing.FIRE);
        put("山下火", WuXing.FIRE); put("覆灯火", WuXing.FIRE); put("天上火", WuXing.FIRE);
        put("大林木", WuXing.WOOD); put("杨柳木", WuXing.WOOD); put("泉中水", WuXing.WOOD);
        put("松柏木", WuXing.WOOD); put("长流水", WuXing.WOOD); put("平地木", WuXing.WOOD);
        put("桑柘木", WuXing.WOOD); put("大溪水", WuXing.WOOD); put("沙中土", WuXing.WOOD);
        put("石榴木", WuXing.WOOD); put("大海水", WuXing.WOOD);
        put("路旁土", WuXing.EARTH); put("城头土", WuXing.EARTH); put("屋上土", WuXing.EARTH);
        put("大驿土", WuXing.EARTH); put("壁上土", WuXing.EARTH); put("沙中土", WuXing.EARTH);
        put("涧下水", WuXing.WATER); put("天河水", WuXing.WATER);
    }};
}
