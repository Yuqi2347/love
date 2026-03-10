package com.campus.love.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.love.admin.dto.AdminFeedItem;
import com.campus.love.admin.dto.AdminInviteItem;
import com.campus.love.admin.dto.AdminUserItem;
import com.campus.love.admin.service.AdminService;
import com.campus.love.ai.service.AiTokenStatsService;
import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理端接口：仅 isAdmin 用户可访问。
 */
@Tag(name = "管理端", description = "后台管理，仅管理员")
@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AiTokenStatsService aiTokenStatsService;

    private void requireAdmin() {
        adminService.requireAdmin(CurrentUser.getId());
    }

    @Operation(summary = "用户列表（分页）")
    @GetMapping("/users")
    public Result<IPage<AdminUserItem>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        requireAdmin();
        return Result.success(adminService.listUsers(page, size, keyword));
    }

    @Operation(summary = "邀约列表（分页）")
    @GetMapping("/invites")
    public Result<IPage<AdminInviteItem>> listInvites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        requireAdmin();
        return Result.success(adminService.listInvites(page, size, status));
    }

    @Operation(summary = "删除邀约（软删）")
    @DeleteMapping("/invite/{id}")
    public Result<Void> deleteInvite(@PathVariable Long id) {
        requireAdmin();
        adminService.deleteInvite(id);
        return Result.success();
    }

    @Operation(summary = "帖子列表（分页）")
    @GetMapping("/feed/list")
    public Result<IPage<AdminFeedItem>> listFeeds(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long userId) {
        requireAdmin();
        return Result.success(adminService.listFeeds(page, size, userId));
    }

    @Operation(summary = "删除帖子")
    @DeleteMapping("/feed/{id}")
    public Result<Void> deleteFeed(@PathVariable Long id) {
        requireAdmin();
        adminService.deleteFeed(id);
        return Result.success();
    }

    @Operation(summary = "彻底删除用户及其全部相关数据")
    @DeleteMapping("/user/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        requireAdmin();
        adminService.deleteUserCompletely(id);
        return Result.success();
    }

    @Operation(summary = "修改用户信用分")
    @PutMapping("/user/{id}/credit")
    public Result<Void> updateUserCredit(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        requireAdmin();
        adminService.updateUserCredit(id, body.get("creditScore"));
        return Result.success();
    }

    @Operation(summary = "修改用户数值（信用分、活跃度、等级）")
    @PutMapping("/user/{id}/stats")
    public Result<Void> updateUserStats(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        requireAdmin();
        adminService.updateUserStats(id, body);
        return Result.success();
    }

    @Operation(summary = "仪表盘统计")
    @GetMapping("/stats")
    public Result<AdminService.DashboardStats> dashboardStats() {
        requireAdmin();
        return Result.success(adminService.getDashboardStats());
    }

    @Operation(summary = "AI Token 消耗统计（缘分解析）")
    @GetMapping("/ai/token-stats")
    public Result<AiTokenStatsService.AiTokenStats> aiTokenStats(
            @RequestParam(defaultValue = "week") String range) {
        requireAdmin();
        return Result.success(aiTokenStatsService.getStats(range));
    }
}
