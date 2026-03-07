package com.campus.love.follow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.enums.FollowStatusEnum;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.follow.dto.FollowResponse;
import com.campus.love.follow.entity.Follow;
import com.campus.love.follow.mapper.FollowMapper;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowMapper followMapper;
    private final UserMapper userMapper;

    @Transactional
    public void follow(Long targetUserId) {
        Long currentUserId = CurrentUser.getId();
        if (currentUserId.equals(targetUserId)) {
            throw new BusinessException(ResultCode.CANNOT_FOLLOW_SELF);
        }

        Follow existing = followMapper.selectOne(
                new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowerId, currentUserId)
                        .eq(Follow::getFollowingId, targetUserId));
        if (existing != null) {
            throw new BusinessException(ResultCode.ALREADY_FOLLOWED);
        }

        Follow follow = new Follow();
        follow.setFollowerId(currentUserId);
        follow.setFollowingId(targetUserId);
        follow.setIsMutual(false);
        followMapper.insert(follow);

        // Check if target already follows current user -> mutual
        Follow reverse = followMapper.selectOne(
                new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowerId, targetUserId)
                        .eq(Follow::getFollowingId, currentUserId));
        if (reverse != null) {
            follow.setIsMutual(true);
            followMapper.updateById(follow);
            reverse.setIsMutual(true);
            followMapper.updateById(reverse);
        }
    }

    @Transactional
    public void unfollow(Long targetUserId) {
        Long currentUserId = CurrentUser.getId();

        Follow follow = followMapper.selectOne(
                new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowerId, currentUserId)
                        .eq(Follow::getFollowingId, targetUserId));
        if (follow == null) {
            throw new BusinessException(ResultCode.NOT_FOLLOWED);
        }

        followMapper.deleteById(follow.getId());

        // If was mutual, update reverse to non-mutual
        if (Boolean.TRUE.equals(follow.getIsMutual())) {
            Follow reverse = followMapper.selectOne(
                    new LambdaQueryWrapper<Follow>()
                            .eq(Follow::getFollowerId, targetUserId)
                            .eq(Follow::getFollowingId, currentUserId));
            if (reverse != null) {
                reverse.setIsMutual(false);
                followMapper.updateById(reverse);
            }
        }
    }

    public FollowStatusEnum getFollowStatus(Long targetUserId) {
        Long currentUserId = CurrentUser.getId();
        Follow follow = followMapper.selectOne(
                new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowerId, currentUserId)
                        .eq(Follow::getFollowingId, targetUserId));
        if (follow == null) return FollowStatusEnum.NONE;
        return Boolean.TRUE.equals(follow.getIsMutual()) ? FollowStatusEnum.MUTUAL : FollowStatusEnum.ONE_WAY;
    }

    public boolean isMutual(Long userId1, Long userId2) {
        Follow follow = followMapper.selectOne(
                new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowerId, userId1)
                        .eq(Follow::getFollowingId, userId2)
                        .eq(Follow::getIsMutual, true));
        return follow != null;
    }

    public List<FollowResponse> getFollowingList(Long userId) {
        List<Follow> follows = followMapper.selectList(
                new LambdaQueryWrapper<Follow>().eq(Follow::getFollowerId, userId));
        return follows.stream().map(f -> buildFollowResponse(f.getFollowingId(), f.getIsMutual()))
                .collect(Collectors.toList());
    }

    public List<FollowResponse> getFollowerList(Long userId) {
        List<Follow> followers = followMapper.selectList(
                new LambdaQueryWrapper<Follow>().eq(Follow::getFollowingId, userId));
        return followers.stream().map(f -> buildFollowResponse(f.getFollowerId(), f.getIsMutual()))
                .collect(Collectors.toList());
    }

    public List<Long> getMutualFollowIds(Long userId) {
        return followMapper.selectList(
                        new LambdaQueryWrapper<Follow>()
                                .eq(Follow::getFollowerId, userId)
                                .eq(Follow::getIsMutual, true))
                .stream()
                .map(Follow::getFollowingId)
                .collect(Collectors.toList());
    }

    /** 新粉丝数量（关注我且 created_at > 上次查看粉丝时间），用于导航/个人页红点；从未查看过则返回 0 */
    public int getNewFollowerCount(Long userId) {
        User user = userMapper.selectById(userId);
        LocalDateTime since = user != null ? user.getLastFollowerViewedAt() : null;
        if (since == null) return 0;
        Long count = followMapper.selectCount(
                new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowingId, userId)
                        .gt(Follow::getCreatedAt, since));
        return count != null ? count.intValue() : 0;
    }

    /** 标记粉丝列表已查看，消除新粉丝红点 */
    @Transactional
    public void markFollowersViewed(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return;
        user.setLastFollowerViewedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    private FollowResponse buildFollowResponse(Long userId, Boolean isMutual) {
        User user = userMapper.selectById(userId);
        if (user == null) return null;
        return FollowResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .isMutual(isMutual)
                .build();
    }
}
