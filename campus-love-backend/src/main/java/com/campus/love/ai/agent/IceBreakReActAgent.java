package com.campus.love.ai.agent;

import com.campus.love.chat.dto.IceBreakTopicsResponse;
import com.campus.love.chat.entity.Message;
import com.campus.love.chat.mapper.MessageMapper;
import com.campus.love.ai.rag.RagContextBuilder;
import com.campus.love.ai.service.AiService;
import com.campus.love.ai.service.YuanFenService;
import com.campus.love.feed.entity.FeedPost;
import com.campus.love.feed.mapper.FeedPostMapper;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 破冰话题 Agent（借鉴缘分分析格式）
 * 先分析聊天记录（关系状态、话题偏好、冷场原因），再给出具体可发内容。
 *
 * 架构说明：非严格 ReAct 循环。ReAct 典型流程为 Thought→Action→Observation→重复。
 * 本实现为「单轮工具增强」：一次性调用 checkRecentFeed、getCommonInterests、聊天历史等
 * 构建上下文，再单次 AI 调用输出。与文档中 ReAct 的「多轮」描述不同，此处简化为一轮。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IceBreakReActAgent {

    private final AiService aiService;
    private final YuanFenService yuanFenService;
    private final RagContextBuilder ragContextBuilder;
    private final FeedPostMapper feedPostMapper;
    private final UserMapper userMapper;
    private final MessageMapper messageMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT_OPPOSITE = """
            你是 Campus Love 的「破冰灵感分析师」。借鉴缘分分析的深度与格式，先分析再给建议。

            你的准则：
            1. 有理有据：基于【聊天记录】判断当前关系状态（初识/熟络/冷场）、话题偏好、冷场原因。
            2. 深度剖析：不要泛泛而谈，要指出具体现象（如：对方最近一条是问句但未回复、双方都爱发表情包等）。
            3. 具体建议：基于分析给出3条可直接发送的破冰内容，每条20字以内，温暖有趣、略带暧昧。

            ⚠️ 严格输出 JSON，禁止 Markdown 包裹：
            {"analysis":"（约150字）基于聊天记录的分析，含关系状态、话题偏好、冷场原因","topics":["话题1","话题2","话题3"]}
            """;

    private static final String SYSTEM_PROMPT_SAME = """
            你是 Campus Love 的「破冰灵感分析师」。借鉴缘分分析的深度与格式，先分析再给建议。

            你的准则：
            1. 有理有据：基于【聊天记录】判断当前关系状态、话题偏好、冷场原因。
            2. 深度剖析：指出具体现象，同性话题偏向朋友/知己向。
            3. 具体建议：基于分析给出3条可直接发送的破冰内容，每条20字以内，轻松自然。

            ⚠️ 严格输出 JSON，禁止 Markdown 包裹：
            {"analysis":"（约150字）基于聊天记录的分析","topics":["话题1","话题2","话题3"]}
            """;

    /**
     * 生成破冰灵感：先分析聊天记录，再给话题建议
     */
    public IceBreakTopicsResponse generateIceBreakTopics(Long selfId, Long targetId) {
        try {
            User u1 = userMapper.selectById(selfId);
            User u2 = userMapper.selectById(targetId);
            boolean sameGender = (u1 != null && u2 != null && Objects.equals(u1.getGender(), u2.getGender()));

            String chatHistoryText = buildChatHistoryText(selfId, targetId);
            FeedCheckResult feedResult = checkRecentFeed(targetId);
            List<String> commonInterests = getCommonInterests(selfId, targetId);

            StringBuilder context = new StringBuilder();
            context.append("【聊天记录（最近30条）】\n").append(chatHistoryText);
            context.append("\n【对方近期动态】").append(feedResult.hasFeed() ? String.join("、", feedResult.tags()) : "无");
            context.append("\n【共同兴趣】").append(String.join("、", commonInterests));
            if (feedResult.latestSnippet() != null) context.append("\n【最新动态摘要】").append(feedResult.latestSnippet());

            yuanFenService.getCachedAnalysis(selfId, targetId).ifPresent(yf -> {
                context.append("\n【缘分分析（若有）】");
                if (yf.getYuanFenIndex() != null) context.append("\n缘分指数：").append(yf.getYuanFenIndex());
                if (yf.getInterestChemistry() != null) context.append("\n兴趣契合：").append(yf.getInterestChemistry());
                if (yf.getRecommendActivities() != null && !yf.getRecommendActivities().isEmpty())
                    context.append("\n推荐活动：").append(String.join("、", yf.getRecommendActivities()));
            });

            String systemPrompt = sameGender ? SYSTEM_PROMPT_SAME : SYSTEM_PROMPT_OPPOSITE;
            var result = aiService.chatCompletion(systemPrompt, "【上下文】\n" + context + "\n\n请先分析聊天记录，再输出 analysis + topics 的 JSON。");
            if (result != null && result.getContent() != null) {
                return parseResponse(result.getContent(), sameGender);
            }
        } catch (Exception e) {
            log.warn("IceBreakReActAgent failed: {}", e.getMessage());
        }
        return IceBreakTopicsResponse.builder()
                .analysis("暂无聊天记录或分析失败，不妨从共同兴趣或最近动态切入～")
                .topics(List.of("最近在忙什么呀？", "有什么兴趣爱好吗？", "周末一般怎么过？"))
                .build();
    }

    /** 构建聊天记录文本（供 AI 分析） */
    private String buildChatHistoryText(Long selfId, Long targetId) {
        List<Message> messages = messageMapper.selectPageForChatHistory(selfId, targetId, 0, 30);
        if (messages == null || messages.isEmpty()) return "（暂无聊天记录）";
        Map<Long, String> nicknames = new HashMap<>();
        for (User u : userMapper.selectBatchIds(List.of(selfId, targetId))) {
            if (u != null) nicknames.put(u.getId(), u.getNickname() != null ? u.getNickname() : "用户");
        }
        StringBuilder sb = new StringBuilder();
        for (Message m : messages) {
            boolean isDeleted = m.getDeleted() != null && m.getDeleted() == 1;
            String who = nicknames.getOrDefault(m.getSenderId(), "?");
            String content = isDeleted ? "[已撤回]" : (m.getContent() != null ? m.getContent() : "");
            if (m.getMsgType() != null && m.getMsgType() == 3) content = "[图片]";
            if (m.getMsgType() != null && m.getMsgType() == 4) content = "[邀约]";
            sb.append(who).append(": ").append(content).append("\n");
        }
        return sb.toString();
    }

    /** 工具：检查对方近期动态 */
    public FeedCheckResult checkRecentFeed(Long targetUserId) {
        LocalDateTime since = LocalDateTime.now().minusDays(14);
        List<FeedPost> posts = feedPostMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FeedPost>()
                        .eq(FeedPost::getUserId, targetUserId)
                        .ge(FeedPost::getCreatedAt, since)
                        .orderByDesc(FeedPost::getCreatedAt)
                        .last("LIMIT 5"));
        List<String> tags = new ArrayList<>();
        String snippet = null;
        if (posts != null && !posts.isEmpty()) {
            for (FeedPost p : posts) {
                if (p.getAiTags() != null) {
                    tags.addAll(Arrays.asList(p.getAiTags().split("[,，、]")));
                }
                if (p.getPrimaryCategory() != null) tags.add(p.getPrimaryCategory());
                if (snippet == null && p.getContent() != null) {
                    snippet = p.getContent().length() > 30 ? p.getContent().substring(0, 30) : p.getContent();
                }
            }
            tags = tags.stream().map(String::trim).filter(t -> !t.isEmpty()).distinct().limit(10).collect(Collectors.toList());
        }
        return new FeedCheckResult(!posts.isEmpty(), tags, snippet);
    }

    /** 工具：获取共同兴趣 */
    public List<String> getCommonInterests(Long selfId, Long targetId) {
        User u1 = userMapper.selectById(selfId);
        User u2 = userMapper.selectById(targetId);
        if (u1 == null || u2 == null) return Collections.emptyList();
        Set<String> s1 = parseInterests(u1.getInterests());
        Set<String> s2 = parseInterests(u2.getInterests());
        s1.retainAll(s2);
        return new ArrayList<>(s1);
    }

    private Set<String> parseInterests(String interests) {
        if (interests == null || interests.isEmpty()) return Set.of();
        return Arrays.stream(interests.split("[,，、;；]")).map(String::trim).filter(t -> !t.isEmpty()).collect(Collectors.toSet());
    }

    private IceBreakTopicsResponse parseResponse(String content, boolean sameGender) {
        try {
            int start = content.indexOf('{');
            int end = content.lastIndexOf('}') + 1;
            if (start >= 0 && end > start) {
                String json = content.substring(start, end).replace("「", "").replace("」", "");
                Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
                String analysis = map.get("analysis") != null ? String.valueOf(map.get("analysis")).trim() : null;
                Object topicsObj = map.get("topics");
                List<String> topics = topicsObj instanceof List
                        ? ((List<?>) topicsObj).stream().map(String::valueOf).limit(3).toList()
                        : List.of("最近在忙什么呀？", "有什么兴趣爱好吗？", "周末一般怎么过？");
                return IceBreakTopicsResponse.builder()
                        .analysis(analysis != null && !analysis.isEmpty() ? analysis : "基于你们的聊天和兴趣，不妨从共同爱好聊起～")
                        .topics(topics)
                        .build();
            }
        } catch (Exception e) {
            log.debug("parseResponse failed: {}", e.getMessage());
        }
        return IceBreakTopicsResponse.builder()
                .analysis("分析解析失败，不妨从共同兴趣切入～")
                .topics(List.of("最近在忙什么呀？", "有什么兴趣爱好吗？", "周末一般怎么过？"))
                .build();
    }

    public record FeedCheckResult(boolean hasFeed, List<String> tags, String latestSnippet) {}
}
