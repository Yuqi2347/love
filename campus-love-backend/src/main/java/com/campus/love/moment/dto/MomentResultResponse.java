package com.campus.love.moment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 与 {@link lombok.Builder} 同时使用时建议显式无参/全参构造，避免增量编译或热部署时
 * 出现「已编译的调用方」与「旧的 .class」不一致导致的 NoSuchMethodError。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MomentResultResponse {
    private boolean matched;
    private String weekTag;
    private String yuanfenTitle;

    /** t_moment_match_result.id，用于「约一下」接口 */
    private Long matchResultId;

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

    private List<String> complementaryModes;
    private List<String> insightCards;
    private String goldenSentence;
    private List<String> dimensionLabels;
    private String aboutMatchedUser;
    private String confirmStatus;
    private String myChoice;
    private Boolean datePrepUnlocked;

    /** 与对方的综合匹配度（0–100，来自匹配算法 finalScore） */
    private Integer matchScorePercent;
}
