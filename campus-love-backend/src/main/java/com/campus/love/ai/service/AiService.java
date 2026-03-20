package com.campus.love.ai.service;

import com.campus.love.ai.config.AiConfig;
import com.campus.love.ai.dto.AiChatResult;
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
import java.util.LinkedHashMap;
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
     * 调用 chat/completions（OpenAI 兼容格式），返回内容与 Token 消耗
     */
    public AiChatResult chatCompletion(String systemPrompt, String userMessage) {
        return chatCompletion(systemPrompt, userMessage, null);
    }

    /**
     * @param maxTokensOverride 非空且 &gt;0 时覆盖全局 max-tokens（如缘分解析单独上限）
     */
    public AiChatResult chatCompletion(String systemPrompt, String userMessage, Integer maxTokensOverride) {
        return chatCompletion(systemPrompt, userMessage, maxTokensOverride, null);
    }

    /**
     * @param timeoutSecondsOverride 非空且 &gt;0 时覆盖全局 AI 超时（缘分解析等）
     */
    public AiChatResult chatCompletion(
            String systemPrompt,
            String userMessage,
            Integer maxTokensOverride,
            Integer timeoutSecondsOverride) {
        String url = aiConfig.requiredBaseUrl() + "/chat/completions";

        int cap = aiConfig.getMaxTokens() > 0 ? aiConfig.getMaxTokens() : 4096;
        int maxTok = maxTokensOverride != null && maxTokensOverride > 0
                ? Math.min(maxTokensOverride, cap)
                : (aiConfig.getMaxTokens() > 0 ? aiConfig.getMaxTokens() : 4096);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", aiConfig.requiredModel());
        body.put("max_tokens", maxTok);
        body.put("stream", false);
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
        ));

        log.info("AI request start -> model={}, url={}", aiConfig.requiredModel(), url);
        long start = System.currentTimeMillis();

        try {
            String bodyJson = objectMapper.writeValueAsString(body);
            int timeoutSec = timeoutSecondsOverride != null && timeoutSecondsOverride > 0
                    ? timeoutSecondsOverride
                    : (aiConfig.getTimeoutSeconds() > 0 ? aiConfig.getTimeoutSeconds() : 90);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Authorization", "Bearer " + aiConfig.requiredApiKey())
                    .timeout(java.time.Duration.ofSeconds(timeoutSec))
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
            String content = root.path("choices").get(0).path("message").path("content").asText();
            Integer tokensUsed = parseTokensUsed(root);
            if (tokensUsed != null) {
                log.info("AI tokens used: {}", tokensUsed);
            }
            return AiChatResult.of(content, tokensUsed);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("AI API FAILED ({}ms): {} - {}", elapsed, e.getClass().getSimpleName(), e.getMessage(), e);
            throw new RuntimeException("AI 服务暂时不可用，请稍后重试", e);
        }
    }

    /** 解析 usage.total_tokens（OpenAI/DashScope 兼容格式） */
    private Integer parseTokensUsed(JsonNode root) {
        try {
            JsonNode usage = root.path("usage");
            if (usage.isMissingNode()) return null;
            JsonNode total = usage.path("total_tokens");
            if (total.isNumber()) return total.asInt();
            int prompt = usage.path("prompt_tokens").asInt(0);
            int completion = usage.path("completion_tokens").asInt(0);
            return prompt + completion > 0 ? prompt + completion : null;
        } catch (Exception e) {
            log.debug("Parse usage failed: {}", e.getMessage());
            return null;
        }
    }
}
