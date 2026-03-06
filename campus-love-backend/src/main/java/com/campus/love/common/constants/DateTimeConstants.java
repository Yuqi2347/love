package com.campus.love.common.constants;

import java.time.format.DateTimeFormatter;

/**
 * 日期时间格式常量，供 ChatService、FeedService、InviteService 等统一使用。
 */
public final class DateTimeConstants {

    private DateTimeConstants() {}

    /** 完整日期时间：yyyy-MM-dd HH:mm:ss */
    public static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 简短时间：MM-dd HH:mm */
    public static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");
}
