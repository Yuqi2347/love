package com.campus.love.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.ai.entity.AiUsageLog;
import com.campus.love.ai.mapper.AiUsageLogMapper;
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

    private final AiUsageLogMapper aiUsageLogMapper;

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

        LambdaQueryWrapper<AiUsageLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(AiUsageLog::getCreatedAt, start);
        wrapper.le(AiUsageLog::getCreatedAt, end);
        wrapper.orderByAsc(AiUsageLog::getCreatedAt);

        List<AiUsageLog> logs = aiUsageLogMapper.selectList(wrapper);

        StatCounter total = new StatCounter();
        StatCounter avatar = new StatCounter();
        StatCounter analysis = new StatCounter();

        Map<LocalDate, DailyCounter> byDate = logs.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getCreatedAt() != null ? l.getCreatedAt().toLocalDate() : LocalDate.now(),
                        Collectors.collectingAndThen(Collectors.toList(), DailyCounter::fromLogs)
                ));

        List<AiTokenDailyStat> dailyStats = new ArrayList<>();
        for (LocalDate d = start.toLocalDate(); !d.isAfter(end.toLocalDate()); d = d.plusDays(1)) {
            DailyCounter counter = byDate.getOrDefault(d, DailyCounter.empty());
            dailyStats.add(new AiTokenDailyStat(
                    d.toString(),
                    counter.total().tokensUsed,
                    counter.total().callCount,
                    counter.avatar().tokensUsed,
                    counter.avatar().callCount,
                    counter.analysis().tokensUsed,
                    counter.analysis().callCount
            ));
        }

        for (AiUsageLog log : logs) {
            long tokensUsed = Math.max(0, log.getTokensUsed() != null ? log.getTokensUsed() : 0);
            long callCount = Math.max(1, log.getCallCount() != null ? log.getCallCount() : 1);
            total.add(tokensUsed, callCount);
            if (AiUsageLogService.BIZ_TYPE_AVATAR.equalsIgnoreCase(log.getBizType())) {
                avatar.add(tokensUsed, callCount);
            } else {
                analysis.add(tokensUsed, callCount);
            }
        }

        return new AiTokenStats(
                total.tokensUsed,
                total.callCount,
                new AiTokenCategoryStat(avatar.tokensUsed, avatar.callCount),
                new AiTokenCategoryStat(analysis.tokensUsed, analysis.callCount),
                dailyStats
        );
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class AiTokenStats {
        private long totalTokens;
        private long callCount;
        private AiTokenCategoryStat avatar;
        private AiTokenCategoryStat analysis;
        private List<AiTokenDailyStat> dailyStats;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class AiTokenCategoryStat {
        private long tokensUsed;
        private long callCount;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class AiTokenDailyStat {
        private String date;
        private long totalTokens;
        private long callCount;
        private long avatarTokens;
        private long avatarCallCount;
        private long analysisTokens;
        private long analysisCallCount;
    }

    private static class StatCounter {
        private long tokensUsed;
        private long callCount;

        private void add(long tokens, long calls) {
            this.tokensUsed += tokens;
            this.callCount += calls;
        }
    }

    private record DailyCounter(StatCounter total, StatCounter avatar, StatCounter analysis) {
        private static DailyCounter empty() {
            return new DailyCounter(new StatCounter(), new StatCounter(), new StatCounter());
        }

        private static DailyCounter fromLogs(List<AiUsageLog> logs) {
            DailyCounter counter = empty();
            for (AiUsageLog log : logs) {
                long tokensUsed = Math.max(0, log.getTokensUsed() != null ? log.getTokensUsed() : 0);
                long callCount = Math.max(1, log.getCallCount() != null ? log.getCallCount() : 1);
                counter.total.add(tokensUsed, callCount);
                if (AiUsageLogService.BIZ_TYPE_AVATAR.equalsIgnoreCase(log.getBizType())) {
                    counter.avatar.add(tokensUsed, callCount);
                } else {
                    counter.analysis.add(tokensUsed, callCount);
                }
            }
            return counter;
        }
    }
}
