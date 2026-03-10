package com.campus.love.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.love.admin.dto.AdminFeedItem;
import com.campus.love.admin.dto.AdminInviteItem;
import com.campus.love.admin.dto.AdminUserItem;
import com.campus.love.admin.mapper.AdminUserDeleteMapper;
import com.campus.love.auth.security.CurrentUser;
import com.campus.love.chat.entity.ChatGroup;
import com.campus.love.chat.entity.ChatGroupMember;
import com.campus.love.chat.entity.Message;
import com.campus.love.chat.mapper.ChatGroupMapper;
import com.campus.love.chat.mapper.ChatGroupMemberMapper;
import com.campus.love.chat.mapper.MessageMapper;
import com.campus.love.common.constants.RedisKeyConstants;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.feed.entity.FeedComment;
import com.campus.love.feed.entity.FeedLike;
import com.campus.love.feed.entity.FeedPost;
import com.campus.love.feed.mapper.FeedCommentMapper;
import com.campus.love.feed.mapper.FeedLikeMapper;
import com.campus.love.feed.mapper.FeedPostMapper;
import com.campus.love.feed.service.FeedService;
import com.campus.love.follow.entity.Follow;
import com.campus.love.follow.mapper.FollowMapper;
import com.campus.love.invite.entity.Invite;
import com.campus.love.invite.entity.InviteDecline;
import com.campus.love.invite.entity.InviteParticipant;
import com.campus.love.invite.entity.InviteRating;
import com.campus.love.invite.entity.InviteWait;
import com.campus.love.invite.mapper.InviteDeclineMapper;
import com.campus.love.invite.mapper.InviteMapper;
import com.campus.love.invite.mapper.InviteParticipantMapper;
import com.campus.love.invite.mapper.InviteRatingMapper;
import com.campus.love.invite.mapper.InviteWaitMapper;
import com.campus.love.match.entity.UserWeights;
import com.campus.love.match.mapper.UserWeightsMapper;
import com.campus.love.moment.entity.MomentEnrollment;
import com.campus.love.moment.entity.MomentMatchResult;
import com.campus.love.moment.entity.MomentProfile;
import com.campus.love.moment.mapper.MomentEnrollmentMapper;
import com.campus.love.moment.mapper.MomentMatchResultMapper;
import com.campus.love.moment.mapper.MomentProfileMapper;
import com.campus.love.ai.entity.YuanFenAnalysisLog;
import com.campus.love.ai.mapper.YuanFenAnalysisLogMapper;
import com.campus.love.notification.entity.Notification;
import com.campus.love.notification.mapper.NotificationMapper;
import com.campus.love.user.entity.ActivityLog;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.ActivityLogMapper;
import com.campus.love.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 管理端业务逻辑，供 AdminController 调用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserMapper userMapper;
    private final InviteMapper inviteMapper;
    private final FeedPostMapper feedPostMapper;
    private final FeedService feedService;
    private final AdminUserDeleteMapper adminUserDeleteMapper;
    private final FollowMapper followMapper;
    private final MessageMapper messageMapper;
    private final FeedLikeMapper feedLikeMapper;
    private final FeedCommentMapper feedCommentMapper;
    private final InviteParticipantMapper inviteParticipantMapper;
    private final InviteWaitMapper inviteWaitMapper;
    private final InviteRatingMapper inviteRatingMapper;
    private final InviteDeclineMapper inviteDeclineMapper;
    private final ChatGroupMapper chatGroupMapper;
    private final ChatGroupMemberMapper chatGroupMemberMapper;
    private final NotificationMapper notificationMapper;
    private final UserWeightsMapper userWeightsMapper;
    private final YuanFenAnalysisLogMapper yuanFenAnalysisLogMapper;
    private final MomentProfileMapper momentProfileMapper;
    private final MomentEnrollmentMapper momentEnrollmentMapper;
    private final MomentMatchResultMapper momentMatchResultMapper;
    private final ActivityLogMapper activityLogMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public IPage<AdminUserItem> listUsers(int page, int size, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(User::getNickname, keyword)
                    .or().like(User::getEmail, keyword));
        }
        wrapper.orderByDesc(User::getCreatedAt);

        Page<User> pageReq = new Page<>(page, size);
        IPage<User> userPage = userMapper.selectPage(pageReq, wrapper);
        List<User> records = userPage.getRecords();

        List<AdminUserItem> items = records.stream()
                .map(u -> AdminUserItem.builder()
                        .id(u.getId())
                        .email(u.getEmail())
                        .nickname(u.getNickname())
                        .school(u.getSchool())
                        .status(u.getStatus())
                        .isAdmin(Boolean.TRUE.equals(u.getIsAdmin()))
                        .creditScore(u.getCreditScore())
                        .activityScore(u.getActivityScore())
                        .userLevel(u.getUserLevel())
                        .inviteCount(u.getInviteCount())
                        .participateCount(u.getParticipateCount())
                        .createdAt(u.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        IPage<AdminUserItem> result = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        result.setRecords(items);
        return result;
    }

    public IPage<AdminInviteItem> listInvites(int page, int size, String status) {
        LambdaQueryWrapper<Invite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Invite::getDeleted, false);
        if (StringUtils.hasText(status)) {
            wrapper.eq(Invite::getStatus, status);
        }
        wrapper.orderByDesc(Invite::getCreatedAt);

        Page<Invite> pageReq = new Page<>(page, size);
        IPage<Invite> invitePage = inviteMapper.selectPage(pageReq, wrapper);
        List<Invite> records = invitePage.getRecords();

        List<Long> creatorIds = records.stream()
                .map(Invite::getCreatorId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> creatorNames = creatorIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(creatorIds).stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(User::getId, u -> u.getNickname() != null ? u.getNickname() : "", (a, b) -> a));

        List<AdminInviteItem> items = records.stream()
                .map(inv -> AdminInviteItem.builder()
                        .id(inv.getId())
                        .creatorId(inv.getCreatorId())
                        .creatorNickname(creatorNames.getOrDefault(inv.getCreatorId(), ""))
                        .inviteType(inv.getInviteType())
                        .inviteMode(inv.getInviteMode())
                        .title(inv.getTitle())
                        .status(inv.getStatus())
                        .inviteTime(inv.getInviteTime())
                        .participantCount(inv.getParticipantCount())
                        .maxParticipants(inv.getMaxParticipants())
                        .createdAt(inv.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        IPage<AdminInviteItem> result = new Page<>(invitePage.getCurrent(), invitePage.getSize(), invitePage.getTotal());
        result.setRecords(items);
        return result;
    }

    public void deleteInvite(Long id) {
        Invite inv = inviteMapper.selectById(id);
        if (inv == null) throw new BusinessException(ResultCode.NOT_FOUND, "邀约不存在");
        inviteMapper.deleteById(id);
    }

    public IPage<AdminFeedItem> listFeeds(int page, int size, Long userId) {
        LambdaQueryWrapper<FeedPost> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(FeedPost::getUserId, userId);
        }
        wrapper.orderByDesc(FeedPost::getCreatedAt);

        Page<FeedPost> pageReq = new Page<>(page, size);
        IPage<FeedPost> feedPage = feedPostMapper.selectPage(pageReq, wrapper);
        List<FeedPost> records = feedPage.getRecords();

        List<Long> authorIds = records.stream()
                .map(FeedPost::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> authorNames = authorIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(authorIds).stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(User::getId, u -> u.getNickname() != null ? u.getNickname() : "", (a, b) -> a));

        List<AdminFeedItem> items = records.stream()
                .map(p -> AdminFeedItem.builder()
                        .id(p.getId())
                        .userId(p.getUserId())
                        .nickname(authorNames.getOrDefault(p.getUserId(), ""))
                        .content(p.getContent() != null ? (p.getContent().length() > 80 ? p.getContent().substring(0, 80) + "..." : p.getContent()) : "")
                        .postType(p.getPostType())
                        .likeCount(p.getLikeCount() != null ? p.getLikeCount() : 0)
                        .commentCount(p.getCommentCount() != null ? p.getCommentCount() : 0)
                        .createdAt(p.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        IPage<AdminFeedItem> result = new Page<>(feedPage.getCurrent(), feedPage.getSize(), feedPage.getTotal());
        result.setRecords(items);
        return result;
    }

    public void deleteFeed(Long id) {
        feedService.deletePost(id);
    }

    public void updateUserCredit(Long id, Integer creditScore) {
        if (creditScore == null) throw new BusinessException(ResultCode.BAD_REQUEST, "creditScore 必填");
        if (creditScore < 0 || creditScore > 1000) throw new BusinessException(ResultCode.BAD_REQUEST, "信用分需在 0-1000 之间");
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        user.setCreditScore(creditScore);
        userMapper.updateById(user);
    }

    public void updateUserStats(Long id, Map<String, Integer> body) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        if (body.containsKey("creditScore")) {
            int v = body.get("creditScore");
            if (v < 0 || v > 1000) throw new BusinessException(ResultCode.BAD_REQUEST, "信用分需在 0-1000 之间");
            user.setCreditScore(v);
        }
        if (body.containsKey("activityScore")) {
            user.setActivityScore(body.get("activityScore"));
        }
        if (body.containsKey("userLevel")) {
            user.setUserLevel(body.get("userLevel"));
        }
        userMapper.updateById(user);
    }

    /**
     * 彻底删除指定用户及其全部相关数据。
     * 禁止删除管理员或当前登录用户。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteUserCompletely(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        if (Boolean.TRUE.equals(user.getIsAdmin())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "禁止删除管理员账号");
        }
        Long currentUserId = CurrentUser.getId();
        if (userId.equals(currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "禁止删除当前登录用户");
        }

        // 1. 关注关系
        followMapper.delete(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, userId).or().eq(Follow::getFollowingId, userId));

        // 2. 活跃度、相册、匹配权重、匹配行为
        activityLogMapper.delete(new LambdaQueryWrapper<ActivityLog>().eq(ActivityLog::getUserId, userId));
        adminUserDeleteMapper.deleteUserAlbumByUserId(userId);
        userWeightsMapper.delete(new LambdaQueryWrapper<UserWeights>().eq(UserWeights::getUserId, userId));
        adminUserDeleteMapper.deleteUserMatchActionByUserId(userId);

        // 3. 心动一刻
        momentProfileMapper.delete(new LambdaQueryWrapper<MomentProfile>().eq(MomentProfile::getUserId, userId));
        momentEnrollmentMapper.delete(new LambdaQueryWrapper<MomentEnrollment>().eq(MomentEnrollment::getUserId, userId));
        momentMatchResultMapper.delete(new LambdaQueryWrapper<MomentMatchResult>()
                .eq(MomentMatchResult::getUserIdA, userId).or().eq(MomentMatchResult::getUserIdB, userId));

        // 4. 缘分解析
        yuanFenAnalysisLogMapper.delete(new LambdaQueryWrapper<YuanFenAnalysisLog>()
                .eq(YuanFenAnalysisLog::getUserIdA, userId).or().eq(YuanFenAnalysisLog::getUserIdB, userId));

        // 5. 朋友圈：先删点赞/评论，再删帖子（含软删帖子，避免孤儿数据）
        List<Long> postIds = adminUserDeleteMapper.selectPostIdsByUserId(userId);
        if (!postIds.isEmpty()) {
            feedLikeMapper.delete(new LambdaQueryWrapper<FeedLike>().in(FeedLike::getPostId, postIds));
            feedCommentMapper.delete(new LambdaQueryWrapper<FeedComment>().in(FeedComment::getPostId, postIds));
        }
        feedLikeMapper.delete(new LambdaQueryWrapper<FeedLike>().eq(FeedLike::getUserId, userId));
        feedCommentMapper.delete(new LambdaQueryWrapper<FeedComment>().eq(FeedComment::getUserId, userId));
        adminUserDeleteMapper.deleteFeedPostsByUserId(userId);

        // 6. 邀约（用户为创建者）：删除群聊、消息、参与者、评价、拒绝记录、邀约
        List<Invite> createdInvites = inviteMapper.selectList(
                new LambdaQueryWrapper<Invite>().eq(Invite::getCreatorId, userId));
        for (Invite inv : createdInvites) {
            Long groupId = inv.getChatGroupId();
            if (groupId != null) {
                messageMapper.delete(new LambdaQueryWrapper<Message>().eq(Message::getGroupId, groupId));
                chatGroupMemberMapper.delete(new LambdaQueryWrapper<ChatGroupMember>().eq(ChatGroupMember::getGroupId, groupId));
                chatGroupMapper.deleteById(groupId);
            }
            inviteParticipantMapper.delete(new LambdaQueryWrapper<InviteParticipant>().eq(InviteParticipant::getInviteId, inv.getId()));
            inviteRatingMapper.delete(new LambdaQueryWrapper<InviteRating>().eq(InviteRating::getInviteId, inv.getId()));
            inviteDeclineMapper.delete(new LambdaQueryWrapper<InviteDecline>().eq(InviteDecline::getInviteId, inv.getId()));
            inviteMapper.deleteById(inv.getId());
        }

        // 7. 邀约（用户为参与者/目标/评价相关）
        inviteParticipantMapper.delete(new LambdaQueryWrapper<InviteParticipant>().eq(InviteParticipant::getUserId, userId));
        inviteWaitMapper.delete(new LambdaQueryWrapper<InviteWait>().eq(InviteWait::getUserId, userId));
        inviteRatingMapper.delete(new LambdaQueryWrapper<InviteRating>()
                .eq(InviteRating::getRaterId, userId).or().eq(InviteRating::getRatedUserId, userId));
        inviteDeclineMapper.delete(new LambdaQueryWrapper<InviteDecline>().eq(InviteDecline::getUserId, userId));
        inviteMapper.delete(new LambdaQueryWrapper<Invite>().eq(Invite::getTargetUserId, userId));

        // 8. 聊天：群成员、私聊消息
        chatGroupMemberMapper.delete(new LambdaQueryWrapper<ChatGroupMember>().eq(ChatGroupMember::getUserId, userId));
        messageMapper.delete(new LambdaQueryWrapper<Message>()
                .eq(Message::getSenderId, userId).or().eq(Message::getReceiverId, userId));
        // 群聊创建者：删除其创建的独立群（无 invite_id）
        List<ChatGroup> ownedGroups = chatGroupMapper.selectList(
                new LambdaQueryWrapper<ChatGroup>().eq(ChatGroup::getCreatedBy, userId).isNull(ChatGroup::getInviteId));
        for (ChatGroup g : ownedGroups) {
            messageMapper.delete(new LambdaQueryWrapper<Message>().eq(Message::getGroupId, g.getId()));
            chatGroupMemberMapper.delete(new LambdaQueryWrapper<ChatGroupMember>().eq(ChatGroupMember::getGroupId, g.getId()));
            chatGroupMapper.deleteById(g.getId());
        }

        // 9. 通知
        notificationMapper.delete(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId).or().eq(Notification::getSenderId, userId));

        // 10. Redis 缓存（避免残留）
        clearUserRedisCache(userId);

        // 11. 用户主表
        userMapper.deleteById(userId);
        log.info("Admin deleted user completely: id={}, email={}", userId, user.getEmail());
    }

    private void clearUserRedisCache(Long userId) {
        try {
            redisTemplate.delete(RedisKeyConstants.userProfile(userId));
            redisTemplate.delete(RedisKeyConstants.followMutual(userId));
            redisTemplate.delete(RedisKeyConstants.matchRecommend(userId));
            redisTemplate.delete(RedisKeyConstants.feedTimeline(userId));
            Set<String> chatKeys = redisTemplate.keys(RedisKeyConstants.CHAT_DAILY_COUNT_PREFIX + userId + ":*");
            if (chatKeys != null && !chatKeys.isEmpty()) redisTemplate.delete(chatKeys);
            Set<String> chatKeys2 = redisTemplate.keys(RedisKeyConstants.CHAT_DAILY_COUNT_PREFIX + "*:" + userId + ":*");
            if (chatKeys2 != null && !chatKeys2.isEmpty()) redisTemplate.delete(chatKeys2);
        } catch (Exception e) {
            log.warn("Failed to clear Redis cache for user {}: {}", userId, e.getMessage());
        }
    }

    public void requireAdmin(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || !Boolean.TRUE.equals(user.getIsAdmin())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "需要管理员权限");
        }
    }

    public DashboardStats getDashboardStats() {
        long userTotal = userMapper.selectCount(null);
        long inviteTotal = inviteMapper.selectCount(
                new LambdaQueryWrapper<Invite>().eq(Invite::getDeleted, false));
        return new DashboardStats(userTotal, inviteTotal);
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class DashboardStats {
        private long userTotal;
        private long inviteTotal;
    }
}
