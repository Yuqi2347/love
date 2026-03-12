package com.campus.love.chat.websocket;

import com.campus.love.chat.dto.ChatMessageResponse;
import com.campus.love.chat.service.ChatService;
import com.campus.love.chat.service.ChatGroupService;
import com.campus.love.common.utils.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final JwtUtil jwtUtil;
    private final ChatService chatService;
    private final ChatGroupService chatGroupService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Map<Long, WebSocketSession> SESSIONS = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = extractUserId(session);
        if (userId != null) {
            SESSIONS.put(userId, session);
            log.info("WebSocket连接建立: userId={}", userId);
        } else {
            try {
                session.close();
            } catch (IOException e) {
                log.debug("关闭无效 WebSocket 连接时异常", e);
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long senderId = extractUserId(session);
        if (senderId == null) return;

        JsonNode node = objectMapper.readTree(message.getPayload());
        Long receiverId = node.has("receiverId") && !node.get("receiverId").isNull()
                ? node.get("receiverId").asLong()
                : null;
        Long groupId = node.has("groupId") && !node.get("groupId").isNull()
                ? node.get("groupId").asLong()
                : null;
        String content = node.get("content").asText();
        Integer msgType = node.has("msgType") ? node.get("msgType").asInt() : 1;

        ChatMessageResponse response;
        if (groupId != null) {
            // 群聊消息
            response = chatService.sendGroupMessage(senderId, groupId, content, msgType);
        } else if (receiverId != null) {
            // 单聊消息
            response = chatService.sendMessage(senderId, receiverId, content, msgType);
        } else {
            return;
        }
        String responseJson = objectMapper.writeValueAsString(response);

        // Send to sender
        sendToUser(senderId, responseJson);
        if (groupId != null) {
            // 群聊广播：发送给所有群成员
            chatGroupService.getMembers(groupId).forEach(member -> {
                if (!member.getUserId().equals(senderId)) {
                    sendToUser(member.getUserId(), responseJson);
                }
            });
        } else if (receiverId != null) {
            // 单聊：发送给对方
            sendToUser(receiverId, responseJson);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = extractUserId(session);
        if (userId != null) {
            SESSIONS.remove(userId);
            log.info("WebSocket连接关闭: userId={}", userId);
        }
    }

    public void sendToUser(Long userId, String message) {
        WebSocketSession session = SESSIONS.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                log.error("发送WebSocket消息失败: userId={}", userId, e);
            }
        }
    }

    private Long extractUserId(WebSocketSession session) {
        try {
            String query = session.getUri() != null ? session.getUri().getQuery() : null;
            if (query == null) return null;
            Map<String, String> params = UriComponentsBuilder.newInstance()
                    .query(query).build().getQueryParams().toSingleValueMap();
            String token = params.get("token");
            if (token != null && jwtUtil.isTokenValid(token)) {
                return jwtUtil.getUserIdFromToken(token);
            }
        } catch (Exception e) {
            log.error("WebSocket认证失败", e);
        }
        return null;
    }
}
