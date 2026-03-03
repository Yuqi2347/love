package com.campus.love.common.utils;

import java.time.LocalDate;
import java.time.temporal.ChronoField;

/**
 * 简化版八字（生辰四柱）计算工具。
 * MVP阶段基于出生年份的天干地支推算年柱，用于匹配评分。
 */
public final class BaziUtil {

    private BaziUtil() {}

    private static final String[] TIAN_GAN = {"甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"};
    private static final String[] DI_ZHI = {"子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"};
    private static final String[] WU_XING_GAN = {"木", "木", "火", "火", "土", "土", "金", "金", "水", "水"};
    private static final String[] WU_XING_ZHI = {"水", "土", "木", "木", "土", "火", "火", "土", "金", "金", "土", "水"};

    public static String getYearPillar(int year) {
        int ganIdx = (year - 4) % 10;
        int zhiIdx = (year - 4) % 12;
        return TIAN_GAN[ganIdx] + DI_ZHI[zhiIdx];
    }

    public static String getBazi(LocalDate date) {
        return getYearPillar(date.getYear());
    }

    public static String getWuxing(LocalDate date) {
        int ganIdx = (date.getYear() - 4) % 10;
        int zhiIdx = (date.getYear() - 4) % 12;
        return WU_XING_GAN[ganIdx] + WU_XING_ZHI[zhiIdx];
    }

    /**
     * 五行相生相克匹配分数 (0-100)。
     * 相生关系得高分，相克关系得低分。
     */
    public static int getWuxingCompatibility(LocalDate date1, LocalDate date2) {
        int ganIdx1 = (date1.getYear() - 4) % 10;
        int ganIdx2 = (date2.getYear() - 4) % 10;
        String wx1 = WU_XING_GAN[ganIdx1];
        String wx2 = WU_XING_GAN[ganIdx2];

        if (wx1.equals(wx2)) return 80;
        if (isGenerating(wx1, wx2) || isGenerating(wx2, wx1)) return 90;
        if (isOvercoming(wx1, wx2) || isOvercoming(wx2, wx1)) return 40;
        return 60;
    }

    private static boolean isGenerating(String a, String b) {
        return (a.equals("木") && b.equals("火")) ||
               (a.equals("火") && b.equals("土")) ||
               (a.equals("土") && b.equals("金")) ||
               (a.equals("金") && b.equals("水")) ||
               (a.equals("水") && b.equals("木"));
    }

    private static boolean isOvercoming(String a, String b) {
        return (a.equals("木") && b.equals("土")) ||
               (a.equals("土") && b.equals("水")) ||
               (a.equals("水") && b.equals("火")) ||
               (a.equals("火") && b.equals("金")) ||
               (a.equals("金") && b.equals("木"));
    }
}
