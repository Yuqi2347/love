package com.campus.love.ai.skill;

import com.campus.love.ai.dto.AiChatResult;
import com.campus.love.ai.dto.YuanFenAnalysisResponse;
import com.campus.love.ai.dto.YuanFenAnalysisResult;
import com.campus.love.ai.prompt.YuanFenPromptTemplates;
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

    /** 用户 Prompt 模板（异性） */
    private static final String PROMPT_TEMPLATE_OPPOSITE = """
            ------------------------------------------------

            【用户A信息】

            - 昵称：{nicknameA}
            - 性别：{genderA}
            - 年龄：{ageA} 岁
            - 学校：{schoolA}
            - 年级：{gradeA}
            - MBTI：{mbtiA}
            - 星座：{zodiacA}
            - 兴趣爱好：{interestsA}
            - 专业：{majorA}

            【用户B信息】

            - 昵称：{nicknameB}
            - 性别：{genderB}
            - 年龄：{ageB} 岁
            - 学校：{schoolB}
            - 年级：{gradeB}
            - MBTI：{mbtiB}
            - 星座：{zodiacB}
            - 兴趣爱好：{interestsB}
            - 专业：{majorB}

            ------------------------------------------------

            匹配系统评分（分析时请参考）：

            - 综合匹配度：{totalScore} /100
            - 兴趣契合：{interestScore}
            - MBTI 契合：{mbtiScore}
            - 星座契合：{zodiacScore}
            - 八字缘分：{baziScore}
            - 专业匹配：{majorScore}

            ------------------------------------------------

            【分析要求】

            请综合以上信息，从性格、兴趣、学习环境、恋爱模式等角度进行分析。

            分析要做到：
            1. 有逻辑（解释为什么适合或不适合）
            2. 有生活感（举简单的校园或日常相处场景）
            3. 有情绪温度（读起来像朋友在认真分析）

            ------------------------------------------------

            请按以下 JSON 结构输出：

            {
              "yuanFenIndex": "（用一个有创意、有情绪感的短语描述两人缘分等级，例如：命运的小小暗号、慢慢靠近的频率、可能错过也可能相遇、怦然心动型缘分等）",
              "overallInterpretation": "（150字以内，总体评价两人的缘分气质，语气像一个懂他们的朋友在总结这段关系的感觉）",
              "personalityAnalysis": "（150字以内，分析MBTI和性格互动模式，例如谁更主动、谁更理性，举一个具体生活场景）",
              "interestChemistry": "（120字以内，分析兴趣爱好带来的互动方式，比如一起做什么会更容易产生好感）",
              "campusStoryScene": "（100字以内，用一个很具体的校园场景描写两人的可能互动，例如图书馆、自习室、操场、咖啡店等，让用户产生画面感）",
              "recommendActivities": [
                "活动1（结合两人兴趣设计一个有点浪漫的约会或互动方式）",
                "活动2",
                "活动3"
              ],
              "potentialChallenge": "（80字以内，指出一个现实中可能的摩擦点，但语气温柔，并给出一个简单建议）",
              "developmentPotential": "（120字以内，结合评分理性判断两人的发展潜力，不要做绝对化结论，而是给出鼓励式建议）",
              "exclusiveQuote": "（一句20字以内的浪漫金句，适合截图分享）"
            }
            """ + YuanFenPromptTemplates.JSON_EXAMPLE_OPPOSITE;

    /** 用户 Prompt 模板（同性） */
    private static final String PROMPT_TEMPLATE_SAME = """
            ------------------------------------------------

            【用户A信息】

            - 昵称：{nicknameA}
            - 性别：{genderA}
            - 年龄：{ageA} 岁
            - 学校：{schoolA}
            - 年级：{gradeA}
            - MBTI：{mbtiA}
            - 星座：{zodiacA}
            - 兴趣爱好：{interestsA}
            - 专业：{majorA}

            【用户B信息】

            - 昵称：{nicknameB}
            - 性别：{genderB}
            - 年龄：{ageB} 岁
            - 学校：{schoolB}
            - 年级：{gradeB}
            - MBTI：{mbtiB}
            - 星座：{zodiacB}
            - 兴趣爱好：{interestsB}
            - 专业：{majorB}

            ------------------------------------------------

            匹配系统评分（请参考）：

            - 综合匹配度：{totalScore} /100
            - 兴趣契合：{interestScore}
            - MBTI 契合：{mbtiScore}
            - 星座契合：{zodiacScore}
            - 八字缘分：{baziScore}
            - 专业匹配：{majorScore}

            ------------------------------------------------

            【分析要求】

            请综合以上信息，从以下角度分析两人的关系：

            - 性格互动模式
            - 兴趣与生活方式契合度
            - 在校园环境中的互动氛围
            - 情感连接方式
            - 关系发展的潜在可能

            分析风格要求：

            - 真实、有温度
            - 有生活场景感
            - 像朋友在认真观察两人的关系
            - 可以适当描述日常互动情景

            请确保分析内容 **大体符合系统评分**：
            - 高分 → 更容易产生深度连接
            - 中分 → 有潜力但需要磨合
            - 低分 → 更像不同轨道的人

            ------------------------------------------------

            请按以下 JSON 结构输出：

            {
              "yuanFenIndex": "（用一个有情绪感的短语描述两人关系，例如：同频的灵魂、默契搭子、奇妙组合、慢慢靠近的关系、意外的同路人等）",
              "overallInterpretation": "（150字以内，总体描述两人之间的关系气质，例如更像朋友型关系、互补型关系、灵魂交流型关系等）",
              "personalityInteraction": "（150字以内，分析两人MBTI性格互动方式，并举一个具体生活互动场景）",
              "interestChemistry": "（120字以内，分析兴趣爱好带来的互动方式，例如一起做什么会更有默契）",
              "campusMoment": "（100字以内，用一个具体的校园场景描写两人互动，例如自习室、社团活动、咖啡店聊天等，让用户产生画面感）",
              "relationshipPotential": "（120字以内，分析这种关系未来可能的发展方向，例如成为长期好友、彼此支持的伙伴，或存在更深情感可能）",
              "potentialChallenge": "（80字以内，指出可能的摩擦点，但语气温柔，并给出简单建议）",
              "recommendActivities": [
                "互动建议1（适合两人的活动）",
                "互动建议2",
                "互动建议3"
              ],
              "exclusiveQuote": "（一句20字以内的关系金句，可以偏友情，也可以带一点暧昧氛围，适合截图分享）"
            }
            """ + YuanFenPromptTemplates.JSON_EXAMPLE_SAME;

    public YuanFenAnalysisResult analyze(User userA, User userB, MatchResultResponse matchResult) {
        boolean sameGender = isSameGender(userA.getGender(), userB.getGender());
        String systemPrompt = sameGender ? YuanFenPromptTemplates.SYSTEM_SAME : YuanFenPromptTemplates.SYSTEM_OPPOSITE;
        String userPrompt = buildPrompt(userA, userB, matchResult, sameGender);

        AiChatResult aiResult;
        try {
            aiResult = aiService.chatCompletion(systemPrompt, userPrompt);
        } catch (Exception e) {
            log.warn("YuanFen AI call FAILED: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            return new YuanFenAnalysisResult(buildFallbackResult(matchResult, sameGender), 0);
        }

        // 清理可能的 markdown 代码块包裹
        String json = aiResult.getContent().trim();
        if (json.startsWith("```")) {
            json = json.replaceFirst("```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }
        // 智谱等模型可能返回中文/弯引号包裹内容，直接移除这些字符（避免替换为"导致双引号破坏JSON）
        json = json.replace("「", "").replace("」", "").replace("『", "").replace("』", "")
                .replace("\u201C", "").replace("\u201D", "");  // " " 弯引号

        try {
            YuanFenAnalysisResponse result = objectMapper.readValue(json, YuanFenAnalysisResponse.class);
            // 同性版本字段映射到统一 DTO
            if (sameGender) {
                if (result.getPersonalityInteraction() != null && result.getPersonalityAnalysis() == null) {
                    result.setPersonalityAnalysis(result.getPersonalityInteraction());
                }
                if (result.getCampusMoment() != null && result.getCampusStoryScene() == null) {
                    result.setCampusStoryScene(result.getCampusMoment());
                }
                if (result.getRelationshipPotential() != null && result.getDevelopmentPotential() == null) {
                    result.setDevelopmentPotential(result.getRelationshipPotential());
                }
            }
            result.setGeneratedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            result.setNextAvailableAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            fillMissingFields(result, matchResult, sameGender);
            int tokens = aiResult.getTokensUsed() != null ? aiResult.getTokensUsed() : 0;
            return new YuanFenAnalysisResult(result, tokens);
        } catch (Exception e) {
            log.error("YuanFen JSON parse FAILED, len={}, preview={}", json.length(), json.length() > 200 ? json.substring(0, 200) + "..." : json, e);
            return new YuanFenAnalysisResult(buildFallbackResult(matchResult, sameGender), 0);
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
                .replace("{schoolA}", safe(a.getSchool()))
                .replace("{gradeA}", safe(a.getGrade()))
                .replace("{mbtiA}", safe(a.getMbti()))
                .replace("{zodiacA}", safe(a.getZodiac()))
                .replace("{interestsA}", safe(a.getInterests()))
                .replace("{majorA}", safe(a.getMajor()))
                .replace("{nicknameB}", desensitize(b.getNickname()))
                .replace("{genderB}", genderLabel(b.getGender()))
                .replace("{ageB}", String.valueOf(calcAge(b.getBirthDate())))
                .replace("{schoolB}", safe(b.getSchool()))
                .replace("{gradeB}", safe(b.getGrade()))
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

    /** 填充 AI 可能遗漏的必填字段 */
    private void fillMissingFields(YuanFenAnalysisResponse r, MatchResultResponse match, boolean sameGender) {
        int score = match.getMatchScore() != null ? match.getMatchScore() : 50;
        if (r.getYuanFenIndex() == null || r.getYuanFenIndex().isBlank()) {
            r.setYuanFenIndex(sameGender
                    ? (score >= 70 ? "同频的灵魂" : score >= 50 ? "默契搭子" : "有缘相识")
                    : (score >= 70 ? "有缘千里" : score >= 50 ? "妙不可言" : "缘起缘落"));
        }
        if (r.getOverallInterpretation() == null || r.getOverallInterpretation().isBlank()) {
            r.setOverallInterpretation("你们各有特点，在相处中可以互相欣赏、互相成长。");
        }
        if (r.getPersonalityAnalysis() == null || r.getPersonalityAnalysis().isBlank()) {
            r.setPersonalityAnalysis("性格上各有互补，在相处中会互相吸引。");
        }
        if (r.getInterestChemistry() == null || r.getInterestChemistry().isBlank()) {
            r.setInterestChemistry("兴趣有交集的话，一起活动会很自然。");
        }
        if (r.getCampusStoryScene() == null || r.getCampusStoryScene().isBlank()) {
            r.setCampusStoryScene("图书馆、操场、咖啡店，校园里有很多相遇的可能。");
        }
        if (r.getRecommendActivities() == null || r.getRecommendActivities().isEmpty()) {
            r.setRecommendActivities(java.util.List.of("一起去校园咖啡馆聊天", "参加社团活动", "周末一起探索校园周边"));
        }
        if (r.getPotentialChallenge() == null || r.getPotentialChallenge().isBlank()) {
            r.setPotentialChallenge("多一些耐心倾听，少一些急于求成。");
        }
        if (r.getDevelopmentPotential() == null || r.getDevelopmentPotential().isBlank()) {
            r.setDevelopmentPotential("保持真诚的交流，用心去了解对方。");
        }
        if (r.getExclusiveQuote() == null || r.getExclusiveQuote().isBlank()) {
            r.setExclusiveQuote("缘分是两颗心恰好在同一个频率上跳动。");
        }
    }

    /** AI 调用失败时的降级结果 */
    private YuanFenAnalysisResponse buildFallbackResult(MatchResultResponse match, boolean sameGender) {
        YuanFenAnalysisResponse result = new YuanFenAnalysisResponse();
        int score = match.getMatchScore() != null ? match.getMatchScore() : 50;

        if (sameGender) {
            result.setYuanFenIndex(score >= 70 ? "同频的灵魂" : score >= 50 ? "默契搭子" : "有缘相识");
            result.setOverallInterpretation("你们各有特点，在相处中可以互相学习、互相支持，有成为挚友的潜质。");
            result.setPersonalityAnalysis("性格上各有互补，一个更主动一个更沉稳，在小组作业或社团活动中会配合得不错。");
            result.setInterestChemistry("兴趣有交集的话，一起自习、打球、约饭都会很自然。");
            result.setCampusStoryScene("图书馆里偶遇，或者在操场跑步时打个招呼，慢慢就从点头之交变成可以一起聊天的朋友。");
            result.setRecommendActivities(java.util.List.of("一起去校园咖啡馆聊天", "参加学校社团活动", "组队打比赛或一起自习"));
            result.setPotentialChallenge("好朋友之间也需要空间，学会尊重彼此的节奏，友谊会更长久。");
            result.setDevelopmentPotential("保持真诚的交流，多分享生活中的小事，你们会成为彼此最好的伙伴。");
            result.setExclusiveQuote("最好的友情是彼此成就，一起发光。");
        } else {
            result.setYuanFenIndex(score >= 70 ? "有缘千里" : score >= 50 ? "妙不可言" : "缘起缘落");
            result.setOverallInterpretation("你们各有特点，在相处中可以相互欣赏、互相成长，未来充满可能性。");
            result.setPersonalityAnalysis("性格上各有互补，一个更主动一个更沉稳，在相处中会互相吸引。");
            result.setInterestChemistry("兴趣有交集的话，一起看电影、约会吃饭都会很自然。");
            result.setCampusStoryScene("图书馆里偶遇，或者在操场散步时打个招呼，慢慢就从点头之交变成可以约出来的人。");
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
