package com.campus.love.ai.agent;

import com.campus.love.ai.dto.AiChatResult;
import com.campus.love.ai.service.AiService;
import com.campus.love.moment.dto.MomentDatePrepResponse;
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
public class MomentDatePrepAgent {

    private static final String SYSTEM_PROMPT = """
            你是 Campus Love 的「约会准备策划师」。
            请根据双方资料输出一个合法 JSON，不要输出 Markdown。

            JSON 结构固定：
            {
              "dateSuggestion": "60-100字，给出具体场景建议",
              "iceBreakTopics": [
                {"title":"话题一标题","opener":"怎么开口"},
                {"title":"话题二标题","opener":"怎么开口"},
                {"title":"话题三标题","opener":"怎么开口"}
              ],
              "surpriseIdea": "80-120字，必须按当前用户性别生成不同逻辑",
              "outfitAdvice": "50-80字，不得提颜值数据",
              "mindsetAdvice": "50-80字，帮助用户降低焦虑"
            }

            约束：
            1. dateSuggestion 必须具体到校园或城市里的真实场景。
            2. iceBreakTopics 必须正好 3 个，且每个都要有 opener。
            3. surpriseIdea 必须结合当前用户性别、对方性格和兴趣来写。
            4. outfitAdvice 只谈场景、氛围、舒适度，不谈颜值。
            5. 同性和异性都按恋爱约会方向来写。
            """;

    private final AiService aiService;
    private final ObjectMapper objectMapper;

    public MomentDatePrepResponse generate(
            User requester,
            User target,
            MomentProfile requesterProfile,
            MomentProfile targetProfile,
            UserPortrait requesterPortrait,
            UserPortrait targetPortrait,
            String dateSceneType
    ) {
        List<String> commonInterests = MomentPromptHelper.commonInterestNames(requesterPortrait, requester, targetPortrait, target, 3);
        List<String> targetInterests = MomentPromptHelper.interestNames(targetPortrait, target, 3);
        String prompt = """
                【当前用户】
                昵称：%s
                性别：%s
                亲密节奏：%s
                约会偏好：%s

                【对方】
                昵称：%s
                性别：%s
                性格底色：%s
                情绪表达：%s
                校园重心：%s
                兴趣前三：%s

                【共同兴趣】
                %s

                【约会场景类型】
                %s

                请输出约会准备 JSON。
                """.formatted(
                safe(requester != null ? requester.getNickname() : null, "我"),
                MomentPromptHelper.genderLabel(requester != null ? requester.getGender() : null),
                MomentPromptHelper.intimacyPace(requesterProfile != null ? requesterProfile.getIntimacyPace() : null),
                MomentPromptHelper.dateStyle(requesterProfile != null ? requesterProfile.getDateStyle() : null),
                safe(target != null ? target.getNickname() : null, "TA"),
                MomentPromptHelper.genderLabel(target != null ? target.getGender() : null),
                MomentPromptHelper.personalityBase(targetProfile != null ? targetProfile.getPersonalityBase() : null),
                MomentPromptHelper.emotionStyle(targetProfile != null ? targetProfile.getEmotionStyle() : null),
                MomentPromptHelper.campusFocus(targetProfile != null ? targetProfile.getCampusFocus() : null),
                MomentPromptHelper.joinOrDefault(targetInterests, "暂无明显兴趣标签"),
                MomentPromptHelper.joinOrDefault(commonInterests, "暂无明显共同兴趣"),
                MomentPromptHelper.dateSceneTypeLabel(dateSceneType)
        );
        try {
            AiChatResult result = aiService.chatCompletion(SYSTEM_PROMPT, prompt);
            String json = MomentAiJsonSupport.clean(result != null ? result.getContent() : "");
            MomentDatePrepResponse response = objectMapper.readValue(json, MomentDatePrepResponse.class);
            fillFallbackIfBlank(response, requester, target, requesterProfile, targetProfile, commonInterests, targetInterests, dateSceneType);
            response.setDateSceneType(MomentPromptHelper.dateSceneTypeLabel(dateSceneType));
            return response;
        } catch (Exception e) {
            log.warn("MomentDatePrepAgent failed: {}", e.getMessage());
            return fallback(requester, target, requesterProfile, targetProfile, commonInterests, targetInterests, dateSceneType);
        }
    }

    private void fillFallbackIfBlank(
            MomentDatePrepResponse response,
            User requester,
            User target,
            MomentProfile requesterProfile,
            MomentProfile targetProfile,
            List<String> commonInterests,
            List<String> targetInterests,
            String dateSceneType
    ) {
        MomentDatePrepResponse fallback = fallback(requester, target, requesterProfile, targetProfile, commonInterests, targetInterests, dateSceneType);
        if (response.getDateSuggestion() == null || response.getDateSuggestion().isBlank()) {
            response.setDateSuggestion(fallback.getDateSuggestion());
        }
        if (response.getIceBreakTopics() == null || response.getIceBreakTopics().size() != 3) {
            response.setIceBreakTopics(fallback.getIceBreakTopics());
        }
        if (response.getSurpriseIdea() == null || response.getSurpriseIdea().isBlank()) {
            response.setSurpriseIdea(fallback.getSurpriseIdea());
        }
        if (response.getOutfitAdvice() == null || response.getOutfitAdvice().isBlank()) {
            response.setOutfitAdvice(fallback.getOutfitAdvice());
        }
        if (response.getMindsetAdvice() == null || response.getMindsetAdvice().isBlank()) {
            response.setMindsetAdvice(fallback.getMindsetAdvice());
        }
    }

    private MomentDatePrepResponse fallback(
            User requester,
            User target,
            MomentProfile requesterProfile,
            MomentProfile targetProfile,
            List<String> commonInterests,
            List<String> targetInterests,
            String dateSceneType
    ) {
        String scene = MomentPromptHelper.dateSceneTypeLabel(dateSceneType);
        String targetNickname = safe(target != null ? target.getNickname() : null, "TA");
        List<String> topics = commonInterests.isEmpty() ? targetInterests : commonInterests;
        String topicA = topics.size() > 0 ? topics.get(0) : "最近的日常";
        String topicB = topics.size() > 1 ? topics.get(1) : "校园里一个喜欢的地方";
        String topicC = topics.size() > 2 ? topics.get(2) : "最近在想的一件事";
        String dateSuggestion = switch (scene) {
            case "户外探索型" -> "第一次见面更适合轻松一点的户外路线，比如校园附近能边走边聊的公园、展览或市集。留一点移动中的时间，比坐定之后硬找话题更自然。";
            case "室内舒适型" -> "第一次见面建议选一个安静、可停留的室内空间，比如咖啡馆、书店或熟悉的小店。这样既有安全感，也方便把话题慢慢往深处带。";
            default -> "第一次约会更适合从低门槛的校园日常开始，比如一起去食堂吃顿饭、逛校园、找个安静角落坐一会儿。轻一点的开始，更容易把紧张感放下来。";
        };
        String surpriseIdea = surpriseIdea(requester, targetProfile, targetInterests);
        String outfitAdvice = "穿搭重点不是惊艳，而是和%s的氛围一致。保持干净、舒服、方便行动就够了，留一点你自己的风格，比刻意正式更容易让人放松。".formatted(scene);
        String mindsetAdvice = "和%s见面时，不必急着判断是不是“对的人”。先把这次见面当成一次真实地认识彼此，把注意力放在当下的交流和感受上，反而更容易看见缘分怎么往前走。".formatted(targetNickname);
        return MomentDatePrepResponse.builder()
                .dateSceneType(scene)
                .dateSuggestion(dateSuggestion)
                .iceBreakTopics(List.of(
                        MomentDatePrepResponse.IceBreakTopic.builder().title(topicA).opener("可以先聊聊你最近为什么会被这件事吸引，看看TA会自然接到哪里。").build(),
                        MomentDatePrepResponse.IceBreakTopic.builder().title(topicB).opener("可以问一句“如果下次你来选地方，你会带我去哪里”，很容易带出TA的生活习惯和偏好。").build(),
                        MomentDatePrepResponse.IceBreakTopic.builder().title(topicC).opener("不如直接说一个你最近真的在想的问题，比标准答案式聊天更容易让人放下防备。").build()
                ))
                .surpriseIdea(surpriseIdea)
                .outfitAdvice(outfitAdvice)
                .mindsetAdvice(mindsetAdvice)
                .build();
    }

    private String surpriseIdea(User requester, MomentProfile targetProfile, List<String> targetInterests) {
        boolean requesterIsMale = requester != null && requester.getGender() != null && requester.getGender() == 1;
        String targetInterest = targetInterests.isEmpty() ? "TA喜欢的事情" : targetInterests.get(0);
        if (requesterIsMale) {
            if (targetInterests.stream().anyMatch(it -> it.contains("咖啡") || it.contains("茶"))) {
                return "如果是你来准备，可以提前问一句TA平时更喜欢喝什么，见面时顺手把这件小事照顾好。很多时候，真正让人心动的不是排场，而是“你有被提前想到”的感觉。";
            }
            if (targetInterests.stream().anyMatch(it -> it.contains("文学") || it.contains("写作"))) {
                return "可以带着一个你最近真实在想、但还没想明白的问题去见TA。别刻意设计成高深话题，真诚的困惑本身，就会比准备好的标准答案更能打动人。";
            }
            if ("B".equals(targetProfile != null ? targetProfile.getEmotionStyle() : null)) {
                return "不一定要准备明显的惊喜，反而可以在约会结束后发一条消息，说出你今天注意到的一个细节。对情绪更内收的人来说，被认真观察到往往比热烈表达更有分量。";
            }
            return "可以提前把见面的节奏想清楚一点，比如去哪、什么时候散场、有没有顺手的下一站。对很多人来说，第一次见面里这种稳定而自然的准备感，本身就是很加分的体贴。";
        }
        if (targetInterests.stream().anyMatch(it -> it.contains("音乐") || it.contains("现场"))) {
            return "可以在见面前发给TA一首你最近喜欢的歌，只要轻轻说一句“路上可以听”。这种提前一点点建立氛围的方式，会让见面前的心情自然升温。";
        }
        if ("A".equals(targetProfile != null ? targetProfile.getLifeRhythm() : null)) {
            return "见面时可以顺手问一句TA对某个地方熟不熟、会不会有更好的安排。对计划型的人来说，被信任和被请教会让TA很自然地进入照顾和投入的状态。";
        }
        if ("A".equals(targetProfile != null ? targetProfile.getPersonalityBase() : null)) {
            return "可以提前准备一个有点轻巧的小互动，比如一个有趣的问题、一张照片、一个小游戏。外向的人很擅长接住这种被抛出来的轻松氛围。";
        }
        return "比起刻意制造惊喜，更适合准备一点让气氛变柔和的小细节，比如提前到几分钟、带着轻松的话题、或者在见面前给TA一个自然的提示。很多好感，都是这样慢慢被托住的。";
    }

    private String safe(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
