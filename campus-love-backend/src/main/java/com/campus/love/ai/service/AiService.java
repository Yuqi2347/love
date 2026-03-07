package com.campus.love.ai.service;

import com.campus.love.ai.config.AiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * AI 调用网关 —— DashScope（兼容 OpenAI 格式，支持 qwen3.5-flash）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final AiConfig aiConfig;
    private final ObjectMapper objectMapper;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();

    /**
     * 调用 chat/completions（OpenAI 兼容格式），返回 assistant 消息文本
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

        log.info("AI request start -> model={}, url={}", aiConfig.getModel(), url);
        long start = System.currentTimeMillis();

        try {
            String bodyJson = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Authorization", "Bearer " + aiConfig.getApiKey())
                    .timeout(java.time.Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            long elapsed = System.currentTimeMillis() - start;
            byte[] raw = response.body() != null ? response.body() : new byte[0];
            if (raw.length >= 3 && raw[0] == (byte) 0xEF && raw[1] == (byte) 0xBB && raw[2] == (byte) 0xBF) {
                raw = java.util.Arrays.copyOfRange(raw, 3, raw.length);
            }
            String bodyStr = new String(raw, StandardCharsets.UTF_8);

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("AI API error {} <- {}ms, body={}", response.statusCode(), elapsed, bodyStr.length() > 500 ? bodyStr.substring(0, 500) + "..." : bodyStr);
                throw new RuntimeException("AI 服务返回错误 " + response.statusCode() + ": " + bodyStr);
            }

            log.info("AI response OK <- {}ms, status={}", elapsed, response.statusCode());
            JsonNode root = objectMapper.readTree(bodyStr);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("AI API FAILED ({}ms): {} - {}", elapsed, e.getClass().getSimpleName(), e.getMessage(), e);
            throw new RuntimeException("AI 服务暂时不可用，请稍后重试", e);
        }
    }
}
