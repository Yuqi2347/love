package com.campus.love.feed.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FeedPostRequest {

    @NotBlank(message = "动态内容不能为空")
    private String content;

    private String images;

    /**
     * 帖子类型：TIMELINE(朋友圈) / DISCOVERY(发现模块)
     * 默认为 TIMELINE
     */
    private String postType;
}
