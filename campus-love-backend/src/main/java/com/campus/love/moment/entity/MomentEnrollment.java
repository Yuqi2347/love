package com.campus.love.moment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_moment_enrollment")
public class MomentEnrollment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String weekTag;
    private String pool;
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public static final String STATUS_WAITING = "WAITING";
    public static final String STATUS_MATCHED = "MATCHED";
    public static final String STATUS_UNMATCHED = "UNMATCHED";
}
