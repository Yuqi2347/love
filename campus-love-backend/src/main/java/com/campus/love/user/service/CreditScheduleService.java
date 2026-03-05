package com.campus.love.user.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 信用分相关定时任务：
 * - 每日自然恢复 +1，最高不超过 100
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreditScheduleService {

    private final UserMapper userMapper;

    /**
     * 每天凌晨 3:00 执行一次信用分自然恢复：
     * - credit_score 为空的视为 100，不再增加
     * - credit_score < 100 的用户 +1
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void recoverCreditScoreDaily() {
        try {
            LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
                    .isNotNull(User::getCreditScore)
                    .lt(User::getCreditScore, 100)
                    .setSql("credit_score = credit_score + 1");
            int updated = userMapper.update(null, wrapper);
            log.info("每日信用分自然恢复任务完成，本次更新用户数：{}", updated);
        } catch (Exception e) {
            log.error("每日信用分自然恢复任务执行异常", e);
        }
    }
}

