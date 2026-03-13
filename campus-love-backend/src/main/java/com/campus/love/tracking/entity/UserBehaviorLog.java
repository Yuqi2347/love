package com.campus.love.tracking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_user_behavior_log")
public class UserBehaviorLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String behaviorType;
    private Long targetId;
    private String metadata;  // JSON
    private LocalDateTime createdAt;
}
