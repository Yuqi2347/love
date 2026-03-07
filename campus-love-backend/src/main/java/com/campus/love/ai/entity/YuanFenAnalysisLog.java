package com.campus.love.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_yuanfen_analysis_log")
public class YuanFenAnalysisLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 较小的用户ID（保证对称性） */
    private Long userIdA;

    /** 较大的用户ID */
    private Long userIdB;

    /** 触发时的综合匹配分 */
    private Integer totalScore;

    /** AI 返回的 JSON 结果 */
    private String aiResult;

    /** Token 消耗量 */
    private Integer tokensUsed;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
