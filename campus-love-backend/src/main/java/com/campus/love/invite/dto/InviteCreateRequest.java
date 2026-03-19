package com.campus.love.invite.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建邀约请求
 */
@Data
public class InviteCreateRequest {

    @NotBlank(message = "邀约模式不能为空")
    private String inviteMode;

    private Long targetUserId;

    @NotBlank(message = "邀约类型不能为空")
    private String inviteType;

    @NotBlank(message = "标题不能为空")
    private String title;

    private String content;

    @NotBlank(message = "邀约周期不能为空")
    private String invitePeriod;

    private String periodConfig;

    @NotNull(message = "邀约时间不能为空")
    private String inviteTime;

    private String inviteEndTime;

    private String location;
    /** 校区：ALL 表示不限 */
    private String campus;

    private Integer maxParticipants;

    private Integer deadlineHours;

    private String atmosphereTags;

    private Boolean isUrgent;
}
