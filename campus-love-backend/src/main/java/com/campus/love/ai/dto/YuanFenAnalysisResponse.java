package com.campus.love.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 缘分解析 AI 返回结果（响应 DTO）
 */
@Data
public class YuanFenAnalysisResponse {

    /** 缘分指数（创意短语） */
    private String yuanFenIndex;

    /** 总体评价（缘分气质），新模板为 dataBackedOverview */
    private String overallInterpretation;

    /** 性格/认知分析，新模板为 cognitiveArchitecture */
    private String personalityAnalysis;

    /** 性格互动分析（同性，解析后映射到 personalityAnalysis） */
    private String personalityInteraction;

    /** 兴趣互动分析，新模板为 interestResonance */
    private String interestChemistry;

    /** 校园场景描写，新模板为 cinematicScene */
    private String campusStoryScene;

    /** 校园时刻（同性，解析后映射到 campusStoryScene） */
    private String campusMoment;

    /** 推荐共同活动（新模板无此字段，可留空） */
    private List<String> recommendActivities;

    /** 潜在相处挑战，新模板为 frictionAndEvolution */
    private String potentialChallenge;

    /** 发展可能性（异性） */
    private String developmentPotential;

    /** 关系潜力（同性，解析后映射到 developmentPotential） */
    private String relationshipPotential;

    /** 专属缘分金句 */
    private String exclusiveQuote;

    /** 新模板字段：数据支撑的综合判词 */
    @JsonProperty("dataBackedOverview")
    private String dataBackedOverview;

    /** 新模板字段：认知架构分析 */
    @JsonProperty("cognitiveArchitecture")
    private String cognitiveArchitecture;

    /** 新模板字段：兴趣共振分析 */
    @JsonProperty("interestResonance")
    private String interestResonance;

    /** 新模板字段：电影级场景预演 */
    @JsonProperty("cinematicScene")
    private String cinematicScene;

    /** 新模板字段：摩擦与进化 */
    @JsonProperty("frictionAndEvolution")
    private String frictionAndEvolution;

    /** 生成时间 */
    private String generatedAt;

    /** 下次可用时间 */
    private String nextAvailableAt;
}
