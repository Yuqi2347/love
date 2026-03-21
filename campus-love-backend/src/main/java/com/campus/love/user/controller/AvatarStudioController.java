package com.campus.love.user.controller;

import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.result.Result;
import com.campus.love.user.dto.AvatarStudioGenerateResponse;
import com.campus.love.user.dto.AvatarStudioQuotaResponse;
import com.campus.love.user.service.AvatarStudioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "用户-AI头像工作室")
@RestController
@RequestMapping("/user/avatar-studio")
@RequiredArgsConstructor
public class AvatarStudioController {

    private final AvatarStudioService avatarStudioService;

    @Operation(summary = "查询 AI 头像工作室剩余次数")
    @GetMapping("/quota")
    public Result<AvatarStudioQuotaResponse> quota() {
        return Result.success(avatarStudioService.getQuota(CurrentUser.getId()));
    }

    @Operation(summary = "按选定风格生成头像图（消耗一次免费次数，成功返回 PNG Base64）")
    @PostMapping("/generate")
    public Result<AvatarStudioGenerateResponse> generate(
            @RequestParam("file") MultipartFile file,
            @RequestParam("style") String style) {
        return Result.success(avatarStudioService.generate(CurrentUser.getId(), file, style));
    }
}
