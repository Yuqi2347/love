package com.campus.love.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端邀约列表项
 */
@Data
@Builder
public class AdminInviteItem {

    private Long id;
    private Long creatorId;
    private String creatorNickname;
    private String inviteType;
    private String inviteMode;
    private String title;
    private String status;
    private LocalDateTime inviteTime;
    private Integer participantCount;
    private Integer maxParticipants;
    private LocalDateTime createdAt;
}
