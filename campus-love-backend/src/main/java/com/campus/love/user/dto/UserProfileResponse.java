package com.campus.love.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileResponse {

    private Long id;
    private String email;
    private String nickname;
    private Integer gender;
    private String birthDate;
    private String birthTime;
    private String school;
    private String major;
    private String grade;
    private Integer activityScore;
    private Integer userLevel;
    private Boolean isAdmin;
    private String mbti;
    private String zodiac;
    private String bazi;
    private String avatarUrl;
    private String bio;
    private String interests;
    private Boolean profileComplete;
}
