package com.campus.love.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 聊天完成结果，含内容与 Token 消耗
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResult {

    private String content;
    private Integer tokensUsed;

    public static AiChatResult of(String content, Integer tokensUsed) {
        return new AiChatResult(content, tokensUsed != null ? tokensUsed : 0);
    }
}
