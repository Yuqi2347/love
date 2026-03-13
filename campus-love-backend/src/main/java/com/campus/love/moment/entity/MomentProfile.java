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

    // 第一步：关于你自己
    private java.math.BigDecimal appearanceScore;   // 1.1 颜值自评
    private String socialStyle;                     // 1.2 社交风格
    private String lifeRhythm;                      // 1.3 生活节奏
    private String personalityBase;                // 1.4 性格底色
    private String campusFocus;                     // 1.5 校园生活重心
    private String emotionStyle;                    // 1.6 情绪表达

    // 第二步：关于期待的 TA
    private String appearanceRequirement;           // 2.2 颜值要求
    private String ageRangePreference;              // 2.3 年龄范围
    private Integer agePreferenceMin;
    private Integer agePreferenceMax;
    private String gradeRangePreference;            // 2.4 年级范围
    private Integer gradeRangeMin;
    private Integer gradeRangeMax;
    private String partnerPersonality;               // 2.5 性格偏好
    private String majorPreference;                  // 2.6 专业偏好
    private String careerAmbitionPref;               // 2.7 事业心偏好
    private String companionshipStyle;             // 2.8 陪伴方式
    private String dateStyle;                        // 2.9 约会方式
    private String intimacyPace;                     // 2.10 亲密节奏

    // 第三步：关于价值观
    private String honestyLevel;                     // 3.1 坦诚度
    private String premaritalCohabitation;           // 3.2 婚前同居
    private String premaritalSex;                    // 3.3 婚前性行为（硬筛选）
    private String relationshipCoreValue;          // 3.4 核心价值
    private String conflictStyle;                    // 3.5 矛盾解决
    private String socialBoundary;                   // 3.6 社交边界
    private String futureLifestyle;                  // 3.7 未来生活方式
    private String campusLovePlan;                  // 3.8 校园恋爱规划
    private String idolRole;                         // 3.9 偶像角色认知
    private String temptationResponse;              // 3.10 面对诱惑
    private String realityCondition;                // 3.11 现实条件
    private String humanNatureView;                 // 3.12 人性观
    private String breakupView;                      // 3.13 分手观
    private String careerLoveConflict;              // 3.14 事业爱情冲突
    private String emotionPriority;                 // 3.15 情感排序
    private String lifeGoalPriority;                // 3.16 人生目标优先级

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
