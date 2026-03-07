package com.campus.love.invite.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 邀约拒绝记录（一对一邀约被目标用户拒绝）
 */
@Data
@TableName("t_invite_decline")
public class InviteDecline {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long inviteId;

    private Long userId;

    private LocalDateTime createdAt;
}
