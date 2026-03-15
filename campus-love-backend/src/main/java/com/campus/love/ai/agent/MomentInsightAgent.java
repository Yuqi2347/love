package com.campus.love.ai.agent;

import com.campus.love.ai.dto.AiChatResult;
import com.campus.love.ai.service.AiService;
import com.campus.love.moment.entity.MomentProfile;
import com.campus.love.moment.service.MomentPromptHelper;
import com.campus.love.profile.entity.UserPortrait;
import com.campus.love.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MomentInsightAgent {

    private static final String SYSTEM_PROMPT = """
            你是 Campus Love 的「心动一刻洞察编辑」。
            你的任务是把匹配数据改写成适合卡片展示的三段洞察。

            输出要求：
            1. 必须输出合法 JSON，不要输出 Markdown。
            2. JSON 结构固定：
            {
              "card1": "为什么你们可能互相吸引，50-80字",
              "card2": "你们在一起可能是什么感觉，50-80字",
              "card3": "你们可能需要磨合的地方，50-80字",
              "goldenSentence": "15-24字的收尾金句"
            }
            3. card1 必须引用互补模式或共同兴趣。
            4. card2 必须引用陪伴方式、约会方式或生活节奏。
            5. card3 必须引用软惩罚或低一致项，但语气要温和，像提前提醒，不像警告。
            6. 语言要有画面感，但不要空泛，不要出现分数。
            7. 同性与异性都按恋爱/心动方向来写。
            """;

    private final AiService aiService;
    private final ObjectMapper objectMapper;

    public InsightResult generate(
            User userA,
            User userB,
            MomentProfile profileA,
            MomentProfile profileB,
            UserPortrait portraitA,
            UserPortrait portraitB,
            List<String> complementaryModes,
            List<String> softPenaltyReasons
    ) {
        List<String> commonInterests = MomentPromptHelper.commonInterestNames(portraitA, userA, portraitB, userB, 3);
        String prompt = """
                【双方昵称】
                A：%s
                B：%s

                【命中的互补模式】
                %s

                【共同兴趣锚点】
                %s

                【A的心动画像】
                %s

                【B的心动画像】
                %s

                【需要磨合的地方】
                %s

                请按固定 JSON 输出三张卡片与金句。
                """.formatted(
                safeName(userA),
                safeName(userB),
                MomentPromptHelper.joinOrDefault(complementaryModes, "暂无明显互补模式"),
                MomentPromptHelper.joinOrDefault(commonInterests, "暂无明显共同兴趣"),
                MomentPromptHelper.summarizeProfile(profileA),
                MomentPromptHelper.summarizeProfile(profileB),
                MomentPromptHelper.joinOrDefault(softPenaltyReasons, "整体节奏较为顺畅")
        );
        try {
            AiChatResult result = aiService.chatCompletion(SYSTEM_PROMPT, prompt);
            String json = MomentAiJsonSupport.clean(result != null ? result.getContent() : "");
            InsightResult parsed = objectMapper.readValue(json, InsightResult.class);
            fillFallbackIfBlank(parsed, complementaryModes, commonInterests, softPenaltyReasons, profileA, profileB);
            return parsed;
        } catch (Exception e) {
            log.warn("MomentInsightAgent failed: {}", e.getMessage());
            return fallback(complementaryModes, commonInterests, softPenaltyReasons, profileA, profileB);
        }
    }

    private void fillFallbackIfBlank(
            InsightResult parsed,
            List<String> complementaryModes,
            List<String> commonInterests,
            List<String> softPenaltyReasons,
            MomentProfile profileA,
            MomentProfile profileB
    ) {
        InsightResult fallback = fallback(complementaryModes, commonInterests, softPenaltyReasons, profileA, profileB);
        if (parsed.card1 == null || parsed.card1.isBlank()) {
            parsed.card1 = fallback.card1;
        }
        if (parsed.card2 == null || parsed.card2.isBlank()) {
            parsed.card2 = fallback.card2;
        }
        if (parsed.card3 == null || parsed.card3.isBlank()) {
            parsed.card3 = fallback.card3;
        }
        if (parsed.goldenSentence == null || parsed.goldenSentence.isBlank()) {
            parsed.goldenSentence = fallback.goldenSentence;
        }
    }

    private InsightResult fallback(
            List<String> complementaryModes,
            List<String> commonInterests,
            List<String> softPenaltyReasons,
            MomentProfile profileA,
            MomentProfile profileB
    ) {
        String mode = complementaryModes != null && !complementaryModes.isEmpty() ? complementaryModes.get(0) : "刚好对频";
        String interests = MomentPromptHelper.joinOrDefault(commonInterests, "一些细小但舒服的兴趣点");
        String card1 = "你会发现，最先把你们拉近的，往往不是用力制造的话题，而是「%s」这种天然的吸引感，再加上%s，让靠近变得顺理成章。".formatted(mode, interests);
        String card2 = "大概是那种一边保留自己原本的节奏，一边又愿意为对方多走一步的感觉。%s遇上%s，日常相处里会慢慢长出默契。".formatted(
                MomentPromptHelper.companionshipStyle(profileA != null ? profileA.getCompanionshipStyle() : null),
                MomentPromptHelper.companionshipStyle(profileB != null ? profileB.getCompanionshipStyle() : null)
        );
        String friction = softPenaltyReasons != null && !softPenaltyReasons.isEmpty()
                ? softPenaltyReasons.get(0)
                : "彼此对亲近和独处的需求未必完全同步";
        String card3 = "需要提前知道的是，%s 可能会让你们在某些时刻出现误读。好消息是，只要愿意把自己的节奏说清楚，这种差异反而会变成理解彼此的入口。".formatted(friction);
        String goldenSentence = "缘分不是相同，而是愿意刚好靠近。";
        return new InsightResult(card1, card2, card3, goldenSentence);
    }

    private String safeName(User user) {
        if (user == null || user.getNickname() == null || user.getNickname().isBlank()) {
            return "匿名";
        }
        return user.getNickname().trim();
    }

    public static class InsightResult {
        public String card1;
        public String card2;
        public String card3;
        public String goldenSentence;

        public InsightResult() {
        }

        public InsightResult(String card1, String card2, String card3, String goldenSentence) {
            this.card1 = card1;
            this.card2 = card2;
            this.card3 = card3;
            this.goldenSentence = goldenSentence;
        }
    }
}
