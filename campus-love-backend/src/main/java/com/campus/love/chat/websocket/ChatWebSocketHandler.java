package com.campus.love.chat.websocket;

import com.campus.love.chat.dto.ChatMessageResponse;
import com.campus.love.chat.service.ChatService;
import com.campus.love.common.utils.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final JwtUtil jwtUtil;
    private final ChatService chatService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Map<Long, WebSocketSession> SESSIONS = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = extractUserId(session);
        if (userId != null) {
            SESSIONS.put(userId, session);
            log.info("WebSocket连接建立: userId={}", userId);
        } else {
            try { session.close(); } catch (IOException ignored) {}
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long senderId = extractUserId(session);
        if (senderId == null) return;

        JsonNode node = objectMapper.readTree(message.getPayload());
        Long receiverId = node.get("receiverId").asLong();
        String content = node.get("content").asText();
        Integer msgType = node.has("msgType") ? node.get("msgType").asInt() : 1;

        ChatMessageResponse response = chatService.sendMessage(senderId, receiverId, content, msgType);
        String responseJson = objectMapper.writeValueAsString(response);

        // Send to sender
        sendToUser(senderId, responseJson);
        // Send to receiver if online
        sendToUser(receiverId, responseJson);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = extractUserId(session);
        if (userId != null) {
            SESSIONS.remove(userId);
            log.info("WebSocket连接关闭: userId={}", userId);
        }
    }

    private void sendToUser(Long userId, String message) {
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
