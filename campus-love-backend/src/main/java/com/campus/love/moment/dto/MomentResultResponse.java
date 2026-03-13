package com.campus.love.moment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class MomentResultResponse {
    private boolean matched;
    private String weekTag;

    // 匹配对象信息（matched=true时有值）
    private Long matchedUserId;
    private String nickname;
    private String avatarUrl;
    private Integer gender;
    private String school;
    private String major;
    private String grade;
    private String bio;
    private String mbti;
    private String zodiac;
    private Integer age;

    // 分数
    private BigDecimal totalScore;
    private Map<String, Object> scoreDetail;

    /** AI 配对总结（借鉴缘分分析格式，多段落深度内容） */
    private String summary;
}
