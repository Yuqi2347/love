package com.campus.love.moment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.moment.entity.MomentActivityWeek;
import com.campus.love.moment.entity.MomentEnrollment;
import com.campus.love.moment.entity.MomentMatchConfig;
import com.campus.love.moment.mapper.MomentEnrollmentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * 心动时刻自动调度：
 * - 到达配置时间后自动截止报名
 * - 若存在待匹配用户，则自动执行匹配
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MomentAutoMatchScheduler {

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final long SCHEDULER_FIXED_DELAY_MS = 60 * 1000L;

    private final MomentMatchConfigService matchConfigService;
    private final MomentAdminService momentAdminService;
    private final MomentService momentService;
    private final MomentEnrollmentMapper enrollmentMapper;
    private final MomentActivityWeekService activityWeekService;

    @Scheduled(fixedDelay = SCHEDULER_FIXED_DELAY_MS)
    public void autoTriggerCurrentWeek() {
        MomentMatchConfig config = matchConfigService.getConfig();
        LocalDateTime now = LocalDateTime.now(SHANGHAI);
        if (!shouldTriggerNow(config, now)) {
            return;
        }

        String weekTag = momentService.getCurrentWeekTag();
        MomentActivityWeek week = activityWeekService.getOrCreateWeek(weekTag);
        if (week.getAutoMatchAt() != null || MomentWeekStatusPolicy.blocksTriggerMatching(week.getStatus())) {
            return;
        }

        long waitingCount = countWaitingUsers(weekTag);
        if (waitingCount == 0 && !Boolean.TRUE.equals(week.getEnrollmentOpen())) {
            return;
        }

        try {
            if (waitingCount == 0) {
                momentAdminService.closeEnrollmentBySystem(weekTag, weekTag);
                log.info("心动时刻自动调度执行：weekTag={}，当前无待匹配用户，已自动截止报名", weekTag);
                return;
            }

            Map<String, Object> result = momentAdminService.triggerMatchingBySystem(weekTag, weekTag);
            log.info("心动时刻自动匹配完成：weekTag={}，result={}", weekTag, result);
        } catch (Exception e) {
            log.error("心动时刻自动匹配执行失败: weekTag={}", weekTag, e);
        }
    }

    private boolean shouldTriggerNow(MomentMatchConfig config, LocalDateTime now) {
        if (config == null || !Boolean.TRUE.equals(config.getAutoMatchEnabled())) {
            return false;
        }
        if (config.getAutoMatchDayOfWeek() == null || config.getAutoMatchTime() == null || config.getAutoMatchTime().isBlank()) {
            return false;
        }
        if (now.getDayOfWeek().getValue() != config.getAutoMatchDayOfWeek()) {
            return false;
        }
        LocalTime triggerTime = LocalTime.parse(config.getAutoMatchTime());
        return !now.toLocalTime().isBefore(triggerTime);
    }

    private long countWaitingUsers(String weekTag) {
        return enrollmentMapper.selectList(
                        new LambdaQueryWrapper<MomentEnrollment>()
                                .eq(MomentEnrollment::getWeekTag, weekTag)
                                .eq(MomentEnrollment::getStatus, MomentEnrollment.STATUS_WAITING)
                ).stream()
                .map(MomentEnrollment::getUserId)
                .distinct()
                .count();
    }
}
