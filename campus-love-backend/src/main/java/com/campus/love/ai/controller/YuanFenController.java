package com.campus.love.ai.controller;

import com.campus.love.ai.dto.YuanFenAnalysisResponse;
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

    @Operation(summary = "缘分解析", description = "互关可请求；冷却期内同一对返回缓存不重复调 AI；一小时内仅能对「一位新好友」触发 AI，其余互关需等待")
    @PostMapping("/yuanfen-analysis")
    public Result<YuanFenAnalysisResponse> analyze(@Valid @RequestBody YuanFenRequest request) {
        return Result.success(yuanFenService.getAnalysis(request.getTargetUserId()));
    }

    @Operation(summary = "缘分解析冷却状态", description = "与某互关在本冷却窗口内已有结果时为 0（可点开看缓存）；若本小时已解析过其他好友且本对尚无结果，返回全局剩余秒数")
    @GetMapping("/yuanfen-cooldown/{targetUserId}")
    public Result<Map<String, Long>> getCooldown(@PathVariable Long targetUserId) {
        long remaining = yuanFenService.getCooldownRemaining(targetUserId);
        return Result.success(Map.of("remainingSeconds", remaining));
    }
}
