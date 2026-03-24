package com.campus.love.ai.service;

import com.campus.love.ai.entity.AiUsageLog;
import com.campus.love.ai.mapper.AiUsageLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiUsageLogService {

    public static final String BIZ_TYPE_AVATAR = "AVATAR";
    public static final String BIZ_TYPE_ANALYSIS = "ANALYSIS";
    public static final String SCENE_AVATAR_STUDIO = "AVATAR_STUDIO";

    private final AiUsageLogMapper aiUsageLogMapper;

    public void logAnalysisUsage(Integer tokensUsed, String modelName, Long userId, String scene, String bizKey) {
        insertUsage(BIZ_TYPE_ANALYSIS, normalizeScene(scene, "GENERAL"), "TEXT_CHAT", modelName, userId, bizKey, tokensUsed);
    }

    public void logAvatarUsage(Long userId, Integer tokensUsed, String modelName, String bizKey) {
        insertUsage(BIZ_TYPE_AVATAR, SCENE_AVATAR_STUDIO, "IMAGE_GENERATION", modelName, userId, bizKey, tokensUsed);
    }

    private void insertUsage(
            String bizType,
            String scene,
            String provider,
            String modelName,
            Long userId,
            String bizKey,
            Integer tokensUsed) {
        try {
            AiUsageLog logEntry = new AiUsageLog();
            logEntry.setBizType(bizType);
            logEntry.setScene(scene);
            logEntry.setProvider(provider);
            logEntry.setModelName(modelName);
            logEntry.setUserId(userId);
            logEntry.setBizKey(bizKey);
            logEntry.setTokensUsed(Math.max(0, tokensUsed != null ? tokensUsed : 0));
            logEntry.setCallCount(1);
            logEntry.setCreatedAt(LocalDateTime.now());
            aiUsageLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.warn("记录 AI 用量失败 bizType={} scene={} userId={} - {}", bizType, scene, userId, e.getMessage());
        }
    }

    private String normalizeScene(String scene, String fallback) {
        if (scene == null || scene.isBlank()) {
            return fallback;
        }
        return scene.trim();
    }
}
