package com.campus.love.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.constants.UserLevelConstants;
import com.campus.love.common.enums.ActivityTypeEnum;
import com.campus.love.user.entity.ActivityLog;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.ActivityLogMapper;
import com.campus.love.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityLogMapper activityLogMapper;
    private final UserMapper userMapper;

    /**
     * 记录活跃度并更新用户等级
     */
    @Transactional
    public void recordActivity(ActivityTypeEnum activityType, Long targetId) {
        Long userId = CurrentUser.getId();
        if (userId == null) return;

        // 记录活跃度日志
        ActivityLog log = new ActivityLog();
        log.setUserId(userId);
        log.setActivityType(activityType.name());
        log.setTargetId(targetId);
        log.setScore(activityType.getScore());
        activityLogMapper.insert(log);

        // 更新用户活跃度积分
        User user = userMapper.selectById(userId);
        if (user != null) {
            int newScore = (user.getActivityScore() != null ? user.getActivityScore() : 0) + activityType.getScore();
            user.setActivityScore(newScore);

            // 计算并更新用户等级
            int newLevel = UserLevelConstants.getLevelByScore(newScore);
            user.setUserLevel(newLevel);

            userMapper.updateById(user);
        }
    }

    /**
     * 获取用户当前活跃度和等级信息
     */
    public UserLevelInfo getUserLevelInfo(Long userId) {
        // 如果没有传userId，使用当前登录用户ID
        if (userId == null) {
            userId = CurrentUser.getId();
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return new UserLevelInfo(0, 1, 0, 50);
        }

        int score = user.getActivityScore() != null ? user.getActivityScore() : 0;
        int level = user.getUserLevel() != null ? user.getUserLevel() : 1;
        int progress = UserLevelConstants.getLevelProgress(score);
        int scoreToNext = UserLevelConstants.getScoreToNextLevel(score);

        return new UserLevelInfo(score, level, progress, scoreToNext);
    }

    /**
     * 检查用户是否可以发布发现模块帖子
     */
    public boolean canPostFeed(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return false;

        // 管理员可以发布
        if (Boolean.TRUE.equals(user.getIsAdmin())) {
            return true;
        }

        // 检查用户等级
        int userLevel = user.getUserLevel() != null ? user.getUserLevel() : 1;
        return userLevel >= UserLevelConstants.POST_FEED_MIN_LEVEL;
    }

    /**
     * 用户等级信息
     */
    public record UserLevelInfo(
        int score,          // 当前活跃度积分
        int level,          // 当前等级
        int progress,       // 当前等级进度百分比
        int scoreToNext     // 距离下一等级所需积分
    ) {}
}
