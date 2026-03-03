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
    private MatchDetail detail;

    @Data
    @Builder
    public static class MatchDetail {
        private Integer interestScore;
        private Integer mbtiScore;
        private Integer zodiacScore;
        private Integer baziScore;
        private Integer majorScore;
        private Integer ageScore;
    }
}
