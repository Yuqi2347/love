package com.campus.love.feed.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FeedPostRequest {

    @Size(max = 500, message = "动态内容不能超过500字")
    private String content;

    private String images;
    private String videos;

    /** @deprecated 已下线通用外链，服务端忽略 */
    private String linkUrl;
    /** @deprecated 已下线通用外链，服务端忽略 */
    private String linkTitle;
    /** @deprecated 已下线通用外链，服务端忽略 */
    private String linkImage;

    /** 引用的邀约 ID（须为本人发起或参与过的有效邀约） */
    private Long inviteId;

    /**
     * 帖子类型：TIMELINE(朋友圈) / DISCOVERY(发现模块)
     * 默认为 TIMELINE
     */
    private String postType;

    /** 可见范围：ALL / FOLLOWING(我关注的人) / FOLLOWERS(关注我的人) / FRIENDS(互关) / SELF；省略时用用户资料默认 */
    private String visibility;
}
