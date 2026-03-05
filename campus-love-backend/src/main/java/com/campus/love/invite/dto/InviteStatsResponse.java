package com.campus.love.invite.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 邀约统计响应
 */
@Data
@Builder
public class InviteStatsResponse {

    // 发起的邀约数
    private Integer inviteCount;

    // 参与的邀约数
    private Integer participateCount;

    // 成功率（已完成/已发起）
    private BigDecimal successRate;

    // 平均社交体验评分
    private BigDecimal avgSocialRating;

    // 平均组织力评分
    private BigDecimal avgOrgRating;

    // 获得的社交体验评分
    private BigDecimal receivedSocialRating;

    // 获得的组织力评分
    private BigDecimal receivedOrgRating;
}
