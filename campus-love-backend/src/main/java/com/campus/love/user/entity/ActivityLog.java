package com.campus.love.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_activity_log")
public class ActivityLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String activityType;
    private Long targetId;
    private Integer score;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
