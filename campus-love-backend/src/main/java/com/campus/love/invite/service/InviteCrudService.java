package com.campus.love.invite.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.love.invite.dto.InviteCreateRequest;
import com.campus.love.invite.dto.InviteResponse;
import com.campus.love.invite.dto.InviteTypeCountResponse;
import com.campus.love.invite.entity.Invite;
import com.campus.love.invite.entity.InviteParticipant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 邀约 CRUD 门面：对外保持原有 API，内部委托给 InviteQueryService、InviteCommandService。
 * 供 InviteService、InviteStatsService 使用。
 */
@Service
@RequiredArgsConstructor
public class InviteCrudService {

    private final InviteQueryService queryService;
    private final InviteCommandService commandService;

    public Invite getInviteOrThrow(Long inviteId) {
        return queryService.getInviteOrThrow(inviteId);
    }

    public InviteParticipant findParticipant(Long inviteId, Long userId) {
        return queryService.findParticipant(inviteId, userId);
    }

    public LocalDateTime getHistoryStartTime(String range) {
        return queryService.getHistoryStartTime(range);
    }

    @Transactional(rollbackFor = Exception.class)
    public Invite saveNewInvite(InviteCreateRequest request) {
        return commandService.saveNewInvite(request);
    }

    @Transactional(readOnly = true)
    public IPage<InviteResponse> getInviteList(String type, String status, String timeRange, Integer page, Integer size) {
        return queryService.getInviteList(type, status, timeRange, page, size);
    }

    @Transactional(readOnly = true)
    public List<InviteResponse> getRecommendInvites(Integer limit) {
        return queryService.getRecommendInvites(limit);
    }

    @Transactional(readOnly = true)
    public List<InviteResponse> getMyCreatedInvites(String range) {
        return queryService.getMyCreatedInvites(range);
    }

    @Transactional(readOnly = true)
    public List<InviteResponse> getMyJoinedInvites(String range) {
        return queryService.getMyJoinedInvites(range);
    }

    @Transactional(readOnly = true)
    public List<InviteResponse> getReceivedPendingInvites(String range) {
        return queryService.getReceivedPendingInvites(range);
    }

    @Transactional(readOnly = true)
    public List<InviteResponse> getMyInvitesList(String range) {
        return queryService.getMyInvitesList(range);
    }

    public void ensureChatGroupForInvite(Long inviteId) {
        commandService.ensureChatGroupForInvite(inviteId);
    }

    @Transactional(readOnly = true)
    public InviteResponse getInviteDetail(Long inviteId) {
        return queryService.getInviteDetail(inviteId);
    }

    @Transactional(readOnly = true)
    public List<InviteTypeCountResponse> getHotInviteTypeCounts(Integer limit) {
        return queryService.getHotInviteTypeCounts(limit);
    }

    @Transactional
    public void joinInvite(Long inviteId) {
        commandService.joinInvite(inviteId);
    }

    @Transactional
    public void leaveInvite(Long inviteId) {
        commandService.leaveInvite(inviteId);
    }

    @Transactional
    public void declineInvite(Long inviteId) {
        commandService.declineInvite(inviteId);
    }

    @Transactional
    public void approveRejoin(Long inviteId, Long applicantUserId) {
        commandService.approveRejoin(inviteId, applicantUserId);
    }

    @Transactional
    public void cancelInvite(Long inviteId, String reason) {
        commandService.cancelInvite(inviteId, reason);
    }

    public void sendPrivateInviteMessage(Long senderId, Invite invite) {
        commandService.sendPrivateInviteMessage(senderId, invite);
    }

    @Transactional
    public void confirmParticipants(Long inviteId, List<Long> userIds) {
        commandService.confirmParticipants(inviteId, userIds);
    }
}
