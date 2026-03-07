package com.campus.love.ai.service;

import com.campus.love.ai.config.AiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * 统一 AI 调用网关 —— 兼容 OpenAI / GLM / DeepSeek 等 chat/completions 接口
 */
@Slf4j
@Service
public class AiService {

    private final AiConfig aiConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AiService(AiConfig aiConfig,
                     @Qualifier("aiRestTemplate") RestTemplate restTemplate,
                     ObjectMapper objectMapper) {
        this.aiConfig = aiConfig;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 调用 chat/completions（非流式），返回 assistant 消息文本
     */
    public String chatCompletion(String systemPrompt, String userMessage) {
        String url = aiConfig.getBaseUrl() + "/chat/completions";

        Map<String, Object> body = Map.of(
                "model", aiConfig.getModel(),
                "max_tokens", aiConfig.getMaxTokens(),
                "stream", false,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiConfig.getApiKey());

        log.info("AI 请求发起 -> model={}, url={}", aiConfig.getModel(), url);
        long start = System.currentTimeMillis();

        try {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

            long elapsed = System.currentTimeMillis() - start;
            log.info("AI 响应成功 <- {}ms, status={}", elapsed, resp.getStatusCode());

            JsonNode root = objectMapper.readTree(resp.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("AI API 调用失败 ({}ms): {}", elapsed, e.getMessage(), e);
            throw new RuntimeException("AI 服务暂时不可用，请稍后重试", e);
        }
    }
}
