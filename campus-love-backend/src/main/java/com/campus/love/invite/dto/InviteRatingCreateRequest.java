package com.campus.love.invite.dto;

import lombok.Data;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * 创建评价请求
 */
@Data
public class InviteRatingCreateRequest {

    @NotNull(message = "邀约ID不能为空")
    private Long inviteId;

    @NotNull(message = "被评价人ID不能为空")
    private Long ratedUserId;

    @NotNull(message = "社交体验评分不能为空")
    @DecimalMin(value = "0.0", message = "评分不能小于0")
    @DecimalMax(value = "5.0", message = "评分不能大于5")
    private Double socialRating;

    @DecimalMin(value = "0.0", message = "评分不能小于0")
    @DecimalMax(value = "5.0", message = "评分不能大于5")
    private Double orgRating;

    private String content;
}
