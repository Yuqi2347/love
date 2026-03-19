package com.campus.love.moment.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_moment_activity_week")
public class MomentActivityWeek {

    public static final String STATUS_ENROLLING = "ENROLLING";
    public static final String STATUS_WAITING_MATCH = "WAITING_MATCH";
    public static final String STATUS_RESULT_READY = "RESULT_READY";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String weekTag;
    private String status;
    private Boolean enrollmentOpen;
    private LocalDateTime autoMatchAt;
    private LocalDateTime closedAt;
    private LocalDateTime matchedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
