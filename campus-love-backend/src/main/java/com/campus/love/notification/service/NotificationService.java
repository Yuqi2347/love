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
        if (userId == null || type == null) {
            throw new IllegalArgumentException("userId and type are required");
        }
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setSenderId(senderId);
        notification.setInviteId(inviteId);
        notification.setType(type.name());
        notification.setTitle(title);
        notification.setContent(content);
        notification.setIsRead(false);
        notificationMapper.insert(notification);
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
                .type(notification.getType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}

