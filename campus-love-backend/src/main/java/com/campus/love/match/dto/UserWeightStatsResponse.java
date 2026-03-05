package com.campus.love.match.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 用户权重统计响应
 */
@Data
@Builder
@Schema(description = "用户权重统计响应")
public class UserWeightStatsResponse {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "累计行为次数")
    private Integer actionCount;

    @Schema(description = "是否可以使用个性化权重")
    private Boolean canUsePersonalizedWeights;

    @Schema(description = "当前权重配置")
    private Map<String, Double> weights;

    @Schema(description = "权重来源: default(默认) / personalized(个性化)")
    private String source;

    @Schema(description = "最后更新时间")
    private LocalDateTime lastUpdated;
}
