package com.campus.love.invite.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.result.Result;
import com.campus.love.invite.dto.*;
import com.campus.love.invite.service.InviteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 邀约控制器
 */
@Tag(name = "邀约", description = "邀约管理")
@RestController
@RequestMapping("/invite")
@RequiredArgsConstructor
public class InviteController {

    private final InviteService inviteService;

    @Operation(summary = "发起邀约")
    @PostMapping
    public Result<Long> createInvite(@Valid @RequestBody InviteCreateRequest request) {
        Long inviteId = inviteService.createInvite(request);
        return Result.success(inviteId);
    }

    @Operation(summary = "获取邀约列表")
    @GetMapping("/list")
    public Result<IPage<InviteResponse>> getInviteList(
            @Parameter(description = "邀约类型") @RequestParam(required = false) String type,
            @Parameter(description = "邀约状态") @RequestParam(required = false) String status,
            @Parameter(description = "邀约时间范围：week/month/year") @RequestParam(required = false) String timeRange,
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer size) {
        IPage<InviteResponse> invites = inviteService.getInviteList(type, status, timeRange, page, size);
        return Result.success(invites);
    }

    @Operation(summary = "获取邀约详情")
    @GetMapping("/{id}")
    public Result<InviteResponse> getInviteDetail(@PathVariable Long id) {
        InviteResponse invite = inviteService.getInviteDetail(id);
        return Result.success(invite);
    }

    @Operation(summary = "加入邀约")
    @PostMapping("/{id}/join")
    public Result<Void> joinInvite(@PathVariable Long id) {
        inviteService.joinInvite(id);
        return Result.success();
    }

    @Operation(summary = "退出邀约")
    @DeleteMapping("/{id}/leave")
    public Result<Void> leaveInvite(@PathVariable Long id) {
        inviteService.leaveInvite(id);
        return Result.success();
    }

    @Operation(summary = "申请再次加入邀约（已退出用户）")
    @PostMapping("/{id}/rejoin-request")
    public Result<Void> requestRejoin(@PathVariable Long id) {
        inviteService.requestRejoin(id);
        return Result.success();
    }

    @Operation(summary = "发起人：获取待处理的再次加入申请列表")
    @GetMapping("/{id}/rejoin-requests")
    public Result<List<InviteRejoinRequestItem>> getRejoinRequests(@PathVariable Long id) {
        return Result.success(inviteService.getRejoinRequests(id));
    }

    @Operation(summary = "发起人：同意某人再次加入")
    @PostMapping("/{id}/rejoin-approve/{userId}")
    public Result<Void> approveRejoin(@PathVariable Long id, @PathVariable Long userId) {
        inviteService.approveRejoin(id, userId);
        return Result.success();
    }

    @Operation(summary = "发起人：拒绝某人再次加入")
    @PostMapping("/{id}/rejoin-reject/{userId}")
    public Result<Void> rejectRejoin(@PathVariable Long id, @PathVariable Long userId) {
        inviteService.rejectRejoin(id, userId);
        return Result.success();
    }

    @Operation(summary = "取消邀约")
    @DeleteMapping("/{id}/cancel")
    public Result<Void> cancelInvite(
            @PathVariable Long id,
            @Parameter(description = "取消原因") @RequestParam(required = false) String reason) {
        inviteService.cancelInvite(id, reason);
        return Result.success();
    }

    @Operation(summary = "确认参与者")
    @PutMapping("/{id}/confirm")
    public Result<Void> confirmParticipants(
            @PathVariable Long id,
            @RequestBody InviteConfirmParticipantsRequest request) {
        inviteService.confirmParticipants(id, request.getUserIds());
        return Result.success();
    }

    @Operation(summary = "创建等待邀约")
    @PostMapping("/wait/create")
    public Result<Long> createInviteWait(@Valid @RequestBody InviteWaitCreateRequest request) {
        Long waitId = inviteService.createInviteWait(request);
        return Result.success(waitId);
    }

    @Operation(summary = "获取我的等待邀约")
    @GetMapping("/wait/list")
    public Result<List<InviteWaitResponse>> getMyInviteWaits() {
        List<InviteWaitResponse> waits = inviteService.getMyInviteWaits();
        return Result.success(waits);
    }

    @Operation(summary = "取消等待邀约")
    @DeleteMapping("/wait/{id}")
    public Result<Void> cancelInviteWait(@PathVariable Long id) {
        inviteService.cancelInviteWait(id);
        return Result.success();
    }

    @Operation(summary = "创建评价")
    @PostMapping("/{id}/rating")
    public Result<Void> createRating(
            @PathVariable Long id,
            @Valid @RequestBody InviteRatingCreateRequest request) {
        request.setInviteId(id);
        inviteService.createRating(request);
        return Result.success();
    }

    @Operation(summary = "获取我的邀约统计")
    @GetMapping("/stats")
    public Result<InviteStatsResponse> getInviteStats() {
        InviteStatsResponse stats = inviteService.getMyInviteStats();
        return Result.success(stats);
    }

    @Operation(summary = "获取指定用户的邀约统计")
    @GetMapping("/user/{userId}/stats")
    public Result<InviteStatsResponse> getUserInviteStats(@PathVariable Long userId) {
        InviteStatsResponse stats = inviteService.getUserInviteStats(userId);
        return Result.success(stats);
    }

    @Operation(summary = "获取推荐邀约列表")
    @GetMapping("/recommend")
    public Result<List<InviteResponse>> getRecommendInvites(
            @Parameter(description = "推荐数量（默认 10，最大 20）")
            @RequestParam(name = "limit", required = false) Integer limit) {
        List<InviteResponse> list = inviteService.getRecommendInvites(limit);
        return Result.success(list);
    }

    @Operation(summary = "热门邀约看板：按类型统计邀约中数量")
    @GetMapping("/board/type-counts")
    public Result<List<InviteTypeCountResponse>> getHotInviteTypeCounts(
            @Parameter(description = "返回条数（默认 10，最大 10）")
            @RequestParam(name = "limit", required = false) Integer limit) {
        return Result.success(inviteService.getHotInviteTypeCounts(limit));
    }

    @Operation(summary = "获取我发起的邀约历史")
    @GetMapping("/history/created")
    public Result<List<InviteResponse>> getMyCreatedInvites(
            @Parameter(description = "时间范围：week / month / all，默认 week")
            @RequestParam(name = "range", required = false) String range) {
        List<InviteResponse> list = inviteService.getMyCreatedInvites(range);
        return Result.success(list);
    }

    @Operation(summary = "获取我参与的邀约历史")
    @GetMapping("/history/joined")
    public Result<List<InviteResponse>> getMyJoinedInvites(
            @Parameter(description = "时间范围：week / month / all，默认 week")
            @RequestParam(name = "range", required = false) String range) {
        List<InviteResponse> list = inviteService.getMyJoinedInvites(range);
        return Result.success(list);
    }

    @Operation(summary = "我的邀约列表（我发起的 + 我参与的，含已退出，按邀约时间倒序）")
    @GetMapping("/my-list")
    public Result<List<InviteResponse>> getMyInvitesList(
            @Parameter(description = "时间范围：week / month / all，默认 week")
            @RequestParam(name = "range", required = false) String range) {
        return Result.success(inviteService.getMyInvitesList(range));
    }

    @Operation(summary = "邀约新活动数量（我的邀约有人加入/发言、等待匹配成功，用于导航红点）")
    @GetMapping("/activity/new-count")
    public Result<Integer> getNewInviteActivityCount() {
        return Result.success(inviteService.getNewInviteActivityCount(CurrentUser.getId()));
    }

    @Operation(summary = "标记邀约活动已查看，消除红点")
    @PutMapping("/activity/mark-viewed")
    public Result<Void> markInviteActivityViewed() {
        inviteService.markInviteActivityViewed(CurrentUser.getId());
        return Result.success();
    }
}
