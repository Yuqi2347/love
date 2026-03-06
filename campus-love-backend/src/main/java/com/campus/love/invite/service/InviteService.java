package com.campus.love.invite.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.constants.InviteCreditConstants;
import com.campus.love.follow.service.FollowService;
import com.campus.love.invite.dto.*;
import com.campus.love.invite.entity.Invite;
import com.campus.love.invite.enums.InviteModeEnum;
import com.campus.love.user.service.ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 邀约门面：对外保持原有 API，内部委托给 InviteCrudService、InviteMatchService、InviteStatsService、InviteCreditService。
 * Controller 仅依赖本类，后续迭代时新逻辑按职责放入对应子 Service，避免单类继续膨胀。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InviteService {

    private final InviteCrudService crudService;
    private final InviteMatchService matchService;
    private final InviteStatsService statsService;
    private final InviteCreditService creditService;
    private final FollowService followService;
    private final ActivityService activityService;

    @Transactional
    public Long createInvite(InviteCreateRequest request) {
        Long currentUserId = CurrentUser.getId();

        creditService.checkUserCredit(currentUserId, InviteCreditConstants.CREDIT_CREATE_THRESHOLD);
        creditService.checkInviteCreateLimit(currentUserId);

        if (InviteModeEnum.PRIVATE.name().equals(request.getInviteMode())) {
            if (request.getTargetUserId() == null) {
                throw new com.campus.love.common.exception.BusinessException(
                        com.campus.love.common.result.ResultCode.BAD_REQUEST, "一对一邀约需要指定目标用户");
            }
            if (!followService.isMutual(currentUserId, request.getTargetUserId())) {
                throw new com.campus.love.common.exception.BusinessException(
                        com.campus.love.common.result.ResultCode.NOT_MUTUAL_FOLLOW);
            }
        }

        Invite invite = crudService.saveNewInvite(request);

        creditService.incrementInviteCount(currentUserId);

        if (InviteModeEnum.PUBLIC.name().equals(invite.getInviteMode())) {
            matchService.matchAndAutoJoinWaitUsers(invite);
        }

        activityService.recordActivity(com.campus.love.common.enums.ActivityTypeEnum.INVITE_CREATE, invite.getId());

        if (InviteModeEnum.PRIVATE.name().equals(invite.getInviteMode()) && invite.getTargetUserId() != null) {
            crudService.sendPrivateInviteMessage(currentUserId, invite);
        }

        return invite.getId();
    }

    @Transactional(readOnly = true)
    public IPage<InviteResponse> getInviteList(String type, String status, Integer page, Integer size) {
        return crudService.getInviteList(type, status, page, size);
    }

    @Transactional(readOnly = true)
    public List<InviteResponse> getRecommendInvites(Integer limit) {
        return crudService.getRecommendInvites(limit);
    }

    @Transactional(readOnly = true)
    public List<InviteTypeCountResponse> getHotInviteTypeCounts(Integer limit) {
        return crudService.getHotInviteTypeCounts(limit);
    }

    @Transactional(readOnly = true)
    public List<InviteResponse> getMyCreatedInvites(String range) {
        return crudService.getMyCreatedInvites(range);
    }

    @Transactional(readOnly = true)
    public List<InviteResponse> getMyJoinedInvites(String range) {
        return crudService.getMyJoinedInvites(range);
    }

    @Transactional(readOnly = true)
    public InviteResponse getInviteDetail(Long inviteId) {
        crudService.ensureChatGroupForInvite(inviteId);
        return crudService.getInviteDetail(inviteId);
    }

    @Transactional
    public void joinInvite(Long inviteId) {
        crudService.joinInvite(inviteId);
    }

    @Transactional
    public void leaveInvite(Long inviteId) {
        crudService.leaveInvite(inviteId);
    }

    @Transactional
    public void cancelInvite(Long inviteId, String reason) {
        crudService.cancelInvite(inviteId, reason);
    }

    @Transactional
    public void confirmParticipants(Long inviteId, List<Long> userIds) {
        crudService.confirmParticipants(inviteId, userIds);
    }

    @Transactional
    public Long createInviteWait(InviteWaitCreateRequest request) {
        return matchService.createInviteWait(request);
    }

    public List<InviteWaitResponse> getMyInviteWaits() {
        return matchService.getMyInviteWaits();
    }

    @Transactional
    public void cancelInviteWait(Long waitId) {
        matchService.cancelInviteWait(waitId);
    }

    @Transactional
    public void createRating(InviteRatingCreateRequest request) {
        statsService.createRating(request);
    }

    @Transactional(readOnly = true)
    public InviteStatsResponse getMyInviteStats() {
        return statsService.getMyInviteStats();
    }

    @Transactional(readOnly = true)
    public InviteStatsResponse getUserInviteStats(Long userId) {
        return statsService.getUserInviteStats(userId);
    }
}
