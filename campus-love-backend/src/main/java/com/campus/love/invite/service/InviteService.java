package com.campus.love.invite.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.love.auth.security.CurrentUser;
import com.campus.love.chat.entity.ChatGroup;
import com.campus.love.chat.service.ChatService;
import com.campus.love.chat.service.ChatGroupService;
import com.campus.love.common.enums.ActivityTypeEnum;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.follow.service.FollowService;
import com.campus.love.invite.dto.*;
import com.campus.love.invite.entity.*;
import com.campus.love.invite.enums.InviteModeEnum;
import com.campus.love.invite.enums.InviteStatusEnum;
import com.campus.love.invite.mapper.*;
import com.campus.love.notification.service.NotificationService;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import com.campus.love.user.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 邀约服务
 */
@Service
@RequiredArgsConstructor
public class InviteService {

    private final InviteMapper inviteMapper;
    private final InviteParticipantMapper participantMapper;
    private final InviteWaitMapper waitMapper;
    private final InviteRatingMapper ratingMapper;
    private final UserMapper userMapper;
    private final FollowService followService;
    private final ActivityService activityService;
    private final ChatService chatService;
    private final ChatGroupService chatGroupService;
    private final NotificationService notificationService;

    /**
     * 发起邀约所需最低信用分
     */
    private static final int CREDIT_CREATE_THRESHOLD = 80;
    /**
     * 参与邀约所需最低信用分
     */
    private static final int CREDIT_JOIN_THRESHOLD = 60;
    /**
     * 同时参与邀约上限
     */
    private static final int MAX_CONCURRENT_PARTICIPATES = 2;
    /**
     * 活动开始前 N 小时内视为「临时取消」
     */
    private static final int TEMP_CANCEL_HOURS = 1;
    /**
     * 成功完成邀约 +2
     */
    private static final int CREDIT_SUCCESS_COMPLETE = 2;
    /**
     * 临时取消（开始前 TEMP_CANCEL_HOURS 内取消/退出）-5
     */
    private static final int CREDIT_TEMP_CANCEL = -5;
    /**
     * 放鸽子（活动开始时间已到仍取消/退出）-10
     */
    private static final int CREDIT_NO_SHOW = -10;
    private static final int MSG_TYPE_INVITE = 2;

    /**
     * 发起邀约
     */
    @Transactional
    public Long createInvite(InviteCreateRequest request) {
        Long currentUserId = CurrentUser.getId();

        // 检查信用分（低于 80 不允许发起邀约）
        checkUserCredit(currentUserId, CREDIT_CREATE_THRESHOLD);

        // 检查等级对应的每日发起次数与同时进行数限制
        checkInviteCreateLimit(currentUserId);

        // 如果是一对一邀约，检查互关
        if (InviteModeEnum.PRIVATE.name().equals(request.getInviteMode())) {
            if (request.getTargetUserId() == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "一对一邀约需要指定目标用户");
            }
            if (!followService.isMutual(currentUserId, request.getTargetUserId())) {
                throw new BusinessException(ResultCode.NOT_MUTUAL_FOLLOW);
            }
        }

        Invite invite = new Invite();
        invite.setCreatorId(currentUserId);
        invite.setInviteMode(request.getInviteMode());
        invite.setTargetUserId(request.getTargetUserId());
        invite.setInviteType(request.getInviteType());
        invite.setTitle(request.getTitle());
        invite.setContent(request.getContent());
        invite.setInvitePeriod(request.getInvitePeriod());
        invite.setPeriodConfig(request.getPeriodConfig());
        // 前端使用 new Date().toISOString() 传入 UTC 时间（带 Z），这里转换为系统时区的 LocalDateTime
        Instant inviteInstant = Instant.parse(request.getInviteTime());
        invite.setInviteTime(LocalDateTime.ofInstant(inviteInstant, ZoneId.systemDefault()));
        invite.setLocation(request.getLocation());
        // 一对一邀约默认仅允许 1 名参与者（不含发起人），避免显示“不限”
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

        // 更新用户统计
        incrementInviteCount(currentUserId);

        // 匹配等待邀约用户并自动加入（仅公开邀约）
        if (InviteModeEnum.PUBLIC.name().equals(invite.getInviteMode())) {
            matchAndAutoJoinWaitUsers(invite);
        }

        // 记录活跃度
        activityService.recordActivity(ActivityTypeEnum.INVITE_CREATE, invite.getId());

        // 一对一邀约：在聊天中发送一条邀约消息
        if (InviteModeEnum.PRIVATE.name().equals(invite.getInviteMode())
                && invite.getTargetUserId() != null) {
            sendPrivateInviteMessage(currentUserId, invite);
        }

        return invite.getId();
    }

    /**
     * 获取邀约列表
     */
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

        // 转换为响应
        IPage<InviteResponse> responsePage = new Page<>(current, pageSize, invitePage.getTotal());
        List<InviteResponse> responses = invitePage.getRecords().stream()
                .map(this::buildInviteResponse)
                .collect(Collectors.toList());
        responsePage.setRecords(responses);

        return responsePage;
    }

    /**
     * 邀约推荐：简单规则版 1.0
     * - 只推荐公开、招募中、未删除、时间未过期的邀约
     * - 排序：急需优先，其次按开始时间升序
     * - 不推荐自己发起的邀约
     */
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

        return invitePage.getRecords().stream()
                .map(this::buildInviteResponse)
                .collect(Collectors.toList());
    }

    /**
     * 我发起的邀约历史
     *
     * @param range 时间范围：week / month / all
     */
    public List<InviteResponse> getMyCreatedInvites(String range) {
        Long currentUserId = CurrentUser.getId();
        if (currentUserId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        LocalDateTime from = resolveHistoryFrom(range);

        LambdaQueryWrapper<Invite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Invite::getCreatorId, currentUserId)
                .eq(Invite::getDeleted, false);
        if (from != null) {
            // 使用邀约时间作为历史范围判断依据，更符合“最近一周/一月的活动时间”直觉
            wrapper.ge(Invite::getInviteTime, from);
        }
        wrapper.orderByDesc(Invite::getInviteTime);

        List<Invite> list = inviteMapper.selectList(wrapper);
        return list.stream()
                .map(this::buildInviteResponse)
                .collect(Collectors.toList());
    }

    /**
     * 我参与的邀约历史
     *
     * @param range 时间范围：week / month / all
     */
    public List<InviteResponse> getMyJoinedInvites(String range) {
        Long currentUserId = CurrentUser.getId();
        if (currentUserId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        LocalDateTime from = resolveHistoryFrom(range);

        List<InviteParticipant> myParticipants = participantMapper.selectList(
                new LambdaQueryWrapper<InviteParticipant>()
                        .eq(InviteParticipant::getUserId, currentUserId)
        );
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
            // 同样按邀约时间过滤
            wrapper.ge(Invite::getInviteTime, from);
        }
        wrapper.orderByDesc(Invite::getInviteTime);

        List<Invite> list = inviteMapper.selectList(wrapper);
        return list.stream()
                .map(this::buildInviteResponse)
                .collect(Collectors.toList());
    }

    /**
     * 根据范围解析起始时间：week / month / all
     */
    private LocalDateTime resolveHistoryFrom(String range) {
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

    /**
     * 获取邀约详情
     */
    public InviteResponse getInviteDetail(Long inviteId) {
        Invite invite = inviteMapper.selectById(inviteId);
        if (invite == null || Boolean.TRUE.equals(invite.getDeleted())) {
            throw new BusinessException(ResultCode.INVITE_NOT_FOUND);
        }
        return buildInviteDetailResponse(invite);
    }

    /**
     * 加入邀约
     */
    @Transactional
    public void joinInvite(Long inviteId) {
        Long currentUserId = CurrentUser.getId();

        // 检查信用分（低于 60 不允许参与邀约）
        checkUserCredit(currentUserId, CREDIT_JOIN_THRESHOLD);

        // 检查同时参与邀约数量上限
        checkParticipateLimit(currentUserId);

        Invite invite = inviteMapper.selectById(inviteId);
        if (invite == null || Boolean.TRUE.equals(invite.getDeleted())) {
            throw new BusinessException(ResultCode.INVITE_NOT_FOUND);
        }

        // 检查状态
        if (!InviteStatusEnum.RECRUITING.name().equals(invite.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "邀约不在招募中");
        }

        // 检查是否已加入
        InviteParticipant existing = participantMapper.selectOne(
                new LambdaQueryWrapper<InviteParticipant>()
                        .eq(InviteParticipant::getInviteId, inviteId)
                        .eq(InviteParticipant::getUserId, currentUserId));
        if (existing != null) {
            throw new BusinessException(ResultCode.ALREADY_JOINED);
        }

        // 检查人数限制
        if (invite.getMaxParticipants() != null && invite.getParticipantCount() >= invite.getMaxParticipants()) {
            throw new BusinessException(ResultCode.INVITE_FULL);
        }

        // 如果是一对一邀约，检查是否是目标用户
        if (InviteModeEnum.PRIVATE.name().equals(invite.getInviteMode())) {
            if (!currentUserId.equals(invite.getTargetUserId())) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "不是受邀目标用户");
            }
        }

        // 添加参与者
        InviteParticipant participant = new InviteParticipant();
        participant.setInviteId(inviteId);
        participant.setUserId(currentUserId);
        participantMapper.insert(participant);

        // 更新参与人数
        invite.setParticipantCount(invite.getParticipantCount() + 1);
        if (invite.getMaxParticipants() != null && invite.getParticipantCount() >= invite.getMaxParticipants()) {
            invite.setStatus(InviteStatusEnum.FULL.name());
        }
        inviteMapper.updateById(invite);

        // 更新用户统计
        incrementParticipateCount(currentUserId);

        // 记录活跃度
        activityService.recordActivity(ActivityTypeEnum.INVITE_JOIN, inviteId);

        // 通知：报名成功（给参与者自己）与有人加入（给发起人）
        notificationService.notifyInviteJoinSuccess(currentUserId, currentUserId, invite);
        if (!currentUserId.equals(invite.getCreatorId())) {
            notificationService.notifyInviteNewParticipant(invite.getCreatorId(), currentUserId, invite);
        }

        // 公开邀约首次/后续加入：确保存在群聊并加入群成员
        if (InviteModeEnum.PUBLIC.name().equals(invite.getInviteMode())) {
            // 创建或获取群聊
            ChatGroup group = chatGroupService.createGroupIfAbsent(invite);
            invite.setChatGroupId(group.getId());
            inviteMapper.updateById(invite);
            // 发起人和当前参与者加入群（内部已做去重）
            chatGroupService.addMemberIfAbsent(group.getId(), invite.getCreatorId());
            chatGroupService.addMemberIfAbsent(group.getId(), currentUserId);
        }
    }

    /**
     * 退出邀约
     */
    @Transactional
    public void leaveInvite(Long inviteId) {
        Long currentUserId = CurrentUser.getId();

        Invite invite = inviteMapper.selectById(inviteId);
        if (invite == null || Boolean.TRUE.equals(invite.getDeleted())) {
            throw new BusinessException(ResultCode.INVITE_NOT_FOUND);
        }

        // 检查是否已加入
        InviteParticipant participant = participantMapper.selectOne(
                new LambdaQueryWrapper<InviteParticipant>()
                        .eq(InviteParticipant::getInviteId, inviteId)
                        .eq(InviteParticipant::getUserId, currentUserId));
        if (participant == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "未加入该邀约");
        }

        // 检查邀约状态
        if (InviteStatusEnum.IN_PROGRESS.name().equals(invite.getStatus()) ||
            InviteStatusEnum.ENDED.name().equals(invite.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "邀约进行中或已结束，无法退出");
        }

        // 根据时间施加信用分惩罚：临时退出 / 放鸽子
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime inviteTime = invite.getInviteTime();
        if (inviteTime != null) {
            if (now.isAfter(inviteTime)) {
                // 活动已开始仍退出，视为放鸽子
                adjustCreditScore(currentUserId, CREDIT_NO_SHOW);
            } else if (!now.isBefore(inviteTime.minusHours(TEMP_CANCEL_HOURS))) {
                // 开始前 TEMP_CANCEL_HOURS 小时内退出，视为临时取消
                adjustCreditScore(currentUserId, CREDIT_TEMP_CANCEL);
            }
        }

        // 删除参与者
        participantMapper.deleteById(participant.getId());

        // 更新参与人数
        invite.setParticipantCount(invite.getParticipantCount() - 1);
        if (InviteStatusEnum.FULL.name().equals(invite.getStatus()) &&
            invite.getMaxParticipants() != null &&
            invite.getParticipantCount() < invite.getMaxParticipants()) {
            invite.setStatus(InviteStatusEnum.RECRUITING.name());
        }
        inviteMapper.updateById(invite);

        // 更新用户统计
        decrementParticipateCount(currentUserId);

        // 通知发起人：有人退出邀约
        if (!currentUserId.equals(invite.getCreatorId())) {
            notificationService.notifyParticipantLeave(invite.getCreatorId(), currentUserId, invite);
        }
    }

    /**
     * 取消邀约
     */
    @Transactional
    public void cancelInvite(Long inviteId, String reason) {
        Long currentUserId = CurrentUser.getId();

        Invite invite = inviteMapper.selectById(inviteId);
        if (invite == null || Boolean.TRUE.equals(invite.getDeleted())) {
            throw new BusinessException(ResultCode.INVITE_NOT_FOUND);
        }

        // 检查是否是发起人
        if (!invite.getCreatorId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只有发起人可以取消邀约");
        }

        // 检查邀约状态
        if (!InviteStatusEnum.RECRUITING.name().equals(invite.getStatus()) &&
            !InviteStatusEnum.FULL.name().equals(invite.getStatus()) &&
            !InviteStatusEnum.CONFIRMED.name().equals(invite.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "邀约已进行中或已结束，无法取消");
        }
        LocalDateTime now = LocalDateTime.now();

        // 根据时间与是否填写原因施加信用分惩罚
        LocalDateTime inviteTime = invite.getInviteTime();
        if (inviteTime != null) {
            boolean hasReason = reason != null && !reason.isBlank();
            if (now.isAfter(inviteTime)) {
                // 活动开始时间已到仍由发起人取消，视为放鸽子
                adjustCreditScore(currentUserId, CREDIT_NO_SHOW);
            } else if (!now.isBefore(inviteTime.minusHours(TEMP_CANCEL_HOURS)) && !hasReason) {
                // 开始前 TEMP_CANCEL_HOURS 小时内且无理由，视为临时取消
                adjustCreditScore(currentUserId, CREDIT_TEMP_CANCEL);
            }
        }

        // 查询当前参与者，用于发送通知
        List<InviteParticipant> participants = participantMapper.selectList(
                new LambdaQueryWrapper<InviteParticipant>()
                        .eq(InviteParticipant::getInviteId, inviteId)
        );

        // 更新状态
        invite.setStatus(InviteStatusEnum.CANCELLED.name());
        inviteMapper.updateById(invite);

        // 向所有参与者发送取消通知
        if (!participants.isEmpty()) {
            List<Long> participantIds = participants.stream()
                    .map(InviteParticipant::getUserId)
                    .collect(Collectors.toList());
            notificationService.notifyInviteCancelled(participantIds, currentUserId, invite, reason);
        }
    }

    /**
     * 一对一邀约：向目标用户发送一条「邀约消息」
     */
    private void sendPrivateInviteMessage(Long senderId, Invite invite) {
        if (invite.getTargetUserId() == null) {
            return;
        }
        try {
            String timeStr = invite.getInviteTime() != null
                    ? invite.getInviteTime().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
                    : "";
            StringBuilder content = new StringBuilder();
            content.append("邀约邀请：").append(invite.getTitle());
            if (!timeStr.isEmpty()) {
                content.append("｜时间 ").append(timeStr);
            }
            if (invite.getLocation() != null && !invite.getLocation().isEmpty()) {
                content.append("｜地点 ").append(invite.getLocation());
            }
            // 方便前端从消息内容中解析出邀约ID，跳转详情
            content.append("｜INVITE#").append(invite.getId());

            chatService.sendMessage(senderId, invite.getTargetUserId(), content.toString(), MSG_TYPE_INVITE);
        } catch (Exception ignored) {
            // 聊天消息发送失败不影响邀约创建本身
        }
    }

    /**
     * 确认参与者
     */
    @Transactional
    public void confirmParticipants(Long inviteId, List<Long> userIds) {
        Long currentUserId = CurrentUser.getId();

        Invite invite = inviteMapper.selectById(inviteId);
        if (invite == null || Boolean.TRUE.equals(invite.getDeleted())) {
            throw new BusinessException(ResultCode.INVITE_NOT_FOUND);
        }

        // 检查是否是发起人
        if (!invite.getCreatorId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只有发起人可以确认参与者");
        }

        // 检查状态
        if (!InviteStatusEnum.FULL.name().equals(invite.getStatus()) &&
            !InviteStatusEnum.CONFIRMED.name().equals(invite.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "邀约状态不正确");
        }

        // 更新状态
        invite.setStatus(InviteStatusEnum.CONFIRMED.name());
        inviteMapper.updateById(invite);
    }

    /**
     * 创建等待邀约
     */
    @Transactional
    public Long createInviteWait(InviteWaitCreateRequest request) {
        Long currentUserId = CurrentUser.getId();

        InviteWait wait = new InviteWait();
        wait.setUserId(currentUserId);
        wait.setInviteTypes(String.join(",", request.getInviteTypes()));
        wait.setPeriodConfig(request.getPeriodConfig());
        wait.setLocationPref(request.getLocationPref());
        wait.setAutoAccept(request.getAutoAccept() != null && request.getAutoAccept());
        wait.setExpireHours(request.getExpireHours());

        waitMapper.insert(wait);

        // 创建等待邀约后，尝试匹配当前已存在的公开邀约（满足条件时自动加入）
        if (Boolean.TRUE.equals(wait.getAutoAccept())) {
            matchExistingInvitesForWait(wait);
        }

        return wait.getId();
    }

    /**
     * 获取我的等待邀约
     */
    public List<InviteWaitResponse> getMyInviteWaits() {
        Long currentUserId = CurrentUser.getId();

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
        }).collect(Collectors.toList());
    }

    /**
     * 取消等待邀约
     */
    @Transactional
    public void cancelInviteWait(Long waitId) {
        Long currentUserId = CurrentUser.getId();

        InviteWait wait = waitMapper.selectById(waitId);
        if (wait == null || !wait.getUserId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "等待邀约不存在");
        }

        waitMapper.deleteById(waitId);
    }

    /**
     * 创建评价
     */
    @Transactional
    public void createRating(InviteRatingCreateRequest request) {
        Long currentUserId = CurrentUser.getId();

        Invite invite = inviteMapper.selectById(request.getInviteId());
        if (invite == null || Boolean.TRUE.equals(invite.getDeleted())) {
            throw new BusinessException(ResultCode.INVITE_NOT_FOUND);
        }

        // 检查邀约状态
        if (!InviteStatusEnum.ENDED.name().equals(invite.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "邀约未结束，无法评价");
        }

        // 检查是否参与过
        InviteParticipant participant = participantMapper.selectOne(
                new LambdaQueryWrapper<InviteParticipant>()
                        .eq(InviteParticipant::getInviteId, request.getInviteId())
                        .eq(InviteParticipant::getUserId, currentUserId));
        if (participant == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "未参与该邀约，无法评价");
        }

        // 检查是否已评价过
        InviteRating existing = ratingMapper.selectOne(
                new LambdaQueryWrapper<InviteRating>()
                        .eq(InviteRating::getInviteId, request.getInviteId())
                        .eq(InviteRating::getRaterId, currentUserId)
                        .eq(InviteRating::getRatedUserId, request.getRatedUserId()));
        if (existing != null) {
            throw new BusinessException(ResultCode.ALREADY_RATED);
        }

        // 成功完成邀约奖励：同一邀约中，评价他人时每个用户只奖励一次
        long myRatingsForInvite = ratingMapper.selectCount(
                new LambdaQueryWrapper<InviteRating>()
                        .eq(InviteRating::getInviteId, request.getInviteId())
                        .eq(InviteRating::getRaterId, currentUserId)
        );
        if (myRatingsForInvite == 0) {
            adjustCreditScore(currentUserId, CREDIT_SUCCESS_COMPLETE);
        }

        // 创建评价
        InviteRating rating = new InviteRating();
        rating.setInviteId(request.getInviteId());
        rating.setRaterId(currentUserId);
        rating.setRatedUserId(request.getRatedUserId());
        rating.setSocialRating(BigDecimal.valueOf(request.getSocialRating()).setScale(1, RoundingMode.HALF_UP));
        if (request.getOrgRating() != null) {
            rating.setOrgRating(BigDecimal.valueOf(request.getOrgRating()).setScale(1, RoundingMode.HALF_UP));
        }
        rating.setContent(request.getContent());
        ratingMapper.insert(rating);

        // 高评分奖励：社交体验评分 >= 4 星时，被评价人信用分 +3
        if (request.getSocialRating() >= 4.0) {
            adjustCreditScore(request.getRatedUserId(), 3);
        }

        // 更新参与者评分
        if (request.getRatedUserId().equals(invite.getCreatorId())) {
            // 评价发起人，更新邀约评分
            updateInviteRating(request.getInviteId());
        }
    }

    /**
     * 获取我的邀约统计
     */
    public InviteStatsResponse getMyInviteStats() {
        Long currentUserId = CurrentUser.getId();

        User user = userMapper.selectById(currentUserId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 统计发起的邀约
        long totalInvites = inviteMapper.selectCount(
                new LambdaQueryWrapper<Invite>()
                        .eq(Invite::getCreatorId, currentUserId)
                        .eq(Invite::getDeleted, false));

        // 统计完成的邀约
        long completedInvites = inviteMapper.selectCount(
                new LambdaQueryWrapper<Invite>()
                        .eq(Invite::getCreatorId, currentUserId)
                        .eq(Invite::getStatus, InviteStatusEnum.ENDED.name())
                        .eq(Invite::getDeleted, false));

        // 统计获得的评价
        List<InviteRating> receivedRatings = ratingMapper.selectList(
                new LambdaQueryWrapper<InviteRating>()
                        .eq(InviteRating::getRatedUserId, currentUserId));

        BigDecimal avgSocialRating = receivedRatings.stream()
                .map(InviteRating::getSocialRating)
                .filter(r -> r != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(receivedRatings.isEmpty() ? BigDecimal.ONE : BigDecimal.valueOf(receivedRatings.size()), 1, RoundingMode.HALF_UP);

        BigDecimal avgOrgRating = receivedRatings.stream()
                .map(InviteRating::getOrgRating)
                .filter(r -> r != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(receivedRatings.isEmpty() || receivedRatings.stream().noneMatch(r -> r.getOrgRating() != null)
                        ? BigDecimal.ONE : BigDecimal.valueOf(receivedRatings.stream().filter(r -> r.getOrgRating() != null).count()), 1, RoundingMode.HALF_UP);

        // 统计发起邀约获得的评价（无发起邀约时跳过 IN 查询，避免 SQL invite_id IN () 语法错误）
        List<Long> myInviteIds = inviteMapper.selectList(
                        new LambdaQueryWrapper<Invite>()
                                .eq(Invite::getCreatorId, currentUserId)
                                .select(Invite::getId))
                .stream()
                .map(Invite::getId)
                .collect(Collectors.toList());

        List<InviteRating> orgRatings = myInviteIds.isEmpty()
                ? List.of()
                : ratingMapper.selectList(
                        new LambdaQueryWrapper<InviteRating>()
                                .in(InviteRating::getInviteId, myInviteIds)
                                .eq(InviteRating::getRatedUserId, currentUserId));

        BigDecimal receivedOrgRating = orgRatings.stream()
                .map(InviteRating::getOrgRating)
                .filter(r -> r != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(orgRatings.isEmpty() || orgRatings.stream().noneMatch(r -> r.getOrgRating() != null)
                        ? BigDecimal.ONE : BigDecimal.valueOf(orgRatings.stream().filter(r -> r.getOrgRating() != null).count()), 1, RoundingMode.HALF_UP);

        return InviteStatsResponse.builder()
                .inviteCount((int) totalInvites)
                .participateCount(user.getParticipateCount() != null ? user.getParticipateCount() : 0)
                .successRate(totalInvites > 0 ? BigDecimal.valueOf(completedInvites)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalInvites), 1, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .avgSocialRating(avgSocialRating.compareTo(BigDecimal.ZERO) == 0 && !receivedRatings.isEmpty() ? null : avgSocialRating)
                .avgOrgRating(avgOrgRating.compareTo(BigDecimal.ZERO) == 0 && receivedRatings.stream().noneMatch(r -> r.getOrgRating() != null) ? null : avgOrgRating)
                .receivedSocialRating(avgSocialRating.compareTo(BigDecimal.ZERO) == 0 && !receivedRatings.isEmpty() ? null : avgSocialRating)
                .receivedOrgRating(receivedOrgRating.compareTo(BigDecimal.ZERO) == 0 && !orgRatings.isEmpty() ? null : receivedOrgRating)
                .build();
    }

    /**
     * 获取指定用户的邀约统计
     */
    public InviteStatsResponse getUserInviteStats(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 统计发起的邀约
        long totalInvites = inviteMapper.selectCount(
                new LambdaQueryWrapper<Invite>()
                        .eq(Invite::getCreatorId, userId)
                        .eq(Invite::getDeleted, false));

        // 统计完成的邀约
        long completedInvites = inviteMapper.selectCount(
                new LambdaQueryWrapper<Invite>()
                        .eq(Invite::getCreatorId, userId)
                        .eq(Invite::getStatus, InviteStatusEnum.ENDED.name())
                        .eq(Invite::getDeleted, false));

        // 统计获得的评价
        List<InviteRating> receivedRatings = ratingMapper.selectList(
                new LambdaQueryWrapper<InviteRating>()
                        .eq(InviteRating::getRatedUserId, userId));

        BigDecimal avgSocialRating = receivedRatings.stream()
                .map(InviteRating::getSocialRating)
                .filter(r -> r != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(receivedRatings.isEmpty() ? BigDecimal.ONE : BigDecimal.valueOf(receivedRatings.size()), 1, RoundingMode.HALF_UP);

        BigDecimal avgOrgRating = receivedRatings.stream()
                .map(InviteRating::getOrgRating)
                .filter(r -> r != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(receivedRatings.isEmpty() || receivedRatings.stream().noneMatch(r -> r.getOrgRating() != null)
                        ? BigDecimal.ONE : BigDecimal.valueOf(receivedRatings.stream().filter(r -> r.getOrgRating() != null).count()), 1, RoundingMode.HALF_UP);

        // 统计发起邀约获得的评价（无发起邀约时跳过 IN 查询，避免 SQL invite_id IN () 语法错误）
        List<Long> myInviteIds = inviteMapper.selectList(
                        new LambdaQueryWrapper<Invite>()
                                .eq(Invite::getCreatorId, userId)
                                .select(Invite::getId))
                .stream()
                .map(Invite::getId)
                .collect(Collectors.toList());

        List<InviteRating> orgRatings = myInviteIds.isEmpty()
                ? List.of()
                : ratingMapper.selectList(
                        new LambdaQueryWrapper<InviteRating>()
                                .in(InviteRating::getInviteId, myInviteIds)
                                .eq(InviteRating::getRatedUserId, userId));

        BigDecimal receivedOrgRating = orgRatings.stream()
                .map(InviteRating::getOrgRating)
                .filter(r -> r != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(orgRatings.isEmpty() || orgRatings.stream().noneMatch(r -> r.getOrgRating() != null)
                        ? BigDecimal.ONE : BigDecimal.valueOf(orgRatings.stream().filter(r -> r.getOrgRating() != null).count()), 1, RoundingMode.HALF_UP);

        return InviteStatsResponse.builder()
                .inviteCount((int) totalInvites)
                .participateCount(user.getParticipateCount() != null ? user.getParticipateCount() : 0)
                .successRate(totalInvites > 0 ? BigDecimal.valueOf(completedInvites)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalInvites), 1, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .avgSocialRating(avgSocialRating.compareTo(BigDecimal.ZERO) == 0 && !receivedRatings.isEmpty() ? null : avgSocialRating)
                .avgOrgRating(avgOrgRating.compareTo(BigDecimal.ZERO) == 0 && receivedRatings.stream().noneMatch(r -> r.getOrgRating() != null) ? null : avgOrgRating)
                .receivedSocialRating(avgSocialRating.compareTo(BigDecimal.ZERO) == 0 && !receivedRatings.isEmpty() ? null : avgSocialRating)
                .receivedOrgRating(receivedOrgRating.compareTo(BigDecimal.ZERO) == 0 && !orgRatings.isEmpty() ? null : receivedOrgRating)
                .build();
    }

    /**
     * 检查用户信用分
     */
    private void checkUserCredit(Long userId, int minScore) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (user.getCreditScore() != null && user.getCreditScore() < minScore) {
            throw new BusinessException(ResultCode.CREDIT_TOO_LOW);
        }
    }

    /**
     * 检查今日邀约次数
     */
    private void checkInviteCreateLimit(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        int level = user.getUserLevel() != null ? user.getUserLevel() : 1;
        int dailyLimit;
        int concurrentLimit;
        if (level <= 1) {
            dailyLimit = 3;
            concurrentLimit = 2;
        } else if (level == 2) {
            dailyLimit = 5;
            concurrentLimit = 3;
        } else {
            dailyLimit = 10;
            concurrentLimit = 5;
        }

        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);

        long todayCount = inviteMapper.selectCount(
                new LambdaQueryWrapper<Invite>()
                        .eq(Invite::getCreatorId, userId)
                        .ge(Invite::getCreatedAt, todayStart)
                        .eq(Invite::getDeleted, false));

        if (todayCount >= dailyLimit) {
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
                                InviteStatusEnum.IN_PROGRESS.name())
        );

        if (concurrentCount >= concurrentLimit) {
            throw new BusinessException(ResultCode.CONFLICT, "同时进行的邀约已达上限");
        }
    }

    /**
     * 检查用户当前同时参与的邀约数量是否超过上限
     */
    private void checkParticipateLimit(Long userId) {
        List<InviteParticipant> participants = participantMapper.selectList(
                new LambdaQueryWrapper<InviteParticipant>()
                        .eq(InviteParticipant::getUserId, userId)
        );
        if (participants.isEmpty()) {
            return;
        }

        int activeCount = 0;
        for (InviteParticipant participant : participants) {
            Invite invite = inviteMapper.selectById(participant.getInviteId());
            if (invite == null || Boolean.TRUE.equals(invite.getDeleted())) {
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

        if (activeCount >= MAX_CONCURRENT_PARTICIPATES) {
            throw new BusinessException(ResultCode.PARTICIPATE_LIMIT_EXCEEDED);
        }
    }

    /**
     * 邀约创建后，匹配等待邀约用户并自动加入
     * 匹配条件（当前简化版）：
     * - 类型匹配：waiting.inviteTypes 中包含当前邀约类型
     * - 时间匹配：若 waiting.periodConfig 有 start/end，则邀约时间需落在区间内
     * - 未过期：createdAt + expireHours 仍在当前时间之后
     * - 公开邀约且未超过人数上限
     * autoAccept = true 的用户自动加入邀约。
     */
    private void matchAndAutoJoinWaitUsers(Invite invite) {
        // 没有人数上限时也允许自动加入，但无需额外处理 FULL 状态
        LocalDateTime now = LocalDateTime.now();

        List<InviteWait> waits = waitMapper.selectList(
                new LambdaQueryWrapper<InviteWait>()
                        .ne(InviteWait::getUserId, invite.getCreatorId())
        );

        if (waits.isEmpty()) {
            return;
        }

        for (InviteWait wait : waits) {
            // 过期过滤
            if (wait.getCreatedAt() != null && wait.getExpireHours() != null) {
                LocalDateTime expireTime = wait.getCreatedAt().plusHours(wait.getExpireHours());
                if (now.isAfter(expireTime)) {
                    continue;
                }
            }

            // 类型匹配
            if (!typeMatches(wait.getInviteTypes(), invite.getInviteType())) {
                continue;
            }

            // 时间匹配
            if (!timeMatches(wait.getPeriodConfig(), invite.getInviteTime())) {
                continue;
            }

            // 人数限制
            if (invite.getMaxParticipants() != null &&
                    invite.getParticipantCount() >= invite.getMaxParticipants()) {
                break;
            }

            Long userId = wait.getUserId();
            if (userId == null) {
                continue;
            }

            if (Boolean.TRUE.equals(wait.getAutoAccept())) {
                // 自动受邀：直接加入邀约
                // 已经是参与者则跳过
                InviteParticipant existing = participantMapper.selectOne(
                        new LambdaQueryWrapper<InviteParticipant>()
                                .eq(InviteParticipant::getInviteId, invite.getId())
                                .eq(InviteParticipant::getUserId, userId)
                );
                if (existing != null) {
                    continue;
                }

                // 信用分过低的跳过（不抛异常）
            try {
                checkUserCredit(userId, CREDIT_JOIN_THRESHOLD);
                } catch (BusinessException e) {
                    if (!Objects.equals(e.getResultCode(), ResultCode.CREDIT_TOO_LOW)) {
                        throw e;
                    }
                    continue;
                }

                // 添加参与者
                InviteParticipant participant = new InviteParticipant();
                participant.setInviteId(invite.getId());
                participant.setUserId(userId);
                participantMapper.insert(participant);

                // 更新人数与状态
                invite.setParticipantCount(invite.getParticipantCount() + 1);
                if (invite.getMaxParticipants() != null &&
                        invite.getParticipantCount() >= invite.getMaxParticipants()) {
                    invite.setStatus(InviteStatusEnum.FULL.name());
                }
                inviteMapper.updateById(invite);

                // 更新用户参与统计
                incrementParticipateCount(userId);

                // 若人数已满，则停止继续自动加入
                if (invite.getMaxParticipants() != null &&
                        invite.getParticipantCount() >= invite.getMaxParticipants()) {
                    break;
                }
            } else {
                // 未开启自动受邀：仅发送匹配通知
                notificationService.notifyWaitMatch(userId, invite);
            }
        }
    }

    /**
     * 创建等待邀约后，匹配当前已存在的公开邀约并自动加入
     */
    private void matchExistingInvitesForWait(InviteWait wait) {
        Long userId = wait.getUserId();
        if (userId == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();

        List<Invite> invites = inviteMapper.selectList(
                new LambdaQueryWrapper<Invite>()
                        .eq(Invite::getDeleted, false)
                        .eq(Invite::getInviteMode, InviteModeEnum.PUBLIC.name())
                        .eq(Invite::getStatus, InviteStatusEnum.RECRUITING.name())
        );

        for (Invite invite : invites) {
            // 已开始或已过期的忽略（这里只做简单保护）
            if (invite.getInviteTime() != null && invite.getInviteTime().isBefore(now)) {
                continue;
            }

            // 类型匹配
            if (!typeMatches(wait.getInviteTypes(), invite.getInviteType())) {
                continue;
            }

            // 时间匹配
            if (!timeMatches(wait.getPeriodConfig(), invite.getInviteTime())) {
                continue;
            }

            // 人数限制
            if (invite.getMaxParticipants() != null &&
                    invite.getParticipantCount() >= invite.getMaxParticipants()) {
                continue;
            }

            if (Boolean.TRUE.equals(wait.getAutoAccept())) {
                // 已经是参与者则跳过
                InviteParticipant existing = participantMapper.selectOne(
                        new LambdaQueryWrapper<InviteParticipant>()
                                .eq(InviteParticipant::getInviteId, invite.getId())
                                .eq(InviteParticipant::getUserId, userId)
                );
                if (existing != null) {
                    continue;
                }

                // 信用分过低的跳过（不抛异常）
            try {
                checkUserCredit(userId, CREDIT_JOIN_THRESHOLD);
                } catch (BusinessException e) {
                    if (!Objects.equals(e.getResultCode(), ResultCode.CREDIT_TOO_LOW)) {
                        throw e;
                    }
                    continue;
                }

                // 添加参与者
                InviteParticipant participant = new InviteParticipant();
                participant.setInviteId(invite.getId());
                participant.setUserId(userId);
                participantMapper.insert(participant);

                // 更新人数与状态
                invite.setParticipantCount(invite.getParticipantCount() + 1);
                if (invite.getMaxParticipants() != null &&
                        invite.getParticipantCount() >= invite.getMaxParticipants()) {
                    invite.setStatus(InviteStatusEnum.FULL.name());
                }
                inviteMapper.updateById(invite);

                // 更新用户参与统计
                incrementParticipateCount(userId);

                // 先匹配到一个合适邀约就够了，避免一次性自动加入过多邀约
                break;
            } else {
                // 未开启自动受邀：仅发送匹配通知
                notificationService.notifyWaitMatch(userId, invite);
            }
        }
    }

    private boolean typeMatches(String inviteTypes, String inviteType) {
        if (inviteTypes == null || inviteType == null) {
            return false;
        }
        String[] parts = inviteTypes.split(",");
        for (String part : parts) {
            if (inviteType.equals(part.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean timeMatches(String periodConfig, LocalDateTime inviteTime) {
        if (inviteTime == null) {
            return false;
        }
        if (periodConfig == null || periodConfig.isEmpty()) {
            // 未设置时间偏好则视为匹配
            return true;
        }
        try {
            // 期望格式：{"start":"2026-03-06T10:00:00.000Z","end":"2026-03-06T14:00:00.000Z"}
            String json = periodConfig.trim();
            int startIdx = json.indexOf("\"start\"");
            int endIdx = json.indexOf("\"end\"");
            if (startIdx == -1 || endIdx == -1) {
                return true;
            }
            int startQuote = json.indexOf('"', json.indexOf(':', startIdx) + 1);
            int startQuoteEnd = json.indexOf('"', startQuote + 1);
            int endQuote = json.indexOf('"', json.indexOf(':', endIdx) + 1);
            int endQuoteEnd = json.indexOf('"', endQuote + 1);
            if (startQuote == -1 || startQuoteEnd == -1 || endQuote == -1 || endQuoteEnd == -1) {
                return true;
            }
            String startStr = json.substring(startQuote + 1, startQuoteEnd);
            String endStr = json.substring(endQuote + 1, endQuoteEnd);
            Instant startInstant = Instant.parse(startStr);
            Instant endInstant = Instant.parse(endStr);
            LocalDateTime startTime = LocalDateTime.ofInstant(startInstant, ZoneId.systemDefault());
            LocalDateTime endTime = LocalDateTime.ofInstant(endInstant, ZoneId.systemDefault());
            return !inviteTime.isBefore(startTime) && !inviteTime.isAfter(endTime);
        } catch (Exception e) {
            // 解析失败则不限制时间
            return true;
        }
    }

    /**
     * 调整用户信用分
     */
    private void adjustCreditScore(Long userId, int delta) {
        if (userId == null || delta == 0) {
            return;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }
        int current = user.getCreditScore() != null ? user.getCreditScore() : 100;
        user.setCreditScore(current + delta);
        userMapper.updateById(user);
    }

    /**
     * 增加发起邀约次数
     */
    private void incrementInviteCount(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            int newCount = (user.getInviteCount() != null ? user.getInviteCount() : 0) + 1;
            user.setInviteCount(newCount);
            userMapper.updateById(user);
        }
    }

    /**
     * 增加参与邀约次数
     */
    private void incrementParticipateCount(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            int newCount = (user.getParticipateCount() != null ? user.getParticipateCount() : 0) + 1;
            user.setParticipateCount(newCount);
            userMapper.updateById(user);
        }
    }

    /**
     * 减少参与邀约次数
     */
    private void decrementParticipateCount(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null && user.getParticipateCount() != null && user.getParticipateCount() > 0) {
            user.setParticipateCount(user.getParticipateCount() - 1);
            userMapper.updateById(user);
        }
    }

    /**
     * 更新邀约评分
     */
    private void updateInviteRating(Long inviteId) {
        List<InviteRating> ratings = ratingMapper.selectList(
                new LambdaQueryWrapper<InviteRating>()
                        .eq(InviteRating::getInviteId, inviteId));

        if (ratings.isEmpty()) {
            return;
        }

        BigDecimal avgSocial = ratings.stream()
                .map(InviteRating::getSocialRating)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(ratings.size()), 1, RoundingMode.HALF_UP);

        BigDecimal avgOrg = ratings.stream()
                .map(InviteRating::getOrgRating)
                .filter(r -> r != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        avgOrg = avgOrg.divide(
                BigDecimal.valueOf(ratings.stream().filter(r -> r.getOrgRating() != null).count()),
                1, RoundingMode.HALF_UP);

        Invite invite = inviteMapper.selectById(inviteId);
        if (invite != null) {
            invite.setSocialRating(avgSocial);
            invite.setOrgRating(avgOrg);
            invite.setRatingCount(ratings.size());
            inviteMapper.updateById(invite);
        }
    }

    /**
     * 构建邀约响应
     */
    private InviteResponse buildInviteResponse(Invite invite) {
        User creator = userMapper.selectById(invite.getCreatorId());

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
                .creator(creator != null ? InviteResponse.CreatorInfo.builder()
                        .id(creator.getId())
                        .nickname(creator.getNickname())
                        .avatarUrl(creator.getAvatarUrl())
                        .creditScore(creator.getCreditScore())
                        .build() : null)
                .build();
    }

    /**
     * 构建邀约详情响应（包含参与者列表）
     */
    private InviteResponse buildInviteDetailResponse(Invite invite) {
        InviteResponse response = buildInviteResponse(invite);

        // 获取参与者列表
        List<InviteParticipant> participants = participantMapper.selectList(
                new LambdaQueryWrapper<InviteParticipant>()
                        .eq(InviteParticipant::getInviteId, invite.getId()));

        List<InviteResponse.ParticipantInfo> participantInfos = participants.stream().map(p -> {
            User user = userMapper.selectById(p.getUserId());
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
