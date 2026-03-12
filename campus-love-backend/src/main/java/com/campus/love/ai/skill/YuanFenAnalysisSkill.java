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
            ---------------------------------
            【用户A客观数据】
            - 基础画像：{nicknameA} | {genderA} | {ageA}岁 | {schoolA}{gradeA} | {majorA}专业
            - 心理与星象基建：MBTI={mbtiA} | 星座={zodiacA}
            - 偏好标签：[{interestsA}]

            【用户B客观数据】
            - 基础画像：{nicknameB} | {genderB} | {ageB}岁 | {schoolB}{gradeB} | {majorB}专业
            - 心理与星象基建：MBTI={mbtiB} | 星座={zodiacB}
            - 偏好标签：[{interestsB}]

            【系统匹配硬指标】
            - 综合缘分评分：{totalScore}/100
            - 细分维度得分：兴趣={interestScore} | MBTI={mbtiScore} | 星座={zodiacScore} | 八字={baziScore} | 专业跨度={majorScore}
            ---------------------------------

            【输出结构与深度指令】
            重要：在输出 JSON 各字段时，请使用上述两人的真实昵称「{nicknameA}」和「{nicknameB}」来指代双方，不要使用 A、B 或用户A、用户B 等抽象称呼。
            请基于上述所有硬数据，深度推演两人的关系网络，并严格按照以下 JSON 字段和篇幅要求输出：

            {
              "_dataReasoning": "（必填草稿，不对用户展示）请先在此处用200字进行硬核逻辑推演。列出支持他们相吸的具体数据点（如：{nicknameA}的什么专业特质完美嵌合了{nicknameB}的什么MBTI功能？85分的兴趣分具体体现在哪两个爱好的化学反应上？低分维度会引发什么具体现实矛盾？）",

              "yuanFenIndex": "（15字以内）基于数据推演出的核心精神羁绊词，需具有物理或建筑学美感。",

              "dataBackedOverview": "（约120字）将冷冰冰的【系统匹配硬指标】翻译成兼具理性和宿命感的综合判词。必须隐晦但不突兀地融入综合分、最高分维度和最低分维度背后的现实意义。例如解释为什么某一项得分不高，但在总体上却构成了绝妙的制衡。",

              "cognitiveArchitecture": "（约400字）MBTI底层逻辑与专业属性的深度融合分析。
                  要求：
                  1. 必须使用具体的认知功能术语（如Ni, Ne, Ti, Fe等）剖析两人信息处理和决策模型的差异。
                  2. 必须将两人的【专业属性】（如人工智能的底层逻辑 vs 建筑学的空间感知）作为认知功能的放大器进行分析。
                  3. 举证分析：在面临认知重构或重大决策的场景中，对方的劣势功能如何被己方的优势功能完美接管（实现数据上的互补）。叙述时请用昵称「{nicknameA}」「{nicknameB}」指代。",

              "interestResonance": "（约150字）基于具体的【偏好标签】和【兴趣得分】，进行跨界化学反应分析。不要仅停留在"都有摄影爱好"的重合点，更要深挖"咖啡+瑜伽"或"独立音乐+电影"这种不同爱好之间如何相互渗透，形成更高阶的生活方式闭环。",

              "cinematicScene": "（约250字）高颗粒度、强张力的校园宿命感微电影预演。
                  要素要求：
                  1. 场景必须是一个能同时触发两人【专业属性】或【爱好】的具体地点（如实验室、模型室、暗房等）。
                  2. 必须包含一个具有心理推拉感（Push & Pull）的微小冲突或动作交锋。
                  3. 必须包含一句符合双方 MBTI 逻辑的高智感对白。
                  4. 描写要侧重光影、声音和微小的肢体语言（如指尖、眼神、咖啡杯的水渍）。",

              "frictionAndEvolution": "（约150字）基于得分最低的维度（或MBTI的天然盲区）推演出的致命摩擦点。
                  要求：精准预判在长期相处中，哪种特定场景会触发危机（例如{nicknameA}的系统化冷漠伤害了{nicknameB}的情感阈值），并给出基于双方认知逻辑的"降维化解方案"。叙述时请用昵称指代。",

              "exclusiveQuote": "（20字以内）结合双方专业意象与性格张力，极具诗意和宿命感的专属金句。"
            }

            请基于以上设定和 JSON 格式，开始深度解析：
            """;

    /** 用户 Prompt 模板（同性） */
    private static final String PROMPT_TEMPLATE_SAME = """
            ---------------------------------
            【用户A客观数据】
            - 基础画像：{nicknameA} | {genderA} | {ageA}岁 | {schoolA}{gradeA} | {majorA}专业
            - 心理与星象基建：MBTI={mbtiA} | 星座={zodiacA}
            - 偏好标签：[{interestsA}]

            【用户B客观数据】
            - 基础画像：{nicknameB} | {genderB} | {ageB}岁 | {schoolB}{gradeB} | {majorB}专业
            - 心理与星象基建：MBTI={mbtiB} | 星座={zodiacB}
            - 偏好标签：[{interestsB}]

            【系统匹配硬指标】
            - 综合羁绊评分：{totalScore}/100
            - 细分维度得分：兴趣={interestScore} | MBTI={mbtiScore} | 星座={zodiacScore} | 八字={baziScore} | 专业跨度={majorScore}
            ---------------------------------

            【输出结构与深度指令】
            重要：在输出 JSON 各字段时，请使用上述两人的真实昵称「{nicknameA}」和「{nicknameB}」来指代双方，不要使用 A、B 或用户A、用户B 等抽象称呼。
            请基于上述数据，深度推演两人的关系网络，并严格按照以下 JSON 字段要求输出：

            {
              "_dataReasoning": "（必填草稿，不对用户展示）在此处用200字进行硬核逻辑推演。列出支持他们产生深层羁绊的具体数据点（例如：{nicknameA}的专业逻辑如何补足{nicknameB}的思维盲区？同性之间的高MBTI得分意味着怎样的精神镜像？低分维度会带来怎样的良性博弈或现实阵痛？）",

              "yuanFenIndex": "（15字以内）基于数据推演出的核心精神羁绊词，如'双子星式的智性交锋'或'同频共振的深海锚点'。",

              "dataBackedOverview": "（约120字）将冷冰冰的【系统匹配硬指标】翻译成兼具理性和宿命感的综合判词。需指明这段高分同性羁绊的底色——是能在顶峰相见的战友，还是能抚平彼此精神褶皱的灵魂伴侣。隐晦地说明最高分与最低分维度的现实意义。",

              "cognitiveArchitecture": "（约400字）MBTI底层逻辑与专业属性的深度融合分析。
                  要求：
                  1. 使用具体的认知功能术语（如Ti, Te, Ni, Ne等）剖析两人的思维链路。
                  2. 结合两人的【专业属性】，分析他们在面对现实挑战或精神探索时，如何形成"镜像互补"或"协同放电"。例如：一个在构建底层算法，一个在统筹全局框架。
                  3. 说明对方如何成为了自己内心理想化或被压抑部分的投射，实现 1+1>2 的灵魂补全。",

              "interestResonance": "（约150字）基于具体的【偏好标签】和【兴趣得分】，深挖同性之间特有的互动模式。分析那些看似不相关的兴趣（如一方的遥感影像与另一方的徒步探险）如何通过内在的逻辑或审美统一起来，形成精神上的闭环。",

              "cinematicScene": "（约250字）高颗粒度、强张力的校园/职场微电影预演。
                  要素要求：
                  1. 场景需触发两人的【专业属性】（如实验室的白板前、深夜的会议室或天台）。
                  2. 展现同性之间特有的推拉感：可以是理念的激烈碰撞后相视一笑，也可以是极度疲惫时无需多言的默契递递咖啡。
                  3. 包含一句符合双方智力水平与 MBTI 逻辑的高智感对白。
                  4. 描写侧重微小的肢体语言、光影切割和沉默中的情绪流转。",

              "frictionAndEvolution": "（约150字）基于得分最低的维度（或MBTI的同极相斥/盲区）推演出的致命摩擦点。精准预判在长期并肩前行中可能爆发的危机（如掌控欲与自由意志的冲突），并给出高维度的破局建议。",

              "exclusiveQuote": "（20字以内）结合双方专业意象与性格张力，极具宿命感与力量感的专属金句（超越性别的狭隘，直击灵魂）。"
            }

            请严格按照上述 JSON 格式，为当前两位用户输出深度解析报告：
            """;

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
            // 新模板字段映射到前端展示字段
            if (result.getDataBackedOverview() != null && result.getOverallInterpretation() == null) {
                result.setOverallInterpretation(result.getDataBackedOverview());
            }
            if (result.getCognitiveArchitecture() != null && result.getPersonalityAnalysis() == null) {
                result.setPersonalityAnalysis(result.getCognitiveArchitecture());
            }
            if (result.getInterestResonance() != null && result.getInterestChemistry() == null) {
                result.setInterestChemistry(result.getInterestResonance());
            }
            if (result.getCinematicScene() != null && result.getCampusStoryScene() == null) {
                result.setCampusStoryScene(result.getCinematicScene());
            }
            if (result.getFrictionAndEvolution() != null && result.getPotentialChallenge() == null) {
                result.setPotentialChallenge(result.getFrictionAndEvolution());
            }
            // 同性版本旧字段映射（兼容）
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
                .replace("{nicknameA}", a.getNickname() != null && !a.getNickname().isBlank() ? a.getNickname().trim() : "匿名")
                .replace("{genderA}", genderLabel(a.getGender()))
                .replace("{ageA}", String.valueOf(calcAge(a.getBirthDate())))
                .replace("{schoolA}", safe(a.getSchool()))
                .replace("{gradeA}", safe(a.getGrade()))
                .replace("{mbtiA}", safe(a.getMbti()))
                .replace("{zodiacA}", safe(a.getZodiac()))
                .replace("{interestsA}", safe(a.getInterests()))
                .replace("{majorA}", safe(a.getMajor()))
                .replace("{nicknameB}", b.getNickname() != null && !b.getNickname().isBlank() ? b.getNickname().trim() : "匿名")
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
