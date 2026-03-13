package com.campus.love.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.campus.love.profile.entity.UserAiProfile;
import com.campus.love.profile.mapper.UserAiProfileMapper;
import com.campus.love.user.mapper.UserMapper;
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

    private final UserAiProfileMapper userAiProfileMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 用户修正自然语言标签
     * @param userId 当前用户
     * @param tags 新标签列表
     * @param isCore 是否核心标签（需 AI 验证）
     */
    public void correctNaturalTags(Long userId, List<String> tags, boolean isCore) {
        UserAiProfile profile = userAiProfileMapper.selectById(userId);
        if (profile == null) {
            log.warn("No AI profile for user {}, skip correction", userId);
            return;
        }
        try {
            if (isCore) {
                // TODO: 调用 Haiku 验证，温和提示
                log.info("Core tag correction for user {} (validation placeholder)", userId);
            }
            profile.setNaturalLanguageTags(tags != null ? objectMapper.writeValueAsString(tags) : null);
            userAiProfileMapper.updateById(profile);
        } catch (Exception e) {
            log.warn("Profile correction failed for user {}: {}", userId, e.getMessage());
        }
    }
}
