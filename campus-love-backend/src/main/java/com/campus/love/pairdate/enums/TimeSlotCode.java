package com.campus.love.pairdate.enums;

import java.util.Collection;
import java.util.Comparator;

/**
 * 放榜周五次日周六起至下周四，每天上/下/晚。声明顺序即优先级（SAT_AM 最先）。
 */
public enum TimeSlotCode {
    SAT_AM(1, 8),
    SAT_PM(1, 12),
    SAT_EVE(1, 18),
    SUN_AM(2, 8),
    SUN_PM(2, 12),
    SUN_EVE(2, 18),
    MON_AM(3, 8),
    MON_PM(3, 12),
    MON_EVE(3, 18),
    TUE_AM(4, 8),
    TUE_PM(4, 12),
    TUE_EVE(4, 18),
    WED_AM(5, 8),
    WED_PM(5, 12),
    WED_EVE(5, 18),
    THU_AM(6, 8),
    THU_PM(6, 12),
    THU_EVE(6, 18);

    private final int daysAfterRevealFriday;
    private final int startHour;

    TimeSlotCode(int daysAfterRevealFriday, int startHour) {
        this.daysAfterRevealFriday = daysAfterRevealFriday;
        this.startHour = startHour;
    }

    public int getDaysAfterRevealFriday() {
        return daysAfterRevealFriday;
    }

    public int getStartHour() {
        return startHour;
    }

    public static TimeSlotCode fromString(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return TimeSlotCode.valueOf(s.trim());
    }

    public static TimeSlotCode minIntersect(Collection<TimeSlotCode> a, Collection<TimeSlotCode> b) {
        if (a == null || b == null) {
            return null;
        }
        return a.stream().filter(b::contains).min(Comparator.comparingInt(Enum::ordinal)).orElse(null);
    }
}
