package com.campus.love.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long senderId;
    private Long receiverId;
    /**
     * 群聊ID（单聊消息为空）
     */
    private Long groupId;
    private String content;
    private Integer msgType;
    private Boolean isRead;

    /** 软删除：0 正常，1 已撤回（业务手动设置，不用 @TableLogic） */
    private Integer deleted;

    public static final int DELETED = 1;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
