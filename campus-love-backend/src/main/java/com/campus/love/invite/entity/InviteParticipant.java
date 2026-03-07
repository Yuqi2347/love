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

    /** 退出时间，非空表示已退出该邀约 */
    private LocalDateTime leftAt;

    /** 退出理由（被踢时由发起人填写，被踢人可见） */
    private String leftReason;
}
