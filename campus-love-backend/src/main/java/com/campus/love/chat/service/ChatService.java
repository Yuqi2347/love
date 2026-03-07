package com.campus.love.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.love.auth.security.CurrentUser;
import com.campus.love.chat.dto.ChatGroupItemResponse;
import com.campus.love.chat.dto.ChatMessageResponse;
import com.campus.love.chat.dto.ConversationResponse;
import com.campus.love.chat.entity.ChatGroup;
import com.campus.love.chat.entity.ChatGroupMember;
import com.campus.love.chat.entity.Message;
import com.campus.love.chat.mapper.MessageMapper;
import com.campus.love.chat.service.ChatGroupService;
import com.campus.love.chat.constants.ChatConstants;
import com.campus.love.common.constants.DateTimeConstants;
import com.campus.love.common.constants.RedisKeyConstants;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.common.service.FileUploadService;
import com.campus.love.follow.service.FollowService;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final FollowService followService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChatGroupService chatGroupService;
    private final FileUploadService fileUploadService;

    @Value("${app.follow.daily-chat-limit}")
    private int dailyChatLimit;

    /**
     * 上传聊天图片，返回可访问的 URL（供 msgType=3 图片消息使用，msgType=2 保留给邀约链接）
     */
    public String uploadChatImage(MultipartFile file) throws IOException {
        return fileUploadService.uploadImage(file, "chat_");
    }

    public ChatMessageResponse sendMessage(Long senderId, Long receiverId, String content, Integer msgType) {
        if (receiverId == null || content == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "接收人与消息内容不能为空");
        }
        if (senderId != null && senderId.equals(receiverId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能给自己发消息");
        }
        boolean mutual = followService.isMutual(senderId, receiverId);

        if (!mutual) {
            String key = RedisKeyConstants.chatDailyCount(senderId, receiverId, LocalDate.now().toString());
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, 1, TimeUnit.DAYS);
            }
            if (count != null && count > dailyChatLimit) {
                throw new BusinessException(ResultCode.CHAT_LIMIT_EXCEEDED);
            }
        }

        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setGroupId(null);
        message.setContent(content);
        message.setMsgType(msgType != null ? msgType : 1);
        message.setIsRead(false);
        // 确保数据库与 WebSocket 回执中都有一致的创建时间（使用北京时间）
        message.setCreatedAt(ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toLocalDateTime());
        messageMapper.insert(message);

        User sender = userMapper.selectById(senderId);
        return ChatMessageResponse.builder()
                .id(message.getId())
                .senderId(senderId)
                .receiverId(receiverId)
                .groupId(null)
                .senderNickname(sender != null ? sender.getNickname() : "")
                .senderAvatar(sender != null ? sender.getAvatarUrl() : "")
                .content(content)
                .msgType(message.getMsgType())
                .isRead(false)
                .createdAt(message.getCreatedAt() != null ? message.getCreatedAt().format(DateTimeConstants.DATETIME_FMT) : "")
                .build();
    }

    public List<ChatMessageResponse> getChatHistory(Long otherUserId, int page, int size) {
        Long currentUserId = CurrentUser.getId();
        int current = (page <= 0) ? 1 : page;
        int pageSize = (size <= 0) ? 20 : Math.min(size, 100);
        Page<Message> pageReq = new Page<>(current, pageSize);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .and(w -> w
                        .and(q -> q.eq(Message::getSenderId, currentUserId).eq(Message::getReceiverId, otherUserId))
                        .or(q -> q.eq(Message::getSenderId, otherUserId).eq(Message::getReceiverId, currentUserId))
                )
                .isNull(Message::getGroupId)
                .orderByDesc(Message::getCreatedAt);
        Page<Message> messagePage = messageMapper.selectPage(pageReq, wrapper);
        List<Message> messages = messagePage.getRecords();
        if (messages.isEmpty()) {
            return List.of();
        }
        List<Long> senderIds = messages.stream().map(Message::getSenderId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, User> userCache = userMapper.selectBatchIds(senderIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        return messages.stream().map(msg -> {
            User sender = userCache.get(msg.getSenderId());
            return ChatMessageResponse.builder()
                    .id(msg.getId())
                    .senderId(msg.getSenderId())
                    .receiverId(msg.getReceiverId())
                    .senderNickname(sender != null ? sender.getNickname() : "")
                    .senderAvatar(sender != null ? sender.getAvatarUrl() : "")
                    .content(msg.getContent())
                    .msgType(msg.getMsgType())
                    .isRead(msg.getIsRead())
                    .createdAt(msg.getCreatedAt() != null ? msg.getCreatedAt().format(DateTimeConstants.DATETIME_FMT) : "")
                    .build();
        }).collect(Collectors.toList());
    }

    public List<ConversationResponse> getConversations() {
        Long currentUserId = CurrentUser.getId();
        List<Message> allMessages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .isNull(Message::getGroupId)
                        .and(w -> w
                                .eq(Message::getSenderId, currentUserId)
                                .or(q -> q.eq(Message::getReceiverId, currentUserId))
                        )
                        .orderByDesc(Message::getCreatedAt)
        );

        Map<Long, Message> latestByUser = new LinkedHashMap<>();
        Map<Long, Integer> unreadCounts = new HashMap<>();

        for (Message msg : allMessages) {
            Long otherUserId = msg.getSenderId().equals(currentUserId) ? msg.getReceiverId() : msg.getSenderId();
            latestByUser.putIfAbsent(otherUserId, msg);
            if (msg.getReceiverId().equals(currentUserId) && !Boolean.TRUE.equals(msg.getIsRead())) {
                unreadCounts.merge(otherUserId, 1, Integer::sum);
            }
        }

        List<Long> otherUserIds = new java.util.ArrayList<>(latestByUser.keySet());
        Map<Long, User> userMap = otherUserIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(otherUserIds).stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        return latestByUser.entrySet().stream().map(entry -> {
            Long otherUserId = entry.getKey();
            Message lastMsg = entry.getValue();
            User otherUser = userMap.get(otherUserId);
            int inviteMsgType = com.campus.love.common.enums.MsgTypeEnum.INVITE.getCode();
            String lastMsgText = (lastMsg.getMsgType() != null && lastMsg.getMsgType().intValue() == inviteMsgType)
                    ? "[邀约邀请]" : (lastMsg.getContent() != null ? lastMsg.getContent() : "");
            return ConversationResponse.builder()
                    .userId(otherUserId)
                    .nickname(otherUser != null ? otherUser.getNickname() : "")
                    .avatarUrl(otherUser != null ? otherUser.getAvatarUrl() : "")
                    .lastMessage(lastMsgText)
                    .lastTime(lastMsg.getCreatedAt() != null ? lastMsg.getCreatedAt().format(DateTimeConstants.DATETIME_FMT) : "")
                    .unreadCount(unreadCounts.getOrDefault(otherUserId, 0))
                    .build();
        }).collect(Collectors.toList());
    }

    /** 当前用户私聊未读消息总数（用于导航红点） */
    public int getTotalUnreadCount() {
        Long currentUserId = CurrentUser.getId();
        Long count = messageMapper.selectCount(
                new LambdaQueryWrapper<Message>()
                        .isNull(Message::getGroupId)
                        .eq(Message::getReceiverId, currentUserId)
                        .eq(Message::getIsRead, false));
        return count != null ? count.intValue() : 0;
    }

    public void markAsRead(Long otherUserId) {
        Long currentUserId = CurrentUser.getId();
        messageMapper.update(null,
                new LambdaUpdateWrapper<Message>()
                        .isNull(Message::getGroupId)
                        .eq(Message::getSenderId, otherUserId)
                        .eq(Message::getReceiverId, currentUserId)
                        .eq(Message::getIsRead, false)
                        .set(Message::getIsRead, true));
    }

    /**
     * 向群聊发送消息（不做互关/频率限制）
     */
    public ChatMessageResponse sendGroupMessage(Long senderId, Long groupId, String content, Integer msgType) {
        // 这里只负责持久化消息，具体的 WebSocket 广播由 WebSocketHandler 根据 groupId + 群成员列表完成
        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(ChatConstants.GROUP_RECEIVER_ID_NONE);
        message.setGroupId(groupId);
        message.setContent(content);
        message.setMsgType(msgType != null ? msgType : 1);
        message.setIsRead(false);
        // 确保群聊消息也有创建时间，供讨论区显示（使用北京时间）
        message.setCreatedAt(ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toLocalDateTime());
        messageMapper.insert(message);

        User sender = userMapper.selectById(senderId);
        return ChatMessageResponse.builder()
                .id(message.getId())
                .senderId(senderId)
                .receiverId(ChatConstants.GROUP_RECEIVER_ID_NONE)
                .groupId(groupId)
                .senderNickname(sender != null ? sender.getNickname() : "")
                .senderAvatar(sender != null ? sender.getAvatarUrl() : "")
                .content(content)
                .msgType(message.getMsgType())
                .isRead(false)
                .createdAt(message.getCreatedAt() != null ? message.getCreatedAt().format(DateTimeConstants.DATETIME_FMT) : "")
                .build();
    }

    /**
     * 我加入的群聊列表（用于展示「群聊」入口及最近一条消息）
     */
    public List<ChatGroupItemResponse> getMyGroupList() {
        Long currentUserId = CurrentUser.getId();
        if (currentUserId == null) {
            return List.of();
        }
        List<ChatGroup> groups = chatGroupService.getGroupsByUserId(currentUserId);
        if (groups.isEmpty()) {
            return List.of();
        }
        List<Long> groupIds = groups.stream().map(ChatGroup::getId).filter(Objects::nonNull).toList();
        List<Message> recentMessages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .in(Message::getGroupId, groupIds)
                        .orderByDesc(Message::getCreatedAt));
        Map<Long, Message> lastByGroup = new LinkedHashMap<>();
        for (Message m : recentMessages) {
            if (m.getGroupId() != null) {
                lastByGroup.putIfAbsent(m.getGroupId(), m);
            }
        }
        return groups.stream().map(g -> {
            Message last = lastByGroup.get(g.getId());
            List<ChatGroupMember> members = chatGroupService.getMembers(g.getId());
            int memberCount = members.size();
            List<Long> userIds = members.stream()
                    .map(ChatGroupMember::getUserId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            List<String> memberAvatarUrls = userIds.isEmpty() ? List.of()
                    : userMapper.selectBatchIds(userIds).stream()
                            .filter(Objects::nonNull)
                            .map(User::getAvatarUrl)
                            .filter(Objects::nonNull)
                            .toList();
            return ChatGroupItemResponse.builder()
                    .groupId(g.getId())
                    .inviteId(g.getInviteId())
                    .name(g.getName() != null ? g.getName() : "邀约群聊")
                    .memberCount(memberCount)
                    .lastMessage(last != null ? last.getContent() : null)
                    .lastTime(last != null && last.getCreatedAt() != null
                            ? last.getCreatedAt().format(DateTimeConstants.TIME_FMT) : null)
                    .memberAvatarUrls(memberAvatarUrls)
                    .build();
        }).toList();
    }

    /**
     * 群聊历史消息（仅群成员可拉取）
     */
    public List<ChatMessageResponse> getGroupChatHistory(Long groupId, int page, int size) {
        Long currentUserId = CurrentUser.getId();
        if (currentUserId == null || groupId == null) {
            return List.of();
        }
        List<ChatGroupMember> members = chatGroupService.getMembers(groupId);
        boolean isMember = members.stream().anyMatch(m -> currentUserId.equals(m.getUserId()));
        if (!isMember) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅群成员可查看群聊记录");
        }
        int current = (page <= 0) ? 1 : page;
        int pageSize = (size <= 0) ? 20 : Math.min(size, 100);
        Page<Message> pageReq = new Page<>(current, pageSize);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getGroupId, groupId)
                .orderByDesc(Message::getCreatedAt);
        Page<Message> messagePage = messageMapper.selectPage(pageReq, wrapper);
        List<Message> messages = messagePage.getRecords();
        if (messages.isEmpty()) {
            return List.of();
        }
        List<Long> senderIds = messages.stream().map(Message::getSenderId).filter(Objects::nonNull).distinct().toList();
        Map<Long, User> userCache = userMapper.selectBatchIds(senderIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        return messages.stream().map(msg -> {
            User sender = userCache.get(msg.getSenderId());
            return ChatMessageResponse.builder()
                    .id(msg.getId())
                    .senderId(msg.getSenderId())
                    .receiverId(msg.getReceiverId())
                    .groupId(msg.getGroupId())
                    .senderNickname(sender != null ? sender.getNickname() : "")
                    .senderAvatar(sender != null ? sender.getAvatarUrl() : "")
                    .content(msg.getContent())
                    .msgType(msg.getMsgType())
                    .isRead(msg.getIsRead())
                    .createdAt(msg.getCreatedAt() != null ? msg.getCreatedAt().format(DateTimeConstants.DATETIME_FMT) : "")
                    .build();
        }).toList();
    }
}
