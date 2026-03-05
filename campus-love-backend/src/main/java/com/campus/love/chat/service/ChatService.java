package com.campus.love.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.love.auth.security.CurrentUser;
import com.campus.love.chat.dto.ChatMessageResponse;
import com.campus.love.chat.dto.ConversationResponse;
import com.campus.love.chat.entity.Message;
import com.campus.love.chat.mapper.MessageMapper;
import com.campus.love.chat.service.ChatGroupService;
import com.campus.love.common.constants.RedisKeyConstants;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.follow.service.FollowService;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    @Value("${app.follow.daily-chat-limit}")
    private int dailyChatLimit;

    public ChatMessageResponse sendMessage(Long senderId, Long receiverId, String content, Integer msgType) {
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
                .createdAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .build();
    }

    public List<ChatMessageResponse> getChatHistory(Long otherUserId, int page, int size) {
        Long currentUserId = CurrentUser.getId();
        List<Message> messages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .and(w -> w
                                .and(q -> q.eq(Message::getSenderId, currentUserId).eq(Message::getReceiverId, otherUserId))
                                .or(q -> q.eq(Message::getSenderId, otherUserId).eq(Message::getReceiverId, currentUserId))
                        )
                        .isNull(Message::getGroupId)
                        .orderByDesc(Message::getCreatedAt)
                        .last("LIMIT " + (page * size) + "," + size)
        );

        Map<Long, User> userCache = new HashMap<>();
        return messages.stream().map(msg -> {
            User sender = userCache.computeIfAbsent(msg.getSenderId(), userMapper::selectById);
            return ChatMessageResponse.builder()
                    .id(msg.getId())
                    .senderId(msg.getSenderId())
                    .receiverId(msg.getReceiverId())
                    .senderNickname(sender != null ? sender.getNickname() : "")
                    .senderAvatar(sender != null ? sender.getAvatarUrl() : "")
                    .content(msg.getContent())
                    .msgType(msg.getMsgType())
                    .isRead(msg.getIsRead())
                    .createdAt(msg.getCreatedAt() != null ? msg.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "")
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

        return latestByUser.entrySet().stream().map(entry -> {
            Long otherUserId = entry.getKey();
            Message lastMsg = entry.getValue();
            User otherUser = userMapper.selectById(otherUserId);
            return ConversationResponse.builder()
                    .userId(otherUserId)
                    .nickname(otherUser != null ? otherUser.getNickname() : "")
                    .avatarUrl(otherUser != null ? otherUser.getAvatarUrl() : "")
                    .lastMessage(lastMsg.getContent())
                    .lastTime(lastMsg.getCreatedAt() != null ? lastMsg.getCreatedAt().format(DateTimeFormatter.ofPattern("MM-dd HH:mm")) : "")
                    .unreadCount(unreadCounts.getOrDefault(otherUserId, 0))
                    .build();
        }).collect(Collectors.toList());
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
        // 群聊消息不依赖 receiverId，可设为 0
        message.setReceiverId(0L);
        message.setGroupId(groupId);
        message.setContent(content);
        message.setMsgType(msgType != null ? msgType : 1);
        message.setIsRead(false);
        messageMapper.insert(message);

        User sender = userMapper.selectById(senderId);
        return ChatMessageResponse.builder()
                .id(message.getId())
                .senderId(senderId)
                .receiverId(0L)
                .groupId(groupId)
                .senderNickname(sender != null ? sender.getNickname() : "")
                .senderAvatar(sender != null ? sender.getAvatarUrl() : "")
                .content(content)
                .msgType(message.getMsgType())
                .isRead(false)
                .createdAt(message.getCreatedAt() != null ? message.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "")
                .build();
    }
}
