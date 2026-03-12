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
    private String videos;
    private String linkUrl;
    private String linkTitle;
    private String linkImage;
    private Integer likeCount;
    private Integer commentCount;
    private Boolean liked;
    private String visibility;
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
        private String images;
        private Long parentId;
        private String repliedToName;  // 被回复的用户昵称
        private String createdAt;
        private Boolean deleted;  // 是否已删除
    }
}
