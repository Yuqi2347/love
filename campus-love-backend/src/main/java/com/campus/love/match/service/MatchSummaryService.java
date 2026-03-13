package com.campus.love.match.service;

import com.campus.love.match.dto.MatchResultResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 匹配一句话总结（技术文档 V1.1.0 第 8 节）
 * 基于各维度分生成约30字总结，可后续接入 Haiku 生成
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchSummaryService {

    /**
     * 根据各维度得分生成一句话总结
     */
    public String generateOneLiner(MatchResultResponse.MatchDetail detail, String interestsA, String interestsB) {
        if (detail == null) return "你们各有特点，在相处中可以互相欣赏、互相成长。";

        int interest = detail.getInterestScore() != null ? detail.getInterestScore() : 50;
        int mbti = detail.getMbtiScore() != null ? detail.getMbtiScore() : 50;
        int zodiac = detail.getZodiacScore() != null ? detail.getZodiacScore() : 50;
        int major = detail.getMajorScore() != null ? detail.getMajorScore() : 50;

        if (interest >= 80) return "你们在兴趣上很有共鸣，可以从共同爱好聊起。";
        if (mbti >= 80) return "你们在性格上很有共鸣，相处起来会很舒服。";
        if (zodiac >= 80) return "星座缘分不错，不妨多了解一下对方。";
        if (major >= 70) return "专业上有交集，学业话题会很自然。";
        if (interest >= 70 && mbti >= 70) return "你们在兴趣和性格上很有共鸣，值得多相处。";
        return "你们各有特点，在相处中可以互相欣赏、互相成长。";
    }
}
