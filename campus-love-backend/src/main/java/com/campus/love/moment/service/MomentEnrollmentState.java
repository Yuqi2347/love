package com.campus.love.moment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 兼容旧调用的状态门面。
 * 实际状态已落库到 t_moment_activity_week，不再依赖进程内存。
 */
@Component
@RequiredArgsConstructor
public class MomentEnrollmentState {

    private final MomentActivityWeekService activityWeekService;

    public Boolean isClosed(String weekTag) {
        return !activityWeekService.isEnrollmentOpen(weekTag);
    }

    public void close(String weekTag) {
        activityWeekService.closeEnrollment(weekTag);
    }

    public void reopen(String weekTag) {
        activityWeekService.reopenEnrollment(weekTag);
    }
}
