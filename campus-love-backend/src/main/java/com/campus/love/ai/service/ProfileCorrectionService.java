package com.campus.love.ai.service;

import com.campus.love.user.entity.User;
import com.campus.love.profile.entity.UserAiProfile;
import com.campus.love.profile.entity.UserPortrait;
import com.campus.love.profile.mapper.UserAiProfileMapper;
import com.campus.love.profile.service.UserPortraitService;
import com.campus.love.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户标签修正（技术文档 V1.1.0 第 1.2 节）
 * 用户主动在「我的性格画像」页点击标签修改时触发
 * 核心标签弹提示；次要标签直接生效
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileCorrectionService {

    private final AiService aiService;
    private final UserAiProfileMapper userAiProfileMapper;
    private final UserPortraitService userPortraitService;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 用户修正自然语言标签
     * @param userId 当前用户
     * @param tags 新标签列表
     * @param isCore 是否核心标签（需 AI 验证）
     */
    public void correctNaturalTags(Long userId, List<String> tags, boolean isCore) {
        UserAiProfile profile = userPortraitService.getAiProfileView(userId);
        if (profile == null) profile = userAiProfileMapper.selectById(userId);
        if (profile == null) {
            log.warn("No AI profile for user {}, skip correction", userId);
            return;
        }
        try {
            if (isCore) {
                validateCoreTags(userId, tags);
            }
            String tagsJson = tags != null ? objectMapper.writeValueAsString(tags) : null;
            profile.setNaturalLanguageTags(tagsJson);
            userAiProfileMapper.updateById(profile);
            UserPortrait portrait = userPortraitService.getPortrait(userId);
            if (portrait != null) {
                portrait.setNaturalLanguageTags(tagsJson);
                userPortraitService.savePortrait(portrait);
            }
        } catch (Exception e) {
            log.warn("Profile correction failed for user {}: {}", userId, e.getMessage());
        }
    }

    private void validateCoreTags(Long userId, List<String> tags) {
        try {
            User user = userMapper.selectById(userId);
            UserPortrait portrait = userPortraitService.getPortrait(userId);
            String prompt = String.format(
                    "用户MBTI=%s，当前画像标签=%s，用户想改成=%s。请判断这些核心人格标签是否明显矛盾。若明显矛盾，仅输出 WARN；否则输出 OK。",
                    user != null ? user.getMbti() : "未知",
                    portrait != null ? portrait.getNaturalLanguageTags() : "[]",
                    tags
            );
            var result = aiService.chatCompletion("你是人物画像校验助手，只输出 OK 或 WARN。", prompt);
            String content = result != null && result.getContent() != null ? result.getContent().trim() : "OK";
            log.info("Core tag correction validation for user {} => {}", userId, content);
        } catch (Exception e) {
            log.warn("Core tag validation failed for user {}: {}", userId, e.getMessage());
        }
    }
}
