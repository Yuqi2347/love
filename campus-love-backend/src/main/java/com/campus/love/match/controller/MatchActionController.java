package com.campus.love.match.controller;

import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.result.Result;
import com.campus.love.common.result.ResultCode;
import com.campus.love.match.dto.UserActionRequest;
import com.campus.love.match.dto.UserWeightStatsResponse;
import com.campus.love.match.service.UserWeightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 匹配行为追踪控制器（V2.0）
 *
 * 用于收集用户行为数据，驱动动态权重更新
 *
 * @author Campus Love Team
 * @version 2.0
 */
@Tag(name = "匹配行为追踪", description = "用户行为上报和权重管理相关接口")
@Slf4j
@RestController
@RequestMapping("/match")
@RequiredArgsConstructor
public class MatchActionController {

    private final UserWeightService userWeightService;

    /**
     * 用户行为类型枚举
     */
    public enum UserActionType {
        FOLLOW("关注", 1),
        IGNORE("忽略", -1),
        CHAT_INIT("主动发消息", 2),
        BLOCK("拉黑", -3),
        PROFILE_VIEW("查看详情", 0);

        private final String description;
        private final int signalStrength;

        UserActionType(String description, int signalStrength) {
            this.description = description;
            this.signalStrength = signalStrength;
        }

        public String getDescription() {
            return description;
        }

        public int getSignalStrength() {
            return signalStrength;
        }
    }

    @PostMapping("/action")
    @Operation(summary = "上报用户行为", description = "用户对推荐对象执行操作后上报，触发权重更新")
    public Result<Void> reportAction(@Valid @RequestBody UserActionRequest request) {
        Long currentUserId = CurrentUser.getId();

        UserActionType actionType;
        try {
            actionType = UserActionType.valueOf(request.getActionType());
        } catch (IllegalArgumentException e) {
            return Result.error(ResultCode.BAD_REQUEST, "无效的行为类型: " + request.getActionType());
        }

        userWeightService.updateWeightsOnAction(
                currentUserId,
                request.getTargetUserId(),
                request.getActionType(),
                actionType.getSignalStrength()
        );

        log.info("User {} reported action: {} on target {}",
                currentUserId, request.getActionType(), request.getTargetUserId());

        return Result.success();
    }

    @GetMapping("/weights/stats")
    @Operation(summary = "获取用户权重统计", description = "查看当前用户的权重配置和状态")
    public Result<UserWeightStatsResponse> getWeightStats() {
        Long currentUserId = CurrentUser.getId();

        Map<String, Object> stats = userWeightService.getWeightStats(currentUserId);

        UserWeightStatsResponse response = UserWeightStatsResponse.builder()
                .userId((Long) stats.get("userId"))
                .actionCount((Integer) stats.get("actionCount"))
                .canUsePersonalizedWeights((Boolean) stats.get("canUsePersonalizedWeights"))
                .weights((Map<String, Double>) stats.get("weights"))
                .source((Boolean) stats.get("canUsePersonalizedWeights") ? "personalized" : "default")
                .lastUpdated((java.time.LocalDateTime) stats.get("lastUpdated"))
                .build();

        return Result.success(response);
    }

    @PostMapping("/weights/reset")
    @Operation(summary = "重置用户权重", description = "将权重重置为全局默认值")
    public Result<Void> resetWeights() {
        Long currentUserId = CurrentUser.getId();
        userWeightService.resetUserWeights(currentUserId);
        log.info("User {} reset weights to default", currentUserId);
        return Result.success();
    }

    @GetMapping("/actions/types")
    @Operation(summary = "获取支持的行为类型", description = "返回所有支持的用户行为类型及说明")
    public Result<Map<String, Object>> getActionTypes() {
        return Result.success(Map.of(
                "actionTypes", UserActionType.values(),
                "description", "用户行为类型定义",
                "note", "每次用户对推荐对象执行操作后，应调用POST /match/action上报行为"
        ));
    }
}
