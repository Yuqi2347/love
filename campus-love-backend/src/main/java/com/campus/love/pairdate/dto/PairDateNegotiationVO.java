package com.campus.love.pairdate.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PairDateNegotiationVO {

    private Long id;
    private Long matchResultId;
    private String status;
    private String weekTag;

    private JsonNode dateOptions;

    private boolean iAmUserA;

    private Integer myExcludedRank;
    private List<String> myTimeSlots;
    private String myLocationChoice;

    private boolean partnerFinishedAll;
    private Boolean revealPartnerChoices;

    private Integer partnerExcludedRank;
    private List<String> partnerTimeSlots;
    private String partnerLocationChoice;

    private JsonNode finalDateOption;
    private String meetingTimeSlot;
    private Long meetingTimestamp;
    private Long locationDeciderId;
    private String deciderReasonKey;

    private Boolean timeMismatch;

    /** 协商完成后的一对一邀约，跳转 /invite/{id} */
    private Long pairInviteId;

    /** 卡片用：地点决定者=发起人 */
    private String initiatorNickname;
    private String guestNickname;
    private Long guestUserId;
    private String initiatorAvatarUrl;
    private String guestAvatarUrl;
}
