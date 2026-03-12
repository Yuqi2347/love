package com.campus.love.chat.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConversationResponse {

    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String lastMessage;
    private String lastTime;
    private Integer unreadCount;
    private Integer msgType;
}
