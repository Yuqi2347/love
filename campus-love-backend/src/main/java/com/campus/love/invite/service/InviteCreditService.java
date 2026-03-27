package com.campus.love.invite.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.common.constants.InviteCreditConstants;
import com.campus.love.common.constants.InviteLevelLimit;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.invite.entity.Invite;
import com.campus.love.invite.entity.InviteParticipant;
import com.campus.love.invite.enums.InviteModeEnum;
import com.campus.love.invite.enums.InviteStatusEnum;
import com.campus.love.invite.mapper.InviteMapper;
import com.campus.love.invite.mapper.InviteParticipantMapper;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 邀约信用分、等级限制与用户邀约统计（发起/参与次数）的校验与更新。
 */
@Service
@RequiredArgsConstructor
public class InviteCreditService {

    private static final List<String> ACTIVE_INVITE_STATUSES = List.of(
            InviteStatusEnum.RECRUITING.name(),
            InviteStatusEnum.FULL.name(),
            InviteStatusEnum.CONFIRMED.name(),
            InviteStatusEnum.IN_PROGRESS.name()
    );

    private final UserMapper userMapper;
    private final InviteMapper inviteMapper;
    private final InviteParticipantMapper participantMapper;

    private record ActiveInviteCounts(int publicCount, int privateCount) {
        int countForMode(String inviteMode) {
            return InviteModeEnum.PRIVATE.name().equals(inviteMode) ? privateCount : publicCount;
        }

        int total() {
            return publicCount + privateCount;
        }
    }

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
    public void checkInviteCreateLimit(Long userId, String inviteMode) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        String normalizedMode = normalizeInviteMode(inviteMode);
        InviteLevelLimit limit = resolveLevelLimit(user);

        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);

        long todayCount = inviteMapper.selectCount(
                new LambdaQueryWrapper<Invite>()
                        .eq(Invite::getCreatorId, userId)
                        .ge(Invite::getCreatedAt, todayStart)
                        .eq(Invite::getDeleted, false));

        if (todayCount >= limit.getDailyLimit()) {
            throw new BusinessException(ResultCode.INVITE_LIMIT_EXCEEDED);
        }

        ActiveInviteCounts activeCounts = countActiveInviteInvolvementByMode(userId);
        int modeLimit = resolveModeLimit(limit, normalizedMode);
        int modeCount = activeCounts.countForMode(normalizedMode);
        if (modeCount >= modeLimit) {
            throw new BusinessException(ResultCode.CONFLICT,
                    "当前最多可同时参与" + modeLimit + "场" + resolveModeLabel(normalizedMode) + "邀约");
        }
    }

    /** 兼容旧调用：默认按公共邀约限额校验。 */
    public void checkInviteCreateLimit(Long userId) {
        checkInviteCreateLimit(userId, InviteModeEnum.PUBLIC.name());
    }

    /**
     * 检查用户当前同时参与的邀约数量是否超过上限
     */
    public void checkParticipateLimit(Long userId, String inviteMode) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        String normalizedMode = normalizeInviteMode(inviteMode);
        InviteLevelLimit limit = resolveLevelLimit(user);
        ActiveInviteCounts activeCounts = countActiveInviteInvolvementByMode(userId);
        int modeLimit = resolveModeLimit(limit, normalizedMode);
        int modeCount = activeCounts.countForMode(normalizedMode);
        if (modeCount >= modeLimit) {
            throw new BusinessException(ResultCode.PARTICIPATE_LIMIT_EXCEEDED,
                    "当前最多可同时参与" + modeLimit + "场" + resolveModeLabel(normalizedMode) + "邀约");
        }
    }

    /** 兼容旧调用：默认按公共邀约限额校验。 */
    public void checkParticipateLimit(Long userId) {
        checkParticipateLimit(userId, InviteModeEnum.PUBLIC.name());
    }

    private InviteLevelLimit resolveLevelLimit(User user) {
        int level = user.getUserLevel() != null ? user.getUserLevel() : 1;
        return InviteLevelLimit.fromLevel(level);
    }

    private String normalizeInviteMode(String inviteMode) {
        if (InviteModeEnum.PRIVATE.name().equalsIgnoreCase(inviteMode)) {
            return InviteModeEnum.PRIVATE.name();
        }
        return InviteModeEnum.PUBLIC.name();
    }

    private int resolveModeLimit(InviteLevelLimit limit, String inviteMode) {
        if (InviteModeEnum.PRIVATE.name().equals(inviteMode)) {
            return limit.getPrivateConcurrentLimit();
        }
        return limit.getPublicConcurrentLimit();
    }

    private String resolveModeLabel(String inviteMode) {
        return InviteModeEnum.PRIVATE.name().equals(inviteMode) ? "私密" : "公共";
    }

    private static boolean isActiveInvite(Invite invite) {
        return invite != null
                && invite.getId() != null
                && !Boolean.TRUE.equals(invite.getDeleted())
                && ACTIVE_INVITE_STATUSES.contains(invite.getStatus());
    }

    private static boolean isPrivateInvite(Invite invite) {
        return invite != null && InviteModeEnum.PRIVATE.name().equals(invite.getInviteMode());
    }

    /**
     * 统计用户当前“同时参与”的邀约数（发起 + 参与，去重，并区分公共/私密）。
     */
    private ActiveInviteCounts countActiveInviteInvolvementByMode(Long userId) {
        Set<Long> activeInviteIds = new HashSet<>();
        int publicCount = 0;
        int privateCount = 0;

        List<Invite> activeCreatedInvites = inviteMapper.selectList(
                new LambdaQueryWrapper<Invite>()
                        .eq(Invite::getCreatorId, userId)
                        .eq(Invite::getDeleted, false)
                        .in(Invite::getStatus, ACTIVE_INVITE_STATUSES));
        if (activeCreatedInvites != null) {
            for (Invite invite : activeCreatedInvites) {
                if (!isActiveInvite(invite) || !activeInviteIds.add(invite.getId())) {
                    continue;
                }
                if (isPrivateInvite(invite)) {
                    privateCount++;
                } else {
                    publicCount++;
                }
            }
        }

        List<InviteParticipant> participants = participantMapper.selectList(
                new LambdaQueryWrapper<InviteParticipant>()
                        .eq(InviteParticipant::getUserId, userId));
        List<Long> participantInviteIds = participants.stream()
                .map(InviteParticipant::getInviteId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (!participantInviteIds.isEmpty()) {
            List<Invite> invites = inviteMapper.selectBatchIds(participantInviteIds);
            if (invites != null) {
                for (Invite invite : invites) {
                    if (!isActiveInvite(invite) || !activeInviteIds.add(invite.getId())) {
                        continue;
                    }
                    if (isPrivateInvite(invite)) {
                        privateCount++;
                    } else {
                        publicCount++;
                    }
                }
            }
        }

        return new ActiveInviteCounts(publicCount, privateCount);
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
