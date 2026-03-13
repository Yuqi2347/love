package com.campus.love.ai.privacy;

import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * AI 信息公开过滤器
 * 技术文档 V1.1.0 第 5.3 节
 * 根据 t_user.ai_disclosure_settings 过滤，构建可进入 AI Prompt 的画像
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileDisclosureFilter {

    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    private static final String DEFAULT_SETTINGS = "{\"mbti\":true,\"zodiac\":true,\"majorCategory\":true,\"interestTags\":true,\"naturalLangTags\":false,\"baziInfo\":false,\"questionnaireHints\":false}";

    /**
     * 构建目标用户经授权过滤后的可公开画像
     */
    public DisclosedProfile buildDisclosedProfile(Long targetUserId) {
        User user = userMapper.selectById(targetUserId);
        if (user == null) return new DisclosedProfile();

        JsonNode s = parseSettings(user.getAiDisclosureSettings());
        DisclosedProfile d = new DisclosedProfile();

        if (bool(s, "mbti")) d.setMbti(user.getMbti());
        if (bool(s, "zodiac")) d.setZodiac(user.getZodiac());
        if (bool(s, "majorCategory")) d.setMajorCategory(majorToCategory(user.getMajor()));
        if (bool(s, "interestTags")) d.setInterestTags(parseInterests(user.getInterests()));

        // 默认关闭，需用户主动开启
        if (bool(s, "naturalLangTags")) d.setNaturalLangTags(Collections.emptyList()); // TODO: t_user_ai_profile
        if (bool(s, "baziInfo") && user.getBazi() != null && !Boolean.TRUE.equals(user.getBaziUnknown())) {
            d.setBaziSummary(user.getBazi());
        }
        if (bool(s, "questionnaireHints")) d.setQuestionnaireHints(null); // TODO: 问卷摘要

        return d;
    }

    private JsonNode parseSettings(String json) {
        if (json == null || json.isBlank()) json = DEFAULT_SETTINGS;
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.warn("Parse ai_disclosure_settings failed: {}", e.getMessage());
            try {
                return objectMapper.readTree(DEFAULT_SETTINGS);
            } catch (Exception ex) {
                return objectMapper.createObjectNode();
            }
        }
    }

    private boolean bool(JsonNode s, String key) {
        if (s == null || !s.has(key)) return false;
        JsonNode v = s.get(key);
        return v != null && v.asBoolean(false);
    }

    private String majorToCategory(String major) {
        if (major == null || major.isBlank()) return "未填写";
        if (major.contains("计算机") || major.contains("软件") || major.contains("信息")) return "计算机";
        if (major.contains("经济") || major.contains("金融") || major.contains("管理")) return "经管";
        if (major.contains("文学") || major.contains("历史") || major.contains("哲学")) return "人文";
        if (major.contains("建筑") || major.contains("设计") || major.contains("艺术")) return "建筑艺术";
        if (major.contains("医学") || major.contains("护理")) return "医学";
        if (major.contains("法律")) return "法学";
        if (major.contains("教育") || major.contains("心理")) return "教育心理";
        return "其他";
    }

    private List<String> parseInterests(String interests) {
        if (interests == null || interests.isBlank()) return Collections.emptyList();
        return Arrays.stream(interests.split("[,，、]")).map(String::trim).filter(s -> !s.isBlank()).toList();
    }
}
