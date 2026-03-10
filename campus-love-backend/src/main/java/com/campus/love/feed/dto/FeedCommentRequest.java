package com.campus.love.feed.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FeedCommentRequest {

    @NotNull(message = "动态ID不能为空")
    private Long postId;

    @NotBlank(message = "评论内容不能为空")
    private String content;

    /** 父评论ID（用于回复评论，null表示直接评论动态） */
    private Long parentId;

    /** 被回复的用户ID（用于显示"回复 @用户名"） */
    private Long repliedUserId;
}
