package com.campus.love.follow.controller;

import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.enums.FollowStatusEnum;
import com.campus.love.common.result.Result;
import com.campus.love.follow.dto.FollowResponse;
import com.campus.love.follow.service.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "关注", description = "关注/取消关注管理")
@Slf4j
@RestController
@RequestMapping("/follow")
@RequiredArgsConstructor
@Validated
public class FollowController {

    private final FollowService followService;

    @Operation(summary = "关注用户")
    @PostMapping("/{targetUserId}")
    public Result<Void> follow(@PathVariable Long targetUserId) {
        followService.follow(targetUserId);
        return Result.success();
    }

    @Operation(summary = "取消关注")
    @DeleteMapping("/{targetUserId}")
    public Result<Void> unfollow(@PathVariable Long targetUserId) {
        followService.unfollow(targetUserId);
        return Result.success();
    }

    @Operation(summary = "获取与目标用户的关注状态")
    @GetMapping("/status/{targetUserId}")
    public Result<FollowStatusEnum> getStatus(@PathVariable Long targetUserId) {
        return Result.success(followService.getFollowStatus(targetUserId));
    }

    @Operation(summary = "我的关注列表")
    @GetMapping("/following")
    public Result<List<FollowResponse>> getFollowing() {
        return Result.success(followService.getFollowingList(CurrentUser.getId()));
    }

    @Operation(summary = "我的粉丝列表")
    @GetMapping("/followers")
    public Result<List<FollowResponse>> getFollowers() {
        return Result.success(followService.getFollowerList(CurrentUser.getId()));
    }

    @Operation(summary = "获取指定用户的关注列表")
    @GetMapping("/user/{userId}/following")
    public Result<List<FollowResponse>> getUserFollowing(@PathVariable Long userId) {
        return Result.success(followService.getFollowingList(userId));
    }

    @Operation(summary = "获取指定用户的粉丝列表")
    @GetMapping("/user/{userId}/followers")
    public Result<List<FollowResponse>> getUserFollowers(@PathVariable Long userId) {
        return Result.success(followService.getFollowerList(userId));
    }

    @Operation(summary = "新粉丝数量（自上次查看粉丝列表以来）")
    @GetMapping("/new-followers-count")
    public Result<Integer> getNewFollowersCount() {
        return Result.success(followService.getNewFollowerCount(CurrentUser.getId()));
    }

    @Operation(summary = "标记粉丝列表已查看，消除新粉丝红点")
    @PutMapping("/mark-followers-viewed")
    public Result<Void> markFollowersViewed() {
        followService.markFollowersViewed(CurrentUser.getId());
        return Result.success();
    }

    @Operation(summary = "设置关注用户的备注名")
    @PutMapping("/{targetUserId}/remark")
    public Result<Void> setRemark(
        @PathVariable Long targetUserId,
        @RequestParam(required = false, defaultValue = "") @Size(max = 10, message = "备注最多10个字符") String remark
    ) {
        followService.setRemark(targetUserId, remark);
        return Result.success();
    }

    @Operation(summary = "我的互关朋友列表", description = "返回当前用户互相关注的朋友列表，用于分享帖子等场景")
    @GetMapping("/mutual")
    public Result<List<FollowResponse>> getMutualFriends() {
        return Result.success(followService.getMutualFriendList(CurrentUser.getId()));
    }
}
