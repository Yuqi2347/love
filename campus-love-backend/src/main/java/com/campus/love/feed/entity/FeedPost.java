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

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
