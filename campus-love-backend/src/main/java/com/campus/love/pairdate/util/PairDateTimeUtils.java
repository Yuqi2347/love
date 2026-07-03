package com.campus.love.pairdate.util;

import com.campus.love.pairdate.enums.TimeSlotCode;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.WeekFields;

/**
 * weekTag 字符串为「周日为首日、最少 1 天即算第一周」的周序号（与 {@link com.campus.love.moment.service.MomentService#getCurrentWeekTag} 一致），
 * 解析周界时使用 {@link WeekFields#ISO} 的周一锚点推算该周周五；放榜日为该周周五，时间格从上海时区起算。
 */
public final class PairDateTimeUtils {

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    private PairDateTimeUtils() {
    }

    public static LocalDate mondayOfIsoWeek(String weekTag) {
        if (weekTag == null || !weekTag.contains("-W")) {
            throw new IllegalArgumentException("weekTag");
        }
        int w = weekTag.indexOf("-W");
        int year = Integer.parseInt(weekTag.substring(0, w));
        int week = Integer.parseInt(weekTag.substring(w + 2));
        WeekFields wf = WeekFields.ISO;
        return LocalDate.of(year, 1, 4)
                .with(wf.weekOfWeekBasedYear(), week)
                .with(wf.dayOfWeek(), DayOfWeek.MONDAY.getValue());
    }

    public static LocalDate revealFriday(String weekTag) {
        return mondayOfIsoWeek(weekTag).plusDays(4);
    }

    /** 时段起点（上海时区）的毫秒时间戳 */
    public static long slotStartEpochMillis(String weekTag, TimeSlotCode slot) {
        LocalDate day = revealFriday(weekTag).plusDays(slot.getDaysAfterRevealFriday());
        return day.atTime(slot.getStartHour(), 0).atZone(SHANGHAI).toInstant().toEpochMilli();
    }
}
