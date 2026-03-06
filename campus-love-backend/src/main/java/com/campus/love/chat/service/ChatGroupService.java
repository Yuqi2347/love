package com.campus.love.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.chat.entity.ChatGroup;
import com.campus.love.chat.entity.ChatGroupMember;
import com.campus.love.chat.mapper.ChatGroupMapper;
import com.campus.love.chat.mapper.ChatGroupMemberMapper;
import com.campus.love.chat.constants.ChatConstants;
import com.campus.love.invite.entity.Invite;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatGroupService {

    private final ChatGroupMapper chatGroupMapper;
    private final ChatGroupMemberMapper chatGroupMemberMapper;

    /**
     * 为公开邀约创建临时群聊（若已存在则直接返回）
     */
    @Transactional
    public ChatGroup createGroupIfAbsent(Invite invite) {
        if (invite.getChatGroupId() != null) {
            ChatGroup existingById = chatGroupMapper.selectById(invite.getChatGroupId());
            if (existingById != null) {
                return existingById;
            }
            // 邀约记录了 groupId 但群已被删或不存在，下面按 inviteId 查或新建
        }
        ChatGroup existing = chatGroupMapper.selectOne(
                new LambdaQueryWrapper<ChatGroup>()
                        .eq(ChatGroup::getInviteId, invite.getId())
        );
        if (existing != null) {
            return existing;
        }

        ChatGroup group = new ChatGroup();
        group.setInviteId(invite.getId());
        group.setName(invite.getTitle() != null && !invite.getTitle().isEmpty() ? invite.getTitle() : "邀约群聊");
        group.setCreatedBy(invite.getCreatorId());
        group.setStatus(ChatConstants.STATUS_ACTIVE);
        chatGroupMapper.insert(group);

        // 创建者自动加入群
        addMemberIfAbsent(group.getId(), invite.getCreatorId());

        return group;
    }

    /**
     * 将用户加入群聊（若已存在则跳过）
     */
    @Transactional
    public void addMemberIfAbsent(Long groupId, Long userId) {
        if (groupId == null || userId == null) {
            return;
        }
        ChatGroupMember existing = chatGroupMemberMapper.selectOne(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getGroupId, groupId)
                        .eq(ChatGroupMember::getUserId, userId)
        );
        if (existing != null) {
            return;
        }
        ChatGroupMember member = new ChatGroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setIsMuted(false);
        chatGroupMemberMapper.insert(member);
    }

    /**
     * 查询群成员ID列表
     */
    public List<ChatGroupMember> getMembers(Long groupId) {
        return chatGroupMemberMapper.selectList(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getGroupId, groupId)
        );
    }

    /**
     * 查询用户加入的群聊列表
     */
    public List<ChatGroup> getGroupsByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        List<ChatGroupMember> members = chatGroupMemberMapper.selectList(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getUserId, userId));
        if (members.isEmpty()) {
            return List.of();
        }
        List<Long> groupIds = members.stream()
                .map(ChatGroupMember::getGroupId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (groupIds.isEmpty()) {
            return List.of();
        }
        return chatGroupMapper.selectBatchIds(groupIds);
    }
}

