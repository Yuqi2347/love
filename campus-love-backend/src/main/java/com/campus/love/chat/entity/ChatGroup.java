package com.campus.love.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天群实体（用于公开邀约临时群聊）
 */
@Data
@TableName("t_chat_group")
public class ChatGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long inviteId;

    private String name;

    private Long createdBy;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

