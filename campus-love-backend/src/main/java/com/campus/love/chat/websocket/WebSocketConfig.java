package com.campus.love.chat.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final String[] allowedOriginPatterns;

    public WebSocketConfig(
            ChatWebSocketHandler chatWebSocketHandler,
            @Value("${app.security.ws-allowed-origins}") String wsAllowedOrigins
    ) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.allowedOriginPatterns = Arrays.stream(wsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .setAllowedOriginPatterns(allowedOriginPatterns);
    }

    // Spring WebSocket 传输层配置：禁用 session 空闲超时，保持长连接
    @Bean
    public org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean
            createWebSocketContainer() {
        var container = new org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean();
        container.setMaxSessionIdleTimeout(0L);   // 0 = 不超时
        container.setMaxTextMessageBufferSize(64 * 1024);
        container.setMaxBinaryMessageBufferSize(64 * 1024);
        return container;
    }
}
