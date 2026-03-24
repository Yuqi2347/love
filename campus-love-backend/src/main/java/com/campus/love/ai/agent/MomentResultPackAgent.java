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
public class MomentResultPackAgent {

    private static final String SYSTEM_PROMPT = """
            你是 Campus Love 的「心动时刻结果编辑」。
            你要基于同一对匹配对象，一次性输出结果页需要的完整内容。
            只输出合法 JSON，不要输出 Markdown，不要输出解释。

            JSON 结构固定：
            {
              "insight": {
                "card1": "为什么你们可能互相吸引，50-80字",
                "card2": "你们在一起可能是什么感觉，50-80字",
                "card3": "你们可能需要磨合的地方，50-80字",
                "goldenSentence": "15-24字的收尾金句"
              },
              "aboutUserA": "像朋友介绍A的自然段，90-150字",
              "aboutUserB": "像朋友介绍B的自然段，90-150字"
            }

            约束：
            1. insight.card1 必须引用互补模式或共同兴趣。
            2. insight.card2 必须引用陪伴方式、约会方式或生活节奏。
            3. insight.card3 必须引用软惩罚或低一致项，但语气要温和，不像警告。
            4. aboutUserA/aboutUserB 必须像“如果你见到TA，大概会觉得……”这样的自然介绍，不能列标签。
            5. aboutUserA/aboutUserB 必须结合社交风格、生活节奏、性格底色、校园重心、情绪表达和兴趣。
            6. 不要提分数，不要评价颜值。
            7. 同性与异性都按恋爱/心动方向来写。
            """;

    private final AiService aiService;
    private final ObjectMapper objectMapper;

    /**
     * 与 LLM 无关的规则/模板生成（用于匹配落库阶段，避免阻塞在同步大模型调用上）。
     */
    public ResultPack buildRuleBasedPack(
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
        List<String> interestsA = MomentPromptHelper.interestNames(portraitA, userA, 3);
        List<String> interestsB = MomentPromptHelper.interestNames(portraitB, userB, 3);
        return fallback(complementaryModes, commonInterests, softPenaltyReasons, userA, userB, profileA, profileB, interestsA, interestsB);
    }

    public ResultPack generate(
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
        List<String> interestsA = MomentPromptHelper.interestNames(portraitA, userA, 3);
        List<String> interestsB = MomentPromptHelper.interestNames(portraitB, userB, 3);
        String prompt = """
                【双方昵称】
                A：%s
                B：%s

                【命中的互补模式】
                %s

                【共同兴趣锚点】
                %s

                【需要磨合的地方】
                %s

                【A的心动画像】
                %s

                【A的兴趣前三】
                %s

                【B的心动画像】
                %s

                【B的兴趣前三】
                %s

                请按固定 JSON 输出。
                """.formatted(
                safeName(userA),
                safeName(userB),
                MomentPromptHelper.joinOrDefault(complementaryModes, "暂无明显互补模式"),
                MomentPromptHelper.joinOrDefault(commonInterests, "暂无明显共同兴趣"),
                MomentPromptHelper.joinOrDefault(softPenaltyReasons, "整体节奏较为顺畅"),
                MomentPromptHelper.summarizeProfile(profileA),
                MomentPromptHelper.joinOrDefault(interestsA, "暂无明显兴趣标签"),
                MomentPromptHelper.summarizeProfile(profileB),
                MomentPromptHelper.joinOrDefault(interestsB, "暂无明显兴趣标签")
        );
        try {
            AiChatResult result = aiService.chatCompletion(SYSTEM_PROMPT, prompt);
            String json = MomentAiJsonSupport.clean(result != null ? result.getContent() : "");
            ResultPack parsed = objectMapper.readValue(json, ResultPack.class);
            return fillFallbackIfBlank(parsed, complementaryModes, commonInterests, softPenaltyReasons, userA, userB, profileA, profileB, interestsA, interestsB);
        } catch (Exception e) {
            log.warn("MomentResultPackAgent failed: {}", e.getMessage());
            return fallback(complementaryModes, commonInterests, softPenaltyReasons, userA, userB, profileA, profileB, interestsA, interestsB);
        }
    }

    private ResultPack fillFallbackIfBlank(
            ResultPack parsed,
            List<String> complementaryModes,
            List<String> commonInterests,
            List<String> softPenaltyReasons,
            User userA,
            User userB,
            MomentProfile profileA,
            MomentProfile profileB,
            List<String> interestsA,
            List<String> interestsB
    ) {
        ResultPack fallback = fallback(complementaryModes, commonInterests, softPenaltyReasons, userA, userB, profileA, profileB, interestsA, interestsB);
        if (parsed == null) {
            return fallback;
        }
        if (parsed.insight == null) {
            parsed.insight = fallback.insight;
        } else {
            if (isBlank(parsed.insight.card1)) parsed.insight.card1 = fallback.insight.card1;
            if (isBlank(parsed.insight.card2)) parsed.insight.card2 = fallback.insight.card2;
            if (isBlank(parsed.insight.card3)) parsed.insight.card3 = fallback.insight.card3;
            if (isBlank(parsed.insight.goldenSentence)) parsed.insight.goldenSentence = fallback.insight.goldenSentence;
        }
        if (isBlank(parsed.aboutUserA)) parsed.aboutUserA = fallback.aboutUserA;
        if (isBlank(parsed.aboutUserB)) parsed.aboutUserB = fallback.aboutUserB;
        return parsed;
    }

    private ResultPack fallback(
            List<String> complementaryModes,
            List<String> commonInterests,
            List<String> softPenaltyReasons,
            User userA,
            User userB,
            MomentProfile profileA,
            MomentProfile profileB,
            List<String> interestsA,
            List<String> interestsB
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
        return new ResultPack(
                new InsightBlock(card1, card2, card3, goldenSentence),
                buildAboutFallback(userA, profileA, interestsA),
                buildAboutFallback(userB, profileB, interestsB)
        );
    }

    private String buildAboutFallback(User targetUser, MomentProfile profile, List<String> interests) {
        return "%s给人的第一感觉，不是特别用力地表现自己，而是在%s和%s之间保持一种舒服的分寸。TA更像那种%s的人，平时会把注意力放在%s上，也会被%s这类事情轻易打动。和TA相处，不一定一开始就热烈，但很容易在细节里慢慢觉得安心。".formatted(
                safe(targetUser != null ? targetUser.getNickname() : null, "TA"),
                MomentPromptHelper.socialStyle(profile != null ? profile.getSocialStyle() : null),
                MomentPromptHelper.emotionStyle(profile != null ? profile.getEmotionStyle() : null),
                MomentPromptHelper.personalityBase(profile != null ? profile.getPersonalityBase() : null),
                MomentPromptHelper.campusFocus(profile != null ? profile.getCampusFocus() : null),
                MomentPromptHelper.joinOrDefault(interests, "细小的日常兴趣")
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String safeName(User user) {
        if (user == null || user.getNickname() == null || user.getNickname().isBlank()) {
            return "匿名";
        }
        return user.getNickname().trim();
    }

    private String safe(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    public static class ResultPack {
        public InsightBlock insight;
        public String aboutUserA;
        public String aboutUserB;

        public ResultPack() {
        }

        public ResultPack(InsightBlock insight, String aboutUserA, String aboutUserB) {
            this.insight = insight;
            this.aboutUserA = aboutUserA;
            this.aboutUserB = aboutUserB;
        }
    }

    public static class InsightBlock {
        public String card1;
        public String card2;
        public String card3;
        public String goldenSentence;

        public InsightBlock() {
        }

        public InsightBlock(String card1, String card2, String card3, String goldenSentence) {
            this.card1 = card1;
            this.card2 = card2;
            this.card3 = card3;
            this.goldenSentence = goldenSentence;
        }
    }
}
