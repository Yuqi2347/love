package com.campus.love.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

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

    /** 缘分解析冷却时间（小时），0表示无限制 */
    private long yuanfenCooldownHours = 0;

    @Bean("aiRestTemplate")
    public RestTemplate aiRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);  // 10秒连接超时
        factory.setReadTimeout(60_000);     // 60秒读取超时（AI 生成较慢）
        return new RestTemplate(factory);
    }
}
