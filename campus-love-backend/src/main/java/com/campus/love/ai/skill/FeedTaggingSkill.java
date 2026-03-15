package com.campus.love.ai.skill;

import com.campus.love.ai.service.AiService;
import com.campus.love.feed.entity.FeedPost;
import com.campus.love.feed.mapper.FeedPostMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 朋友圈标签化（技术文档 V1.1.0）
 * 发布动态时同步生成标签，确保列表展示时已有标签
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeedTaggingSkill {

    private final AiService aiService;
    private final FeedPostMapper feedPostMapper;
    private final ObjectMapper objectMapper;

    private static final String TAG_PROMPT = """
            你是一个内容标签提取助手。请根据动态内容输出一个 JSON 对象，不要输出 markdown。
            输出格式：
            {
              "tags": ["标签1","标签2","标签3"],
              "primaryCategory": "主要分类",
              "tagSentiment": "positive|neutral|negative",
              "oceanHints": {"O": 50, "C": 50, "E": 50, "A": 50, "N": 50},
              "tagConfidence": 0.75
            }
            要求：
            1. tags 输出 3-5 个贴近校园社交场景的兴趣或话题标签
            2. primaryCategory 从 tags 中选最核心的一个
            3. tagSentiment 表示整条动态的整体情绪倾向
            4. oceanHints 是启发式信号，仅作统计输入，不要求每维都很极端，范围 0-100
            5. tagConfidence 范围 0-1
            """;

    public void tagPost(Long postId) {
        try {
            FeedPost post = feedPostMapper.selectById(postId);
            if (post == null || post.getContent() == null) return;
            String content = post.getContent().length() > 500 ? post.getContent().substring(0, 500) : post.getContent();
            var result = aiService.chatCompletion(TAG_PROMPT, content);
            if (result != null && result.getContent() != null) {
                ParsedTagResult parsed = parseModelOutput(result.getContent().trim());
                if (parsed.tags() != null && !parsed.tags().isEmpty()) {
                    String tags = String.join(",", parsed.tags());
                    post.setAiTags(tags);
                    post.setPrimaryCategory(parsed.primaryCategory() != null ? parsed.primaryCategory() : extractPrimaryCategory(tags));
                    post.setTagSentiment(parsed.tagSentiment());
                    post.setOceanHints(parsed.oceanHintsJson());
                    post.setTagConfidence(parsed.tagConfidence());
                    feedPostMapper.updateById(post);
                    log.info("Feed post {} tagged: {}", postId, tags);
                }
            }
        } catch (Exception e) {
            log.warn("Feed tagging failed for post {}: {}", postId, e.getMessage());
        }
    }

    private String extractPrimaryCategory(String tags) {
        if (tags == null) return null;
        String[] arr = tags.split("[,，、]");
        return arr.length > 0 ? arr[0].trim() : null;
    }

    private ParsedTagResult parseModelOutput(String raw) {
        if (raw == null || raw.isBlank()) return new ParsedTagResult(List.of(), null, null, null, null);
        try {
            JsonNode root = objectMapper.readTree(raw);
            List<String> tags = new ArrayList<>();
            if (root.path("tags").isArray()) {
                root.path("tags").forEach(node -> {
                    String tag = node.asText().trim();
                    if (!tag.isBlank()) tags.add(tag);
                });
            }
            String primaryCategory = textOrNull(root.get("primaryCategory"));
            String tagSentiment = normalizeSentiment(textOrNull(root.get("tagSentiment")));
            String oceanHintsJson = normalizeOceanHints(root.get("oceanHints"));
            BigDecimal tagConfidence = normalizeConfidence(root.get("tagConfidence"));
            if (!tags.isEmpty()) {
                return new ParsedTagResult(tags, primaryCategory, tagSentiment, oceanHintsJson, tagConfidence);
            }
        } catch (Exception ignored) {
        }
        String tags = extractTagsFromModelOutput(raw);
        List<String> list = tags == null || tags.isBlank() ? List.of() : List.of(tags.split(","));
        return new ParsedTagResult(
                list.stream().map(String::trim).filter(s -> !s.isBlank()).toList(),
                extractPrimaryCategory(tags),
                null,
                null,
                null
        );
    }

    /** 从模型输出中提取逗号分隔的标签（去除编号、markdown、多余说明） */
    private String extractTagsFromModelOutput(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        String s = raw
                .replaceAll("(?m)^\\d+[.．、]\\s*", "")   // 1. 2. 3.
                .replaceAll("标签[：:]\\s*", "")
                .replaceAll("^[\\s：:]+", "")
                .replaceAll("[\\s：:]+$", "")
                .trim();
        if (s.isEmpty()) return null;
        s = s.replaceAll("[\\s]+", ",").replace("，", ",").replace("、", ",");
        return s.replaceAll(",+,", ",").replaceAll("^,|,$", "").trim();
    }

    private String normalizeSentiment(String sentiment) {
        if (sentiment == null) return null;
        String lower = sentiment.trim().toLowerCase();
        if ("positive".equals(lower) || "neutral".equals(lower) || "negative".equals(lower)) {
            return lower;
        }
        return null;
    }

    private String normalizeOceanHints(JsonNode node) {
        if (node == null || !node.isObject()) return null;
        try {
            JsonNode normalized = objectMapper.createObjectNode();
            putHint(normalized, "O", node.path("O").asDouble(50d));
            putHint(normalized, "C", node.path("C").asDouble(50d));
            putHint(normalized, "E", node.path("E").asDouble(50d));
            putHint(normalized, "A", node.path("A").asDouble(50d));
            putHint(normalized, "N", node.path("N").asDouble(50d));
            return objectMapper.writeValueAsString(normalized);
        } catch (Exception e) {
            return null;
        }
    }

    private void putHint(JsonNode normalized, String key, double value) {
        ((com.fasterxml.jackson.databind.node.ObjectNode) normalized).put(key, Math.max(0d, Math.min(100d, value)));
    }

    private BigDecimal normalizeConfidence(JsonNode node) {
        if (node == null || !node.isNumber()) return null;
        double value = Math.max(0d, Math.min(1d, node.asDouble(0.7d)));
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) return null;
        String text = node.asText();
        return text == null || text.isBlank() ? null : text.trim();
    }

    private record ParsedTagResult(
            List<String> tags,
            String primaryCategory,
            String tagSentiment,
            String oceanHintsJson,
            BigDecimal tagConfidence
    ) {}
}
