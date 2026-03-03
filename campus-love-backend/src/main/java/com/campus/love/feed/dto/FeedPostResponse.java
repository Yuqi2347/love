package com.campus.love.feed.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FeedPostResponse {

    private Long id;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String content;
    private String images;
    private Integer likeCount;
    private Integer commentCount;
    private Boolean liked;
    private String createdAt;
    private List<CommentItem> comments;

    @Data
    @Builder
    public static class CommentItem {
        private Long id;
        private Long userId;
        private String nickname;
        private String avatarUrl;
        private String content;
        private Long parentId;
        private String createdAt;
    }
}
