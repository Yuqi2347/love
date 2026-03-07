package com.campus.love.user.service;

import com.campus.love.auth.security.CurrentUser;
import com.campus.love.chat.service.ChatService;
import com.campus.love.feed.service.FeedService;
import com.campus.love.follow.service.FollowService;
import com.campus.love.invite.service.InviteService;
import com.campus.love.user.dto.BadgeCountsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BadgeService {

    private final ChatService chatService;
    private final FollowService followService;
    private final FeedService feedService;
    private final InviteService inviteService;

    public BadgeCountsResponse getBadgeCounts(Long userId) {
        return BadgeCountsResponse.builder()
                .unreadMessageCount(chatService.getTotalUnreadCount())
                .newFollowerCount(followService.getNewFollowerCount(userId))
                .newFeedActivityCount(feedService.getNewFeedActivityCount(userId))
                .newInviteActivityCount(inviteService.getNewInviteActivityCount(userId))
                .build();
    }
}
