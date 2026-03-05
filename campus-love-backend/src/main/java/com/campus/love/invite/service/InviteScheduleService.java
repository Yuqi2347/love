package com.campus.love.invite.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.common.enums.NotificationTypeEnum;
import com.campus.love.invite.entity.Invite;
import com.campus.love.invite.entity.InviteParticipant;
import com.campus.love.invite.enums.InviteStatusEnum;
import com.campus.love.invite.mapper.InviteMapper;
import com.campus.love.invite.mapper.InviteParticipantMapper;
import com.campus.love.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 邀约提醒定时任务：
 * - 活动开始前 1 天提醒
 * - 活动开始前 1 小时提醒
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InviteScheduleService {

    private final InviteMapper inviteMapper;
    private final InviteParticipantMapper inviteParticipantMapper;
    private final NotificationService notificationService;

    /**
     * 默认认为活动持续时长（小时），用于区分「进行中」与「已结束」
     */
    private static final int DEFAULT_EVENT_DURATION_HOURS = 4;

    /**
     * 每 5 分钟扫描一次，发送 1 天前 / 1 小时前的提醒。
     * 为避免频繁发送，依赖 NotificationService.existsNotification 做去重。
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000L)
    @Transactional
    public void sendInviteReminders() {
        LocalDateTime now = LocalDateTime.now();

        try {
            handleOneDayReminders(now);
            handleOneHourReminders(now);
            updateInviteStatuses(now);
        } catch (Exception e) {
            log.error("发送邀约提醒任务执行异常", e);
        }
    }

    private void handleOneDayReminders(LocalDateTime now) {
        LocalDateTime start = now.plusDays(1);
        LocalDateTime end = now.plusDays(1).plusMinutes(10);

        List<Invite> invites = inviteMapper.selectList(
                new LambdaQueryWrapper<Invite>()
                        .eq(Invite::getDeleted, false)
                        .between(Invite::getInviteTime, start, end)
                        .in(Invite::getStatus,
                                InviteStatusEnum.RECRUITING.name(),
                                InviteStatusEnum.FULL.name(),
                                InviteStatusEnum.CONFIRMED.name())
        );

        for (Invite invite : invites) {
            notifyAllUsersOnce(invite, NotificationTypeEnum.INVITE_REMIND_1D, true);
        }
    }

    private void handleOneHourReminders(LocalDateTime now) {
        LocalDateTime start = now.plusHours(1);
        LocalDateTime end = now.plusHours(1).plusMinutes(10);

        List<Invite> invites = inviteMapper.selectList(
                new LambdaQueryWrapper<Invite>()
                        .eq(Invite::getDeleted, false)
                        .between(Invite::getInviteTime, start, end)
                        .in(Invite::getStatus,
                                InviteStatusEnum.RECRUITING.name(),
                                InviteStatusEnum.FULL.name(),
                                InviteStatusEnum.CONFIRMED.name())
        );

        for (Invite invite : invites) {
            notifyAllUsersOnce(invite, NotificationTypeEnum.INVITE_REMIND_1H, false);
        }
    }

    /**
     * 给发起人 + 所有当前参与者 各发送一次指定类型的提醒，若已存在相同类型通知则跳过。
     *
     * @param invite           邀约
     * @param type             通知类型（1天前/1小时前）
     * @param isOneDayReminder 是否为 1 天前提醒（用于区分标题文案）
     */
    private void notifyAllUsersOnce(Invite invite, NotificationTypeEnum type, boolean isOneDayReminder) {
        if (invite == null || invite.getInviteTime() == null) {
            return;
        }

        // 构建通知目标用户列表：发起人 + 参与者（去重）
        Set<Long> userIds = new HashSet<>();
        if (invite.getCreatorId() != null) {
            userIds.add(invite.getCreatorId());
        }

        List<InviteParticipant> participants = inviteParticipantMapper.selectList(
                new LambdaQueryWrapper<InviteParticipant>()
                        .eq(InviteParticipant::getInviteId, invite.getId())
        );
        userIds.addAll(
                participants.stream()
                        .map(InviteParticipant::getUserId)
                        .collect(Collectors.toSet())
        );

        for (Long userId : userIds) {
            if (userId == null) {
                continue;
            }
            // 已有相同类型通知则不再重复发送
            if (notificationService.existsNotification(userId, invite.getId(), type)) {
                continue;
            }
            if (isOneDayReminder) {
                notificationService.notifyInviteRemindOneDay(userId, invite);
            } else {
                notificationService.notifyInviteRemindOneHour(userId, invite);
            }
        }
    }

    /**
     * 邀约状态自动流转：
     * - 超时无人参与：报名截止后且仍无人报名，状态置为 CANCELLED
     * - 活动开始：到达开始时间后且有人报名，状态置为 IN_PROGRESS
     * - 活动结束：开始时间 + DEFAULT_EVENT_DURATION_HOURS 后仍未结束/取消，状态置为 ENDED
     * - 长期无响应：招募中且超过 72 小时无人报名，状态置为 CANCELLED
     */
    private void updateInviteStatuses(LocalDateTime now) {
        List<Invite> invites = inviteMapper.selectList(
                new LambdaQueryWrapper<Invite>()
                        .eq(Invite::getDeleted, false)
                        .in(Invite::getStatus,
                                InviteStatusEnum.RECRUITING.name(),
                                InviteStatusEnum.FULL.name(),
                                InviteStatusEnum.CONFIRMED.name(),
                                InviteStatusEnum.IN_PROGRESS.name())
        );

        for (Invite invite : invites) {
            if (invite.getInviteTime() == null) {
                continue;
            }
            LocalDateTime inviteTime = invite.getInviteTime();
            String status = invite.getStatus();
            int participantCount = invite.getParticipantCount() != null ? invite.getParticipantCount() : 0;

            boolean changed = false;

            // 超时无人参与：报名截止后仍无人报名
            if (participantCount == 0 && InviteStatusEnum.RECRUITING.name().equals(status)) {
                Integer deadlineHours = invite.getDeadlineHours();
                if (deadlineHours != null) {
                    LocalDateTime deadlineTime = inviteTime.minusHours(deadlineHours);
                    if (now.isAfter(deadlineTime)) {
                        invite.setStatus(InviteStatusEnum.CANCELLED.name());
                        changed = true;
                    }
                }
                // 长期无响应：创建时间超过 72 小时仍无人报名
                if (!changed && invite.getCreatedAt() != null &&
                        invite.getCreatedAt().isBefore(now.minusHours(72))) {
                    invite.setStatus(InviteStatusEnum.CANCELLED.name());
                    changed = true;
                }
            }

            // 活动进行中 / 已结束（仅在存在参与者时才有意义）
            if (!changed && participantCount > 0) {
                LocalDateTime endTime = inviteTime.plusHours(DEFAULT_EVENT_DURATION_HOURS);

                if ((InviteStatusEnum.RECRUITING.name().equals(status)
                        || InviteStatusEnum.FULL.name().equals(status)
                        || InviteStatusEnum.CONFIRMED.name().equals(status))
                        && !now.isBefore(inviteTime) && now.isBefore(endTime)) {
                    // 到达开始时间，标记为进行中
                    invite.setStatus(InviteStatusEnum.IN_PROGRESS.name());
                    changed = true;
                } else if (now.isAfter(endTime)
                        && !InviteStatusEnum.ENDED.name().equals(status)
                        && !InviteStatusEnum.CANCELLED.name().equals(status)) {
                    // 结束时间已过，自动结束
                    invite.setStatus(InviteStatusEnum.ENDED.name());
                    changed = true;
                }
            }

            if (changed) {
                inviteMapper.updateById(invite);
            }
        }
    }
}

