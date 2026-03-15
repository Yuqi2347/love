package com.campus.love.ai.agent;

import com.campus.love.ai.dto.AiChatResult;
import com.campus.love.ai.service.AiService;
import com.campus.love.moment.entity.MomentProfile;
import com.campus.love.moment.service.MomentPromptHelper;
import com.campus.love.profile.entity.UserPortrait;
import com.campus.love.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MomentAboutAgent {

    private static final String SYSTEM_PROMPT = """
            你是 Campus Love 的「心动一刻人物介绍编辑」。
            请把一个人的心动问卷信息写成一段像朋友介绍 TA 的自然语言，不要列标签，不要分点。

            约束：
            1. 只输出一段中文，不要 Markdown，不要前言。
            2. 控制在 90-150 字。
            3. 语气像“如果你见到TA，大概会觉得……”，自然、有温度。
            4. 必须结合 1.2/1.3/1.4/1.6、兴趣前三和情绪表达风格。
            5. 不要提分数，不要评价颜值。
            """;

    private final AiService aiService;

    public String generate(User targetUser, MomentProfile profile, UserPortrait portrait) {
        List<String> interests = MomentPromptHelper.interestNames(portrait, targetUser, 3);
        String prompt = """
                【人物基础】
                昵称：%s
                性别：%s
                学校：%s
                专业：%s

                【心动问卷】
                社交风格：%s
                生活节奏：%s
                性格底色：%s
                校园重心：%s
                情绪表达：%s

                【兴趣前三】
                %s

                请输出一段自然介绍。
                """.formatted(
                safe(targetUser != null ? targetUser.getNickname() : null, "匿名"),
                MomentPromptHelper.genderLabel(targetUser != null ? targetUser.getGender() : null),
                safe(targetUser != null ? targetUser.getSchool() : null, "未填"),
                safe(targetUser != null ? targetUser.getMajor() : null, "未填"),
                MomentPromptHelper.socialStyle(profile != null ? profile.getSocialStyle() : null),
                MomentPromptHelper.lifeRhythm(profile != null ? profile.getLifeRhythm() : null),
                MomentPromptHelper.personalityBase(profile != null ? profile.getPersonalityBase() : null),
                MomentPromptHelper.campusFocus(profile != null ? profile.getCampusFocus() : null),
                MomentPromptHelper.emotionStyle(profile != null ? profile.getEmotionStyle() : null),
                MomentPromptHelper.joinOrDefault(interests, "暂无明显兴趣标签")
        );
        try {
            AiChatResult result = aiService.chatCompletion(SYSTEM_PROMPT, prompt);
            String content = result != null && result.getContent() != null ? result.getContent().trim() : "";
            if (!content.isBlank()) {
                return content;
            }
        } catch (Exception e) {
            log.warn("MomentAboutAgent failed: {}", e.getMessage());
        }
        return fallback(targetUser, profile, interests);
    }

    private String fallback(User targetUser, MomentProfile profile, List<String> interests) {
        return "%s给人的第一感觉，不是特别用力地表现自己，而是在%s和%s之间保持一种舒服的分寸。TA更像那种%s的人，平时会把注意力放在%s上，也会被%s这类事情轻易打动。和TA相处，不一定一开始就热烈，但很容易在细节里慢慢觉得安心。".formatted(
                safe(targetUser != null ? targetUser.getNickname() : null, "TA"),
                MomentPromptHelper.socialStyle(profile != null ? profile.getSocialStyle() : null),
                MomentPromptHelper.emotionStyle(profile != null ? profile.getEmotionStyle() : null),
                MomentPromptHelper.personalityBase(profile != null ? profile.getPersonalityBase() : null),
                MomentPromptHelper.campusFocus(profile != null ? profile.getCampusFocus() : null),
                MomentPromptHelper.joinOrDefault(interests, "细小的日常兴趣")
        );
    }

    private String safe(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
