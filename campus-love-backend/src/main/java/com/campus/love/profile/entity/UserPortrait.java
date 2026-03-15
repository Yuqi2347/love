package com.campus.love.profile.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户画像（基础+问卷+OCEAN）
 * interest_tags 格式: {"body_space":[{"code":"tag_fitness","sharing":0.5,"intensity":0.5}]}
 * sharing/intensity 默认 0.5，不存 null
 */
@Data
@TableName("t_user_portrait")
public class UserPortrait {
    @TableId
    private Long userId;

    private String mbti;
    private String zodiac;
    private String bazi;
    private String bio;
    private String interestTags;  // JSON

    private String targetGender;
    private String socialStyle;
    private String lifeRhythm;
    private String companionshipStyle;
    private String appearanceRequirement;
    private String partnerPersonality;
    private String majorPreference;
    private String ageRangePreference;
    private Integer agePreferenceMin;
    private Integer agePreferenceMax;
    private Integer gradeRangeMin;
    private Integer gradeRangeMax;
    private String gradeRangePreference;
    private Boolean prioritizeMatching;
    private String dateStyle;
    private String intimacyPace;
    private String loyaltyValue;
    private String premaritalCohabitation;
    private String futureLifestyle;
    private String relationshipCoreValue;
    private BigDecimal appearanceScore;
    private String personalityBase;
    private String campusFocus;
    private String emotionStyle;
    private String careerAmbitionPref;
    private String honestyLevel;
    private String premaritalSex;
    private String conflictStyle;
    private String socialBoundary;
    private String campusLovePlan;
    private String idolRole;
    private String temptationResponse;
    private String realityCondition;
    private String humanNatureView;
    private String breakupView;
    private String careerLoveConflict;
    private String emotionPriority;
    private String lifeGoalPriority;
    private String questionnaireSnapshot;  // JSON
    private Integer questionnaireVersion;

    private BigDecimal oceanOLong;
    private BigDecimal oceanCLong;
    private BigDecimal oceanELong;
    private BigDecimal oceanALong;
    private BigDecimal oceanNLong;
    private BigDecimal oceanOShort;
    private BigDecimal oceanCShort;
    private BigDecimal oceanEShort;
    private BigDecimal oceanAShort;
    private BigDecimal oceanNShort;
    private String oceanConfidence;  // JSON
    private Boolean hasRealOcean;
    private String naturalLanguageTags;
    private String loveAttachmentType;
    private String attractedToTraits;
    private String frictionPoints;
    private Integer profileVersion;
    private LocalDate lastLongUpdate;
    private LocalDate lastShortUpdate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
