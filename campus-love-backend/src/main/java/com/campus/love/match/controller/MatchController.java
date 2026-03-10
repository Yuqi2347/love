package com.campus.love.match.controller;

import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.result.Result;
import com.campus.love.match.dto.MatchResultResponse;
import com.campus.love.match.service.MatchService;
import com.campus.love.match.service.UserWeightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "匹配推荐", description = "多维度匹配推荐接口")
@RestController
@RequestMapping("/match")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;
    private final UserWeightService userWeightService;

    @Operation(summary = "获取推荐列表", description = "多维度评分排序的推荐用户列表")
    @GetMapping("/recommendations")
    public Result<List<MatchResultResponse>> getRecommendations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "all") String genderFilter) {
        return Result.success(matchService.getRecommendations(page, size, genderFilter));
    }

    @Operation(summary = "查看与某用户的匹配详情")
    @GetMapping("/detail/{targetUserId}")
    public Result<MatchResultResponse> getMatchDetail(@PathVariable Long targetUserId) {
        return Result.success(matchService.getMatchDetail(targetUserId));
    }

    @Operation(summary = "设置权重偏好（高/中/低）")
    @PostMapping("/weights/preferences")
    public Result<Void> setWeightPreferences(@RequestBody Map<String, String> preferences) {
        userWeightService.setWeightPreferences(CurrentUser.getId(), preferences);
        return Result.success();
    }
}
