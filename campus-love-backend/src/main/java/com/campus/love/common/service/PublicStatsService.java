package com.campus.love.common.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.admin.mapper.AdminStatsMapper;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 公开统计（登录/注册页展示，无需鉴权）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublicStatsService {

    private final AdminStatsMapper adminStatsMapper;
    private final UserMapper userMapper;

    /**
     * 活跃用户数：近7天有行为记录的用户；若为0则回退为总注册用户数
     */
    public long getActiveUserCount() {
        try {
            long active = adminStatsMapper.countActiveUsers7d();
            if (active > 0) return active;
        } catch (Exception e) {
            log.warn("countActiveUsers7d failed, fallback to total: {}", e.getMessage());
        }
        return userMapper.selectCount(new LambdaQueryWrapper<User>().isNull(User::getDeletedAt));
    }
}
