package com.campus.love.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.ai.entity.YuanFenAnalysisLog;
import com.campus.love.ai.mapper.YuanFenAnalysisLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI Token 消耗统计服务（缘分解析）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiTokenStatsService {

    private final YuanFenAnalysisLogMapper logMapper;

    /**
     * 获取 Token 消耗统计
     *
     * @param range day=今日, week=近7天, month=近30天
     */
    public AiTokenStats getStats(String range) {
        LocalDateTime start;
        LocalDateTime end = LocalDateTime.now();
        switch (range != null ? range.toLowerCase() : "week") {
            case "day" -> start = LocalDate.now().atStartOfDay();
            case "month" -> start = LocalDate.now().minusDays(29).atTime(LocalTime.MIN);
            default -> start = LocalDate.now().minusDays(6).atTime(LocalTime.MIN);
        }

        LambdaQueryWrapper<YuanFenAnalysisLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(YuanFenAnalysisLog::getCreatedAt, start);
        wrapper.le(YuanFenAnalysisLog::getCreatedAt, end);
        wrapper.orderByAsc(YuanFenAnalysisLog::getCreatedAt);

        List<YuanFenAnalysisLog> logs = logMapper.selectList(wrapper);

        long totalTokens = 0;
        for (YuanFenAnalysisLog log : logs) {
            totalTokens += (log.getTokensUsed() != null ? log.getTokensUsed() : 0);
        }

        // 按日期聚合
        Map<LocalDate, List<YuanFenAnalysisLog>> byDate = logs.stream()
                .collect(Collectors.groupingBy(l -> l.getCreatedAt() != null ? l.getCreatedAt().toLocalDate() : LocalDate.now()));

        List<AiTokenDailyStat> dailyStats = new ArrayList<>();
        for (LocalDate d = start.toLocalDate(); !d.isAfter(end.toLocalDate()); d = d.plusDays(1)) {
            List<YuanFenAnalysisLog> dayLogs = byDate.getOrDefault(d, List.of());
            int dayTokens = dayLogs.stream()
                    .mapToInt(l -> l.getTokensUsed() != null ? l.getTokensUsed() : 0)
                    .sum();
            dailyStats.add(new AiTokenDailyStat(d.toString(), dayTokens, dayLogs.size()));
        }

        return new AiTokenStats(totalTokens, logs.size(), dailyStats);
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class AiTokenStats {
        private long totalTokens;
        private int callCount;
        private List<AiTokenDailyStat> dailyStats;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class AiTokenDailyStat {
        private String date;
        private int tokensUsed;
        private int callCount;
    }
}
