package com.campus.love.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_ai_usage_log")
public class AiUsageLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 大类：AVATAR / ANALYSIS */
    private String bizType;

    /** 具体场景：YUANFEN / ICE_BREAK / AVATAR_STUDIO 等 */
    private String scene;

    /** 调用通道：TEXT_CHAT / IMAGE_GENERATION */
    private String provider;

    private String modelName;

    private Long userId;

    private String bizKey;

    private Integer tokensUsed;

    private Integer callCount;

    private String sourceTable;

    private Long sourceId;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
