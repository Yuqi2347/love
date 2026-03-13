package com.campus.love.ai.skill;

import com.campus.love.ai.service.AiService;
import com.campus.love.feed.entity.FeedPost;
import com.campus.love.feed.mapper.FeedPostMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

    private static final String TAG_PROMPT = "你是一个内容标签提取助手。根据以下动态内容，提取3-5个兴趣标签（如摄影、音乐、旅行），用英文逗号分隔，直接输出标签列表，不要其他解释。";

    public void tagPost(Long postId) {
        try {
            FeedPost post = feedPostMapper.selectById(postId);
            if (post == null || post.getContent() == null) return;
            String content = post.getContent().length() > 500 ? post.getContent().substring(0, 500) : post.getContent();
            var result = aiService.chatCompletion(TAG_PROMPT, content);
            if (result != null && result.getContent() != null) {
                String raw = result.getContent().trim();
                String tags = extractTagsFromModelOutput(raw);
                if (tags != null && !tags.isEmpty()) {
                    post.setAiTags(tags);
                    post.setPrimaryCategory(extractPrimaryCategory(tags));
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
}
