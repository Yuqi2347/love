package com.campus.love.ai.skill;

import com.campus.love.ai.dto.YuanFenAnalysisResponse;
import com.campus.love.ai.service.AiService;
import com.campus.love.match.dto.MatchResultResponse;
import com.campus.love.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class YuanFenAnalysisSkill {

    private final AiService aiService;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT_OPPOSITE = """
            你是 Campus Love 的专属「缘分解析师」，精通 MBTI 性格理论、中国传统八字命理、以及现代心理学中的亲密关系研究。
            你正在为一对异性用户进行缘分分析，可以从恋爱、感情发展、浪漫关系等角度进行分析。
            你的分析风格：温暖、有趣、真实，像一位了解两人的智慧朋友，而不是冷冰冰的算法。
            你的回答必须严格按照 JSON 格式输出，不要包含任何 JSON 以外的文字（不要用 ```json 包裹）。""";

    private static final String SYSTEM_PROMPT_SAME = """
            你是 Campus Love 的专属「缘分解析师」，精通 MBTI 性格理论、中国传统八字命理、以及现代心理学中的人际关系研究。
            你正在为两位同性用户进行缘分分析，请从友情、知己、灵魂伙伴等角度进行分析，重点分析两人成为好朋友/挚友的可能性。
            你的分析风格：温暖、有趣、真实，像一位了解两人的智慧朋友，而不是冷冰冰的算法。
            你的回答必须严格按照 JSON 格式输出，不要包含任何 JSON 以外的文字（不要用 ```json 包裹）。""";

    private static final String PROMPT_TEMPLATE_OPPOSITE = """
            请根据以下两位用户的真实信息，为他们生成一份「缘分解析报告」。
            他们是一对异性朋友，请从感情发展和恋爱可能性的角度进行分析。

            【用户A信息】
            - 昵称：{nicknameA}
            - 性别：{genderA}
            - 年龄：{ageA} 岁
            - MBTI：{mbtiA}
            - 星座：{zodiacA}
            - 兴趣爱好：{interestsA}
            - 专业：{majorA}

            【用户B信息】
            - 昵称：{nicknameB}
            - 性别：{genderB}
            - 年龄：{ageB} 岁
            - MBTI：{mbtiB}
            - 星座：{zodiacB}
            - 兴趣爱好：{interestsB}
            - 专业：{majorB}

            【匹配系统评分】
            - 综合匹配度：{totalScore} 分（满分100）
            - 兴趣契合：{interestScore} 分
            - MBTI 契合：{mbtiScore} 分
            - 星座契合：{zodiacScore} 分
            - 八字缘分：{baziScore} 分
            - 专业匹配：{majorScore} 分

            【分析要求】
            请按以下 JSON 结构输出，所有文字使用温暖亲切的中文，可以从恋爱和感情角度分析：

            {
              "yuanFenIndex": "（用一个有创意的词或短语描述两人缘分等级，如「命中注定」「有缘千里」「相知恨晚」「怦然心动」等，禁止只输出数字）",
              "personalityAnalysis": "（100字以内，分析两人性格如何互动互补，可以从恋爱相处的角度举一个具体的生活场景例子）",
              "recommendActivities": [
                "活动1（结合两人兴趣，适合约会或增进感情的活动）",
                "活动2",
                "活动3"
              ],
              "potentialChallenge": "（60字以内，真实指出一个感情相处中可能的摩擦点，语气要温柔，给出一句化解建议）",
              "developmentPotential": "（80字以内，综合所有维度，从感情发展角度给出有温度的建议，不要做绝对化断言）",
              "exclusiveQuote": "（一句20字以内的专属缘分金句，要有诗意或浪漫，让人想截图分享）"
            }""";

    private static final String PROMPT_TEMPLATE_SAME = """
            请根据以下两位用户的真实信息，为他们生成一份「友谊缘分解析报告」。
            他们是两位同性朋友，请从友情和挚友角度进行分析，探讨他们成为好朋友、知己的可能性。

            【用户A信息】
            - 昵称：{nicknameA}
            - 性别：{genderA}
            - 年龄：{ageA} 岁
            - MBTI：{mbtiA}
            - 星座：{zodiacA}
            - 兴趣爱好：{interestsA}
            - 专业：{majorA}

            【用户B信息】
            - 昵称：{nicknameB}
            - 性别：{genderB}
            - 年龄：{ageB} 岁
            - MBTI：{mbtiB}
            - 星座：{zodiacB}
            - 兴趣爱好：{interestsB}
            - 专业：{majorB}

            【匹配系统评分】
            - 综合匹配度：{totalScore} 分（满分100）
            - 兴趣契合：{interestScore} 分
            - MBTI 契合：{mbtiScore} 分
            - 星座契合：{zodiacScore} 分
            - 八字缘分：{baziScore} 分
            - 专业匹配：{majorScore} 分

            【分析要求】
            请按以下 JSON 结构输出，所有文字使用温暖亲切的中文，从友情角度分析（不要涉及恋爱内容）：

            {
              "yuanFenIndex": "（用一个有创意的词或短语描述两人友谊缘分等级，如「灵魂搭档」「志同道合」「相见恨晚」「铁杆知己」等，禁止只输出数字）",
              "personalityAnalysis": "（100字以内，分析两人性格如何互补互助，举一个具体的友情生活场景例子）",
              "recommendActivities": [
                "活动1（结合两人兴趣，适合朋友一起做的活动）",
                "活动2",
                "活动3"
              ],
              "potentialChallenge": "（60字以内，真实指出友情相处中可能的摩擦点，语气要温柔，给出一句化解建议）",
              "developmentPotential": "（80字以内，综合所有维度，从友情发展角度给出有温度的建议，不要做绝对化断言）",
              "exclusiveQuote": "（一句20字以内的专属友谊金句，要有趣味或哲理，让人想截图分享）"
            }""";

    public YuanFenAnalysisResponse analyze(User userA, User userB, MatchResultResponse matchResult) {
        boolean sameGender = isSameGender(userA.getGender(), userB.getGender());
        String systemPrompt = sameGender ? SYSTEM_PROMPT_SAME : SYSTEM_PROMPT_OPPOSITE;
        String userPrompt = buildPrompt(userA, userB, matchResult, sameGender);

        String aiResponse;
        try {
            aiResponse = aiService.chatCompletion(systemPrompt, userPrompt);
        } catch (Exception e) {
            log.warn("AI 服务调用失败，使用本地降级结果: {}", e.getMessage());
            return buildFallbackResult(matchResult, sameGender);
        }

        // 清理可能的 markdown 代码块包裹
        String json = aiResponse.trim();
        if (json.startsWith("```")) {
            json = json.replaceFirst("```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }

        try {
            YuanFenAnalysisResponse result = objectMapper.readValue(json, YuanFenAnalysisResponse.class);
            result.setGeneratedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            result.setNextAvailableAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            return result;
        } catch (Exception e) {
            log.error("解析 AI 返回 JSON 失败: {}", json, e);
            return buildFallbackResult(matchResult, sameGender);
        }
    }

    /** 判断是否同性 */
    private boolean isSameGender(Integer genderA, Integer genderB) {
        if (genderA == null || genderB == null || genderA == 0 || genderB == 0) {
            return false; // 未知性别默认按异性处理
        }
        return genderA.equals(genderB);
    }

    private String buildPrompt(User a, User b, MatchResultResponse match, boolean sameGender) {
        String template = sameGender ? PROMPT_TEMPLATE_SAME : PROMPT_TEMPLATE_OPPOSITE;
        MatchResultResponse.MatchDetail detail = match.getDetail();
        return template
                .replace("{nicknameA}", desensitize(a.getNickname()))
                .replace("{genderA}", genderLabel(a.getGender()))
                .replace("{ageA}", String.valueOf(calcAge(a.getBirthDate())))
                .replace("{mbtiA}", safe(a.getMbti()))
                .replace("{zodiacA}", safe(a.getZodiac()))
                .replace("{interestsA}", safe(a.getInterests()))
                .replace("{majorA}", safe(a.getMajor()))
                .replace("{nicknameB}", desensitize(b.getNickname()))
                .replace("{genderB}", genderLabel(b.getGender()))
                .replace("{ageB}", String.valueOf(calcAge(b.getBirthDate())))
                .replace("{mbtiB}", safe(b.getMbti()))
                .replace("{zodiacB}", safe(b.getZodiac()))
                .replace("{interestsB}", safe(b.getInterests()))
                .replace("{majorB}", safe(b.getMajor()))
                .replace("{totalScore}", String.valueOf(match.getMatchScore()))
                .replace("{interestScore}", String.valueOf(detail.getInterestScore()))
                .replace("{mbtiScore}", String.valueOf(detail.getMbtiScore()))
                .replace("{zodiacScore}", String.valueOf(detail.getZodiacScore()))
                .replace("{baziScore}", String.valueOf(detail.getBaziScore()))
                .replace("{majorScore}", String.valueOf(detail.getMajorScore()));
    }

    /** 脱敏：只保留前2字 */
    private String desensitize(String nickname) {
        if (nickname == null || nickname.isEmpty()) return "匿名";
        return nickname.length() <= 2 ? nickname : nickname.substring(0, 2) + "**";
    }

    private String genderLabel(Integer gender) {
        if (gender == null) return "未知";
        return gender == 1 ? "男" : "女";
    }

    private int calcAge(LocalDate birthDate) {
        if (birthDate == null) return 20;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "未填写" : value;
    }

    /** AI 调用失败时的降级结果 */
    private YuanFenAnalysisResponse buildFallbackResult(MatchResultResponse match, boolean sameGender) {
        YuanFenAnalysisResponse result = new YuanFenAnalysisResponse();
        int score = match.getMatchScore() != null ? match.getMatchScore() : 50;

        if (sameGender) {
            result.setYuanFenIndex(score >= 70 ? "灵魂搭档" : score >= 50 ? "志同道合" : "有缘相识");
            result.setPersonalityAnalysis("你们各有特点，在相处中可以互相学习、互相支持，有成为挚友的潜质。");
            result.setRecommendActivities(java.util.List.of("一起去校园咖啡馆聊天", "参加学校社团活动", "组队打比赛或一起自习"));
            result.setPotentialChallenge("好朋友之间也需要空间，学会尊重彼此的节奏，友谊会更长久。");
            result.setDevelopmentPotential("保持真诚的交流，多分享生活中的小事，你们会成为彼此最好的伙伴。");
            result.setExclusiveQuote("最好的友情是彼此成就，一起发光。");
        } else {
            result.setYuanFenIndex(score >= 70 ? "有缘千里" : score >= 50 ? "妙不可言" : "缘起缘落");
            result.setPersonalityAnalysis("你们各有特点，在相处中可以相互欣赏、互相成长，未来充满可能性。");
            result.setRecommendActivities(java.util.List.of("一起去校园咖啡馆聊天", "找一部两人都喜欢的电影看", "周末一起探索校园周边美食"));
            result.setPotentialChallenge("每段感情都需要磨合，多一些耐心倾听，少一些急于求成。");
            result.setDevelopmentPotential("保持真诚的交流，用心去了解对方，美好的事情会自然发生。");
            result.setExclusiveQuote("缘分是两颗心恰好在同一个频率上跳动。");
        }

        result.setGeneratedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        result.setNextAvailableAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return result;
    }
}
