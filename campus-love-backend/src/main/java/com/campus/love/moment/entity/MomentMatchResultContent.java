package com.campus.love.moment.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_moment_match_result_content")
public class MomentMatchResultContent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long matchResultId;
    private String scoreDetail;
    private String aiAnalysis;
    private String yuanfenTitle;
    private String complementaryModes;
    private String softPenaltyReasons;
    private String dateSceneType;
    @TableField("insight_card_1")
    private String insightCard1;
    @TableField("insight_card_2")
    private String insightCard2;
    @TableField("insight_card_3")
    private String insightCard3;
    private String goldenSentence;
    private String dimensionLabels;
    private String aboutUserA;
    private String aboutUserB;
    private String datePrepJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
