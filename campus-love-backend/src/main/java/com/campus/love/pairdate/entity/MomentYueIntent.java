package com.campus.love.pairdate.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_moment_yue_intent")
public class MomentYueIntent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long matchResultId;
    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
