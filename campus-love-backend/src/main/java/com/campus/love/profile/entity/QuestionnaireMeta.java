package com.campus.love.profile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问卷元数据（值域权威定义）
 * questions JSON: [{"field":"social_style","title":"...","options":{"A":"...","B":"..."}}]
 */
@Data
@TableName("t_questionnaire_meta")
public class QuestionnaireMeta {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer version;
    private String questions;  // JSON
    private LocalDateTime createdAt;
}
