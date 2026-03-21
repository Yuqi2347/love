package com.campus.love.user.controller;

import com.campus.love.auth.security.CurrentUser;
import com.campus.love.auth.service.EmailVerifyService;
import com.campus.love.common.result.Result;
import com.campus.love.common.result.ResultCode;
import com.campus.love.user.dto.AiDisclosureSettingsRequest;
import com.campus.love.user.dto.UserAiProfileResponse;
import com.campus.love.user.dto.UserProfileRequest;
import com.campus.love.user.dto.UserProfileResponse;
import com.campus.love.user.dto.UserSearchItemResponse;
import com.campus.love.user.entity.UserAvatar;
import com.campus.love.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

import java.time.ZoneId;

@Tag(name = "用户", description = "个人信息管理")
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final EmailVerifyService emailVerifyService;

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<UserProfileResponse> getMyProfile() {
        return Result.success(userService.getProfile(CurrentUser.getId()));
    }

    @Operation(summary = "获取当前用户 AI 画像（性格画像页）")
    @GetMapping("/ai-profile")
    public Result<UserAiProfileResponse> getMyAiProfile() {
        return Result.success(userService.getMyAiProfile());
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

    @Operation(summary = "开启/关闭破冰功能（全局，所有互关好友可用）")
    @PatchMapping("/ice-break")
    public Result<UserProfileResponse> updateIceBreakEnabled(@RequestParam boolean enabled) {
        return Result.success(userService.updateIceBreakEnabled(enabled));
    }

    @Operation(summary = "更新 AI 信息公开授权设置")
    @PatchMapping("/ai-disclosure")
    public Result<UserProfileResponse> updateAiDisclosureSettings(@RequestBody AiDisclosureSettingsRequest request) {
        return Result.success(userService.updateAiDisclosureSettings(request.getSettings()));
    }

    @Operation(summary = "更新动态可见时间")
    @PatchMapping("/feed-visibility-time")
    public Result<UserProfileResponse> updateFeedVisibilityTime(@RequestParam Integer days) {
        return Result.success(userService.updateFeedVisibilityTime(days));
    }

    @Operation(summary = "上传头像（存储到数据库）")
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            String url = userService.uploadAvatar(file);
            return Result.success("头像上传成功", url);
        } catch (IllegalArgumentException e) {
            return Result.error(ResultCode.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.warn("头像上传失败: {}", e.getMessage());
            return Result.error(ResultCode.INTERNAL_ERROR, "头像上传失败，请稍后重试");
        }
    }

    @Operation(summary = "获取用户头像（从数据库读取二进制）")
    @GetMapping("/avatar/{userId}")
    public ResponseEntity<byte[]> getAvatar(WebRequest request, @PathVariable Long userId) {
        // URL 固定为 /user/avatar/{userId}，换图后地址不变：ETag + no-cache，避免长期命中旧 public 缓存
        String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
        if (org.springframework.util.StringUtils.hasText(ifNoneMatch)) {
            UserAvatar meta = userService.getAvatarMeta(userId);
            if (meta == null) {
                return ResponseEntity.notFound().build();
            }
            String etag = buildAvatarEtag(meta);
            if (request.checkNotModified(etag)) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                        .eTag(etag)
                        .cacheControl(CacheControl.noCache().cachePrivate())
                        .build();
            }
        }

        UserAvatar avatar = userService.getAvatarData(userId);
        if (avatar == null || avatar.getAvatarData() == null) {
            return ResponseEntity.notFound().build();
        }
        String etag = buildAvatarEtag(avatar);
        String ct = avatar.getContentType() != null ? avatar.getContentType() : "image/jpeg";
        return ResponseEntity.ok()
                .eTag(etag)
                .contentType(MediaType.parseMediaType(ct))
                .cacheControl(CacheControl.noCache().cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(avatar.getAvatarData());
    }

    /** 与头像内容变更一致：依赖 updated_at + file_size（上传时由 MyBatis 更新） */
    private static String buildAvatarEtag(UserAvatar a) {
        long t = a.getUpdatedAt() != null
                ? a.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                : 0L;
        int size = a.getFileSize() != null ? a.getFileSize() : 0;
        return a.getUserId() + "-" + t + "-" + size;
    }

    @Operation(summary = "上传个人主页背景图")
    @PutMapping("/cover")
    public Result<String> uploadCover(@RequestParam("file") MultipartFile file) {
        try {
            return Result.success("背景上传成功", userService.uploadCover(file));
        } catch (Exception e) {
            log.warn("背景上传失败: {}", e.getMessage());
            return Result.error(ResultCode.INTERNAL_ERROR, "背景上传失败，请稍后重试");
        }
    }

    @Operation(summary = "清除个人主页背景图")
    @DeleteMapping("/cover")
    public Result<Void> clearCover() {
        userService.clearCover();
        return Result.success();
    }

    @Operation(summary = "发送密码修改验证码到邮箱")
    @PostMapping("/password/send-code")
    public Result<Void> sendPasswordCode() {
        Long userId = CurrentUser.getId();
        String email = userService.getUserEmail(userId);
        if (email == null || email.isEmpty()) {
            return Result.error(ResultCode.BAD_REQUEST, "未绑定邮箱，无法修改密码");
        }
        emailVerifyService.sendVerifyCode(email);
        return Result.success();
    }

    @Operation(summary = "通过邮箱验证码修改密码")
    @PostMapping("/password/reset")
    public Result<Void> resetPassword(
            @RequestParam @jakarta.validation.constraints.NotBlank(message = "验证码不能为空") String code,
            @RequestParam @jakarta.validation.constraints.Size(min = 6, max = 20, message = "密码长度6-20位") String newPassword
    ) {
        Long userId = CurrentUser.getId();
        String email = userService.getUserEmail(userId);
        if (email == null || email.isEmpty()) {
            return Result.error(ResultCode.BAD_REQUEST, "未绑定邮箱");
        }
        if (!emailVerifyService.verifyCode(email, code)) {
            return Result.error(ResultCode.BAD_REQUEST, "验证码错误或已过期");
        }
        userService.updatePassword(userId, newPassword);
        return Result.success();
    }
}
