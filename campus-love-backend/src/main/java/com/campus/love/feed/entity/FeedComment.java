package com.campus.love.feed.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_feed_comment")
public class FeedComment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;
    private Long userId;
    private String content;
    private Long parentId;

    /** 被回复的用户ID（用于显示"回复 @用户名"） */
    private Long repliedUserId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
