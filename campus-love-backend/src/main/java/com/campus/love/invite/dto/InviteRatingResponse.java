package com.campus.love.invite.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评价响应
 */
@Data
@Builder
public class InviteRatingResponse {

    private Long id;
    private Long inviteId;
    private Long raterId;
    private String raterNickname;
    private Long ratedUserId;
    private String ratedNickname;
    private BigDecimal socialRating;
    private BigDecimal orgRating;
    private String content;
    private LocalDateTime createdAt;
}
