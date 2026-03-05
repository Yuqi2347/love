package com.campus.love.match.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户行为上报请求
 */
@Data
@Schema(description = "用户行为上报请求")
public class UserActionRequest {

    @Schema(description = "目标用户ID", required = true)
    @NotNull(message = "目标用户ID不能为空")
    private Long targetUserId;

    @Schema(description = "行为类型: FOLLOW(关注), IGNORE(忽略), CHAT_INIT(主动发消息), BLOCK(拉黑), PROFILE_VIEW(查看详情)", required = true)
    @NotNull(message = "行为类型不能为空")
    private String actionType;

    @Schema(description = "行为时的综合匹配分（可选，用于调试）")
    private Integer matchScore;

    @Schema(description = "行为时各维度得分快照（可选，用于调试）")
    private String detailSnapshot;
}
