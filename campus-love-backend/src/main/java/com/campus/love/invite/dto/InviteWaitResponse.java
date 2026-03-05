package com.campus.love.invite.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 等待邀约响应
 */
@Data
@Builder
public class InviteWaitResponse {

    private Long id;
    private String inviteTypes;
    private String periodConfig;
    private String locationPref;
    private Boolean autoAccept;
    private Integer expireHours;
    private LocalDateTime createdAt;

    // 是否已过期
    private Boolean isExpired;

    // 过期时间
    private LocalDateTime expireTime;
}
