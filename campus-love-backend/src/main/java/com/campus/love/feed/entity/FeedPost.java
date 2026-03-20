package com.campus.love.feed.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_feed_post")
public class FeedPost {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String content;
    /**
     * 帖子类型：TIMELINE(朋友圈) / DISCOVERY(发现模块)
     */
    private String postType;
    private Integer requiredLevel;
    private String images;
    private String videos;
    private String linkUrl;
    private String linkTitle;
    private String linkImage;
    private Integer likeCount;
    private Integer commentCount;

    /** 可见范围：ALL=所有人，FOLLOWERS=关注我的人，FRIENDS=朋友，SELF=仅自己 */
    private String visibility;
    /** V24：AI提取的标签 JSON */
    private String aiTags;
    /** V24：主要兴趣分类 */
    private String primaryCategory;
    /** V24：positive/neutral/negative */
    private String tagSentiment;
    /** V24：OCEAN信号 JSON */
    private String oceanHints;
    /** V24：标签置信权重 */
    private java.math.BigDecimal tagConfidence;

    @TableLogic
    private Integer deleted;

    /** V39：置顶时间，非空表示置顶 */
    private LocalDateTime pinnedAt;
    /** V39：置顶操作人（管理员ID） */
    private Long pinnedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
