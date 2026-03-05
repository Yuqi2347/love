package com.campus.love.invite.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 邀约响应
 */
@Data
@Builder
public class InviteResponse {

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
    private String status;
    private Integer deadlineHours;
    private String atmosphereTags;
    private Boolean isUrgent;
    private BigDecimal socialRating;
    private BigDecimal orgRating;
    private Integer ratingCount;
    private LocalDateTime createdAt;

    // 发起人信息
    private CreatorInfo creator;

    // 参与者列表
    private List<ParticipantInfo> participants;

    @Data
    @Builder
    public static class CreatorInfo {
        private Long id;
        private String nickname;
        private String avatarUrl;
        private Integer creditScore;
    }

    @Data
    @Builder
    public static class ParticipantInfo {
        private Long userId;
        private String nickname;
        private String avatarUrl;
        private LocalDateTime joinAt;
    }
}
