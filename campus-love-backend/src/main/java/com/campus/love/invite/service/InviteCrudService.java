package com.campus.love.invite.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.love.auth.security.CurrentUser;
import com.campus.love.chat.entity.ChatGroup;
import com.campus.love.chat.service.ChatService;
import com.campus.love.chat.service.ChatGroupService;
import com.campus.love.common.constants.DateTimeConstants;
import com.campus.love.common.constants.InviteCreditConstants;
import com.campus.love.common.enums.ActivityTypeEnum;
import com.campus.love.common.enums.MsgTypeEnum;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.common.utils.TimeParseUtil;
import com.campus.love.follow.service.FollowService;
import com.campus.love.invite.dto.*;
import com.campus.love.invite.entity.*;
import com.campus.love.invite.enums.InviteModeEnum;
import com.campus.love.invite.enums.InviteStatusEnum;
import com.campus.love.invite.event.InviteEvent;
import com.campus.love.invite.mapper.InviteMapper;
import com.campus.love.invite.mapper.InviteParticipantMapper;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import com.campus.love.user.service.ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

/**
 * 邀约 CRUD、列表/详情、加入/退出/取消/确认、响应构建。对外提供 getInviteOrThrow 供 Stats 等使用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InviteCrudService {

    private final InviteMapper inviteMapper;
    private final InviteParticipantMapper participantMapper;
    private final UserMapper userMapper;
    private final FollowService followService;
    private final ActivityService activityService;
    private final ChatService chatService;
    private final ChatGroupService chatGroupService;
    private final InviteCreditService creditService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 获取邀约（未删除），不存在则抛 INVITE_NOT_FOUND。供本包内 Stats 等调用。
     */
    public Invite getInviteOrThrow(Long inviteId) {
        Invite invite = inviteMapper.selectById(inviteId);
        if (invite == null || Boolean.TRUE.equals(invite.getDeleted())) {
            throw new BusinessException(ResultCode.INVITE_NOT_FOUND);
        }
        return invite;
    }

    /**
     * 仅构建并插入邀约记录，不包含信用校验、匹配、活动、私信。由门面在 createInvite 中编排后调用。
     */
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
        invite.setLocation(request.getLocation());
        if (InviteModeEnum.PRIVATE.name().equals(request.getInviteMode())) {
            invite.setMaxParticipants(1);
        } else {
            invite.setMaxParticipants(request.getMaxParticipants());
        }
        invite.setParticipantCount(0);
        invite.setStatus(InviteStatusEnum.RECRUITING.name());
        invite.setDeadlineHours(request.getDeadlineHours());
        invite.setAtmosphereTags(request.getAtmosphereTags());
        invite.setIsUrgent(request.getIsUrgent() != null && request.getIsUrgent());
        invite.setDeleted(false);

        inviteMapper.insert(invite);
        return invite;
    }

    @Transactional(readOnly = true)
    public IPage<InviteResponse> getInviteList(String type, String status, Integer page, Integer size) {
        int current = (page == null || page < 1) ? 1 : page;
        int pageSize = (size == null || size < 1) ? 20 : Math.min(size, 100);

        LambdaQueryWrapper<Invite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Invite::getDeleted, false);
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Invite::getInviteType, type);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Invite::getStatus, status);
        }
        wrapper.orderByDesc(Invite::getCreatedAt);

        Page<Invite> pageInfo = new Page<>(current, pageSize);
        IPage<Invite> invitePage = inviteMapper.selectPage(pageInfo, wrapper);
        List<Invite> records = invitePage.getRecords();
        Map<Long, User> creatorMap = batchLoadCreators(records);
        IPage<InviteResponse> responsePage = new Page<>(current, pageSize, invitePage.getTotal());
        responsePage.setRecords(records.stream()
                .map(inv -> buildInviteResponse(inv, creatorMap))
                .collect(Collectors.toList()));
        return responsePage;
    }

    @Transactional(readOnly = true)
    public List<InviteResponse> getRecommendInvites(Integer limit) {
        Long currentUserId = CurrentUser.getId();
        int size = (limit == null || limit < 1) ? 10 : Math.min(limit, 20);

        LambdaQueryWrapper<Invite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Invite::getDeleted, false)
                .eq(Invite::getInviteMode, InviteModeEnum.PUBLIC.name())
                .eq(Invite::getStatus, InviteStatusEnum.RECRUITING.name())
                .gt(Invite::getInviteTime, LocalDateTime.now());
        if (currentUserId != null) {
            wrapper.ne(Invite::getCreatorId, currentUserId);
        }
        wrapper.orderByDesc(Invite::getIsUrgent)
                .orderByAsc(Invite::getInviteTime);

        Page<Invite> pageInfo = new Page<>(1, size);
        IPage<Invite> invitePage = inviteMapper.selectPage(pageInfo, wrapper);
        List<Invite> records = invitePage.getRecords();
        Map<Long, User> creatorMap = batchLoadCreators(records);
        return records.stream()
                .map(inv -> buildInviteResponse(inv, creatorMap))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InviteResponse> getMyCreatedInvites(String range) {
        Long currentUserId = CurrentUser.getId();
        if (currentUserId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        LocalDateTime from = getHistoryStartTime(range);

        LambdaQueryWrapper<Invite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Invite::getCreatorId, currentUserId)
                .eq(Invite::getDeleted, false);
        if (from != null) {
            wrapper.ge(Invite::getInviteTime, from);
        }
        wrapper.orderByDesc(Invite::getInviteTime);

        List<Invite> list = inviteMapper.selectList(wrapper);
        Map<Long, User> creatorMap = batchLoadCreators(list);
        return list.stream()
                .map(inv -> buildInviteResponse(inv, creatorMap))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InviteResponse> getMyJoinedInvites(String range) {
        Long currentUserId = CurrentUser.getId();
        if (currentUserId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        LocalDateTime from = getHistoryStartTime(range);

        List<InviteParticipant> myParticipants = participantMapper.selectList(
                new LambdaQueryWrapper<InviteParticipant>()
                        .eq(InviteParticipant::getUserId, currentUserId));
        if (myParticipants.isEmpty()) {
            return List.of();
        }
        List<Long> inviteIds = myParticipants.stream()
                .map(InviteParticipant::getInviteId)
                .distinct()
                .collect(Collectors.toList());

        LambdaQueryWrapper<Invite> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Invite::getId, inviteIds)
                .eq(Invite::getDeleted, false);
        if (from != null) {
            wrapper.ge(Invite::getInviteTime, from);
        }
        wrapper.orderByDesc(Invite::getInviteTime);

        List<Invite> list = inviteMapper.selectList(wrapper);
        Map<Long, User> creatorMap = batchLoadCreators(list);
        return list.stream()
                .map(inv -> buildInviteResponse(inv, creatorMap))
                .collect(Collectors.toList());
    }

    public LocalDateTime getHistoryStartTime(String range) {
        if (range == null || range.isBlank()) {
            range = "week";
        }
        range = range.toLowerCase();
        LocalDateTime now = LocalDateTime.now();
        return switch (range) {
            case "week" -> now.minusDays(7);
            case "month" -> now.minusDays(30);
            case "all" -> null;
            default -> now.minusDays(7);
        };
    }

    @Transactional(readOnly = true)
    public InviteResponse getInviteDetail(Long inviteId) {
        Invite invite = getInviteOrThrow(inviteId);
        return buildInviteDetailResponse(invite);
    }

    @Transactional
    public void joinInvite(Long inviteId) {
        Long currentUserId = CurrentUser.getId();

        creditService.checkUserCredit(currentUserId, InviteCreditConstants.CREDIT_JOIN_THRESHOLD);
        creditService.checkParticipateLimit(currentUserId);

        Invite invite = getInviteOrThrow(inviteId);

        if (!InviteStatusEnum.RECRUITING.name().equals(invite.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "邀约不在招募中");
        }

        InviteParticipant existing = participantMapper.selectOne(
                new LambdaQueryWrapper<InviteParticipant>()
                        .eq(InviteParticipant::getInviteId, inviteId)
                        .eq(InviteParticipant::getUserId, currentUserId));
        if (existing != null) {
            throw new BusinessException(ResultCode.ALREADY_JOINED);
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

        if (InviteModeEnum.PUBLIC.name().equals(invite.getInviteMode())) {
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

        Invite invite = getInviteOrThrow(inviteId);

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

        participantMapper.deleteById(participant.getId());

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

    @Transactional
    public void cancelInvite(Long inviteId, String reason) {
        Long currentUserId = CurrentUser.getId();

        Invite invite = getInviteOrThrow(inviteId);

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

    /**
     * 一对一邀约：向目标用户发送一条邀约消息
     */
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

        Invite invite = getInviteOrThrow(inviteId);

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

    // ---------- 响应构建 ----------

    private Map<Long, User> batchLoadCreators(List<Invite> invites) {
        if (invites == null || invites.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = invites.stream()
                .map(Invite::getCreatorId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<User> users = userMapper.selectBatchIds(ids);
        return users != null ? users.stream().filter(Objects::nonNull).collect(toMap(User::getId, u -> u, (a, b) -> a)) : Map.of();
    }

    private InviteResponse buildInviteResponse(Invite invite) {
        User creator = userMapper.selectById(invite.getCreatorId());
        return buildInviteResponseWithCreator(invite, creator);
    }

    private InviteResponse buildInviteResponse(Invite invite, Map<Long, User> creatorMap) {
        User creator = creatorMap != null ? creatorMap.get(invite.getCreatorId()) : null;
        return buildInviteResponseWithCreator(invite, creator);
    }

    private InviteResponse buildInviteResponseWithCreator(Invite invite, User creator) {
        return InviteResponse.builder()
                .id(invite.getId())
                .creatorId(invite.getCreatorId())
                .inviteType(invite.getInviteType())
                .inviteMode(invite.getInviteMode())
                .targetUserId(invite.getTargetUserId())
                .title(invite.getTitle())
                .content(invite.getContent())
                .invitePeriod(invite.getInvitePeriod())
                .periodConfig(invite.getPeriodConfig())
                .inviteTime(invite.getInviteTime())
                .location(invite.getLocation())
                .maxParticipants(invite.getMaxParticipants())
                .participantCount(invite.getParticipantCount())
                .status(invite.getStatus())
                .deadlineHours(invite.getDeadlineHours())
                .atmosphereTags(invite.getAtmosphereTags())
                .isUrgent(invite.getIsUrgent())
                .socialRating(invite.getSocialRating())
                .orgRating(invite.getOrgRating())
                .ratingCount(invite.getRatingCount())
                .createdAt(invite.getCreatedAt())
                .chatGroupId(invite.getChatGroupId())
                .creator(creator != null ? InviteResponse.CreatorInfo.builder()
                        .id(creator.getId())
                        .nickname(creator.getNickname())
                        .avatarUrl(creator.getAvatarUrl())
                        .creditScore(creator.getCreditScore())
                        .build() : null)
                .build();
    }

    private InviteResponse buildInviteDetailResponse(Invite invite) {
        InviteResponse response = buildInviteResponse(invite);

        List<InviteParticipant> participants = participantMapper.selectList(
                new LambdaQueryWrapper<InviteParticipant>()
                        .eq(InviteParticipant::getInviteId, invite.getId()));
        if (participants.isEmpty()) {
            response.setParticipants(List.of());
            return response;
        }
        List<Long> participantUserIds = participants.stream()
                .map(InviteParticipant::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> userMap = participantUserIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(participantUserIds).stream()
                        .filter(Objects::nonNull)
                        .collect(toMap(User::getId, u -> u, (a, b) -> a));
        List<InviteResponse.ParticipantInfo> participantInfos = participants.stream().map(p -> {
            User user = userMap.get(p.getUserId());
            return InviteResponse.ParticipantInfo.builder()
                    .userId(p.getUserId())
                    .nickname(user != null ? user.getNickname() : "")
                    .avatarUrl(user != null ? user.getAvatarUrl() : null)
                    .joinAt(p.getJoinAt())
                    .build();
        }).collect(Collectors.toList());
        response.setParticipants(participantInfos);
        return response;
    }
}
