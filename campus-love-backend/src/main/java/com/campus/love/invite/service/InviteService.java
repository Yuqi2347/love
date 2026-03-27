package com.campus.love.invite.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.constants.InviteCreditConstants;
import com.campus.love.follow.service.FollowService;
import com.campus.love.common.enums.NotificationTypeEnum;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.invite.dto.*;
import com.campus.love.invite.entity.Invite;
import com.campus.love.invite.enums.InviteModeEnum;
import com.campus.love.notification.entity.Notification;
import com.campus.love.notification.service.NotificationService;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import com.campus.love.user.service.ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

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
    private final InviteActivityService inviteActivityService;
    private final FollowService followService;
    private final ActivityService activityService;
    private final NotificationService notificationService;
    private final UserMapper userMapper;

    @Transactional
    public Long createInvite(InviteCreateRequest request) {
        Long currentUserId = CurrentUser.getId();

        creditService.checkUserCredit(currentUserId, InviteCreditConstants.CREDIT_CREATE_THRESHOLD);
        creditService.checkInviteCreateLimit(currentUserId, request.getInviteMode());

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
    public IPage<InviteResponse> getInviteList(String type, String status, String timeRange, String keyword, Boolean publicOnly, Integer page, Integer size) {
        return crudService.getInviteList(type, status, timeRange, keyword, publicOnly, page, size, null);
    }

    @Transactional(readOnly = true)
    public IPage<InviteResponse> getInviteList(String type, String status, String timeRange, String keyword, Boolean publicOnly, Integer page, Integer size, String sort) {
        return crudService.getInviteList(type, status, timeRange, keyword, publicOnly, page, size, sort);
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

    /** 我的邀约列表：我发起的 + 我参与的（含已退出），按邀约时间倒序 */
    @Transactional(readOnly = true)
    public List<InviteResponse> getMyInvitesList(String range) {
        return crudService.getMyInvitesList(range);
    }

    /** 动态发帖：可选邀约列表（分页） */
    @Transactional(readOnly = true)
    public IPage<InviteResponse> pageInvitesForFeed(Integer page, Integer size) {
        return crudService.pageInvitesForFeed(page, size);
    }

    public void assertUserCanReferenceInviteInFeed(Long userId, Long inviteId) {
        crudService.assertUserCanReferenceInviteInFeed(userId, inviteId);
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
    public void declineInvite(Long inviteId) {
        crudService.declineInvite(inviteId);
    }

    @Transactional
    public void cancelInvite(Long inviteId, String reason) {
        crudService.cancelInvite(inviteId, reason);
    }

    @Transactional
    public void kickParticipant(Long inviteId, Long targetUserId, String reason) {
        crudService.kickParticipant(inviteId, targetUserId, reason);
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

    @Transactional(readOnly = true)
    public int getNewInviteActivityCount(Long userId) {
        return inviteActivityService.getNewInviteActivityCount(userId);
    }

    @Transactional
    public void markInviteActivityViewed(Long userId) {
        inviteActivityService.markInviteActivityViewed(userId);
    }

    /** 已退出用户申请再次加入：向发起人发送一条待处理申请通知，若已有未读申请则不重复创建 */
    @Transactional
    public void requestRejoin(Long inviteId) {
        Long currentUserId = CurrentUser.getId();
        Invite invite = crudService.getInviteOrThrow(inviteId);
        com.campus.love.invite.entity.InviteParticipant participant = crudService.findParticipant(inviteId, currentUserId);
        if (participant == null || participant.getLeftAt() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "您未退出该邀约或未参与过该邀约");
        }
        if (notificationService.hasUnreadRejoinRequest(inviteId, currentUserId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "您已提交过再次加入申请，请等待发起人处理");
        }
        String title = "有人申请再次加入邀约";
        String content = "有用户申请再次加入你的邀约：「" + invite.getTitle() + "」，请到邀约详情中同意或拒绝。";
        notificationService.createNotification(invite.getCreatorId(), currentUserId, inviteId,
                NotificationTypeEnum.INVITE_REJOIN_REQUEST, title, content);
    }

    /** 发起人：获取该邀约下待处理的再次加入申请列表 */
    @Transactional(readOnly = true)
    public List<InviteRejoinRequestItem> getRejoinRequests(Long inviteId) {
        Long currentUserId = CurrentUser.getId();
        Invite invite = crudService.getInviteOrThrow(inviteId);
        if (!invite.getCreatorId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅发起人可查看再次加入申请");
        }
        List<Notification> notifications = notificationService.getUnreadRejoinRequestsForInvite(inviteId, currentUserId);
        if (notifications.isEmpty()) {
            return List.of();
        }
        List<Long> senderIds = notifications.stream()
                .map(Notification::getSenderId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> userMap = senderIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(senderIds).stream()
                        .filter(u -> u != null)
                        .collect(toMap(User::getId, u -> u, (a, b) -> a));
        return notifications.stream()
                .map(n -> {
                    User u = userMap.get(n.getSenderId());
                    return InviteRejoinRequestItem.builder()
                            .userId(n.getSenderId())
                            .nickname(u != null ? u.getNickname() : null)
                            .avatarUrl(u != null ? u.getAvatarUrl() : null)
                            .requestedAt(n.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /** 发起人同意某人再次加入 */
    @Transactional
    public void approveRejoin(Long inviteId, Long applicantUserId) {
        Long currentUserId = CurrentUser.getId();
        Invite invite = crudService.getInviteOrThrow(inviteId);
        if (!invite.getCreatorId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅发起人可同意再次加入");
        }
        crudService.approveRejoin(inviteId, applicantUserId);
        notificationService.markRejoinRequestsAsRead(inviteId, applicantUserId);
    }

    /** 发起人拒绝某人再次加入（仅将对应申请通知标为已读） */
    @Transactional
    public void rejectRejoin(Long inviteId, Long applicantUserId) {
        Long currentUserId = CurrentUser.getId();
        Invite invite = crudService.getInviteOrThrow(inviteId);
        if (!invite.getCreatorId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅发起人可拒绝再次加入");
        }
        notificationService.markRejoinRequestsAsRead(inviteId, applicantUserId);
    }
}
