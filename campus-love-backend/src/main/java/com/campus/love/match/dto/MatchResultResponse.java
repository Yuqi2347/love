package com.campus.love.match.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MatchResultResponse {

    private Long userId;
    private String nickname;
    private String avatarUrl;
    private Integer gender;
    private String school;
    private String major;
    private String grade;
    private String mbti;
    private String zodiac;
    private String bio;
    private String interests;
    private Integer matchScore;
    /** AI 一句话总结（约30字），技术文档 V1.1.0 第 8 节 */
    private String aiSummary;
    private MatchDetail detail;

    @Data
    @Builder
    public static class MatchDetail {
        private Integer oceanScore;
        private Integer interestScore;
        private Integer valuesScore;
        private Integer ageGradeScore;
        private Integer zodiacScore;
        private Integer majorScore;
    }
}
