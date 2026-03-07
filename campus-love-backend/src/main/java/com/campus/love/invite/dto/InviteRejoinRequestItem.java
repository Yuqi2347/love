package com.campus.love.invite.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 待处理的「再次加入」申请项（发起人视角）
 */
@Data
@Builder
public class InviteRejoinRequestItem {

    private Long userId;
    private String nickname;
    private String avatarUrl;
    private LocalDateTime requestedAt;
}
