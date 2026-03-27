package com.campus.love.auth.controller;

import com.campus.love.auth.dto.AuthResponse;
import com.campus.love.auth.dto.LoginRequest;
import com.campus.love.auth.dto.PublicStatsResponse;
import com.campus.love.auth.dto.RegisterRequest;
import com.campus.love.auth.dto.SchoolItem;
import com.campus.love.auth.dto.WechatCodeRequest;
import com.campus.love.auth.dto.WechatCompleteRequest;
import com.campus.love.auth.service.AuthService;
import com.campus.love.common.service.PublicStatsService;
import com.campus.love.auth.service.EmailVerifyService;
import com.campus.love.auth.service.SchoolService;
import com.campus.love.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "认证", description = "注册/登录接口")
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SchoolService schoolService;
    private final EmailVerifyService emailVerifyService;
    private final PublicStatsService publicStatsService;

    @Operation(summary = "公开统计", description = "登录/注册页展示用，无需鉴权")
    @GetMapping("/stats")
    public Result<PublicStatsResponse> getPublicStats() {
        return Result.success(new PublicStatsResponse(publicStatsService.getActiveUserCount()));
    }

    @Operation(summary = "搜索学校", description = "根据关键词搜索支持的学校列表")
    @GetMapping("/schools")
    public Result<List<SchoolItem>> searchSchools(@RequestParam(required = false) String keyword) {
        List<SchoolItem> list = keyword != null && !keyword.trim().isEmpty()
                ? schoolService.searchSchools(keyword)
                : schoolService.getAllSchools();
        return Result.success(list);
    }

    @Operation(summary = "发送邮箱验证码")
    @PostMapping("/send-verify-code")
    public Result<Void> sendVerifyCode(@RequestParam String email) {
        emailVerifyService.sendVerifyCode(email);
        return Result.success();
    }

    @Operation(summary = "用户注册", description = "使用邮箱注册（不限制后缀），需先发送验证码")
    @PostMapping("/register")
    public Result<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @Operation(summary = "微信小程序一键登录")
    @PostMapping("/wechat/login")
    public Result<AuthResponse> loginByWechat(@Valid @RequestBody WechatCodeRequest request) {
        return Result.success(authService.loginByWechatCode(request.getCode()));
    }

    @Operation(summary = "微信登录补全（邮箱验证码绑定；邮箱未注册则自动注册并绑定）")
    @PostMapping("/wechat/complete")
    public Result<AuthResponse> completeWechat(@Valid @RequestBody WechatCompleteRequest request) {
        return Result.success(authService.completeWechatWithEmail(request));
    }
}
