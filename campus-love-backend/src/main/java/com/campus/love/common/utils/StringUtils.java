package com.campus.love.common.utils;

/**
 * 字符串工具类
 */
public final class StringUtils {

    private StringUtils() {}

    /**
     * 截断字符串，超过 maxLen 时追加 "..."
     */
    public static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
