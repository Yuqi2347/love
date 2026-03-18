package com.campus.love.moment.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_moment_match_config")
public class MomentMatchConfig {

    public static final long DEFAULT_ID = 1L;
    public static final int DEFAULT_BASE_THRESHOLD = 60;
    public static final int DEFAULT_PRIORITIZE_OFFSET = 10;
    public static final int DEFAULT_PRIORITY_OFFSET = 5;
    public static final int DEFAULT_PRIORITY_MAX_STACK = 2;
    public static final boolean DEFAULT_AUTO_MATCH_ENABLED = false;
    public static final int DEFAULT_AUTO_MATCH_DAY_OF_WEEK = 1; // Monday
    public static final String DEFAULT_AUTO_MATCH_TIME = "16:00";

    @TableId(type = IdType.INPUT)
    private Long id;

    private Integer baseThreshold;
    private Integer prioritizeOffset;
    private Integer priorityOffset;
    private Integer priorityMaxStack;

    private Boolean autoMatchEnabled;
    private Integer autoMatchDayOfWeek;
    private String autoMatchTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
