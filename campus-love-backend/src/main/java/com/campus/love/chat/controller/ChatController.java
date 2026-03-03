package com.campus.love.chat.controller;

import com.campus.love.chat.dto.ChatMessageResponse;
import com.campus.love.chat.dto.ConversationResponse;
import com.campus.love.chat.service.ChatService;
import com.campus.love.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "聊天", description = "即时聊天相关接口")
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "获取会话列表")
    @GetMapping("/conversations")
    public Result<List<ConversationResponse>> getConversations() {
        return Result.success(chatService.getConversations());
    }

    @Operation(summary = "获取聊天记录")
    @GetMapping("/history/{otherUserId}")
    public Result<List<ChatMessageResponse>> getChatHistory(
            @PathVariable Long otherUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(chatService.getChatHistory(otherUserId, page, size));
    }

    @Operation(summary = "标记消息为已读")
    @PutMapping("/read/{otherUserId}")
    public Result<Void> markAsRead(@PathVariable Long otherUserId) {
        chatService.markAsRead(otherUserId);
        return Result.success();
    }
}
