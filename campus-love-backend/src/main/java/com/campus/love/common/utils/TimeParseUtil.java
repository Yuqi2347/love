package com.campus.love.common.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 时间解析工具：UTC 字符串、periodConfig JSON 等统一解析。
 */
public final class TimeParseUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

    private TimeParseUtil() {}

    /**
     * 将前端传入的 UTC 时间字符串（如 new Date().toISOString()）转为系统时区 LocalDateTime。
     */
    public static LocalDateTime parseUtcToLocalDateTime(String utcTimeStr) {
        if (utcTimeStr == null || utcTimeStr.isBlank()) {
            return null;
        }
        Instant instant = Instant.parse(utcTimeStr.trim());
        return LocalDateTime.ofInstant(instant, DEFAULT_ZONE);
    }

    /**
     * 解析 periodConfig JSON 中的 start/end 时间，格式如 {"start":"2026-03-06T10:00:00.000Z","end":"..."}。
     * 解析失败返回 null（调用方可按需视为“不限制时间”）。
     */
    public static PeriodBounds parsePeriodConfigStartEnd(String periodConfig) {
        if (periodConfig == null || periodConfig.isBlank()) {
            return null;
        }
        try {
            PeriodConfigDto dto = OBJECT_MAPPER.readValue(periodConfig.trim(), PeriodConfigDto.class);
            if (dto.getStart() == null || dto.getEnd() == null) {
                return null;
            }
            LocalDateTime start = parseUtcToLocalDateTime(dto.getStart());
            LocalDateTime end = parseUtcToLocalDateTime(dto.getEnd());
            if (start == null || end == null) {
                return null;
            }
            return new PeriodBounds(start, end);
        } catch (Exception e) {
            return null;
        }
    }

    @Data
    public static class PeriodBounds {
        private final LocalDateTime start;
        private final LocalDateTime end;
    }

    @Data
    private static class PeriodConfigDto {
        @JsonProperty("start")
        private String start;
        @JsonProperty("end")
        private String end;
    }
}
