package com.campus.love.ai.controller;

import com.campus.love.ai.dto.YuanFenAnalysisResult;
import com.campus.love.ai.dto.YuanFenRequest;
import com.campus.love.ai.service.YuanFenService;
import com.campus.love.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "AI 分析", description = "AI 驱动的缘分解析等功能")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class YuanFenController {

    private final YuanFenService yuanFenService;

    @Operation(summary = "缘分解析", description = "对互相关注的用户进行 AI 缘分分析，每对用户每小时限1次")
    @PostMapping("/yuanfen-analysis")
    public Result<YuanFenAnalysisResult> analyze(@Valid @RequestBody YuanFenRequest request) {
        return Result.success(yuanFenService.getAnalysis(request.getTargetUserId()));
    }

    @Operation(summary = "缘分解析冷却状态", description = "获取与目标用户的缘分解析冷却剩余秒数")
    @GetMapping("/yuanfen-cooldown/{targetUserId}")
    public Result<Map<String, Long>> getCooldown(@PathVariable Long targetUserId) {
        long remaining = yuanFenService.getCooldownRemaining(targetUserId);
        return Result.success(Map.of("remainingSeconds", remaining));
    }
}
