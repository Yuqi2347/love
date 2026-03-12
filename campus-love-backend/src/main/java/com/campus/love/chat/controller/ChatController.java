package com.campus.love.chat.controller;

import com.campus.love.chat.dto.ChatGroupItemResponse;
import com.campus.love.chat.dto.ChatMessageResponse;
import com.campus.love.chat.dto.ConversationResponse;
import com.campus.love.auth.security.CurrentUser;
import com.campus.love.chat.service.ChatService;
import com.campus.love.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "聊天", description = "即时聊天相关接口")
@Slf4j
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

    @Operation(summary = "私聊未读消息总数（用于导航红点）")
    @GetMapping("/unread-total")
    public Result<Integer> getUnreadTotal() {
        return Result.success(chatService.getTotalUnreadCount());
    }

    @Operation(summary = "检查是否可向对方发送消息（未互关时仅允许发一条）")
    @GetMapping("/can-send/{otherUserId}")
    public Result<Boolean> canSendTo(@PathVariable Long otherUserId) {
        return Result.success(chatService.canSendTo(CurrentUser.getId(), otherUserId));
    }

    @Operation(summary = "获取聊天记录")
    @GetMapping("/history/{otherUserId}")
    public Result<List<ChatMessageResponse>> getChatHistory(
            @PathVariable Long otherUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(chatService.getChatHistory(otherUserId, page, size));
    }

    @Operation(summary = "撤回消息（1小时内）")
    @DeleteMapping("/message/{messageId}")
    public Result<Void> recallMessage(@PathVariable Long messageId) {
        chatService.recallMessage(messageId);
        return Result.success();
    }

    @Operation(summary = "标记消息为已读")
    @PutMapping("/read/{otherUserId}")
    public Result<Void> markAsRead(@PathVariable Long otherUserId) {
        chatService.markAsRead(otherUserId);
        return Result.success();
    }

    @Operation(summary = "我加入的群聊列表（邀约临时群等）")
    @GetMapping("/groups")
    public Result<List<ChatGroupItemResponse>> getMyGroups() {
        return Result.success(chatService.getMyGroupList());
    }

    @Operation(summary = "上传聊天图片")
    @PostMapping("/upload")
    public Result<String> uploadChatImage(@RequestParam("file") MultipartFile file) {
        try {
            return Result.success(chatService.uploadChatImage(file));
        } catch (Exception e) {
            throw new com.campus.love.common.exception.BusinessException(
                    com.campus.love.common.result.ResultCode.BAD_REQUEST,
                    e.getMessage() != null ? e.getMessage() : "图片上传失败");
        }
    }

    @Operation(summary = "群聊历史消息")
    @GetMapping("/group/{groupId}/history")
    public Result<List<ChatMessageResponse>> getGroupChatHistory(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(chatService.getGroupChatHistory(groupId, page, size));
    }
}
