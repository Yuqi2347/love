package com.campus.love.feed.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 动态流中展示的邀约卡片摘要（非邀约详情全量）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteFeedCard {

    private Long id;
    private String title;
    private String inviteType;
    private String status;
    private String inviteTime;
    private String inviteEndTime;
    private String location;
    private Integer participantCount;
    private Integer maxParticipants;
    private Long creatorId;
    private String creatorNickname;
    private String creatorAvatarUrl;
}
