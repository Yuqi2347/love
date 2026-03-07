package com.campus.love.moment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_moment_match_result")
public class MomentMatchResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String weekTag;
    private String pool;
    private Long userIdA;
    private Long userIdB;
    private BigDecimal totalScore;
    private String scoreDetail;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
