package com.campus.love.moment.service;

import com.campus.love.moment.entity.MomentActivityWeek;
import com.campus.love.moment.entity.MomentMatchConfig;
import com.campus.love.pairdate.util.PairDateTimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * 心动时刻：在「RESULT_READY（管理员可预览）」且到达配置的北京时间后，自动公布结果（PUBLISHED），用户端可见缘分。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MomentAutoPublishScheduler {

    private static final long SCHEDULER_FIXED_DELAY_MS = 60 * 1000L;
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    private final MomentMatchConfigService matchConfigService;
    private final MomentAdminService momentAdminService;
    private final MomentService momentService;
    private final MomentActivityWeekService activityWeekService;

    @Scheduled(fixedDelay = SCHEDULER_FIXED_DELAY_MS)
    public void autoPublishCurrentWeek() {
        MomentMatchConfig config = matchConfigService.getConfig();
        if (config == null || !Boolean.TRUE.equals(config.getAutoPublishEnabled())) {
            return;
        }
        if (config.getAutoPublishDayOfWeek() == null
                || config.getAutoPublishTime() == null
                || config.getAutoPublishTime().isBlank()) {
            return;
        }

        String weekTag = momentService.getCurrentWeekTag();
        MomentActivityWeek week = activityWeekService.getOrCreateWeek(weekTag);
        if (!MomentActivityWeek.STATUS_RESULT_READY.equals(week.getStatus())) {
            return;
        }

        LocalDateTime shNow = LocalDateTime.now(SHANGHAI);
        if (!isPastPublishSlot(config, shNow, weekTag)) {
            return;
        }

        try {
            momentAdminService.publishResultBySystem(weekTag, weekTag);
            log.info("心动时刻自动公布结果完成 weekTag={}", weekTag);
        } catch (Exception e) {
            log.error("心动时刻自动公布结果失败 weekTag={}", weekTag, e);
        }
    }

    /**
     * 本期活动周内、配置周几 + 时刻；当前北京时间不早于该时刻则允许自动公布（含「已过预定时刻才 RESULT_READY」的补发）。
     */
    private boolean isPastPublishSlot(MomentMatchConfig config, LocalDateTime shNow, String weekTag) {
        try {
            LocalTime triggerTime = LocalTime.parse(config.getAutoPublishTime().trim());
            LocalDate monday = PairDateTimeUtils.mondayOfIsoWeek(weekTag);
            LocalDate day = monday.plusDays(config.getAutoPublishDayOfWeek() - 1L);
            LocalDateTime slot = LocalDateTime.of(day, triggerTime);
            return !shNow.isBefore(slot);
        } catch (Exception e) {
            log.warn("解析自动公布时间失败 weekTag={}", weekTag, e);
            return false;
        }
    }
}
