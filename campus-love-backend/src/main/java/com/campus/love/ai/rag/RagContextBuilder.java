package com.campus.love.ai.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.feed.entity.FeedContentVector;
import com.campus.love.feed.entity.FeedPost;
import com.campus.love.feed.mapper.FeedContentVectorMapper;
import com.campus.love.feed.mapper.FeedPostMapper;
import com.campus.love.profile.entity.UserProfileVector;
import com.campus.love.profile.mapper.UserProfileVectorMapper;
import com.campus.love.tracking.entity.UserBehaviorLog;
import com.campus.love.tracking.mapper.UserBehaviorLogMapper;
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
 * RAG 上下文构建器
 * 技术文档 V1.1.0 第 9.2 节
 * 为缘分解析、破冰话题等 AI 功能构建 RAG 注入上下文
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagContextBuilder {

    private final FeedPostMapper feedPostMapper;
    private final FeedContentVectorMapper feedContentVectorMapper;
    private final UserProfileVectorMapper userProfileVectorMapper;
    private final UserBehaviorLogMapper behaviorLogMapper;
    private final UserMapper userMapper;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 缘分解析 RAG 上下文
     * A 历史关注偏好模式 + B 近期动态标签 + 向量契合度评估
     */
    public String buildYuanFenContext(Long userAId, Long userBId) {
        try {
            String preferencePattern = extractCommonPattern(userAId);
            List<String> bRecentTags = getRecentTags(userBId, 14);
            double similarity = calcTagVectorSimilarity(userAId, bRecentTags);
            return String.format("A历史关注偏好：%s\nB近期动态标签：%s\n契合度：%.0f%%",
                    preferencePattern, String.join("、", bRecentTags), similarity * 100);
        } catch (Exception e) {
            log.warn("buildYuanFenContext failed for {} vs {}: {}", userAId, userBId, e.getMessage());
            return "";
        }
    }

    /**
     * 破冰话题 RAG 上下文
     * 对方近期帖子标签 + 共同兴趣 + 最新动态摘要
     */
    public String buildIceBreakContext(Long selfId, Long targetId) {
        try {
            List<String> targetTags = getRecentTags(targetId, 14);
            List<String> commonInterests = getCommonInterestTags(selfId, targetId);
            String latestSnippet = getLatestSnippet(targetId, 30);
            return String.format("对方近期标签：%s\n共同兴趣：%s\n最新动态：「%s...」",
                    String.join("、", targetTags),
                    String.join("、", commonInterests),
                    latestSnippet != null ? latestSnippet : "");
        } catch (Exception e) {
            log.warn("buildIceBreakContext failed for {} vs {}: {}", selfId, targetId, e.getMessage());
            return "";
        }
    }

    private String extractCommonPattern(Long userId) {
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(30);
            List<UserBehaviorLog> logs = behaviorLogMapper.selectList(
                    new LambdaQueryWrapper<UserBehaviorLog>()
                            .eq(UserBehaviorLog::getUserId, userId)
                            .eq(UserBehaviorLog::getBehaviorType, "MATCH_CARD_VIEW")
                            .ge(UserBehaviorLog::getCreatedAt, since)
                            .orderByDesc(UserBehaviorLog::getCreatedAt)
                            .last("LIMIT 50"));
            if (logs == null || logs.isEmpty()) return "暂无";
            Set<Long> targetIds = logs.stream().map(UserBehaviorLog::getTargetId).filter(Objects::nonNull).distinct().limit(10).collect(Collectors.toSet());
            if (targetIds.isEmpty()) return "暂无";
            Set<String> tags = new HashSet<>();
            for (Long tid : targetIds) {
                User u = userMapper.selectById(tid);
                if (u != null) {
                    if (u.getMbti() != null) tags.add(u.getMbti());
                    if (u.getMajor() != null) tags.add(u.getMajor());
                }
            }
            return tags.isEmpty() ? "暂无" : String.join("、", tags.stream().limit(5).toList());
        } catch (Exception e) {
            return "暂无";
        }
    }

    private List<String> getRecentTags(Long userId, int days) {
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(days);
            List<FeedContentVector> vectors = feedContentVectorMapper.selectList(
                    new LambdaQueryWrapper<FeedContentVector>()
                            .eq(FeedContentVector::getUserId, userId)
                            .ge(FeedContentVector::getCreatedAt, since)
                            .orderByDesc(FeedContentVector::getCreatedAt)
                            .last("LIMIT 10"));
            if (vectors != null && !vectors.isEmpty()) {
                List<String> tags = new ArrayList<>();
                for (FeedContentVector v : vectors) {
                    if (v.getAiTags() != null && !v.getAiTags().isEmpty()) {
                        try {
                            List<String> arr = objectMapper.readValue(v.getAiTags(), new TypeReference<List<String>>() {});
                            if (arr != null) tags.addAll(arr);
                        } catch (Exception e) {
                            tags.add(v.getPrimaryCategory());
                        }
                    }
                    if (v.getPrimaryCategory() != null) tags.add(v.getPrimaryCategory());
                }
                return tags.stream().distinct().limit(10).collect(Collectors.toList());
            }
            List<FeedPost> posts = feedPostMapper.selectList(
                    new LambdaQueryWrapper<FeedPost>()
                            .eq(FeedPost::getUserId, userId)
                            .ge(FeedPost::getCreatedAt, since)
                            .orderByDesc(FeedPost::getCreatedAt)
                            .last("LIMIT 5"));
            if (posts != null) {
                return posts.stream()
                        .map(FeedPost::getAiTags)
                        .filter(Objects::nonNull)
                        .flatMap(s -> Arrays.stream(s.split("[,，、]")))
                        .map(String::trim)
                        .filter(t -> !t.isEmpty())
                        .distinct()
                        .limit(10)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.debug("getRecentTags failed: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    private double calcTagVectorSimilarity(Long userId, List<String> tags) {
        if (tags == null || tags.isEmpty()) return 0.5;
        try {
            UserProfileVector pv = userProfileVectorMapper.selectById(userId);
            if (pv == null || pv.getProfileVector() == null) return 0.5;
            float[] userVec = embeddingService.parseVector(pv.getProfileVector());
            String tagStr = String.join(" ", tags);
            float[] tagVec = embeddingService.embed(tagStr);
            if (userVec != null && tagVec != null) {
                return embeddingService.cosineSimilarity(userVec, tagVec);
            }
        } catch (Exception e) {
            log.debug("calcTagVectorSimilarity failed: {}", e.getMessage());
        }
        return 0.5;
    }

    private List<String> getCommonInterestTags(Long selfId, Long targetId) {
        try {
            User u1 = userMapper.selectById(selfId);
            User u2 = userMapper.selectById(targetId);
            if (u1 == null || u2 == null) return Collections.emptyList();
            Set<String> s1 = parseInterests(u1.getInterests());
            Set<String> s2 = parseInterests(u2.getInterests());
            s1.retainAll(s2);
            return new ArrayList<>(s1);
        } catch (Exception e) {
            log.debug("getCommonInterestTags failed: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    private Set<String> parseInterests(String interests) {
        if (interests == null || interests.isEmpty()) return Set.of();
        return Arrays.stream(interests.split("[,，、;；]"))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .collect(Collectors.toSet());
    }

    private String getLatestSnippet(Long userId, int maxChars) {
        try {
            List<FeedPost> posts = feedPostMapper.selectList(
                    new LambdaQueryWrapper<FeedPost>()
                            .eq(FeedPost::getUserId, userId)
                            .orderByDesc(FeedPost::getCreatedAt)
                            .last("LIMIT 1"));
            if (posts != null && !posts.isEmpty() && posts.get(0).getContent() != null) {
                String c = posts.get(0).getContent().trim();
                return c.length() > maxChars ? c.substring(0, maxChars) : c;
            }
        } catch (Exception e) {
            log.debug("getLatestSnippet failed: {}", e.getMessage());
        }
        return null;
    }
}
