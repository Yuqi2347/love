package com.campus.love.moment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 心动一刻报名请求 — 对齐文档 32 题（3 步骤）
 */
@Data
public class MomentEnrollRequest {

    @NotNull(message = "自评颜值分不能为空")
    @Min(value = 1, message = "自评颜值分最低1分")
    @Max(value = 10, message = "自评颜值分最高10分")
    private Integer selfScore;

    @NotBlank(message = "期望匹配性别不能为空")
    private String targetGender; // male / female / any

    // 第一步：关于你自己
    @NotBlank private String socialStyle;           // 1.2
    @NotBlank private String lifeRhythm;             // 1.3
    @NotBlank private String personalityBase;        // 1.4
    @NotBlank private String campusFocus;            // 1.5
    @NotBlank private String emotionStyle;            // 1.6

    // 第二步：关于期待的 TA
    @NotBlank private String appearanceRequirement;  // 2.2
    private String ageRangePreference;               // 2.3 可多选 A,B,C,D
    private Integer agePreferenceMin;
    private Integer agePreferenceMax;
    @NotBlank private String gradeRangePreference;    // 2.4
    private Integer gradeRangeMin;
    private Integer gradeRangeMax;
    @NotBlank private String partnerPersonality;      // 2.5
    @NotBlank private String majorPreference;         // 2.6
    @NotBlank private String careerAmbitionPref;      // 2.7
    @NotBlank private String companionshipStyle;     // 2.8
    @NotBlank private String dateStyle;               // 2.9
    @NotBlank private String intimacyPace;           // 2.10

    // 第三步：关于价值观
    @NotBlank private String honestyLevel;            // 3.1 坦诚度
    @NotBlank private String premaritalCohabitation;  // 3.2
    @NotBlank private String premaritalSex;           // 3.3 婚前性行为（硬筛选）
    @NotBlank private String relationshipCoreValue;  // 3.4
    @NotBlank private String conflictStyle;           // 3.5
    @NotBlank private String socialBoundary;          // 3.6
    @NotBlank private String futureLifestyle;         // 3.7
    @NotBlank private String campusLovePlan;         // 3.8
    @NotBlank private String idolRole;                // 3.9
    @NotBlank private String temptationResponse;    // 3.10
    @NotBlank private String realityCondition;       // 3.11
    @NotBlank private String humanNatureView;        // 3.12
    @NotBlank private String breakupView;             // 3.13
    @NotBlank private String careerLoveConflict;     // 3.14
    @NotBlank private String emotionPriority;        // 3.15
    @NotBlank private String lifeGoalPriority;       // 3.16
}
