package com.campus.love.invite.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 等待邀约表实体
 */
@Data
@TableName("t_invite_wait")
public class InviteWait {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String inviteTypes;

    private String periodConfig;

    private String locationPref;

    private Boolean autoAccept;

    private Integer expireHours;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
