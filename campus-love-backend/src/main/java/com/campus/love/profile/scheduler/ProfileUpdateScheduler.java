package com.campus.love.profile.scheduler;

import com.campus.love.profile.mapper.UserAiProfileMapper;
import com.campus.love.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 双窗口画像更新调度（技术文档 V1.1.0 第 1.6 节）
 * 短期：每两周一次（周一凌晨）；长期：每月一次（月初凌晨）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileUpdateScheduler {

    private final UserAiProfileMapper userAiProfileMapper;
    private final UserMapper userMapper;

    @Scheduled(cron = "0 0 2 * * MON")
    public void shortWindowUpdate() {
        log.info("Profile short-window update (14-day) starting");
        try {
            // TODO: 聚合近14天行为 → 更新 t_user_behavior_summary → 短期 OCEAN 更新
            log.info("Profile short-window update completed");
        } catch (Exception e) {
            log.error("Profile short-window update failed", e);
        }
    }

    @Scheduled(cron = "0 0 3 1 * *")
    public void longWindowUpdate() {
        log.info("Profile long-window update (6-month) starting");
        try {
            // TODO: 聚合近6个月行为 → 长期 OCEAN 更新 → 重新向量化
            log.info("Profile long-window update completed");
        } catch (Exception e) {
            log.error("Profile long-window update failed", e);
        }
    }
}
