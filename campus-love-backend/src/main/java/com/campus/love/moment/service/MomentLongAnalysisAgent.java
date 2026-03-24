package com.campus.love.moment.service;

import com.campus.love.ai.dto.AiChatResult;
import com.campus.love.ai.service.AiService;
import com.campus.love.moment.entity.MomentProfile;
import com.campus.love.profile.entity.UserPortrait;
import com.campus.love.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 心动时刻长文 AI 分析（异步任务专用，与规则模板 {@link MomentResultPackAgent#buildRuleBasedPack} 分离）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MomentLongAnalysisAgent {

    private static final String SYSTEM = """
            你是校园恋爱产品「心动时刻」的文案作者。请基于给定的双方画像与分数摘要，
            写一段 800～1500 字的中文深度分析，语气真诚、具体、避免说教与空洞形容词。
            不要输出 JSON 或 Markdown 标题，纯段落文本即可。
            """;

    private final AiService aiService;

    public String generateLongAnalysis(
            User userA,
            User userB,
            MomentProfile profileA,
            MomentProfile profileB,
            UserPortrait portraitA,
            UserPortrait portraitB,
            String scoreDetailJson
    ) {
        String prompt = """
                【用户A】%s
                【用户B】%s
                【分数与维度摘要 JSON】
                %s
                """.formatted(
                safe(userA != null ? userA.getNickname() : null),
                safe(userB != null ? userB.getNickname() : null),
                scoreDetailJson != null ? scoreDetailJson : "{}"
        );
        try {
            AiChatResult r = aiService.chatCompletion(SYSTEM, prompt);
            return r != null && r.getContent() != null ? r.getContent().trim() : "";
        } catch (Exception e) {
            log.warn("MomentLongAnalysisAgent failed: {}", e.getMessage());
            return "";
        }
    }

    private static String safe(String s) {
        return s != null && !s.isBlank() ? s : "用户";
    }
}
