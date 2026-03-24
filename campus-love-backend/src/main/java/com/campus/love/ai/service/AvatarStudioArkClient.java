package com.campus.love.ai.service;

import com.campus.love.ai.config.AiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 火山方舟图生图，与 test.py 中 {@code client.images.generate} 参数对齐。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AvatarStudioArkClient {

    private final AiConfig aiConfig;
    private final ObjectMapper objectMapper;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    /**
     * 调用 Ark 生成图片，返回 PNG 字节。
     */
    public AvatarStudioImageResult generateStyledImage(String prompt, String imageDataUri) throws Exception {
        String apiKey = aiConfig.getArkApiKey();
        String baseUrl = aiConfig.getArkBaseUrl();
        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException("ARK_API_KEY 或 ARK_BASE_URL 未配置");
        }
        String trimmed = trimTrailingSlash(baseUrl.trim());
        String url = trimmed + "/images/generations";
        String model = StringUtils.hasText(aiConfig.getArkImageModel())
                ? aiConfig.getArkImageModel().trim()
                : "doubao-seedream-5-0-260128";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("prompt", prompt);
        body.put("image", imageDataUri);
        body.put("size", "2k");
        body.put("output_format", "png");
        body.put("response_format", "url");
        body.put("watermark", false);

        String json = objectMapper.writeValueAsString(body);
        int timeoutSec = aiConfig.getAvatarStudioTimeoutSeconds() > 0
                ? aiConfig.getAvatarStudioTimeoutSeconds()
                : 180;

        log.info("Ark images/generations start model={} url={}", model, url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(timeoutSec))
                .header("Authorization", "Bearer " + apiKey.trim())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("Ark images error status={} body={}", response.statusCode(), truncate(response.body(), 2000));
            throw new IllegalStateException("图生图接口返回 HTTP " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        if (root.has("error")) {
            String msg = root.path("error").path("message").asText("unknown error");
            log.warn("Ark images error payload: {}", truncate(response.body(), 2000));
            throw new IllegalStateException(msg);
        }
        Integer tokensUsed = parseTokensUsed(root);
        if (tokensUsed != null) {
            log.info("Ark image tokens used: {}", tokensUsed);
        }

        JsonNode data = root.path("data");
        if (!data.isArray() || data.size() == 0) {
            throw new IllegalStateException("图生图返回无 data");
        }
        String imageUrl = data.get(0).path("url").asText(null);
        if (!StringUtils.hasText(imageUrl)) {
            String b64 = data.get(0).path("b64_json").asText(null);
            if (StringUtils.hasText(b64)) {
                return new AvatarStudioImageResult(java.util.Base64.getDecoder().decode(b64), tokensUsed, model);
            }
            throw new IllegalStateException("图生图返回无 url/b64_json");
        }

        HttpRequest getImage = HttpRequest.newBuilder()
                .uri(URI.create(imageUrl))
                .timeout(Duration.ofSeconds(Math.min(timeoutSec, 120)))
                .GET()
                .build();
        HttpResponse<byte[]> imgResp = HTTP_CLIENT.send(getImage, HttpResponse.BodyHandlers.ofByteArray());
        if (imgResp.statusCode() < 200 || imgResp.statusCode() >= 300) {
            throw new IllegalStateException("下载生成图失败 HTTP " + imgResp.statusCode());
        }
        return new AvatarStudioImageResult(imgResp.body(), tokensUsed, model);
    }

    private static String trimTrailingSlash(String url) {
        int end = url.length();
        while (end > 0 && url.charAt(end - 1) == '/') {
            end--;
        }
        return url.substring(0, end);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

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
            log.debug("Parse avatar usage failed: {}", e.getMessage());
            return null;
        }
    }

    public record AvatarStudioImageResult(byte[] imageBytes, Integer tokensUsed, String modelName) {}
}
