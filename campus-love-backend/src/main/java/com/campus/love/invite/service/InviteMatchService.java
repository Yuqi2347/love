package com.campus.love.invite.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.common.constants.InviteCreditConstants;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.common.utils.TimeParseUtil;
import com.campus.love.invite.dto.InviteWaitCreateRequest;
import com.campus.love.invite.dto.InviteWaitResponse;
import com.campus.love.invite.entity.Invite;
import com.campus.love.invite.entity.InviteParticipant;
import com.campus.love.invite.entity.InviteWait;
import com.campus.love.invite.enums.InviteModeEnum;
import com.campus.love.invite.enums.InviteStatusEnum;
import com.campus.love.invite.event.InviteEvent;
import com.campus.love.invite.mapper.InviteMapper;
import com.campus.love.invite.mapper.InviteParticipantMapper;
import com.campus.love.invite.mapper.InviteWaitMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 等待邀约的 CRUD 与「邀约-等待」匹配、自动加入逻辑。
 */
@Service
@RequiredArgsConstructor
public class InviteMatchService {

    private final InviteWaitMapper waitMapper;
    private final InviteMapper inviteMapper;
    private final InviteParticipantMapper participantMapper;
    private final InviteCreditService creditService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 创建等待邀约；若 autoAccept 为 true 则尝试匹配现有公开邀约并自动加入。
     */
    @Transactional
    public Long createInviteWait(InviteWaitCreateRequest request) {
        Long currentUserId = com.campus.love.auth.security.CurrentUser.getId();

        InviteWait wait = new InviteWait();
        wait.setUserId(currentUserId);
        wait.setInviteTypes(String.join(",", request.getInviteTypes()));
        wait.setPeriodConfig(request.getPeriodConfig());
        wait.setLocationPref(request.getLocationPref());
        wait.setAutoAccept(request.getAutoAccept() != null && request.getAutoAccept());
        wait.setExpireHours(request.getExpireHours());

        waitMapper.insert(wait);

        if (Boolean.TRUE.equals(wait.getAutoAccept())) {
            matchExistingInvitesForWait(wait);
        }

        return wait.getId();
    }

    /**
     * 获取当前用户的等待邀约列表
     */
    public List<InviteWaitResponse> getMyInviteWaits() {
        Long currentUserId = com.campus.love.auth.security.CurrentUser.getId();

        List<InviteWait> waits = waitMapper.selectList(
                new LambdaQueryWrapper<InviteWait>()
                        .eq(InviteWait::getUserId, currentUserId)
                        .orderByDesc(InviteWait::getCreatedAt));

        return waits.stream().map(wait -> {
            LocalDateTime createdAt = wait.getCreatedAt() != null ? wait.getCreatedAt() : LocalDateTime.now();
            int expireHours = wait.getExpireHours() != null ? wait.getExpireHours() : 0;
            LocalDateTime expireTime = createdAt.plusHours(expireHours);

            return InviteWaitResponse.builder()
                    .id(wait.getId())
                    .inviteTypes(wait.getInviteTypes())
                    .periodConfig(wait.getPeriodConfig())
                    .locationPref(wait.getLocationPref())
                    .autoAccept(wait.getAutoAccept())
                    .expireHours(wait.getExpireHours())
                    .createdAt(wait.getCreatedAt())
                    .isExpired(LocalDateTime.now().isAfter(expireTime))
                    .expireTime(expireTime)
                    .build();
        }).toList();
    }

    /**
     * 取消等待邀约
     */
    @Transactional
    public void cancelInviteWait(Long waitId) {
        Long currentUserId = com.campus.love.auth.security.CurrentUser.getId();

        InviteWait wait = waitMapper.selectById(waitId);
        if (wait == null || !wait.getUserId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "等待邀约不存在");
        }

        waitMapper.deleteById(waitId);
    }

    /**
     * 邀约创建后，匹配等待邀约用户并自动加入（仅公开邀约调用）。
     * 匹配条件：类型、时间、未过期、人数未满；autoAccept=true 时执行自动加入。
     */
    public void matchAndAutoJoinWaitUsers(Invite invite) {
        if (invite == null || !InviteModeEnum.PUBLIC.name().equals(invite.getInviteMode())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();

        List<InviteWait> waits = waitMapper.selectList(
                new LambdaQueryWrapper<InviteWait>()
                        .ne(InviteWait::getUserId, invite.getCreatorId()));

        if (waits.isEmpty()) {
            return;
        }

        for (InviteWait wait : waits) {
            if (wait.getCreatedAt() != null && wait.getExpireHours() != null) {
                LocalDateTime expireTime = wait.getCreatedAt().plusHours(wait.getExpireHours());
                if (now.isAfter(expireTime)) {
                    continue;
                }
            }
            if (!typeMatches(wait.getInviteTypes(), invite.getInviteType())) {
                continue;
            }
            if (!timeMatches(wait.getPeriodConfig(), invite.getInviteTime())) {
                continue;
            }
            if (invite.getMaxParticipants() != null &&
                    invite.getParticipantCount() >= invite.getMaxParticipants()) {
                break;
            }

            Long userId = wait.getUserId();
            if (userId == null) {
                continue;
            }

            if (Boolean.TRUE.equals(wait.getAutoAccept())) {
                InviteParticipant existing = participantMapper.selectOne(
                        new LambdaQueryWrapper<InviteParticipant>()
                                .eq(InviteParticipant::getInviteId, invite.getId())
                                .eq(InviteParticipant::getUserId, userId));
                if (existing != null) {
                    continue;
                }
                try {
                    doAutoJoinInvite(invite, userId);
                } catch (BusinessException e) {
                    if (!Objects.equals(e.getResultCode(), ResultCode.CREDIT_TOO_LOW)) {
                        throw e;
                    }
                    continue;
                }
                if (invite.getMaxParticipants() != null &&
                        invite.getParticipantCount() >= invite.getMaxParticipants()) {
                    break;
                }
            } else {
                eventPublisher.publishEvent(InviteEvent.waitMatch(invite, userId));
            }
        }
    }

    /**
     * 执行一次自动加入：校验信用分、插入参与者、更新邀约人数与状态、增加用户参与统计。
     */
    public void doAutoJoinInvite(Invite invite, Long userId) {
        creditService.checkUserCredit(userId, InviteCreditConstants.CREDIT_JOIN_THRESHOLD);
        creditService.checkParticipateLimit(userId, invite != null ? invite.getInviteMode() : null);
        InviteParticipant participant = new InviteParticipant();
        participant.setInviteId(invite.getId());
        participant.setUserId(userId);
        participantMapper.insert(participant);
        invite.setParticipantCount(invite.getParticipantCount() + 1);
        if (invite.getMaxParticipants() != null &&
                invite.getParticipantCount() >= invite.getMaxParticipants()) {
            invite.setStatus(InviteStatusEnum.FULL.name());
        }
        inviteMapper.updateById(invite);
        creditService.incrementParticipateCount(userId);
    }

    /**
     * 创建等待邀约后，匹配当前已存在的公开邀约并自动加入（仅当 autoAccept 时调用）
     */
    public void matchExistingInvitesForWait(InviteWait wait) {
        Long userId = wait.getUserId();
        if (userId == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();

        List<Invite> invites = inviteMapper.selectList(
                new LambdaQueryWrapper<Invite>()
                        .eq(Invite::getDeleted, false)
                        .eq(Invite::getInviteMode, InviteModeEnum.PUBLIC.name())
                        .eq(Invite::getStatus, InviteStatusEnum.RECRUITING.name()));

        for (Invite invite : invites) {
            if (invite.getInviteTime() != null && invite.getInviteTime().isBefore(now)) {
                continue;
            }
            if (!typeMatches(wait.getInviteTypes(), invite.getInviteType())) {
                continue;
            }
            if (!timeMatches(wait.getPeriodConfig(), invite.getInviteTime())) {
                continue;
            }
            if (invite.getMaxParticipants() != null &&
                    invite.getParticipantCount() >= invite.getMaxParticipants()) {
                continue;
            }

            if (Boolean.TRUE.equals(wait.getAutoAccept())) {
                InviteParticipant existing = participantMapper.selectOne(
                        new LambdaQueryWrapper<InviteParticipant>()
                                .eq(InviteParticipant::getInviteId, invite.getId())
                                .eq(InviteParticipant::getUserId, userId));
                if (existing != null) {
                    continue;
                }
                try {
                    doAutoJoinInvite(invite, userId);
                } catch (BusinessException e) {
                    if (!Objects.equals(e.getResultCode(), ResultCode.CREDIT_TOO_LOW)) {
                        throw e;
                    }
                    continue;
                }
                break;
            } else {
                eventPublisher.publishEvent(InviteEvent.waitMatch(invite, userId));
            }
        }
    }

    private static boolean typeMatches(String inviteTypes, String inviteType) {
        if (inviteTypes == null || inviteType == null) {
            return false;
        }
        for (String part : inviteTypes.split(",")) {
            if (inviteType.equals(part.trim())) {
                return true;
            }
        }
        return false;
    }

    private static boolean timeMatches(String periodConfig, LocalDateTime inviteTime) {
        if (inviteTime == null) {
            return false;
        }
        if (periodConfig == null || periodConfig.isEmpty()) {
            return true;
        }
        TimeParseUtil.PeriodBounds bounds = TimeParseUtil.parsePeriodConfigStartEnd(periodConfig);
        if (bounds == null) {
            return true;
        }
        return !inviteTime.isBefore(bounds.getStart()) && !inviteTime.isAfter(bounds.getEnd());
    }
}
