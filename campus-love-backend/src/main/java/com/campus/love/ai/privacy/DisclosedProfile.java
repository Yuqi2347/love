package com.campus.love.ai.privacy;

import lombok.Data;

import java.util.List;

/**
 * AI 可公开的人物画像（经 ProfileDisclosureFilter 过滤后）
 * 技术文档 V1.1.0 第 5.3 节
 */
@Data
public class DisclosedProfile {
    private String mbti;
    private String zodiac;
    private String majorCategory;
    private List<String> interestTags;
    private List<String> naturalLangTags;
    private String baziSummary;
    private String questionnaireHints;
}
