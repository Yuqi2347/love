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
}
