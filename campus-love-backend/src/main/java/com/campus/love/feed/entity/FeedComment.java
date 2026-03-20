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
    /** 评论图片URL，逗号分隔 */
    private String images;
    private Long parentId;

    /** 被回复的用户ID（用于显示"回复 @用户名"） */
    private Long repliedUserId;

    /** 软删除：0 正常，1 已删除（业务手动设置，不用 @TableLogic） */
    private Integer deleted;

    /** V39：点赞数 */
    private Integer likeCount;

    public static final int DELETED = 1;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
