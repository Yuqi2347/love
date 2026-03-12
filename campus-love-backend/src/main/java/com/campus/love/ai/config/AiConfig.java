package com.campus.love.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiConfig {

    /** API Key（Bearer token） */
    private String apiKey;

    /** OpenAI 兼容 API 基础 URL（如智谱 GLM、DeepSeek 等） */
    private String baseUrl = "https://open.bigmodel.cn/api/paas/v4";

    /** 模型名称 */
    private String model = "glm-4-plus";

    /** 单次请求最大 token */
    private int maxTokens = 1000;

    /** 调用 AI API 超时秒数（缘分解析等长文本生成需更长时间） */
    private int timeoutSeconds = 90;

    /** 缘分解析冷却：-1=永久缓存，0=每次调用AI，>0=冷却N小时（默认1小时） */
    private long yuanfenCooldownHours = 1;
}
