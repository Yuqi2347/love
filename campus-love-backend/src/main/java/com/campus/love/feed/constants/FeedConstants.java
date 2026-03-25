package com.campus.love.feed.constants;

/**
 * 朋友圈模块常量
 */
public final class FeedConstants {

    private FeedConstants() {}

    /** 列表/时间线中每条帖子的评论数量上限 */
    public static final int LIST_COMMENT_LIMIT = 10;
    /** 帖子详情接口用该值区分「详情模式」与列表内嵌评论（列表为 {@link #LIST_COMMENT_LIMIT}） */
    public static final int DETAIL_COMMENT_LIMIT = 200;
    /** 详情页：根评论（楼主）条数上限 */
    public static final int DETAIL_COMMENT_ROOT_LIMIT = 30;
    /** 详情页：拉取候选子回复条数上限，再按所属根评论过滤 */
    public static final int DETAIL_COMMENT_REPLY_FETCH_LIMIT = 400;
    /** 社交通知（点赞/评论）单次拉取上限 */
    public static final int SOCIAL_NOTIFICATION_LIMIT = 50;
    /** 社交通知内容预览最大长度 */
    public static final int NOTIFICATION_CONTENT_PREVIEW_LEN = 30;
    /** 用户帖子摘要拉取上限（用于 total 估算） */
    public static final int USER_POSTS_SUMMARY_LIMIT = 200;
    /** 时间线/用户帖子分页过载倍数（用于 visibility 过滤后仍能取够一页） */
    public static final int TIMELINE_FETCH_MULTIPLIER = 5;
    /** 单条动态允许的图片数量上限（逗号分隔 URL） */
    public static final int POST_IMAGES_MAX = 8;
}
