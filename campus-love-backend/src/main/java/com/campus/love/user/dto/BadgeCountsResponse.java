package com.campus.love.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BadgeCountsResponse {
    /** 私聊未读消息总数 */
    private int unreadMessageCount;
    /** 新粉丝数量（自上次查看粉丝列表以来） */
    private int newFollowerCount;
    /** 我发布的帖子收到的新点赞/评论数 */
    private int newFeedActivityCount;
    /** 邀约新活动数（我的邀约有人加入/发言、我被人加入等） */
    private int newInviteActivityCount;
}
