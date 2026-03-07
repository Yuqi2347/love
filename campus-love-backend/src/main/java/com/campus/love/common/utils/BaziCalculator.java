package com.campus.love.common.utils;

import com.campus.love.common.enums.*;
import com.campus.love.common.model.FourPillars;
import com.campus.love.common.model.Pillar;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * 四柱八字完整合婚算法引擎（V2.0）
 *
 * 功能：
 * 1. buildFourPillars() - 根据出生日期时间排四柱八字
 * 2. analyzeYongShen() - 分析用神（日主强弱判断）
 * 3. calculateGanZhiHarmonyScore() - 计算干支互动得分
 * 4. calculateNayinScore() - 计算纳音五行匹配得分
 * 5. calculateYongShenComplementScore() - 计算用神互补得分
 * 6. calculateHunYinScore() - 综合合婚评分
 *
 * @author Campus Love Team
 * @version 2.0
 */
@Slf4j
public final class BaziCalculator {

    private BaziCalculator() {}

    // ==================== 核心方法 ====================

    /**
     * 根据出生日期时间排四柱八字
     *
     * @param birthDateTime 出生时间
     * @return 四柱对象
     */
    public static FourPillars buildFourPillars(LocalDateTime birthDateTime) {
        if (birthDateTime == null) {
            birthDateTime = LocalDateTime.now();
        }

        LocalDate date = birthDateTime.toLocalDate();
        int hour = birthDateTime.getHour();

        // ---- 年柱 ----
        int year = getChineseYear(date);
        int yearGanIdx = (year - 4) % 10;
        int yearZhiIdx = (year - 4) % 12;

        // ---- 月柱 ----
        int monthIdx = getSolarTermMonth(date);  // 0=寅月（农历正月）
        int monthGanIdx = ((yearGanIdx % 5) * 2 + monthIdx) % 10;
        int monthZhiIdx = (monthIdx + 2) % 12;

        // ---- 日柱 ----
        long julianDay = toJulianDay(date);
        if (hour == 23) julianDay += 1;  // 子时（23:00）起算为次日
        int dayGanIdx = (int) ((julianDay + 40) % 10);
        int dayZhiIdx = (int) ((julianDay + 12) % 12);

        // ---- 时柱 ----
        int hourZhiIdx = getHourZhiIdx(hour);
        int hourGanIdx = ((dayGanIdx % 5) * 2 + hourZhiIdx) % 10;

        return FourPillars.builder()
                .yearPillar(new Pillar(TianGan.of(yearGanIdx), DiZhi.of(yearZhiIdx)))
                .monthPillar(new Pillar(TianGan.of(monthGanIdx), DiZhi.of(monthZhiIdx)))
                .dayPillar(new Pillar(TianGan.of(dayGanIdx), DiZhi.of(dayZhiIdx)))
                .hourPillar(new Pillar(TianGan.of(hourGanIdx), DiZhi.of(hourZhiIdx)))
                .build();
    }

    /**
     * 分析用神（日主强弱判断）
     *
     * @param pillars 四柱
     * @return 用神五行
     */
    public static WuXing analyzeYongShen(FourPillars pillars) {
        if (pillars == null || pillars.getDayPillar() == null) {
            return WuXing.EARTH;  // 默认
        }

        TianGan dayGan = pillars.getDayPillar().getGan();
        WuXing dayWuXing = dayGan != null ? dayGan.getWuXing() : WuXing.EARTH;

        Map<WuXing, Double> wuXingPower = calculateWuXingPower(pillars);
        double selfPower = wuXingPower.getOrDefault(dayWuXing, 0.0);
        double totalPower = wuXingPower.values().stream().mapToDouble(d -> d).sum();

        if (totalPower == 0) return WuXing.EARTH;

        double ratio = selfPower / totalPower;

        if (ratio >= 0.4) {
            // 日主过旺 → 用神为克制或泄耗之五行
            return dayWuXing.getOvercame();  // 我克之行
        } else if (ratio <= 0.2) {
            // 日主偏弱 → 用神为生扶之五行
            return dayWuXing.getGenerating();  // 生我之行
        } else {
            // 日主中和 → 用神为流通之五行（选择我生的）
            return dayWuXing.getGenerated();
        }
    }

    /**
     * 计算干支互动得分（六合/三合/冲/刑）
     */
    public static int calculateGanZhiHarmonyScore(FourPillars pillars1, FourPillars pillars2) {
        if (pillars1 == null || pillars2 == null) return 50;

        int score = 50;  // 基础分

        List<TianGan> gans1 = pillars1.getAllGans();
        List<TianGan> gans2 = pillars2.getAllGans();
        List<DiZhi> zhis1 = pillars1.getAllZhis();
        List<DiZhi> zhis2 = pillars2.getAllZhis();

        // 天干互动
        for (TianGan g1 : gans1) {
            for (TianGan g2 : gans2) {
                String key = g1.getChineseChar() + g2.getChineseChar();
                score += BaziConstants.TIAN_GAN_HE_SCORES.getOrDefault(key, 0);
                score += BaziConstants.TIAN_GAN_CHONG_SCORES.getOrDefault(key, 0);
            }
        }

        // 地支互动
        for (DiZhi z1 : zhis1) {
            for (DiZhi z2 : zhis2) {
                String key = z1.getChineseChar() + z2.getChineseChar();
                score += BaziConstants.DI_ZHI_LIU_HE_SCORES.getOrDefault(key, 0);
                score += BaziConstants.DI_ZHI_CHONG_SCORES.getOrDefault(key, 0);
                score += BaziConstants.DI_ZHI_XING_SCORES.getOrDefault(key, 0);
            }
        }

        // 地支三合（两人合共的地支能否凑成三合局）
        Set<String> allZhiChars = new HashSet<>();
        zhis1.forEach(z -> allZhiChars.add(z.getChineseChar()));
        zhis2.forEach(z -> allZhiChars.add(z.getChineseChar()));
        for (Set<String> sanHeGroup : BaziConstants.DI_ZHI_SAN_HE_GROUPS) {
            if (allZhiChars.containsAll(sanHeGroup)) {
                score += 20;
            }
        }

        return Math.max(10, Math.min(100, score));
    }

    /**
     * 计算纳音五行匹配得分
     */
    public static int calculateNayinScore(FourPillars pillars1, FourPillars pillars2) {
        if (pillars1 == null || pillars2 == null) return 50;

        String nayin1 = getNayin(pillars1.getYearPillar());
        String nayin2 = getNayin(pillars2.getYearPillar());

        if (nayin1 == null || nayin2 == null) return 50;

        WuXing wx1 = BaziConstants.NAYIN_WUXING_MAP.get(nayin1);
        WuXing wx2 = BaziConstants.NAYIN_WUXING_MAP.get(nayin2);

        if (wx1 == null || wx2 == null) return 50;

        return calculateWuXingRelationScore(wx1, wx2);
    }

    /**
     * 计算用神互补得分（合婚核心）
     */
    public static int calculateYongShenComplementScore(FourPillars self, FourPillars target) {
        if (self == null || target == null) return 50;

        WuXing selfYongShen = analyzeYongShen(self);
        WuXing targetYongShen = analyzeYongShen(target);

        Map<WuXing, Double> targetPower = calculateWuXingPower(target);
        Map<WuXing, Double> selfPower = calculateWuXingPower(self);

        double targetHasSelfYongShen = targetPower.getOrDefault(selfYongShen, 0.0);
        double selfHasTargetYongShen = selfPower.getOrDefault(targetYongShen, 0.0);

        double targetTotal = targetPower.values().stream().mapToDouble(d -> d).sum();
        double selfTotal = selfPower.values().stream().mapToDouble(d -> d).sum();

        double ratio1 = targetTotal > 0 ? targetHasSelfYongShen / targetTotal : 0;
        double ratio2 = selfTotal > 0 ? selfHasTargetYongShen / selfTotal : 0;

        // 双向互补得高分，单向补得中等分
        double avgRatio = (ratio1 + ratio2) / 2.0;
        return (int) Math.round(30 + avgRatio * 70);
    }

    /**
     * 综合合婚评分（V2.0核心方法）
     *
     * @param birth1 用户A出生时间
     * @param birth2 用户B出生时间
     * @return 合婚总分 [10, 100]
     */
    public static int calculateHunYinScore(LocalDateTime birth1, LocalDateTime birth2) {
        if (birth1 == null) birth1 = LocalDateTime.now();
        if (birth2 == null) birth2 = LocalDateTime.now();

        FourPillars pillars1 = buildFourPillars(birth1);
        FourPillars pillars2 = buildFourPillars(birth2);

        // 子项得分
        int ganZhiScore = calculateGanZhiHarmonyScore(pillars1, pillars2);
        int yongShenScore = calculateYongShenComplementScore(pillars1, pillars2);
        int nayinScore = calculateNayinScore(pillars1, pillars2);
        int dayGanRelScore = calculateDayGanRelationScore(pillars1, pillars2);
        int wuXingBalanceScore = calculateWuXingBalanceScore(pillars1, pillars2);

        // 加权合计（日干关系权重最高）
        int totalScore = (int) (
                dayGanRelScore * 0.35 +     // 日干关系（代表本人）
                yongShenScore * 0.25 +       // 用神互补
                ganZhiScore * 0.20 +         // 干支互动
                nayinScore * 0.10 +          // 纳音五行
                wuXingBalanceScore * 0.10    // 五行平衡
        );

        return Math.max(10, Math.min(100, totalScore));
    }

    // ==================== 辅助方法 ====================

    /**
     * 计算五行力量值
     */
    private static Map<WuXing, Double> calculateWuXingPower(FourPillars pillars) {
        Map<WuXing, Double> power = new EnumMap<>(WuXing.class);
        List<Pillar> allPillars = pillars.toList();

        for (Pillar pillar : allPillars) {
            if (pillar == null) continue;

            // 天干力量
            if (pillar.getGan() != null) {
                addPower(power, pillar.getGan().getWuXing(), 1.0);
            }

            // 地支本气力量
            if (pillar.getZhi() != null) {
                addPower(power, pillar.getZhi().getWuXing(), 0.8);

                // 藏干力量
                TianGan[] hiddenGans = pillar.getZhi().getHiddenGans();
                if (hiddenGans != null && hiddenGans.length > 0) {
                    double hiddenWeight = 0.3 / hiddenGans.length;
                    for (TianGan hg : hiddenGans) {
                        if (hg != null) {
                            addPower(power, hg.getWuXing(), hiddenWeight);
                        }
                    }
                }
            }
        }

        return power;
    }

    private static void addPower(Map<WuXing, Double> power, WuXing wuXing, double value) {
        if (wuXing == null) return;
        power.put(wuXing, power.getOrDefault(wuXing, 0.0) + value);
    }

    /**
     * 日干关系评分（日干代表本人，最重要）
     */
    private static int calculateDayGanRelationScore(FourPillars p1, FourPillars p2) {
        if (p1 == null || p2 == null ||
            p1.getDayPillar() == null || p2.getDayPillar() == null ||
            p1.getDayPillar().getGan() == null || p2.getDayPillar().getGan() == null) {
            return 50;
        }

        TianGan dg1 = p1.getDayPillar().getGan();
        TianGan dg2 = p2.getDayPillar().getGan();

        if (TianGan.isHe(dg1, dg2)) return 95;      // 日干相合：最佳
        if (dg1.getWuXing() == dg2.getWuXing()) return 80;  // 同五行
        if (WuXing.isGenerating(dg1.getWuXing(), dg2.getWuXing()) ||
            WuXing.isGenerating(dg2.getWuXing(), dg1.getWuXing())) return 85;  // 相生
        if (TianGan.isChong(dg1, dg2)) return 35;   // 日干相冲：最差
        if (WuXing.isOvercoming(dg1.getWuXing(), dg2.getWuXing()) ||
            WuXing.isOvercoming(dg2.getWuXing(), dg1.getWuXing())) return 45;  // 相克
        return 65;  // 无特殊关系
    }

    /**
     * 五行平衡度评分
     */
    private static int calculateWuXingBalanceScore(FourPillars p1, FourPillars p2) {
        Map<WuXing, Double> power1 = calculateWuXingPower(p1);
        Map<WuXing, Double> power2 = calculateWuXingPower(p2);

        // 合并后的五行分布
        Map<WuXing, Double> combined = new EnumMap<>(WuXing.class);
        for (WuXing wx : WuXing.values()) {
            combined.put(wx, power1.getOrDefault(wx, 0.0) + power2.getOrDefault(wx, 0.0));
        }

        double total = combined.values().stream().mapToDouble(d -> d).sum();
        if (total == 0) return 50;

        // 计算分布的均匀度（使用标准差的倒数）
        double avg = total / 5;
        double variance = combined.values().stream()
                .mapToDouble(v -> Math.pow(v - avg, 2))
                .average()
                .orElse(0);
        double stdDev = Math.sqrt(variance);

        // 标准差越小，分布越均匀，得分越高
        if (stdDev < 0.3) return 90;
        if (stdDev < 0.6) return 80;
        if (stdDev < 1.0) return 70;
        if (stdDev < 1.5) return 60;
        return 50;
    }

    /**
     * 五行关系评分
     */
    private static int calculateWuXingRelationScore(WuXing wx1, WuXing wx2) {
        if (wx1 == null || wx2 == null) return 50;
        if (wx1 == wx2) return 80;  // 同五行
        if (WuXing.isGenerating(wx1, wx2) || WuXing.isGenerating(wx2, wx1)) return 90;  // 相生
        if (WuXing.isOvercoming(wx1, wx2) || WuXing.isOvercoming(wx2, wx1)) return 40;  // 相克
        return 60;  // 中性
    }

    /**
     * 获取年柱纳音
     */
    private static String getNayin(Pillar yearPillar) {
        if (yearPillar == null || yearPillar.getGan() == null || yearPillar.getZhi() == null) {
            return null;
        }
        String key = yearPillar.getGan().getChineseChar() + yearPillar.getZhi().getChineseChar();
        return BaziConstants.NAYIN_MAP.get(key);
    }

    // ==================== 历法计算辅助方法 ====================

    /**
     * 获取农历年份（以立春为界）
     */
    private static int getChineseYear(LocalDate date) {
        // 简化实现：立春约在2月4日前后
        int year = date.getYear();
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        // 如果在立春前（2月4日），算作前一年
        if (month == 1 || (month == 2 && day < 4)) {
            return year - 1;
        }
        return year;
    }

    /**
     * 获取农历月份（以节气为界）
     * 返回0-11，0=寅月（农历正月）
     */
    private static int getSolarTermMonth(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        // 节气日期表（简化版）
        int[][] solarTerms = {
            {4, 6},   // 立春（寅月开始）
            {6, 6},   // 惊蛰
            {5, 7},   // 清明
            {5, 7},   // 立夏
            {6, 7},   // 芒种
            {6, 7},   // 小暑
            {7, 8},   // 立秋
            {8, 9},   // 白露
            {8, 9},   // 寒露
            {8, 9},   // 立冬
            {7, 8},   // 大雪
            {7, 8}    // 小寒
        };

        int idx = (month + 1) % 12;
        if (idx < 0) idx += 12;

        // 如果在节气前，算作上一月
        int thresholdDay = solarTerms[idx][0];
        if (day < thresholdDay) {
            idx = (idx - 1 + 12) % 12;
        }

        return idx;
    }

    /**
     * 转换为儒略日数
     */
    private static long toJulianDay(LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        int a = (14 - month) / 12;
        int y = year + 4800 - a;
        int m = month + 12 * a - 3;

        return day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045;
    }

    /**
     * 根据小时获取地支索引（子=0）
     * 十二时辰：子(23-1)丑(1-3)寅(3-5)...
     */
    private static int getHourZhiIdx(int hour) {
        // 子时：23:00-01:00 → 0
        return ((hour + 1) / 2) % 12;
    }

    // ==================== 兼容性方法（支持旧版API） ====================

    /**
     * 兼容旧版：使用LocalDate计算合婚分数
     */
    public static int calculateHunYinScore(LocalDate date1, LocalDate date2) {
        LocalDateTime dt1 = date1 != null ? date1.atStartOfDay() : LocalDateTime.now();
        LocalDateTime dt2 = date2 != null ? date2.atStartOfDay() : LocalDateTime.now();
        return calculateHunYinScore(dt1, dt2);
    }

    /**
     * 兼容旧版：使用LocalDate和LocalTime计算合婚分数
     */
    public static int calculateHunYinScore(LocalDate date, LocalTime time1, LocalTime time2) {
        LocalDateTime dt1 = date != null && time1 != null ? LocalDateTime.of(date, time1) : LocalDateTime.now();
        LocalDateTime dt2 = date != null && time2 != null ? LocalDateTime.of(date, time2) : LocalDateTime.now();
        return calculateHunYinScore(dt1, dt2);
    }
}
