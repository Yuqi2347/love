package com.campus.love.chat.dto;

import lombok.Data;

@Data
public class ChatMessageRequest {

    private Long receiverId;
    private String content;
    private Integer msgType;
}
