package com.campus.love.profile.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_user_ai_profile")
public class UserAiProfile {
    @com.baomidou.mybatisplus.annotation.TableId
    private Long userId;
    private String interestTags;  // JSON
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
    private Boolean hasRealOcean;
    private String naturalLanguageTags;  // JSON
    private String loveAttachmentType;
    private String attractedToTraits;  // JSON
    private String frictionPoints;  // JSON
    private String userCorrectedFields;  // JSON
    private Integer profileVersion;
    private LocalDate lastLongUpdate;
    private LocalDate lastShortUpdate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
