package com.campus.love.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.love.auth.security.CurrentUser;
import com.campus.love.chat.dto.ChatMessageResponse;
import com.campus.love.chat.dto.ConversationResponse;
import com.campus.love.chat.entity.Message;
import com.campus.love.chat.mapper.MessageMapper;
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
        message.setContent(content);
        message.setMsgType(msgType != null ? msgType : 1);
        message.setIsRead(false);
        messageMapper.insert(message);

        User sender = userMapper.selectById(senderId);
        return ChatMessageResponse.builder()
                .id(message.getId())
                .senderId(senderId)
                .receiverId(receiverId)
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
                        .eq(Message::getSenderId, otherUserId)
                        .eq(Message::getReceiverId, currentUserId)
                        .eq(Message::getIsRead, false)
                        .set(Message::getIsRead, true));
    }
}
