package com.campus.love.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 破冰功能按好友授权（V26）
 * user_id: 我，allowed_user_id: 我允许使用破冰的好友
 */
@Data
@TableName("t_user_ice_break_allow")
public class UserIceBreakAllow {

    private Long userId;
    private Long allowedUserId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
