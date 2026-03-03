package com.campus.love.match.controller;

import com.campus.love.common.result.Result;
import com.campus.love.match.dto.MatchResultResponse;
import com.campus.love.match.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "匹配推荐", description = "多维度匹配推荐接口")
@RestController
@RequestMapping("/match")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @Operation(summary = "获取推荐列表", description = "多维度评分排序的推荐用户列表")
    @GetMapping("/recommendations")
    public Result<List<MatchResultResponse>> getRecommendations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(matchService.getRecommendations(page, size));
    }

    @Operation(summary = "查看与某用户的匹配详情")
    @GetMapping("/detail/{targetUserId}")
    public Result<MatchResultResponse> getMatchDetail(@PathVariable Long targetUserId) {
        return Result.success(matchService.getMatchDetail(targetUserId));
    }
}
