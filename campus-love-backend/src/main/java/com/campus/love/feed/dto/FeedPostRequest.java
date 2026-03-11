package com.campus.love.feed.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FeedPostRequest {

    @Size(max = 500, message = "动态内容不能超过500字")
    private String content;

    private String images;
    private String videos;
    private String linkUrl;
    private String linkTitle;
    private String linkImage;

    /**
     * 帖子类型：TIMELINE(朋友圈) / DISCOVERY(发现模块)
     * 默认为 TIMELINE
     */
    private String postType;

    /** 可见范围：ALL=所有人，FOLLOWERS=关注我的人，FRIENDS=朋友，SELF=仅自己，默认 ALL */
    private String visibility;
}
