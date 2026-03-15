package com.campus.love.moment.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_moment_match_confirm")
public class MomentMatchConfirm {

    public static final String CHOICE_YUE = "YUE";
    public static final String CHOICE_GUANZHU = "GUANZHU";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long matchResultId;
    private Long userIdA;
    private Long userIdB;
    private String choiceA;
    private String choiceB;
    private LocalDateTime choiceAAt;
    private LocalDateTime choiceBAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
