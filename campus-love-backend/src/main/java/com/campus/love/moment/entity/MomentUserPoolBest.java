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
@TableName("t_moment_user_pool_best")
public class MomentUserPoolBest {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String weekTag;
    private String pool;
    private Long userId;
    private BigDecimal maxEligibleScore;
    private Boolean hasAnyEligible;
    private Boolean tier2Truncated;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
