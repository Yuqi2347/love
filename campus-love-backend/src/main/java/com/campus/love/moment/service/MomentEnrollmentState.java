package com.campus.love.moment.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 心动时刻报名截止状态（内存）：weekTag -> 是否已截止。
 * 管理员可手动截止，triggerMatching 时也会自动截止。
 */
@Component
public class MomentEnrollmentState {

    private final Map<String, Boolean> closedWeeks = new ConcurrentHashMap<>();

    public Boolean isClosed(String weekTag) {
        return closedWeeks.get(weekTag);
    }

    public void close(String weekTag) {
        closedWeeks.put(weekTag, true);
    }

    public void reopen(String weekTag) {
        closedWeeks.remove(weekTag);
    }
}
