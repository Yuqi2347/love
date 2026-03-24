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
            当前任务默认场景是：第一次线下陌生见面。

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
            1. dateSuggestion 必须具体到校园或城市里的真实场景，并默认优先室内、公开、明亮、可随时结束的第一次见面场景。
            1.1 不能只给泛泛建议，必须明确说明这个场景为什么适合对方的性格、情绪表达、生活节奏或兴趣。
            1.2 dateSuggestion 必须采用“两段式结构”：先给一个室内主场景，再给一个可选延伸，不允许把整个行程写得过满。
            1.3 严禁推荐游泳、温泉、酒吧深夜局、私宅、私密包厢、需要换装或身体接触明显的活动，也不要推荐偏僻夜路、远郊、过强体力活动。
            2. iceBreakTopics 必须正好 3 个，且每个都要有 opener。
            2.1 title 不能只是“游泳”“电影”“咖啡”这类单个兴趣词，必须是一个真正可展开的聊天切入口，建议 8-18 个字。
            2.2 opener 不能是通用寒暄，要能接住对方的表达方式或兴趣点，最好能直接作为第一句话或第二句话说出口。
            3. surpriseIdea 必须结合当前用户性别、对方性格和兴趣来写，但只能是低成本、低压力、非亲密接触型的小细节，不能送贵重礼物，也不能制造身体接触机会。
            4. outfitAdvice 只谈场景、氛围、舒适度，不谈颜值；同时要考虑对方会对什么样的见面状态更放松。禁止出现泳装、贴身运动装、过度性感或强烈约会感穿搭。
            5. mindsetAdvice 不要空泛安慰，要结合对方特性提醒当前用户“这次见面该怎么把握节奏”，重点是先建立安全感、松弛感和可继续见面的感觉，而不是快速升温。
            6. 同性和异性都按恋爱约会方向来写。
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
                生活节奏：%s
                校园重心：%s
                陪伴偏好：%s
                亲密节奏：%s
                未来生活倾向：%s
                兴趣前三：%s

                【对方特性总结】
                %s

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
                MomentPromptHelper.lifeRhythm(targetProfile != null ? targetProfile.getLifeRhythm() : null),
                MomentPromptHelper.campusFocus(targetProfile != null ? targetProfile.getCampusFocus() : null),
                MomentPromptHelper.companionshipStyle(targetProfile != null ? targetProfile.getCompanionshipStyle() : null),
                MomentPromptHelper.intimacyPace(targetProfile != null ? targetProfile.getIntimacyPace() : null),
                MomentPromptHelper.futureLifestyle(targetProfile != null ? targetProfile.getFutureLifestyle() : null),
                MomentPromptHelper.joinOrDefault(targetInterests, "暂无明显兴趣标签"),
                buildTargetSummary(targetProfile, targetInterests),
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
        if (!hasValidIceBreakTopics(response.getIceBreakTopics())) {
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
        String targetSummary = buildTargetSummary(targetProfile, targetInterests);
        String dateSuggestion = switch (scene) {
            case "轻互动熟悉型" -> "第一次见面可以先约在学校附近安静咖啡馆或甜品店坐一会儿，先把聊天节奏放松下来。等彼此熟一点后，再去附近书店、文创店或校园展区慢慢逛一圈，这样既有互动感，也不会让%s觉得被推进得太快。".formatted(targetSummary);
            case "轻松聊天型" -> "第一次见面更适合从室内、公开、明亮的场景开始，比如学校附近安静咖啡馆或熟悉的小店。先坐下来聊聊日常和兴趣，如果气氛自然，再一起去附近书店或商场公共区域走走，这样更适合%s慢慢进入状态。".formatted(targetSummary);
            case "低压力陪伴型" -> "第一次见面建议把安排做得轻一点，先在学校周边安静饮品店坐 40 到 60 分钟，看看彼此聊天是否舒服。若相处自然，再顺路去校园里人多明亮的公共空间散一小段步，这种节奏更容易让%s感到安心。".formatted(targetSummary);
            default -> "第一次约会更适合从低门槛、公开又有安全感的校园日常开始，比如校内咖啡角、甜品店或图书馆旁的休息区。先轻松聊一会儿，如果状态不错，再一起去附近书店或校园展区走走，这样更适合%s自然放松下来。".formatted(targetSummary);
        };
        String surpriseIdea = surpriseIdea(requester, targetProfile, targetInterests);
        String outfitAdvice = "第一次见面建议穿得干净、自然、舒服一点，重点是适合室内久坐和轻松走动。对%s来说，太用力的正式感或过强的约会感都会增加压力，简洁清爽、方便聊天的状态反而更容易让TA放松。".formatted(targetSummary);
        String mindsetAdvice = "和%s见面时，不必急着制造明显火花。%s，所以比起快速推进，更重要的是先把聊天节奏放慢一点，让TA感受到安全、自然、没有负担，这样才更容易留下还想再见一次的感觉。".formatted(targetNickname, targetSummary);
        return MomentDatePrepResponse.builder()
                .dateSceneType(scene)
                .dateSuggestion(dateSuggestion)
                .iceBreakTopics(List.of(
                        buildIceBreakTopic(topicA, 0),
                        buildIceBreakTopic(topicB, 1),
                        buildIceBreakTopic(topicC, 2)
                ))
                .surpriseIdea(surpriseIdea)
                .outfitAdvice(outfitAdvice)
                .mindsetAdvice(mindsetAdvice)
                .build();
    }

    public boolean shouldRefresh(MomentDatePrepResponse response) {
        return response == null || !hasValidIceBreakTopics(response.getIceBreakTopics());
    }

    private boolean hasValidIceBreakTopics(List<MomentDatePrepResponse.IceBreakTopic> topics) {
        if (topics == null || topics.size() != 3) {
            return false;
        }
        return topics.stream().allMatch(this::isValidIceBreakTopic);
    }

    private boolean isValidIceBreakTopic(MomentDatePrepResponse.IceBreakTopic topic) {
        if (topic == null) {
            return false;
        }
        String title = topic.getTitle() == null ? "" : topic.getTitle().trim();
        String opener = topic.getOpener() == null ? "" : topic.getOpener().trim();
        return title.length() >= 6 && opener.length() >= 16;
    }

    private MomentDatePrepResponse.IceBreakTopic buildIceBreakTopic(String topic, int index) {
        if ("最近的日常".equals(topic)) {
            return MomentDatePrepResponse.IceBreakTopic.builder()
                    .title("最近哪件小事最能代表你的状态")
                    .opener("可以直接问：“如果用一件小事形容你最近的状态，你会想到什么？” 这种开法不冒犯，也更容易聊出真实感受。")
                    .build();
        }
        if (topic.contains("地方")) {
            return MomentDatePrepResponse.IceBreakTopic.builder()
                    .title("校园里哪个地方会让你想多待一会儿")
                    .opener("可以顺着聊：“你在学校里有没有一个待着就会放松的地方？” 很容易从地点聊到TA的习惯和节奏。")
                    .build();
        }
        if (topic.contains("一件事")) {
            return MomentDatePrepResponse.IceBreakTopic.builder()
                    .title("你最近一直在想的那件事是什么")
                    .opener("等气氛自然一点后再问：“你最近有没有一件一直在想、但还没怎么和别人聊过的事？” 更容易进入真实表达。")
                    .build();
        }
        return switch (index) {
            case 0 -> MomentDatePrepResponse.IceBreakTopic.builder()
                    .title("你最近怎么开始喜欢" + topic + "的")
                    .opener("可以直接问：“你最近是怎么开始喜欢" + topic + "的？” 这种开法不生硬，也容易让TA讲出真正投入的原因。")
                    .build();
            case 1 -> MomentDatePrepResponse.IceBreakTopic.builder()
                    .title(topic + "在你日常里通常扮演什么角色")
                    .opener("可以顺着聊：“你一般会在什么情况下想到" + topic + "？” 这样能自然带出TA的生活节奏和日常习惯。")
                    .build();
            default -> MomentDatePrepResponse.IceBreakTopic.builder()
                    .title("如果下次由你来安排一次" + topic + "体验")
                    .opener("等气氛放松一点后再问：“如果下次你带我体验一次" + topic + "，你会怎么安排？” 很容易把话题延伸到相处方式。")
                    .build();
        };
    }

    private String buildTargetSummary(MomentProfile targetProfile, List<String> targetInterests) {
        String personality = switch (nz(targetProfile != null ? targetProfile.getPersonalityBase() : null)) {
            case "A" -> "更容易被轻松外放的气氛带动";
            case "B" -> "通常需要一点时间建立熟悉感";
            case "C" -> "会根据当下氛围决定打开程度";
            default -> "会更在意相处时是否自然";
        };
        String emotion = switch (nz(targetProfile != null ? targetProfile.getEmotionStyle() : null)) {
            case "A" -> "表达感受通常比较直接";
            case "B" -> "不一定会立刻说很多，但会默默感受细节";
            case "C" -> "更看情境和对象决定表达深浅";
            default -> "对表达方式比较看重";
        };
        String rhythm = switch (nz(targetProfile != null ? targetProfile.getLifeRhythm() : null)) {
            case "A" -> "偏好有安排和可预期的节奏";
            case "B" -> "更适合松弛、不被催促的推进方式";
            case "C" -> "既能接受计划，也保留临场弹性";
            case "D" -> "对轻松无压力的氛围更有好感";
            default -> "需要一个舒服的见面节奏";
        };
        String interest = targetInterests.isEmpty() ? "并且更容易在真实兴趣里打开状态" : "尤其在聊到" + targetInterests.get(0) + "这类话题时更容易进入状态";
        return personality + "、" + emotion + "、" + rhythm + "，" + interest;
    }

    private String surpriseIdea(User requester, MomentProfile targetProfile, List<String> targetInterests) {
        boolean requesterIsMale = requester != null && requester.getGender() != null && requester.getGender() == 1;
        String targetInterest = targetInterests.isEmpty() ? "TA喜欢的事情" : targetInterests.get(0);
        if (requesterIsMale) {
            if (targetInterests.stream().anyMatch(it -> it.contains("咖啡") || it.contains("茶"))) {
                return "可以提前留意TA平时偏好的饮品口味，见面时自然地把选择照顾到位。第一次见面最合适的小惊喜，不是刻意安排，而是让TA在很轻的细节里感受到“你有认真记住我说过的话”。";
            }
            if (targetInterests.stream().anyMatch(it -> it.contains("文学") || it.contains("写作"))) {
                return "可以提前准备一个和书、电影或最近阅读有关的小问题，见面后自然抛给TA。比起刻意设计惊喜，这种能接住TA兴趣的开场，会让第一次聊天更自然，也更容易显出你的用心。";
            }
            if ("B".equals(nz(targetProfile != null ? targetProfile.getEmotionStyle() : null))) {
                return "不一定要准备明显惊喜，反而可以在见面结束后发一条简短消息，说出你今天注意到的一个细节。对情绪更内收的人来说，被认真留意到，往往比热烈表达更容易产生好感。";
            }
            return "可以提前把见面的节奏想清楚，比如主场景选哪里、如果聊得顺再去哪一站、什么时候自然收尾。第一次见面里，这种稳定又不过度的准备感，本身就是很成熟的体贴。";
        }
        if (targetInterests.stream().anyMatch(it -> it.contains("音乐") || it.contains("现场"))) {
            return "可以在见面前轻轻分享一首你最近喜欢的歌，或者提一句这家店的氛围歌单你觉得TA可能会喜欢。这样的小铺垫不会太用力，但能让第一次见面的气氛更自然地接上。";
        }
        if ("A".equals(nz(targetProfile != null ? targetProfile.getLifeRhythm() : null))) {
            return "见面时可以顺手问一句TA对附近有没有更喜欢的店或更顺路的下一站。对偏计划型的人来说，这种被信任和被请教的感觉，会让TA更自然地投入这次见面。";
        }
        if ("A".equals(nz(targetProfile != null ? targetProfile.getPersonalityBase() : null))) {
            return "可以提前准备一个很轻的小互动，比如一个有趣的问题、一张最近看到的照片，或者一个和共同兴趣有关的话题。外向的人通常很擅长接住这种轻松氛围，但重点还是自然，不要太像设计好的节目。";
        }
        return "比起刻意制造惊喜，更适合准备一点让气氛变柔和的小细节，比如提前到几分钟、带着轻松的话题，或者记住TA提过的小偏好。很多第一次见面的好感，都是这样慢慢被托住的。";
    }

    private String safe(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private String nz(String value) {
        return value == null ? "" : value.trim();
    }
}
