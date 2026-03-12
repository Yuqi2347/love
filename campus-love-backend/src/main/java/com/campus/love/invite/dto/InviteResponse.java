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
    private LocalDateTime inviteEndTime;
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

    /** 公开邀约的临时群聊 ID，有则前端可跳转群聊 */
    private Long chatGroupId;

    /** 当前用户在该邀约中的角色：CREATOR 发起 / PARTICIPANT 参与中 / LEFT 已退出 */
    private String myRole;
    /** 当前用户退出时间（仅当 myRole=LEFT 时有值） */
    private LocalDateTime myLeftAt;
    /** 退出/踢人理由（仅当 myRole=LEFT 且被踢时有值，被踢人可见） */
    private String myLeaveReason;

    // 发起人信息
    private CreatorInfo creator;

    /** 1v1 专属邀约的被邀人信息（仅 inviteMode=PRIVATE 且 targetUserId 有值时） */
    private CreatorInfo targetUser;

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
