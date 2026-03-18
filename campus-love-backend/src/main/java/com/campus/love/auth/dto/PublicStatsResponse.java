package com.campus.love.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "公开统计（登录/注册页）")
public record PublicStatsResponse(
        @Schema(description = "活跃用户数（近7天有行为或总注册数）") long activeUserCount
) {}
