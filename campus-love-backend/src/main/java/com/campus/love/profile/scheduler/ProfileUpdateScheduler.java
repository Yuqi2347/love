package com.campus.love.profile.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.profile.service.BehaviorAggregationService;
import com.campus.love.profile.service.OceanUpdateService;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 双窗口画像更新调度（技术文档 V1.1.0 第 1.6 节）
 * 短期：每两周一次（周一凌晨）；长期：每月一次（月初凌晨）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileUpdateScheduler {

    private final UserMapper userMapper;
    private final BehaviorAggregationService behaviorAggregationService;
    private final OceanUpdateService oceanUpdateService;

    @Scheduled(cron = "0 0 2 * * MON")
    public void shortWindowUpdate() {
        log.info("Profile short-window update (14-day) starting");
        try {
            List<User> users = loadProfileCompleteUsers();
            for (User user : users) {
                if (behaviorAggregationService.hasRecentActivity(user.getId(), 7)) {
                    oceanUpdateService.updateShortOcean(user.getId());
                }
            }
            log.info("Profile short-window update completed");
        } catch (Exception e) {
            log.error("Profile short-window update failed", e);
        }
    }

    @Scheduled(cron = "0 0 3 1 * *")
    public void longWindowUpdate() {
        log.info("Profile long-window update (6-month) starting");
        try {
            List<User> users = loadProfileCompleteUsers();
            for (User user : users) {
                if (behaviorAggregationService.hasRecentActivity(user.getId(), 30)) {
                    oceanUpdateService.updateLongOcean(user.getId());
                }
            }
            log.info("Profile long-window update completed");
        } catch (Exception e) {
            log.error("Profile long-window update failed", e);
        }
    }

    private List<User> loadProfileCompleteUsers() {
        return userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getProfileComplete, true)
                .eq(User::getStatus, 1));
    }
}
