package com.campus.love.user.controller;

import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.result.Result;
import com.campus.love.common.result.ResultCode;
import com.campus.love.user.dto.UserProfileRequest;
import com.campus.love.user.dto.UserProfileResponse;
import com.campus.love.user.dto.UserSearchItemResponse;
import com.campus.love.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "用户", description = "个人信息管理")
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<UserProfileResponse> getMyProfile() {
        return Result.success(userService.getProfile(CurrentUser.getId()));
    }

    @Operation(summary = "按昵称搜索用户")
    @GetMapping("/search")
    public Result<List<UserSearchItemResponse>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(userService.searchUsers(keyword, limit));
    }

    @Operation(summary = "获取指定用户信息")
    @GetMapping("/{userId}")
    public Result<UserProfileResponse> getUserProfile(@PathVariable Long userId) {
        return Result.success(userService.getProfile(userId));
    }

    @Operation(summary = "更新个人信息")
    @PutMapping("/profile")
    public Result<UserProfileResponse> updateProfile(@Valid @RequestBody UserProfileRequest request) {
        return Result.success(userService.updateProfile(request));
    }

    @Operation(summary = "更新昵称")
    @PatchMapping("/nickname")
    public Result<UserProfileResponse> updateNickname(@RequestParam @jakarta.validation.constraints.NotBlank(message = "昵称不能为空") String nickname) {
        return Result.success(userService.updateNickname(nickname));
    }

    @Operation(summary = "更新朋友圈展示设置")
    @PatchMapping("/feed-visibility")
    public Result<UserProfileResponse> updateFeedVisibility(@RequestParam String visibility) {
        return Result.success(userService.updateFeedVisibility(visibility));
    }

    @Operation(summary = "上传头像")
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            return Result.success("头像上传成功", userService.uploadAvatar(file));
        } catch (Exception e) {
            log.warn("头像上传失败: {}", e.getMessage());
            return Result.error(ResultCode.INTERNAL_ERROR, "头像上传失败，请稍后重试");
        }
    }
}
