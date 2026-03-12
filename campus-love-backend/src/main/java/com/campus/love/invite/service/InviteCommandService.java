package com.campus.love.invite.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.love.auth.security.CurrentUser;
import com.campus.love.chat.entity.ChatGroup;
import com.campus.love.chat.service.ChatGroupService;
import com.campus.love.chat.service.ChatService;
import com.campus.love.common.constants.DateTimeConstants;
import com.campus.love.common.constants.InviteCreditConstants;
import com.campus.love.common.enums.ActivityTypeEnum;
import com.campus.love.common.enums.MsgTypeEnum;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.common.utils.TimeParseUtil;
import com.campus.love.invite.dto.InviteCreateRequest;
import com.campus.love.invite.entity.Invite;
import com.campus.love.invite.entity.InviteDecline;
import com.campus.love.invite.entity.InviteParticipant;
import com.campus.love.invite.enums.InviteModeEnum;
import com.campus.love.invite.enums.InviteStatusEnum;
import com.campus.love.invite.event.InviteEvent;
import com.campus.love.invite.mapper.InviteDeclineMapper;
import com.campus.love.invite.mapper.InviteMapper;
import com.campus.love.invite.mapper.InviteParticipantMapper;
import com.campus.love.user.service.ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 邀约命令服务：创建、加入、退出、取消、确认等写操作。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InviteCommandService {

    private final InviteMapper inviteMapper;
    private final InviteParticipantMapper participantMapper;
    private final InviteDeclineMapper declineMapper;
    private final InviteQueryService queryService;
    private final InviteCreditService creditService;
    private final ActivityService activityService;
    private final ChatService chatService;
    private final ChatGroupService chatGroupService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(rollbackFor = Exception.class)
    public Invite saveNewInvite(InviteCreateRequest request) {
        Long currentUserId = CurrentUser.getId();

        Invite invite = new Invite();
        invite.setCreatorId(currentUserId);
        invite.setInviteMode(request.getInviteMode());
        invite.setTargetUserId(request.getTargetUserId());
        invite.setInviteType(request.getInviteType());
        invite.setTitle(request.getTitle());
        invite.setContent(request.getContent());
        invite.setInvitePeriod(request.getInvitePeriod());
        invite.setPeriodConfig(request.getPeriodConfig());
        invite.setInviteTime(TimeParseUtil.parseUtcToLocalDateTime(request.getInviteTime()));
        invite.setInviteEndTime(request.getInviteEndTime() != null && !request.getInviteEndTime().isBlank()
                ? TimeParseUtil.parseUtcToLocalDateTime(request.getInviteEndTime()) : null);
        invite.setLocation(request.getLocation());
        if (InviteModeEnum.PRIVATE.name().equals(request.getInviteMode())) {
            invite.setMaxParticipants(1);
        } else {
            invite.setMaxParticipants(request.getMaxParticipants());
        }
        invite.setStatus(InviteStatusEnum.RECRUITING.name());
        invite.setDeadlineHours(request.getDeadlineHours());
        invite.setAtmosphereTags(request.getAtmosphereTags());
        invite.setIsUrgent(request.getIsUrgent() != null && request.getIsUrgent());
        invite.setDeleted(false);

        if (InviteModeEnum.PUBLIC.name().equals(request.getInviteMode())) {
            invite.setParticipantCount(1);
        } else {
            invite.setParticipantCount(0);
        }

        inviteMapper.insert(invite);

        if (InviteModeEnum.PUBLIC.name().equals(request.getInviteMode())) {
            InviteParticipant creatorParticipant = new InviteParticipant();
            creatorParticipant.setInviteId(invite.getId());
            creatorParticipant.setUserId(currentUserId);
            creatorParticipant.setJoinAt(LocalDateTime.now());
            participantMapper.insert(creatorParticipant);
        }

        if (InviteModeEnum.PUBLIC.name().equals(invite.getInviteMode())
                || InviteModeEnum.PRIVATE.name().equals(invite.getInviteMode())) {
            ChatGroup group = chatGroupService.createGroupIfAbsent(invite);
            invite.setChatGroupId(group.getId());
            inviteMapper.updateById(invite);
        }

        return invite;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false)
    public void ensureChatGroupForInvite(Long inviteId) {
        Invite invite = queryService.getInviteOrThrow(inviteId);
        if (!InviteModeEnum.PUBLIC.name().equals(invite.getInviteMode())
                && !InviteModeEnum.PRIVATE.name().equals(invite.getInviteMode())) {
            return;
        }
        if (invite.getChatGroupId() != null) {
            return;
        }
        ChatGroup group = chatGroupService.createGroupIfAbsent(invite);
        invite.setChatGroupId(group.getId());
        inviteMapper.updateById(invite);
        List<InviteParticipant> participants = participantMapper.selectList(
                new LambdaQueryWrapper<InviteParticipant>().eq(InviteParticipant::getInviteId, inviteId));
        for (InviteParticipant p : participants) {
            if (p.getUserId() != null) {
                chatGroupService.addMemberIfAbsent(group.getId(), p.getUserId());
            }
        }
    }

    @Transactional
    public void joinInvite(Long inviteId) {
        Long currentUserId = CurrentUser.getId();

        creditService.checkUserCredit(currentUserId, InviteCreditConstants.CREDIT_JOIN_THRESHOLD);
        creditService.checkParticipateLimit(currentUserId);

        Invite invite = queryService.getInviteOrThrow(inviteId);

        if (!InviteStatusEnum.RECRUITING.name().equals(invite.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "邀约不在招募中");
        }

        InviteParticipant existing = participantMapper.selectOne(
                new LambdaQueryWrapper<InviteParticipant>()
                        .eq(InviteParticipant::getInviteId, inviteId)
                        .eq(InviteParticipant::getUserId, currentUserId));
        if (existing != null && existing.getLeftAt() == null) {
            throw new BusinessException(ResultCode.ALREADY_JOINED);
        }
        if (existing != null && existing.getLeftAt() != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "您已退出该邀约，无法再次加入，如需参与请联系发起人");
        }

        if (invite.getMaxParticipants() != null && invite.getParticipantCount() >= invite.getMaxParticipants()) {
            throw new BusinessException(ResultCode.INVITE_FULL);
        }

        if (InviteModeEnum.PRIVATE.name().equals(invite.getInviteMode())) {
            if (!currentUserId.equals(invite.getTargetUserId())) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "不是受邀目标用户");
            }
        }

        InviteParticipant participant = new InviteParticipant();
        participant.setInviteId(inviteId);
        participant.setUserId(currentUserId);
        participant.setJoinAt(LocalDateTime.now());
        participantMapper.insert(participant);

        invite.setParticipantCount(invite.getParticipantCount() + 1);
        if (invite.getMaxParticipants() != null && invite.getParticipantCount() >= invite.getMaxParticipants()) {
            invite.setStatus(InviteStatusEnum.FULL.name());
        }
        inviteMapper.updateById(invite);

        creditService.incrementParticipateCount(currentUserId);

        activityService.recordActivity(ActivityTypeEnum.INVITE_JOIN, inviteId);

        eventPublisher.publishEvent(InviteEvent.joinSuccess(invite, currentUserId));
        if (!currentUserId.equals(invite.getCreatorId())) {
            eventPublisher.publishEvent(InviteEvent.newParticipant(invite, invite.getCreatorId(), currentUserId));
        }

        if (InviteModeEnum.PUBLIC.name().equals(invite.getInviteMode())
                || InviteModeEnum.PRIVATE.name().equals(invite.getInviteMode())) {
            ChatGroup group = chatGroupService.createGroupIfAbsent(invite);
            invite.setChatGroupId(group.getId());
            inviteMapper.updateById(invite);
            chatGroupService.addMemberIfAbsent(group.getId(), invite.getCreatorId());
            chatGroupService.addMemberIfAbsent(group.getId(), currentUserId);
        }
    }

    @Transactional
    public void leaveInvite(Long inviteId) {
        Long currentUserId = CurrentUser.getId();

        Invite invite = queryService.getInviteOrThrow(inviteId);

        InviteParticipant participant = participantMapper.selectOne(
                new LambdaQueryWrapper<InviteParticipant>()
                        .eq(InviteParticipant::getInviteId, inviteId)
                        .eq(InviteParticipant::getUserId, currentUserId));
        if (participant == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "未加入该邀约");
        }

        if (InviteStatusEnum.IN_PROGRESS.name().equals(invite.getStatus()) ||
                InviteStatusEnum.ENDED.name().equals(invite.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "邀约进行中或已结束，无法退出");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime inviteTime = invite.getInviteTime();
        if (inviteTime != null) {
            if (now.isAfter(inviteTime)) {
                creditService.adjustCreditScore(currentUserId, InviteCreditConstants.CREDIT_NO_SHOW);
            } else if (!now.isBefore(inviteTime.minusHours(InviteCreditConstants.TEMP_CANCEL_HOURS))) {
                creditService.adjustCreditScore(currentUserId, InviteCreditConstants.CREDIT_TEMP_CANCEL);
            }
        }

        participant.setLeftAt(LocalDateTime.now());
        participantMapper.updateById(participant);

        invite.setParticipantCount(invite.getParticipantCount() - 1);
        if (InviteStatusEnum.FULL.name().equals(invite.getStatus()) &&
                invite.getMaxParticipants() != null &&
                invite.getParticipantCount() < invite.getMaxParticipants()) {
            invite.setStatus(InviteStatusEnum.RECRUITING.name());
        }
        inviteMapper.updateById(invite);

        creditService.decrementParticipateCount(currentUserId);

        if (!currentUserId.equals(invite.getCreatorId())) {
            eventPublisher.publishEvent(InviteEvent.participantLeave(invite, currentUserId, invite.getCreatorId()));
        }
    }

    /**
     * 发起人踢出参与者（必须填写至少10字理由）
     */
    @Transactional
    public void kickParticipant(Long inviteId, Long targetUserId, String reason) {
        Long currentUserId = CurrentUser.getId();

        if (reason == null || reason.trim().length() < 10) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "踢人理由至少10个字");
        }

        Invite invite = queryService.getInviteOrThrow(inviteId);

        if (!invite.getCreatorId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅发起人可踢出参与者");
        }

        if (currentUserId.equals(targetUserId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能踢出自己");
        }

        InviteParticipant participant = participantMapper.selectOne(
                new LambdaQueryWrapper<InviteParticipant>()
                        .eq(InviteParticipant::getInviteId, inviteId)
                        .eq(InviteParticipant::getUserId, targetUserId));
        if (participant == null || participant.getLeftAt() != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该用户不是当前参与者或已退出");
        }

        if (InviteStatusEnum.IN_PROGRESS.name().equals(invite.getStatus())
                || InviteStatusEnum.ENDED.name().equals(invite.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "邀约进行中或已结束，无法踢人");
        }

        participant.setLeftAt(LocalDateTime.now());
        participant.setLeftReason(reason.trim());
        participantMapper.updateById(participant);

        invite.setParticipantCount(invite.getParticipantCount() - 1);
        boolean wasFull = InviteStatusEnum.FULL.name().equals(invite.getStatus())
                || InviteStatusEnum.CONFIRMED.name().equals(invite.getStatus());
        if (wasFull && invite.getMaxParticipants() != null
                && invite.getParticipantCount() < invite.getMaxParticipants()) {
            invite.setStatus(InviteStatusEnum.RECRUITING.name());
        }
        inviteMapper.updateById(invite);

        creditService.decrementParticipateCount(targetUserId);

        if (invite.getChatGroupId() != null) {
            chatGroupService.removeMember(invite.getChatGroupId(), targetUserId);
        }

        eventPublisher.publishEvent(InviteEvent.participantKicked(invite, currentUserId, targetUserId, reason.trim()));
    }

    @Transactional
    public void declineInvite(Long inviteId) {
        Long currentUserId = CurrentUser.getId();
        Invite invite = queryService.getInviteOrThrow(inviteId);
        if (!Long.valueOf(currentUserId).equals(invite.getTargetUserId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅被邀约方可拒绝");
        }
        InviteParticipant existing = participantMapper.selectOne(
                new LambdaQueryWrapper<InviteParticipant>()
                        .eq(InviteParticipant::getInviteId, inviteId)
                        .eq(InviteParticipant::getUserId, currentUserId));
        if (existing != null && existing.getLeftAt() == null) {
            throw new BusinessException(ResultCode.ALREADY_JOINED, "已加入该邀约，请使用退出");
        }
        long count = declineMapper.selectCount(
                new LambdaQueryWrapper<InviteDecline>()
                        .eq(InviteDecline::getInviteId, inviteId)
                        .eq(InviteDecline::getUserId, currentUserId));
        if (count > 0) return;
        InviteDecline decline = new InviteDecline();
        decline.setInviteId(inviteId);
        decline.setUserId(currentUserId);
        declineMapper.insert(decline);
    }

    @Transactional
    public void approveRejoin(Long inviteId, Long applicantUserId) {
        Invite invite = queryService.getInviteOrThrow(inviteId);
        if (!InviteStatusEnum.RECRUITING.name().equals(invite.getStatus()) && !InviteStatusEnum.FULL.name().equals(invite.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "当前邀约状态不允许再次加入");
        }
        InviteParticipant participant = participantMapper.selectOne(
                new LambdaQueryWrapper<InviteParticipant>()
                        .eq(InviteParticipant::getInviteId, inviteId)
                        .eq(InviteParticipant::getUserId, applicantUserId));
        if (participant == null || participant.getLeftAt() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该用户未退出或不存在参与记录");
        }
        if (invite.getMaxParticipants() != null && invite.getParticipantCount() >= invite.getMaxParticipants()) {
            throw new BusinessException(ResultCode.INVITE_FULL, "邀约人数已满");
        }

        LocalDateTime now = LocalDateTime.now();
        participantMapper.update(null,
                new LambdaUpdateWrapper<InviteParticipant>()
                        .eq(InviteParticipant::getId, participant.getId())
                        .set(InviteParticipant::getLeftAt, null)
                        .set(InviteParticipant::getJoinAt, now));

        invite.setParticipantCount(invite.getParticipantCount() + 1);
        if (invite.getMaxParticipants() != null && invite.getParticipantCount() >= invite.getMaxParticipants()) {
            invite.setStatus(InviteStatusEnum.FULL.name());
        }
        inviteMapper.updateById(invite);

        if (InviteModeEnum.PUBLIC.name().equals(invite.getInviteMode())
                || InviteModeEnum.PRIVATE.name().equals(invite.getInviteMode())) {
            ChatGroup group = chatGroupService.createGroupIfAbsent(invite);
            invite.setChatGroupId(group.getId());
            inviteMapper.updateById(invite);
            chatGroupService.addMemberIfAbsent(group.getId(), invite.getCreatorId());
            chatGroupService.addMemberIfAbsent(group.getId(), applicantUserId);
        }

        eventPublisher.publishEvent(InviteEvent.joinSuccess(invite, applicantUserId));
        if (!applicantUserId.equals(invite.getCreatorId())) {
            eventPublisher.publishEvent(InviteEvent.newParticipant(invite, invite.getCreatorId(), applicantUserId));
        }
    }

    @Transactional
    public void cancelInvite(Long inviteId, String reason) {
        Long currentUserId = CurrentUser.getId();

        Invite invite = queryService.getInviteOrThrow(inviteId);

        if (!invite.getCreatorId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只有发起人可以取消邀约");
        }

        if (!InviteStatusEnum.RECRUITING.name().equals(invite.getStatus()) &&
                !InviteStatusEnum.FULL.name().equals(invite.getStatus()) &&
                !InviteStatusEnum.CONFIRMED.name().equals(invite.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "邀约已进行中或已结束，无法取消");
        }

        LocalDateTime inviteTime = invite.getInviteTime();
        if (inviteTime != null) {
            boolean hasReason = reason != null && !reason.isBlank();
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(inviteTime)) {
                creditService.adjustCreditScore(currentUserId, InviteCreditConstants.CREDIT_NO_SHOW);
            } else if (!now.isBefore(inviteTime.minusHours(InviteCreditConstants.TEMP_CANCEL_HOURS)) && !hasReason) {
                creditService.adjustCreditScore(currentUserId, InviteCreditConstants.CREDIT_TEMP_CANCEL);
            }
        }

        List<InviteParticipant> participants = participantMapper.selectList(
                new LambdaQueryWrapper<InviteParticipant>()
                        .eq(InviteParticipant::getInviteId, inviteId));

        invite.setStatus(InviteStatusEnum.CANCELLED.name());
        inviteMapper.updateById(invite);

        if (!participants.isEmpty()) {
            List<Long> participantIds = participants.stream()
                    .map(InviteParticipant::getUserId)
                    .collect(Collectors.toList());
            eventPublisher.publishEvent(InviteEvent.inviteCancelled(invite, currentUserId, participantIds, reason));
        }
    }

    public void sendPrivateInviteMessage(Long senderId, Invite invite) {
        if (invite.getTargetUserId() == null) {
            return;
        }
        try {
            String timeStr = invite.getInviteTime() != null
                    ? invite.getInviteTime().format(DateTimeConstants.TIME_FMT)
                    : "";
            StringBuilder content = new StringBuilder();
            content.append("邀约邀请：").append(invite.getTitle());
            if (!timeStr.isEmpty()) {
                content.append("｜时间 ").append(timeStr);
            }
            if (invite.getLocation() != null && !invite.getLocation().isEmpty()) {
                content.append("｜地点 ").append(invite.getLocation());
            }
            content.append("｜INVITE#").append(invite.getId());
            chatService.sendMessage(senderId, invite.getTargetUserId(), content.toString(), MsgTypeEnum.INVITE.getCode());
        } catch (Exception e) {
            log.warn("发送邀约聊天消息失败, inviteId={}", invite.getId(), e);
        }
    }

    @Transactional
    public void confirmParticipants(Long inviteId, List<Long> userIds) {
        Long currentUserId = CurrentUser.getId();
        if (userIds == null) {
            userIds = Collections.emptyList();
        }

        Invite invite = queryService.getInviteOrThrow(inviteId);

        if (!invite.getCreatorId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只有发起人可以确认参与者");
        }

        if (!InviteStatusEnum.FULL.name().equals(invite.getStatus()) &&
                !InviteStatusEnum.CONFIRMED.name().equals(invite.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "邀约状态不正确");
        }

        invite.setStatus(InviteStatusEnum.CONFIRMED.name());
        inviteMapper.updateById(invite);
    }
}
