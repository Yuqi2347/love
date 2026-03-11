package com.campus.love.report.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_report")
public class Report {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long reporterId;
    private String targetType;
    private Long targetId;
    private String reason;
    private String status;
    private String adminNote;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_REVIEWED = "REVIEWED";
    public static final String STATUS_RESOLVED = "RESOLVED";

    public static final String TARGET_POST = "POST";
    public static final String TARGET_COMMENT = "COMMENT";
    public static final String TARGET_USER = "USER";
    public static final String TARGET_MESSAGE = "MESSAGE";
}
