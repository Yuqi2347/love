package com.campus.love.moment.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MomentResultResponse {
    private boolean matched;
    private String weekTag;
    private String yuanfenTitle;

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
}
