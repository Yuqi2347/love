package com.campus.love.ai.skill;

import com.campus.love.ai.rag.EmbeddingService;
import com.campus.love.ai.service.AiService;
import com.campus.love.feed.entity.FeedContentVector;
import com.campus.love.feed.entity.FeedPost;
import com.campus.love.feed.mapper.FeedContentVectorMapper;
import com.campus.love.feed.mapper.FeedPostMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 朋友圈标签化（技术文档 V1.1.0）
 * 发布动态后由虚拟线程异步调用；无正文时默认「日常」。
 * 有正文且不足 10 字：标签至多 3 个（服务端截断）；不少于 10 字：由模型决定数量，仅设宽松上限防失控。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeedTaggingSkill {

    private final AiService aiService;
    private final FeedPostMapper feedPostMapper;
    private final FeedContentVectorMapper feedContentVectorMapper;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;

    /** 正文不足 10 字时，标签个数硬上限 */
    private static final int MAX_TAGS_SHORT_TEXT = 3;
    /** 正文 ≥10 字时由 AI 决定个数，服务端仅做宽松截断，防止异常长列表 */
    private static final int MAX_TAGS_LONG_TEXT_SAFETY = 15;

    private static final String TAG_PROMPT = """
            你是一个内容标签提取助手。请根据动态内容输出一个 JSON 对象，不要输出 markdown。
            输出格式：
            {
              "tags": ["标签1","标签2"],
              "primaryCategory": "主要分类",
              "tagSentiment": "positive|neutral|negative",
              "oceanHints": {"O": 50, "C": 50, "E": 50, "A": 50, "N": 50},
              "tagConfidence": 0.75
            }
            要求：
            1. tags: 若用户正文不足 10 字，tags 数组长度不得超过 3（可 1～3 个）。若用户正文不少于 10 字，tags 的数量与词条由你根据信息量自行决定，力求精炼、有区分度。
            2. primaryCategory 从 tags 中选最核心的一个
            3. tagSentiment 表示整条动态的整体情绪倾向
            4. oceanHints 是启发式信号，范围 0-100
            5. tagConfidence 范围 0-1
            6. 请简短回答，只输出 JSON
            """;

    public void tagPost(Long postId) {
        try {
            FeedPost post = feedPostMapper.selectById(postId);
            if (post == null) {
                return;
            }
            String text = post.getContent() == null ? "" : post.getContent().trim();
            // 仅图/视频/链接、无正文：不调用大模型，默认「日常」，用户可在详情里自行改标签
            if (text.isEmpty()) {
                persistDailyTagsAndVector(postId);
                log.info("Feed post {}: empty text, tagged 日常 (skip AI)", postId);
                return;
            }

            String contentSlice = text.length() > 500 ? text.substring(0, 500) : text;
            int maxTags = text.length() < 10 ? MAX_TAGS_SHORT_TEXT : MAX_TAGS_LONG_TEXT_SAFETY;
            String userMessage = (text.length() < 10
                    ? "【说明：本条正文不足10字】tags 至多3个。\n"
                    : "【说明：本条正文不少于10字】标签个数由你根据内容决定，精炼即可。\n")
                    + contentSlice;
            var result = aiService.chatCompletion(TAG_PROMPT, userMessage);
            if (result != null && result.getContent() != null) {
                ParsedTagResult parsed = parseModelOutput(result.getContent().trim());
                List<String> capped = clampTags(parsed.tags(), maxTags);
                if (!capped.isEmpty()) {
                    String tags = String.join(",", capped);
                    post.setAiTags(tags);
                    post.setPrimaryCategory(parsed.primaryCategory() != null ? parsed.primaryCategory() : extractPrimaryCategory(tags));
                    post.setTagSentiment(parsed.tagSentiment());
                    post.setOceanHints(parsed.oceanHintsJson());
                    post.setTagConfidence(parsed.tagConfidence());
                    feedPostMapper.updateById(post);
                    log.info("Feed post {} tagged (len={}, cap={}): {}", postId, text.length(), maxTags, tags);
                    saveContentVector(postId, post.getUserId(), text, tags);
                    return;
                }
            }
            persistDailyTagsIfUnset(postId);
        } catch (Exception e) {
            log.warn("Feed tagging failed for post {}: {}", postId, e.getMessage());
            persistDailyTagsIfUnset(postId);
        }
    }

    private static List<String> clampTags(List<String> tags, int max) {
        if (tags == null || max <= 0) {
            return List.of();
        }
        return tags.stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .limit(max)
                .toList();
    }

    /** 默认「日常」并写入向量表（便于推荐侧有嵌入，仅用短文本降低耗时） */
    private void persistDailyTagsAndVector(Long postId) {
        FeedPost p = feedPostMapper.selectById(postId);
        if (p == null) {
            return;
        }
        p.setAiTags("日常");
        p.setPrimaryCategory("日常");
        p.setTagSentiment("neutral");
        p.setOceanHints(null);
        p.setTagConfidence(BigDecimal.valueOf(0.50d).setScale(2, RoundingMode.HALF_UP));
        feedPostMapper.updateById(p);
        saveContentVector(postId, p.getUserId(), "日常", "日常");
    }

    private void persistDailyTagsIfUnset(Long postId) {
        try {
            FeedPost fallback = feedPostMapper.selectById(postId);
            if (fallback != null && fallback.getAiTags() == null) {
                persistDailyTagsAndVector(postId);
                log.info("Feed post {} fallback tagged: 日常", postId);
            }
        } catch (Exception ignored) {
        }
    }

    /** 生成帖子内容向量并存入 t_feed_content_vector，失败时静默忽略 */
    private void saveContentVector(Long postId, Long userId, String content, String tags) {
        try {
            String textForEmbed = (content != null ? content : "") + " " + (tags != null ? tags : "");
            String vectorJson = embeddingService.embedAsJson(textForEmbed.trim());
            if (vectorJson == null) return;
            FeedContentVector existing = feedContentVectorMapper.selectById(postId);
            if (existing == null) {
                FeedContentVector cv = new FeedContentVector();
                cv.setFeedId(postId);
                cv.setUserId(userId);
                cv.setContentVector(vectorJson);
                try { cv.setAiTags(objectMapper.writeValueAsString(tags != null ? List.of(tags.split(",")) : List.of())); } catch (Exception ignored) {}
                cv.setPrimaryCategory(tags != null ? tags.split(",")[0].trim() : null);
                cv.setCreatedAt(LocalDateTime.now());
                feedContentVectorMapper.insert(cv);
            } else {
                existing.setContentVector(vectorJson);
                feedContentVectorMapper.updateById(existing);
            }
            log.info("Feed post {} content vector saved", postId);
        } catch (Exception e) {
            log.warn("Feed vector generation failed for post {}: {}", postId, e.getMessage());
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
