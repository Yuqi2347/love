package com.campus.love.invite.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.chat.entity.Message;
import com.campus.love.chat.mapper.MessageMapper;
import com.campus.love.invite.entity.Invite;
import com.campus.love.invite.entity.InviteParticipant;
import com.campus.love.invite.mapper.InviteMapper;
import com.campus.love.invite.mapper.InviteParticipantMapper;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 邀约活动红点：我的邀约有人加入/发言、等待邀约匹配成功（我被人加入某邀约）
 */
@Service
@RequiredArgsConstructor
public class InviteActivityService {

    private final InviteMapper inviteMapper;
    private final InviteParticipantMapper participantMapper;
    private final MessageMapper messageMapper;
    private final UserMapper userMapper;

    /** 邀约新活动数（我的邀约有人加入/发言、我被人加入）；从未查看过则返回 0 */
    public int getNewInviteActivityCount(Long userId) {
        User user = userMapper.selectById(userId);
        LocalDateTime since = user != null ? user.getLastInviteActivityViewedAt() : null;
        if (since == null) return 0;

        int count = 0;

        // 1) 我发起的邀约有新成员加入
        List<Long> myInviteIds = inviteMapper.selectList(
                        new LambdaQueryWrapper<Invite>()
                                .eq(Invite::getCreatorId, userId)
                                .eq(Invite::getDeleted, false))
                .stream().map(Invite::getId).collect(Collectors.toList());
        if (!myInviteIds.isEmpty()) {
            Long c = participantMapper.selectCount(
                    new LambdaQueryWrapper<InviteParticipant>()
                            .in(InviteParticipant::getInviteId, myInviteIds)
                            .gt(InviteParticipant::getJoinAt, since));
            count += (c != null ? c.intValue() : 0);
        }

        // 2) 我发起的邀约关联的群聊有新消息（非我发的）
        List<Long> myGroupIds = inviteMapper.selectList(
                        new LambdaQueryWrapper<Invite>()
                                .eq(Invite::getCreatorId, userId)
                                .eq(Invite::getDeleted, false)
                                .isNotNull(Invite::getChatGroupId))
                .stream().map(Invite::getChatGroupId).filter(java.util.Objects::nonNull).collect(Collectors.toList());
        if (!myGroupIds.isEmpty()) {
            Long c = messageMapper.selectCount(
                    new LambdaQueryWrapper<Message>()
                            .in(Message::getGroupId, myGroupIds)
                            .ne(Message::getSenderId, userId)
                            .gt(Message::getCreatedAt, since));
            count += (c != null ? c.intValue() : 0);
        }

        // 3) 我被人加入的邀约（含等待匹配成功）
        Long c = participantMapper.selectCount(
                new LambdaQueryWrapper<InviteParticipant>()
                        .eq(InviteParticipant::getUserId, userId)
                        .gt(InviteParticipant::getJoinAt, since));
        count += (c != null ? c.intValue() : 0);

        return count;
    }

    @Transactional
    public void markInviteActivityViewed(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return;
        user.setLastInviteActivityViewedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }
}
