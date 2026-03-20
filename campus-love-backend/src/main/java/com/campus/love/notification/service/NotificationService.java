package com.campus.love.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.enums.NotificationTypeEnum;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.invite.entity.Invite;
import com.campus.love.notification.dto.NotificationResponse;
import com.campus.love.notification.entity.Notification;
import com.campus.love.notification.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 站内通知服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;

    /**
     * 获取当前用户的通知列表
     *
     * @param unreadOnly 是否仅查询未读
     */
    public List<NotificationResponse> getMyNotifications(boolean unreadOnly) {
        Long currentUserId = CurrentUser.getId();

        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, currentUserId)
                .orderByDesc(Notification::getCreatedAt);
        if (unreadOnly) {
            wrapper.eq(Notification::getIsRead, false);
        }

        List<Notification> list = notificationMapper.selectList(wrapper);
        return list.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 标记单条通知为已读
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        Long currentUserId = CurrentUser.getId();

        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null || !notification.getUserId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "通知不存在");
        }
        if (Boolean.TRUE.equals(notification.getIsRead())) {
            return;
        }
        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationMapper.updateById(notification);
    }

    /**
     * 创建通用通知
     */
    @Transactional
    public void createNotification(Long userId, Long senderId, Long inviteId,
                                   NotificationTypeEnum type, String title, String content) {
        createNotification(userId, senderId, inviteId, null, type, title, content);
    }

    /**
     * 创建通用通知（支持 postId）
     */
    @Transactional
    public void createNotification(Long userId, Long senderId, Long inviteId, Long postId,
                                   NotificationTypeEnum type, String title, String content) {
        createNotification(userId, senderId, inviteId, postId, null, type, title, content);
    }

    /**
     * 创建通用通知（支持 postId、commentId）
     */
    @Transactional
    public void createNotification(Long userId, Long senderId, Long inviteId, Long postId, Long commentId,
                                   NotificationTypeEnum type, String title, String content) {
        if (userId == null || type == null) {
            throw new IllegalArgumentException("userId and type are required");
        }
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setSenderId(senderId);
        notification.setInviteId(inviteId);
        notification.setPostId(postId);
        notification.setCommentId(commentId);
        notification.setType(type.name());
        notification.setTitle(title);
        notification.setContent(content);
        notification.setIsRead(false);
        notificationMapper.insert(notification);
    }

    /**
     * 有人回复了你的评论
     */
    public void notifyCommentReply(Long repliedUserId, Long senderId, Long postId,
                                   String senderNickname, String contentPreview) {
        if (repliedUserId == null || senderId == null || repliedUserId.equals(senderId)) {
            return;
        }
        String title = "有人回复了你的评论";
        String content = (senderNickname != null ? senderNickname : "有人") + "回复了你"
                + (contentPreview != null && !contentPreview.isBlank() ? "：" + truncate(contentPreview, 50) : "");
        createNotification(repliedUserId, senderId, null, postId,
                NotificationTypeEnum.COMMENT_REPLY, title, content);
    }

    /**
     * 有人评论了你的动态
     */
    public void notifyCommentOnPost(Long postAuthorId, Long senderId, Long postId,
                                    String senderNickname, String contentPreview) {
        if (postAuthorId == null || senderId == null || postAuthorId.equals(senderId)) {
            return;
        }
        String title = "有人评论了你的动态";
        String content = (senderNickname != null ? senderNickname : "有人") + "评论了你的动态"
                + (contentPreview != null && !contentPreview.isBlank() ? "：" + truncate(contentPreview, 50) : "");
        createNotification(postAuthorId, senderId, null, postId,
                NotificationTypeEnum.COMMENT_ON_POST, title, content);
    }

    /**
     * 有人赞了你的动态（V39）
     */
    public void notifyPostLike(Long postAuthorId, Long likerId, Long postId, String likerNickname) {
        if (postAuthorId == null || likerId == null || postAuthorId.equals(likerId)) {
            return;
        }
        String title = "有人赞了你的动态";
        String content = (likerNickname != null ? likerNickname : "有人") + "赞了你的动态";
        createNotification(postAuthorId, likerId, null, postId,
                NotificationTypeEnum.POST_LIKE, title, content);
    }

    /**
     * 有人赞了你的评论（V39）
     */
    public void notifyCommentLike(Long commentAuthorId, Long likerId, Long postId, Long commentId, String likerNickname) {
        if (commentAuthorId == null || likerId == null || commentAuthorId.equals(likerId)) {
            return;
        }
        String title = "有人赞了你的评论";
        String content = (likerNickname != null ? likerNickname : "有人") + "赞了你的评论";
        createNotification(commentAuthorId, likerId, null, postId, commentId,
                NotificationTypeEnum.COMMENT_LIKE, title, content);
    }

    private static String truncate(String s, int maxLen) {
        if (s == null || s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "...";
    }

    /**
     * 统计某用户在指定时间之后收到的评论点赞数（用于红点计数）
     */
    public long countCommentLikesSince(Long userId, java.time.LocalDateTime since) {
        if (userId == null || since == null) return 0;
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getType, NotificationTypeEnum.COMMENT_LIKE.name())
                        .gt(Notification::getCreatedAt, since));
    }

    /**
     * 获取用户收到的评论点赞通知列表（用于社交消息中心）
     */
    public List<Notification> getCommentLikeNotifications(Long userId, int limit) {
        if (userId == null) return new java.util.ArrayList<>();
        return notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getType, NotificationTypeEnum.COMMENT_LIKE.name())
                        .orderByDesc(Notification::getCreatedAt)
                        .last("LIMIT " + limit));
    }

    /**
     * 获取用户收到的动态点赞通知列表（用于社交消息中心，补充 t_feed_like 直查的数据）
     */
    public List<Notification> getPostLikeNotifications(Long userId, int limit) {
        if (userId == null) return new java.util.ArrayList<>();
        return notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getType, NotificationTypeEnum.POST_LIKE.name())
                        .orderByDesc(Notification::getCreatedAt)
                        .last("LIMIT " + limit));
    }

    /**
     * 判断同一用户、同一邀约下是否已存在某类型通知
     */
    public boolean existsNotification(Long userId, Long inviteId, NotificationTypeEnum type) {
        if (userId == null || inviteId == null || type == null) {
            return false;
        }
        Long count = notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getInviteId, inviteId)
                        .eq(Notification::getType, type.name())
        );
        return count != null && count > 0;
    }

    /**
     * 报名成功通知（发送给参与者自己）
     */
    public void notifyInviteJoinSuccess(Long participantId, Long senderId, Invite invite) {
        if (invite == null) {
            return;
        }
        String title = "报名成功";
        String content = "你已成功报名邀约：「" + invite.getTitle() + "」";
        createNotification(participantId, senderId, invite.getId(),
                NotificationTypeEnum.INVITE_JOIN_SUCCESS, title, content);
    }

    /**
     * 有人加入邀约通知（发送给发起人）
     */
    public void notifyInviteNewParticipant(Long creatorId, Long senderId, Invite invite) {
        if (invite == null) {
            return;
        }
        String title = "有人加入你的邀约";
        String content = "有新伙伴加入了你的邀约：「" + invite.getTitle() + "」";
        createNotification(creatorId, senderId, invite.getId(),
                NotificationTypeEnum.INVITE_NEW_PARTICIPANT, title, content);
    }

    /**
     * 邀约取消通知（发送给所有参与者）
     */
    public void notifyInviteCancelled(List<Long> participantIds, Long senderId, Invite invite, String reason) {
        if (invite == null || participantIds == null || participantIds.isEmpty()) {
            return;
        }
        String title = "邀约已被取消";
        String baseContent = "你参与的邀约「" + invite.getTitle() + "」已被发起人取消";
        String fullContent = reason == null || reason.isBlank()
                ? baseContent
                : baseContent + "，原因：" + reason;
        for (Long userId : participantIds) {
            createNotification(userId, senderId, invite.getId(),
                    NotificationTypeEnum.INVITE_CANCELLED, title, fullContent);
        }
    }

    /**
     * 参与者退出通知（发送给发起人）
     */
    public void notifyParticipantLeave(Long creatorId, Long leaverId, Invite invite) {
        if (invite == null) {
            return;
        }
        String title = "有人退出你的邀约";
        String content = "有参与者退出了你的邀约：「" + invite.getTitle() + "」";
        createNotification(creatorId, leaverId, invite.getId(),
                NotificationTypeEnum.INVITE_PARTICIPANT_LEAVE, title, content);
    }

    /**
     * 被发起人踢出邀约通知（发送给被踢用户）
     */
    public void notifyParticipantKicked(Long kickedUserId, Long creatorId, Invite invite, String reason) {
        if (invite == null || kickedUserId == null) {
            return;
        }
        String title = "你已被移出邀约";
        String baseContent = "你参与的邀约「" + invite.getTitle() + "」已被发起人移出";
        String fullContent = reason == null || reason.isBlank()
                ? baseContent
                : baseContent + "，原因：" + reason;
        createNotification(kickedUserId, creatorId, invite.getId(),
                NotificationTypeEnum.INVITE_PARTICIPANT_KICKED, title, fullContent);
    }

    /**
     * 等待邀约匹配到合适邀约（发送给等待邀约用户）
     */
    public void notifyWaitMatch(Long waitUserId, Invite invite) {
        if (invite == null) {
            return;
        }
        String title = "找到匹配的邀约";
        String content = "已为你找到匹配的邀约：「" + invite.getTitle() + "」，点击查看详情决定是否参加";
        createNotification(waitUserId, invite.getCreatorId(), invite.getId(),
                NotificationTypeEnum.INVITE_MATCH_FOUND, title, content);
    }

    /**
     * 活动开始前1天提醒通知
     */
    public void notifyInviteRemindOneDay(Long userId, Invite invite) {
        if (invite == null) {
            return;
        }
        String title = "活动明天就要开始啦";
        String content = "你参与/发起的邀约「" + invite.getTitle() + "」将在明天开始，请合理安排时间。";
        createNotification(userId, invite.getCreatorId(), invite.getId(),
                NotificationTypeEnum.INVITE_REMIND_1D, title, content);
    }

    /**
     * 活动开始前1小时提醒通知
     */
    public void notifyInviteRemindOneHour(Long userId, Invite invite) {
        if (invite == null) {
            return;
        }
        String title = "活动即将开始（1小时内）";
        String content = "你参与/发起的邀约「" + invite.getTitle() + "」将在1小时内开始，请提前准备。";
        createNotification(userId, invite.getCreatorId(), invite.getId(),
                NotificationTypeEnum.INVITE_REMIND_1H, title, content);
    }

    /** 是否存在未读的「再次加入」申请（同一邀约、同一申请人） */
    public boolean hasUnreadRejoinRequest(Long inviteId, Long applicantUserId) {
        if (inviteId == null || applicantUserId == null) {
            return false;
        }
        Long count = notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getInviteId, inviteId)
                        .eq(Notification::getSenderId, applicantUserId)
                        .eq(Notification::getType, NotificationTypeEnum.INVITE_REJOIN_REQUEST.name())
                        .eq(Notification::getIsRead, false));
        return count != null && count > 0;
    }

    /** 发起人：获取某邀约下所有未读的再次加入申请（接收人为 creatorId） */
    public List<Notification> getUnreadRejoinRequestsForInvite(Long inviteId, Long creatorId) {
        if (inviteId == null || creatorId == null) {
            return List.of();
        }
        return notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getInviteId, inviteId)
                        .eq(Notification::getUserId, creatorId)
                        .eq(Notification::getType, NotificationTypeEnum.INVITE_REJOIN_REQUEST.name())
                        .eq(Notification::getIsRead, false)
                        .orderByAsc(Notification::getCreatedAt));
    }

    /** 将某邀约、某申请人的再次加入申请通知全部标为已读 */
    @Transactional
    public void markRejoinRequestsAsRead(Long inviteId, Long applicantUserId) {
        if (inviteId == null || applicantUserId == null) {
            return;
        }
        List<Notification> list = notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getInviteId, inviteId)
                        .eq(Notification::getSenderId, applicantUserId)
                        .eq(Notification::getType, NotificationTypeEnum.INVITE_REJOIN_REQUEST.name())
                        .eq(Notification::getIsRead, false));
        LocalDateTime now = LocalDateTime.now();
        for (Notification n : list) {
            n.setIsRead(true);
            n.setReadAt(now);
            notificationMapper.updateById(n);
        }
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .senderId(notification.getSenderId())
                .inviteId(notification.getInviteId())
                .postId(notification.getPostId())
                .type(notification.getType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}

