package com.campus.love.chat.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatMessageResponse {

    private Long id;
    private Long senderId;
    private Long receiverId;
    private String senderNickname;
    private String senderAvatar;
    private String content;
    private Integer msgType;
    private Boolean isRead;
    private String createdAt;
}
