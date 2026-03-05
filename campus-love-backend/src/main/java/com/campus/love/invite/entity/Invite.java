package com.campus.love.invite.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 邀约主表实体
 */
@Data
@TableName("t_invite")
public class Invite {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long creatorId;

    private String inviteType;

    private String inviteMode;

    private Long targetUserId;

    private String title;

    private String content;

    private String invitePeriod;

    private String periodConfig;

    private LocalDateTime inviteTime;

    private String location;

    private Integer maxParticipants;

    private Integer participantCount;

    /**
     * 关联的聊天群ID（公开邀约临时群聊）
     */
    private Long chatGroupId;

    private String status;

    private Integer deadlineHours;

    private String atmosphereTags;

    private Boolean isUrgent;

    private BigDecimal socialRating;

    private BigDecimal orgRating;

    private Integer ratingCount;

    private Boolean deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
