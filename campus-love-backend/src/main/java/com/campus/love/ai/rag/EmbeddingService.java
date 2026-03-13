package com.campus.love.ai.rag;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 向量化服务（RAG 基础设施）
 * 技术文档 V1.1.0 第 9 节
 * 将文本转为 1536 维向量，用于人物画像/动态内容向量检索
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final AiConfig aiConfig;
    private final ObjectMapper objectMapper;

    private static final int EMBEDDING_DIM = 1536;
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();

    /**
     * 文本向量化
     * @param text 待向量化文本
     * @return 1536 维向量，失败时返回 null
     */
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String embeddingUrl = aiConfig.getBaseUrl().replaceFirst("/?$", "") + "/embeddings";
        try {
            Map<String, Object> body = Map.of(
                    "model", getEmbeddingModel(),
                    "input", text
            );
            String bodyJson = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(embeddingUrl))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Authorization", "Bearer " + aiConfig.getApiKey())
                    .timeout(java.time.Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            String bodyStr = new String(response.body() != null ? response.body() : new byte[0], StandardCharsets.UTF_8);

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Embedding API error {}: {}", response.statusCode(), bodyStr.length() > 200 ? bodyStr.substring(0, 200) + "..." : bodyStr);
                return null;
            }

            JsonNode root = objectMapper.readTree(bodyStr);
            JsonNode data = root.path("data");
            if (!data.isArray() || data.isEmpty()) {
                return null;
            }
            JsonNode embedding = data.get(0).path("embedding");
            float[] vec = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                vec[i] = (float) embedding.get(i).asDouble();
            }
            return vec;
        } catch (Exception e) {
            log.warn("Embedding failed for text len={}: {}", text.length(), e.getMessage());
            return null;
        }
    }

    /**
     * 转为 JSON 数组字符串，供 MySQL JSON 列存储
     */
    public String embedAsJson(String text) {
        float[] vec = embed(text);
        if (vec == null) return null;
        List<Double> list = new ArrayList<>(vec.length);
        for (float v : vec) {
            list.add((double) v);
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            log.warn("Embedding JSON serialize failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 JSON 字符串解析向量
     */
    public float[] parseVector(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonNode arr = objectMapper.readTree(json);
            if (!arr.isArray()) return null;
            float[] vec = new float[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                vec[i] = (float) arr.get(i).asDouble();
            }
            return vec;
        } catch (Exception e) {
            log.debug("parseVector failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 余弦相似度（应用层实现，MySQL 无原生向量索引时使用）
     */
    public double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private String getEmbeddingModel() {
        return "text-embedding-ada-002";
    }
}
