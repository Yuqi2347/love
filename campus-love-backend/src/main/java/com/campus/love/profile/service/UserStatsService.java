package com.campus.love.profile.service;

import com.campus.love.profile.entity.UserStats;
import com.campus.love.profile.mapper.UserStatsMapper;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户统计读写
 * 替代原 t_user 统计字段更新
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatsService {

    private final UserStatsMapper userStatsMapper;
    private final UserMapper userMapper;

    /**
     * 获取统计，优先 t_user_stats，缺失时从 t_user 读取
     */
    public UserStats getStats(Long userId) {
        UserStats s = userStatsMapper.selectById(userId);
        if (s != null) return s;
        User user = userMapper.selectById(userId);
        if (user == null) return null;
        UserStats stats = new UserStats();
        stats.setUserId(userId);
        stats.setActivityScore(user.getActivityScore() != null ? user.getActivityScore() : 0);
        stats.setUserLevel(user.getUserLevel() != null ? user.getUserLevel() : 1);
        stats.setInviteCount(user.getInviteCount() != null ? user.getInviteCount() : 0);
        stats.setParticipateCount(user.getParticipateCount() != null ? user.getParticipateCount() : 0);
        stats.setCreditScore(user.getCreditScore() != null ? user.getCreditScore() : 100);
        return stats;
    }

    @Transactional(rollbackFor = Exception.class)
    public void createStats(Long userId) {
        if (userStatsMapper.selectById(userId) != null) return;
        UserStats stats = new UserStats();
        stats.setUserId(userId);
        stats.setActivityScore(0);
        stats.setUserLevel(1);
        stats.setInviteCount(0);
        stats.setParticipateCount(0);
        stats.setCreditScore(100);
        userStatsMapper.insert(stats);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveStats(UserStats stats) {
        if (stats == null || stats.getUserId() == null) return;
        UserStats existing = userStatsMapper.selectById(stats.getUserId());
        if (existing != null) {
            userStatsMapper.updateById(stats);
        } else {
            userStatsMapper.insert(stats);
        }
    }

    public void incrementInviteCount(Long userId) {
        UserStats s = getStats(userId);
        if (s == null) return;
        s.setInviteCount((s.getInviteCount() != null ? s.getInviteCount() : 0) + 1);
        saveStats(s);
    }

    public void incrementParticipateCount(Long userId) {
        UserStats s = getStats(userId);
        if (s == null) return;
        s.setParticipateCount((s.getParticipateCount() != null ? s.getParticipateCount() : 0) + 1);
        saveStats(s);
    }
}
