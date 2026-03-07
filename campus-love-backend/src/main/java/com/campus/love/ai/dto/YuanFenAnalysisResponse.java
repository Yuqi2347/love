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

    /** 性格契合度分析 */
    private String personalityAnalysis;

    /** 推荐共同活动 */
    private List<String> recommendActivities;

    /** 潜在相处挑战 */
    private String potentialChallenge;

    /** 发展可能性 */
    private String developmentPotential;

    /** 专属缘分金句 */
    private String exclusiveQuote;

    /** 生成时间 */
    private String generatedAt;

    /** 下次可用时间 */
    private String nextAvailableAt;
}
