package com.campus.love.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;
import org.springframework.util.StringUtils;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiConfig {

    /** API Key（Bearer token） */
    private String apiKey;

    /** OpenAI 兼容 API 基础 URL */
    private String baseUrl;

    /** 模型名称 */
    private String model;

    /** 单次请求最大 token */
    private int maxTokens = 1000;

    /** 调用 AI API 超时秒数（缘分解析等长文本生成需更长时间） */
    private int timeoutSeconds = 90;

    /** 缘分解析冷却：-1=永久缓存，0=每次调用AI，>0=冷却N小时（默认1小时） */
    private long yuanfenCooldownHours = 1;

    public String requiredApiKey() {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("AI_API_KEY 未配置");
        }
        return apiKey.trim();
    }

    public String requiredBaseUrl() {
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException("AI_BASE_URL 未配置");
        }
        return trimTrailingSlash(baseUrl.trim());
    }

    public String requiredModel() {
        if (!StringUtils.hasText(model)) {
            throw new IllegalStateException("AI_MODEL 未配置");
        }
        return model.trim();
    }

    private String trimTrailingSlash(String url) {
        int end = url.length();
        while (end > 0 && url.charAt(end - 1) == '/') {
            end--;
        }
        return url.substring(0, end);
    }
}
