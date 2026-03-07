package com.campus.love.moment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_moment_profile")
public class MomentProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String targetGender;

    // Q3-Q15
    private String socialStyle;
    private String lifeRhythm;
    private String companionshipStyle;
    private String appearanceRequirement;
    private String partnerPersonality;
    private String majorPreference;
    private String ageRangePreference;
    private String dateStyle;
    private String intimacyPace;
    private String loyaltyValue;
    private String premaritalCohabitation;
    private String futureLifestyle;
    private String relationshipCoreValue;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // 非数据库字段，仅用于API返回
    @TableField(exist = false)
    private String momentPhotoUrl;
    @TableField(exist = false)
    private Integer momentSelfScore;
}
