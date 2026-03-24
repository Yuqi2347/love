package com.campus.love.moment.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_moment_reject_summary")
public class MomentRejectSummary {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String weekTag;
    private String pool;
    private Integer hardFilterCount;
    private String hardFilterReasonDist;
    private Integer belowThresholdCount;
    private String scoreDistribution;
    private String softPenaltyReasonDist;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
