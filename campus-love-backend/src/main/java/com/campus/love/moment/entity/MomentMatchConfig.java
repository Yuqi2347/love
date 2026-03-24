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
    public static final int DEFAULT_BASE_THRESHOLD = 70;
    public static final int DEFAULT_PRIORITIZE_OFFSET = 10;
    public static final int DEFAULT_PRIORITY_OFFSET = 5;
    public static final int DEFAULT_PRIORITY_MAX_STACK = 2;
    public static final int MIN_EFFECTIVE_THRESHOLD = 60;
    public static final int DEFAULT_ELIGIBLE_TOP_K = 400;
    public static final boolean DEFAULT_AUTO_MATCH_ENABLED = false;
    public static final int DEFAULT_AUTO_MATCH_DAY_OF_WEEK = 1; // Monday
    public static final String DEFAULT_AUTO_MATCH_TIME = "16:00";
    public static final boolean DEFAULT_AUTO_PUBLISH_ENABLED = false;
    public static final int DEFAULT_AUTO_PUBLISH_DAY_OF_WEEK = 5; // Friday
    public static final String DEFAULT_AUTO_PUBLISH_TIME = "12:00";

    @TableId(type = IdType.INPUT)
    private Long id;

    private Integer baseThreshold;
    private Integer prioritizeOffset;
    private Integer priorityOffset;
    private Integer priorityMaxStack;

    /** 每人进入最大权匹配图的 eligible 边数上限（Top-K），默认 400 */
    private Integer eligibleTopK;

    private Boolean autoMatchEnabled;
    private Integer autoMatchDayOfWeek;
    private String autoMatchTime;

    /** 是否在「RESULT_READY」后按下列时间自动公布（用户可见缘分结果） */
    private Boolean autoPublishEnabled;
    /** 1=周一 … 7=周日，与 auto_match 一致；时刻为北京时间 */
    private Integer autoPublishDayOfWeek;
    private String autoPublishTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
