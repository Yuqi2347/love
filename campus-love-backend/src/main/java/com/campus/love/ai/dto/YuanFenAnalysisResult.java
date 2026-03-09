package com.campus.love.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 缘分解析结果（含 Token 消耗，用于日志记录）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class YuanFenAnalysisResult {

    private YuanFenAnalysisResponse response;
    private int tokensUsed;
}
