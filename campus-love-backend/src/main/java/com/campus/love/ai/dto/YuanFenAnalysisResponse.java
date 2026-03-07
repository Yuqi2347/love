package com.campus.love.ai.dto;

import lombok.Data;

import java.util.List;

/**
 * 缘分解析 AI 返回结果（响应 DTO）
 */
@Data
public class YuanFenAnalysisResponse {

    /** 缘分指数（创意短语） */
    private String yuanFenIndex;

    /** 总体评价（缘分气质） */
    private String overallInterpretation;

    /** 性格契合度分析（异性） */
    private String personalityAnalysis;

    /** 性格互动分析（同性，解析后映射到 personalityAnalysis） */
    private String personalityInteraction;

    /** 兴趣互动分析 */
    private String interestChemistry;

    /** 校园场景描写（异性） */
    private String campusStoryScene;

    /** 校园时刻（同性，解析后映射到 campusStoryScene） */
    private String campusMoment;

    /** 推荐共同活动 */
    private List<String> recommendActivities;

    /** 潜在相处挑战 */
    private String potentialChallenge;

    /** 发展可能性（异性） */
    private String developmentPotential;

    /** 关系潜力（同性，解析后映射到 developmentPotential） */
    private String relationshipPotential;

    /** 专属缘分金句 */
    private String exclusiveQuote;

    /** 生成时间 */
    private String generatedAt;

    /** 下次可用时间 */
    private String nextAvailableAt;
}
