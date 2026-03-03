package com.campus.love.common.utils;

import java.time.LocalDate;
import java.time.MonthDay;

public final class ZodiacUtil {

    private ZodiacUtil() {}

    private static final String[] ZODIAC_NAMES = {
            "摩羯座", "水瓶座", "双鱼座", "白羊座", "金牛座", "双子座",
            "巨蟹座", "狮子座", "处女座", "天秤座", "天蝎座", "射手座", "摩羯座"
    };

    private static final MonthDay[] ZODIAC_BOUNDARIES = {
            MonthDay.of(1, 20), MonthDay.of(2, 19), MonthDay.of(3, 21),
            MonthDay.of(4, 20), MonthDay.of(5, 21), MonthDay.of(6, 22),
            MonthDay.of(7, 23), MonthDay.of(8, 23), MonthDay.of(9, 23),
            MonthDay.of(10, 24), MonthDay.of(11, 23), MonthDay.of(12, 22)
    };

    public static String getZodiac(LocalDate date) {
        MonthDay md = MonthDay.from(date);
        for (int i = 0; i < ZODIAC_BOUNDARIES.length; i++) {
            if (md.isBefore(ZODIAC_BOUNDARIES[i])) {
                return ZODIAC_NAMES[i];
            }
        }
        return ZODIAC_NAMES[12];
    }

    public static int getZodiacIndex(String zodiac) {
        for (int i = 0; i < ZODIAC_NAMES.length - 1; i++) {
            if (ZODIAC_NAMES[i].equals(zodiac)) return i;
        }
        return 0;
    }
}
