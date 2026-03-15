package com.campus.love.moment.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_moment_pair_score")
public class MomentPairScore {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String weekTag;
    private String pool;
    private Long userIdA;
    private Long userIdB;
    private BigDecimal score;
    private String scoreDetail;
    private Boolean hardFilterPassed;
    private String hardFilterReason;
    private Integer softPenalty;
    private String softPenaltyReason;
    private Integer thresholdOffsetA;
    private Integer thresholdOffsetB;
    private Integer effectiveThresholdA;
    private Integer effectiveThresholdB;
    private Integer thresholdRequired;
    private Boolean includedByThreshold;
    private Boolean matched;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
