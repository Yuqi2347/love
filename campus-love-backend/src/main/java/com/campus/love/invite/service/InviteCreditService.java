package com.campus.love.invite.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.common.constants.InviteCreditConstants;
import com.campus.love.common.constants.InviteLevelLimit;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.invite.entity.Invite;
import com.campus.love.invite.entity.InviteParticipant;
import com.campus.love.invite.enums.InviteStatusEnum;
import com.campus.love.invite.mapper.InviteMapper;
import com.campus.love.invite.mapper.InviteParticipantMapper;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

/**
 * 邀约信用分、等级限制与用户邀约统计（发起/参与次数）的校验与更新。
 */
@Service
@RequiredArgsConstructor
public class InviteCreditService {

    private final UserMapper userMapper;
    private final InviteMapper inviteMapper;
    private final InviteParticipantMapper participantMapper;

    /**
     * 检查用户信用分是否不低于 minScore
     */
    public void checkUserCredit(Long userId, int minScore) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (user.getCreditScoreOrDefault() < minScore) {
            throw new BusinessException(ResultCode.CREDIT_TOO_LOW);
        }
    }

    /**
     * 检查今日邀约次数与同时进行数
     */
    public void checkInviteCreateLimit(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        int level = user.getUserLevel() != null ? user.getUserLevel() : 1;
        InviteLevelLimit limit = InviteLevelLimit.fromLevel(level);

        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);

        long todayCount = inviteMapper.selectCount(
                new LambdaQueryWrapper<Invite>()
                        .eq(Invite::getCreatorId, userId)
                        .ge(Invite::getCreatedAt, todayStart)
                        .eq(Invite::getDeleted, false));

        if (todayCount >= limit.getDailyLimit()) {
            throw new BusinessException(ResultCode.INVITE_LIMIT_EXCEEDED);
        }

        long concurrentCount = inviteMapper.selectCount(
                new LambdaQueryWrapper<Invite>()
                        .eq(Invite::getCreatorId, userId)
                        .eq(Invite::getDeleted, false)
                        .in(Invite::getStatus,
                                InviteStatusEnum.RECRUITING.name(),
                                InviteStatusEnum.FULL.name(),
                                InviteStatusEnum.CONFIRMED.name(),
                                InviteStatusEnum.IN_PROGRESS.name()));

        if (concurrentCount >= limit.getConcurrentLimit()) {
            throw new BusinessException(ResultCode.CONFLICT, "同时进行的邀约已达上限");
        }
    }

    /**
     * 检查用户当前同时参与的邀约数量是否超过上限
     */
    public void checkParticipateLimit(Long userId) {
        List<InviteParticipant> participants = participantMapper.selectList(
                new LambdaQueryWrapper<InviteParticipant>()
                        .eq(InviteParticipant::getUserId, userId));
        if (participants.isEmpty()) {
            return;
        }
        List<Long> inviteIds = participants.stream()
                .map(InviteParticipant::getInviteId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (inviteIds.isEmpty()) {
            return;
        }
        List<Invite> invites = inviteMapper.selectBatchIds(inviteIds);
        Map<Long, Invite> inviteMap = invites != null ? invites.stream()
                .filter(Objects::nonNull)
                .filter(inv -> !Boolean.TRUE.equals(inv.getDeleted()))
                .collect(toMap(Invite::getId, inv -> inv, (a, b) -> a)) : Map.of();

        int activeCount = 0;
        for (InviteParticipant participant : participants) {
            Invite invite = inviteMap.get(participant.getInviteId());
            if (invite == null) {
                continue;
            }
            String status = invite.getStatus();
            if (InviteStatusEnum.RECRUITING.name().equals(status)
                    || InviteStatusEnum.FULL.name().equals(status)
                    || InviteStatusEnum.CONFIRMED.name().equals(status)
                    || InviteStatusEnum.IN_PROGRESS.name().equals(status)) {
                activeCount++;
            }
        }

        if (activeCount >= InviteCreditConstants.MAX_CONCURRENT_PARTICIPATES) {
            throw new BusinessException(ResultCode.PARTICIPATE_LIMIT_EXCEEDED);
        }
    }

    /**
     * 调整用户信用分。加分时不超过 {@link InviteCreditConstants#CREDIT_MAX}，扣分时不低于 0。
     */
    public void adjustCreditScore(Long userId, int delta) {
        if (userId == null || delta == 0) {
            return;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }
        int current = user.getCreditScoreOrDefault();
        int newScore = current + delta;
        if (delta > 0 && newScore > InviteCreditConstants.CREDIT_MAX) {
            newScore = InviteCreditConstants.CREDIT_MAX;
        } else if (delta < 0 && newScore < 0) {
            newScore = 0;
        }
        user.setCreditScore(newScore);
        userMapper.updateById(user);
    }

    /**
     * 增加发起邀约次数
     */
    public void incrementInviteCount(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            int newCount = user.getInviteCountOrDefault() + 1;
            user.setInviteCount(newCount);
            userMapper.updateById(user);
        }
    }

    /**
     * 增加参与邀约次数
     */
    public void incrementParticipateCount(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            int newCount = user.getParticipantCountOrDefault() + 1;
            user.setParticipateCount(newCount);
            userMapper.updateById(user);
        }
    }

    /**
     * 减少参与邀约次数
     */
    public void decrementParticipateCount(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null && user.getParticipantCountOrDefault() > 0) {
            user.setParticipateCount(user.getParticipantCountOrDefault() - 1);
            userMapper.updateById(user);
        }
    }
}
