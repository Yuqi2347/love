package com.campus.love.user.controller;

import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.result.Result;
import com.campus.love.user.dto.BadgeCountsResponse;
import com.campus.love.user.service.BadgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "角标", description = "导航红点数量聚合")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeService badgeService;

    @Operation(summary = "获取各模块红点数量（消息、我的、朋友圈、邀约）")
    @GetMapping("/badges")
    public Result<BadgeCountsResponse> getBadges() {
        return Result.success(badgeService.getBadgeCounts(CurrentUser.getId()));
    }
}
