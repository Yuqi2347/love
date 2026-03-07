package com.campus.love.notification.controller;

import com.campus.love.common.result.Result;
import com.campus.love.notification.dto.NotificationResponse;
import com.campus.love.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "通知", description = "站内通知")
@Slf4j
@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "获取当前用户的通知列表")
    @GetMapping
    public Result<List<NotificationResponse>> listNotifications(
            @RequestParam(name = "unreadOnly", defaultValue = "false") boolean unreadOnly) {
        List<NotificationResponse> list = notificationService.getMyNotifications(unreadOnly);
        return Result.success(list);
    }

    @Operation(summary = "将通知标记为已读")
    @PostMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return Result.success();
    }
}

