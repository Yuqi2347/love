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
    /** V24：AI 提取的标签，逗号分隔 */
    private String aiTags;
    /** V39：是否置顶 */
    private Boolean pinned;
    /** V39：置顶时间 */
    private String pinnedAt;

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
        /** V39：点赞数 */
        private Integer likeCount;
        /** V39：当前用户是否已点赞 */
        private Boolean liked;
    }
}
