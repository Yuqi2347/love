package com.campus.love.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端帖子列表项
 */
@Data
@Builder
public class AdminFeedItem {

    private Long id;
    private Long userId;
    private String nickname;
    private String content;
    private String postType;
    private Integer likeCount;
    private Integer commentCount;
    private LocalDateTime createdAt;
}
