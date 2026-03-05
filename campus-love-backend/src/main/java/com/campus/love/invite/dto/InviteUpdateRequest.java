package com.campus.love.invite.dto;

import lombok.Data;

/**
 * 更新邀约请求
 */
@Data
public class InviteUpdateRequest {

    private String title;

    private String content;

    private String periodConfig;

    private String inviteTime;

    private String location;

    private Integer maxParticipants;

    private Integer deadlineHours;

    private String atmosphereTags;

    private Boolean isUrgent;
}
