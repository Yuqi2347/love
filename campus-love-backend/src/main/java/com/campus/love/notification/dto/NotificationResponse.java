package com.campus.love.notification.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知响应 DTO
 */
@Data
@Builder
public class NotificationResponse {

    private Long id;

    private Long userId;

    private Long senderId;

    private Long inviteId;

    private String type;

    private String title;

    private String content;

    private Boolean isRead;

    private LocalDateTime createdAt;

    private LocalDateTime readAt;
}

