package com.campus.love.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class YuanFenRequest {

    @NotNull(message = "目标用户ID不能为空")
    private Long targetUserId;
}
