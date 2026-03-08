package com.campus.love.invite.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.invite.dto.InviteResponse;
import com.campus.love.invite.dto.InviteTypeCountResponse;
import com.campus.love.invite.entity.Invite;
import com.campus.love.invite.entity.InviteDecline;
import com.campus.love.invite.entity.InviteParticipant;
import com.campus.love.invite.enums.InviteModeEnum;
import com.campus.love.invite.enums.InviteStatusEnum;
import com.campus.love.invite.mapper.InviteDeclineMapper;
import com.campus.love.invite.mapper.InviteMapper;
import com.campus.love.invite.mapper.InviteParticipantMapper;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

/**
 * 邀约查询服务：列表、详情、推荐、我的邀约、响应构建等只读操作。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InviteQueryService {

    private final InviteMapper inviteMapper;
    private final InviteParticipantMapper participantMapper;
    private final InviteDeclineMapper declineMapper;
    private final UserMapper userMapper;

    /**
     * 获取邀约（未删除），不存在则抛 INVITE_NOT_FOUND。
     */
    public Invite getInviteOrThrow(Long inviteId) {
        Invite invite = inviteMapper.selectById(inviteId);
        if (invite == null || Boolean.TRUE.equals(invite.getDeleted())) {
            throw new BusinessException(ResultCode.INVITE_NOT_FOUND);
        }
        return invite;
    }

    /** 查询某用户在某邀约的参与记录（含已退出的），不存在则返回 null */
    public InviteParticipant findParticipant(Long inviteId, Long userId) {
        if (inviteId == null || userId == null) {
            return null;
        }
        return participantMapper.selectOne(
                new LambdaQueryWrapper<InviteParticipant>()
                        .eq(InviteParticipant::getInviteId, inviteId)
                        .eq(InviteParticipant::getUserId, userId));
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
    public IPage<InviteResponse> getInviteList(String type, String status, String timeRange, String keyword, Integer page, Integer size) {
        int current = (page == null || page < 1) ? 1 : page;
        int pageSize = (size == null || size < 1) ? 20 : Math.min(size, 100);

        LambdaQueryWrapper<Invite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Invite::getDeleted, false);
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Invite::getInviteType, type);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Invite::getStatus, status);
        } else {
            // 未指定 status 时，仅展示招募中
            wrapper.eq(Invite::getStatus, InviteStatusEnum.RECRUITING.name());
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String k = keyword.trim();
            wrapper.and(w -> w.like(Invite::getTitle, k).or().like(Invite::getContent, k).or().like(Invite::getLocation, k));
        }
        LocalDateTime now = LocalDateTime.now();
        String range = (timeRange != null && !timeRange.isEmpty()) ? timeRange.toUpperCase() : "WEEK";
        if ("WEEK".equals(range)) {
            wrapper.ge(Invite::getInviteTime, now.minusDays(7));
        } else if ("MONTH".equals(range)) {
            wrapper.ge(Invite::getInviteTime, now.minusDays(30));
        } else if ("YEAR".equals(range)) {
            wrapper.ge(Invite::getInviteTime, now.minusDays(365));
        }
        wrapper.orderByDesc(Invite::getInviteTime).orderByDesc(Invite::getCreatedAt);

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
                .map(inv -> buildInviteResponseWithCreator(inv, creatorMap.get(inv.getCreatorId()), "CREATOR", null, null))
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
        Map<Long, InviteParticipant> inviteIdToMyParticipant = myParticipants.stream()
                .collect(Collectors.toMap(InviteParticipant::getInviteId, p -> p, (a, b) -> a));
        List<Long> inviteIds = new java.util.ArrayList<>(inviteIdToMyParticipant.keySet());

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
                .map(inv -> {
                    InviteParticipant p = inviteIdToMyParticipant.get(inv.getId());
                    String role = p != null && p.getLeftAt() != null ? "LEFT" : "PARTICIPANT";
                    LocalDateTime leftAt = p != null ? p.getLeftAt() : null;
                    String leaveReason = p != null && p.getLeftAt() != null ? p.getLeftReason() : null;
                    return buildInviteResponseWithCreator(inv, creatorMap.get(inv.getCreatorId()), role, leftAt, leaveReason);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InviteResponse> getReceivedPendingInvites(String range) {
        Long currentUserId = CurrentUser.getId();
        if (currentUserId == null) return List.of();
        LocalDateTime from = getHistoryStartTime(range);
        LambdaQueryWrapper<Invite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Invite::getTargetUserId, currentUserId)
                .eq(Invite::getDeleted, false)
                .in(Invite::getStatus,
                        InviteStatusEnum.RECRUITING.name(),
                        InviteStatusEnum.FULL.name(),
                        InviteStatusEnum.CONFIRMED.name());
        if (from != null) wrapper.ge(Invite::getInviteTime, from);
        wrapper.orderByDesc(Invite::getInviteTime);
        List<Invite> list = inviteMapper.selectList(wrapper);
        if (list.isEmpty()) return List.of();
        List<Long> inviteIds = list.stream().map(Invite::getId).collect(Collectors.toList());
        List<InviteParticipant> myParts = participantMapper.selectList(
                new LambdaQueryWrapper<InviteParticipant>()
                        .eq(InviteParticipant::getUserId, currentUserId)
                        .in(InviteParticipant::getInviteId, inviteIds)
                        .isNull(InviteParticipant::getLeftAt));
        java.util.Set<Long> alreadyJoinedIds = myParts.stream().map(InviteParticipant::getInviteId).collect(Collectors.toSet());
        List<InviteDecline> declines = declineMapper.selectList(
                new LambdaQueryWrapper<InviteDecline>()
                        .eq(InviteDecline::getUserId, currentUserId)
                        .in(InviteDecline::getInviteId, inviteIds));
        java.util.Set<Long> declinedIds = declines.stream().map(InviteDecline::getInviteId).collect(Collectors.toSet());
        List<Invite> pending = list.stream()
                .filter(inv -> !alreadyJoinedIds.contains(inv.getId()) && !declinedIds.contains(inv.getId()))
                .collect(Collectors.toList());
        if (pending.isEmpty()) return List.of();
        Map<Long, User> creatorMap = batchLoadCreators(pending);
        return pending.stream()
                .map(inv -> buildInviteResponseWithCreator(inv, creatorMap.get(inv.getCreatorId()), "TARGET_PENDING", null, null))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InviteResponse> getMyInvitesList(String range) {
        List<InviteResponse> created = getMyCreatedInvites(range);
        List<InviteResponse> joined = getMyJoinedInvites(range);
        List<InviteResponse> receivedPending = getReceivedPendingInvites(range);
        java.util.Set<Long> createdIds = created.stream().map(InviteResponse::getId).collect(Collectors.toSet());
        java.util.Set<Long> joinedIds = joined.stream().map(InviteResponse::getId).collect(Collectors.toSet());
        List<InviteResponse> joinedOnly = joined.stream().filter(r -> !createdIds.contains(r.getId())).collect(Collectors.toList());
        List<InviteResponse> pendingOnly = receivedPending.stream()
                .filter(r -> !createdIds.contains(r.getId()) && !joinedIds.contains(r.getId()))
                .collect(Collectors.toList());
        List<InviteResponse> merged = new java.util.ArrayList<>(created);
        merged.addAll(joinedOnly);
        merged.addAll(pendingOnly);
        merged.sort((a, b) -> {
            int pa = getDisplayPriority(a);
            int pb = getDisplayPriority(b);
            if (pa != pb) return Integer.compare(pb, pa);
            LocalDateTime ca = a.getCreatedAt();
            LocalDateTime cb = b.getCreatedAt();
            if (ca == null && cb == null) return 0;
            if (ca == null) return 1;
            if (cb == null) return -1;
            return cb.compareTo(ca);
        });
        return merged;
    }

    @Transactional(readOnly = true)
    public InviteResponse getInviteDetail(Long inviteId) {
        Invite invite = getInviteOrThrow(inviteId);
        InviteResponse response = buildInviteDetailResponse(invite);
        Long currentUserId = CurrentUser.getId();
        if (currentUserId != null) {
            if (currentUserId.equals(invite.getCreatorId())) {
                response.setMyRole("CREATOR");
                response.setMyLeftAt(null);
            } else {
                InviteParticipant myParticipant = participantMapper.selectOne(
                        new LambdaQueryWrapper<InviteParticipant>()
                                .eq(InviteParticipant::getInviteId, inviteId)
                                .eq(InviteParticipant::getUserId, currentUserId));
                if (myParticipant != null && myParticipant.getLeftAt() != null) {
                    response.setMyRole("LEFT");
                    response.setMyLeftAt(myParticipant.getLeftAt());
                    response.setMyLeaveReason(myParticipant.getLeftReason());
                } else if (myParticipant != null) {
                    response.setMyRole("PARTICIPANT");
                    response.setMyLeftAt(null);
                }
            }
        }
        return response;
    }

    @Transactional(readOnly = true)
    public List<InviteTypeCountResponse> getHotInviteTypeCounts(Integer limit) {
        int size = Optional.ofNullable(limit).orElse(10);
        size = Math.min(Math.max(size, 1), 10);

        QueryWrapper<Invite> wrapper = new QueryWrapper<>();
        wrapper.select("invite_type AS inviteType", "COUNT(*) AS cnt")
                .eq("deleted", 0)
                .in("status",
                        InviteStatusEnum.RECRUITING.name(),
                        InviteStatusEnum.FULL.name(),
                        InviteStatusEnum.CONFIRMED.name(),
                        InviteStatusEnum.IN_PROGRESS.name())
                .groupBy("invite_type")
                .orderByDesc("cnt")
                .last("LIMIT " + size);

        return inviteMapper.selectMaps(wrapper).stream()
                .map(m -> new InviteTypeCountResponse(
                        (String) m.get("inviteType"),
                        m.get("cnt") == null ? 0L : ((Number) m.get("cnt")).longValue()
                ))
                .collect(Collectors.toList());
    }

    private int getDisplayPriority(InviteResponse r) {
        if (r == null) return -1;
        if ("TARGET_PENDING".equals(r.getMyRole())) return 6;
        if ("LEFT".equals(r.getMyRole())) return 1;
        String status = r.getStatus();
        if (status == null) return 2;
        return switch (status) {
            case "RECRUITING" -> 5;
            case "FULL", "CONFIRMED" -> 4;
            case "IN_PROGRESS" -> 3;
            case "ENDED" -> 2;
            case "CANCELLED" -> 0;
            default -> 2;
        };
    }

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
        return buildInviteResponseWithCreator(invite, creator, null, null, null);
    }

    private InviteResponse buildInviteResponseWithCreator(Invite invite, User creator, String myRole, LocalDateTime myLeftAt, String myLeaveReason) {
        InviteResponse.CreatorInfo targetUserInfo = null;
        if (invite.getTargetUserId() != null) {
            User target = userMapper.selectById(invite.getTargetUserId());
            if (target != null) {
                targetUserInfo = InviteResponse.CreatorInfo.builder()
                        .id(target.getId())
                        .nickname(target.getNickname())
                        .avatarUrl(target.getAvatarUrl())
                        .creditScore(target.getCreditScore())
                        .build();
            }
        }
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
                .myRole(myRole)
                .myLeftAt(myLeftAt)
                .myLeaveReason(myLeaveReason)
                .creator(creator != null ? InviteResponse.CreatorInfo.builder()
                        .id(creator.getId())
                        .nickname(creator.getNickname())
                        .avatarUrl(creator.getAvatarUrl())
                        .creditScore(creator.getCreditScore())
                        .build() : null)
                .targetUser(targetUserInfo)
                .build();
    }

    private InviteResponse buildInviteDetailResponse(Invite invite) {
        InviteResponse response = buildInviteResponse(invite);

        List<InviteParticipant> participants = participantMapper.selectList(
                new LambdaQueryWrapper<InviteParticipant>()
                        .eq(InviteParticipant::getInviteId, invite.getId())
                        .isNull(InviteParticipant::getLeftAt));
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
