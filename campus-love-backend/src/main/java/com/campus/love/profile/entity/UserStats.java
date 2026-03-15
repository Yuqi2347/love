package com.campus.love.profile.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户统计（从 t_user 迁出）
 */
@Data
@TableName("t_user_stats")
public class UserStats {
    @TableId
    private Long userId;
    private Integer activityScore;
    private Integer userLevel;
    private Integer inviteCount;
    private Integer participateCount;
    private Integer creditScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
