package com.campus.love.invite.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 邀约评价表实体
 */
@Data
@TableName("t_invite_rating")
public class InviteRating {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long inviteId;

    private Long raterId;

    private Long ratedUserId;

    private BigDecimal socialRating;

    private BigDecimal orgRating;

    private String content;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
