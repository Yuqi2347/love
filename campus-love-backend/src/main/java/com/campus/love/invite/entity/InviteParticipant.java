package com.campus.love.invite.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 邀约参与者表实体
 */
@Data
@TableName("t_invite_participant")
public class InviteParticipant {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long inviteId;

    private Long userId;

    private BigDecimal socialRating;

    private LocalDateTime joinAt;
}
