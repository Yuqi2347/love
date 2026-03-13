package com.campus.love.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_ice_break_reminder_log")
public class IceBreakReminderLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    @com.baomidou.mybatisplus.annotation.TableField("from_user_id")
    private Long fromUserId;
    @com.baomidou.mybatisplus.annotation.TableField("to_user_id")
    private Long toUserId;
    private String reminderType;
    private LocalDateTime createdAt;
}
