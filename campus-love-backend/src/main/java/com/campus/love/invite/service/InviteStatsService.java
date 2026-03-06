package com.campus.love.invite.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.constants.InviteCreditConstants;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.invite.dto.InviteRatingCreateRequest;
import com.campus.love.invite.dto.InviteStatsResponse;
import com.campus.love.invite.entity.Invite;
import com.campus.love.invite.entity.InviteParticipant;
import com.campus.love.invite.entity.InviteRating;
import com.campus.love.invite.enums.InviteStatusEnum;
import com.campus.love.invite.mapper.InviteMapper;
import com.campus.love.invite.mapper.InviteParticipantMapper;
import com.campus.love.invite.mapper.InviteRatingMapper;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 邀约统计与评价：创建评价、我的/用户统计、邀约评分快照更新。
 */
@Service
@RequiredArgsConstructor
public class InviteStatsService {

    private final InviteMapper inviteMapper;
    private final InviteRatingMapper ratingMapper;
    private final InviteParticipantMapper participantMapper;
    private final UserMapper userMapper;
    private final InviteCreditService creditService;
    private final InviteCrudService crudService;

    @Transactional
    public void createRating(InviteRatingCreateRequest request) {
        Long currentUserId = CurrentUser.getId();
        if (request == null || request.getInviteId() == null || request.getRatedUserId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "邀约ID与被评价人不能为空");
        }
        if (request.getSocialRating() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "社交体验评分不能为空");
        }

        Invite invite = crudService.getInviteOrThrow(request.getInviteId());

        if (!InviteStatusEnum.ENDED.name().equals(invite.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "邀约未结束，无法评价");
        }

        InviteParticipant participant = participantMapper.selectOne(
                new LambdaQueryWrapper<InviteParticipant>()
                        .eq(InviteParticipant::getInviteId, request.getInviteId())
                        .eq(InviteParticipant::getUserId, currentUserId));
        if (participant == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "未参与该邀约，无法评价");
        }

        InviteRating existing = ratingMapper.selectOne(
                new LambdaQueryWrapper<InviteRating>()
                        .eq(InviteRating::getInviteId, request.getInviteId())
                        .eq(InviteRating::getRaterId, currentUserId)
                        .eq(InviteRating::getRatedUserId, request.getRatedUserId()));
        if (existing != null) {
            throw new BusinessException(ResultCode.ALREADY_RATED);
        }

        long myRatingsForInvite = ratingMapper.selectCount(
                new LambdaQueryWrapper<InviteRating>()
                        .eq(InviteRating::getInviteId, request.getInviteId())
                        .eq(InviteRating::getRaterId, currentUserId));
        if (myRatingsForInvite == 0) {
            creditService.adjustCreditScore(currentUserId, InviteCreditConstants.CREDIT_SUCCESS_COMPLETE);
        }

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

        if (request.getSocialRating() >= 4.0) {
            creditService.adjustCreditScore(request.getRatedUserId(), 3);
        }

        if (request.getRatedUserId().equals(invite.getCreatorId())) {
            updateInviteRating(request.getInviteId());
        }
    }

    @Transactional(readOnly = true)
    public InviteStatsResponse getMyInviteStats() {
        Long currentUserId = CurrentUser.getId();
        User user = userMapper.selectById(currentUserId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return buildInviteStatsForUser(currentUserId, user);
    }

    @Transactional(readOnly = true)
    public InviteStatsResponse getUserInviteStats(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return buildInviteStatsForUser(userId, user);
    }

    private InviteStatsResponse buildInviteStatsForUser(Long userId, User user) {
        long totalInvites = inviteMapper.selectCount(
                new LambdaQueryWrapper<Invite>()
                        .eq(Invite::getCreatorId, userId)
                        .eq(Invite::getDeleted, false));
        long completedInvites = inviteMapper.selectCount(
                new LambdaQueryWrapper<Invite>()
                        .eq(Invite::getCreatorId, userId)
                        .eq(Invite::getStatus, InviteStatusEnum.ENDED.name())
                        .eq(Invite::getDeleted, false));
        List<InviteRating> receivedRatings = ratingMapper.selectList(
                new LambdaQueryWrapper<InviteRating>()
                        .eq(InviteRating::getRatedUserId, userId));
        BigDecimal avgSocialRating = computeAvgSocialRating(receivedRatings);
        BigDecimal avgOrgRating = computeAvgOrgRatingFromReceived(receivedRatings);
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
        BigDecimal receivedOrgRating = computeAvgOrgRatingFromReceived(orgRatings);
        return InviteStatsResponse.builder()
                .inviteCount((int) totalInvites)
                .participateCount(user.getParticipantCountOrDefault())
                .successRate(totalInvites > 0 ? BigDecimal.valueOf(completedInvites)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalInvites), 1, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .avgSocialRating(avgSocialRating.compareTo(BigDecimal.ZERO) == 0 && !receivedRatings.isEmpty() ? null : avgSocialRating)
                .avgOrgRating(avgOrgRating.compareTo(BigDecimal.ZERO) == 0 && receivedRatings.stream().noneMatch(rating -> rating.getOrgRating() != null) ? null : avgOrgRating)
                .receivedSocialRating(avgSocialRating.compareTo(BigDecimal.ZERO) == 0 && !receivedRatings.isEmpty() ? null : avgSocialRating)
                .receivedOrgRating(receivedOrgRating.compareTo(BigDecimal.ZERO) == 0 && !orgRatings.isEmpty() ? null : receivedOrgRating)
                .build();
    }

    private static BigDecimal computeAvgSocialRating(List<InviteRating> receivedRatings) {
        if (receivedRatings == null || receivedRatings.isEmpty()) return BigDecimal.ZERO;
        return receivedRatings.stream()
                .map(InviteRating::getSocialRating)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(receivedRatings.size()), 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal computeAvgOrgRatingFromReceived(List<InviteRating> ratings) {
        if (ratings == null || ratings.isEmpty()) return BigDecimal.ZERO;
        long withOrg = ratings.stream().filter(rating -> rating.getOrgRating() != null).count();
        if (withOrg == 0) return BigDecimal.ZERO;
        return ratings.stream()
                .map(InviteRating::getOrgRating)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(withOrg), 1, RoundingMode.HALF_UP);
    }

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

        long withOrg = ratings.stream().filter(r -> r.getOrgRating() != null).count();
        BigDecimal avgOrg = withOrg == 0 ? BigDecimal.ZERO
                : ratings.stream()
                        .map(InviteRating::getOrgRating)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(withOrg), 1, RoundingMode.HALF_UP);

        Invite invite = inviteMapper.selectById(inviteId);
        if (invite != null) {
            invite.setSocialRating(avgSocial);
            invite.setOrgRating(avgOrg);
            invite.setRatingCount(ratings.size());
            inviteMapper.updateById(invite);
        }
    }
}
