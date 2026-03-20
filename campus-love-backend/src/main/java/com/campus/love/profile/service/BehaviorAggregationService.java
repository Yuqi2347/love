package com.campus.love.profile.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.chat.entity.Message;
import com.campus.love.chat.mapper.MessageMapper;
import com.campus.love.common.constants.OceanBehaviorSignals;
import com.campus.love.common.utils.InterestTagConverter;
import com.campus.love.feed.entity.FeedComment;
import com.campus.love.feed.entity.FeedLike;
import com.campus.love.feed.entity.FeedPost;
import com.campus.love.feed.mapper.FeedCommentMapper;
import com.campus.love.feed.mapper.FeedLikeMapper;
import com.campus.love.feed.mapper.FeedPostMapper;
import com.campus.love.invite.entity.Invite;
import com.campus.love.invite.entity.InviteParticipant;
import com.campus.love.invite.mapper.InviteMapper;
import com.campus.love.invite.mapper.InviteParticipantMapper;
import com.campus.love.profile.entity.UserBehaviorSummary;
import com.campus.love.profile.entity.UserPortrait;
import com.campus.love.profile.mapper.UserBehaviorSummaryMapper;
import com.campus.love.report.entity.Report;
import com.campus.love.report.mapper.ReportMapper;
import com.campus.love.tracking.entity.UserBehaviorLog;
import com.campus.love.tracking.mapper.UserBehaviorLogMapper;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BehaviorAggregationService {

    private final UserBehaviorLogMapper behaviorLogMapper;
    private final UserBehaviorSummaryMapper userBehaviorSummaryMapper;
    private final FeedPostMapper feedPostMapper;
    private final FeedLikeMapper feedLikeMapper;
    private final FeedCommentMapper feedCommentMapper;
    private final MessageMapper messageMapper;
    private final InviteMapper inviteMapper;
    private final InviteParticipantMapper inviteParticipantMapper;
    private final ReportMapper reportMapper;
    private final UserMapper userMapper;
    private final UserPortraitService userPortraitService;
    private final OceanConfidenceService oceanConfidenceService;
    private final ObjectMapper objectMapper;

    public UserBehaviorSummary aggregateAndSave(Long userId, int days) {
        AggregationResult result = aggregate(userId, days);
        UserBehaviorSummary summary = userBehaviorSummaryMapper.selectById(userId);
        if (summary == null) {
            summary = new UserBehaviorSummary();
            summary.setUserId(userId);
        }
        if (days <= 14) {
            summary.setBrowsePrefShort(toJson(result.browsePreferences()));
        } else {
            summary.setBrowsePrefLong(toJson(result.browsePreferences()));
        }
        summary.setChatPartnerTraits(toJson(result.chatPartnerTraits()));
        summary.setMatchInterestPattern(toJson(result.matchInterestPattern()));
        if (userBehaviorSummaryMapper.selectById(userId) == null) {
            userBehaviorSummaryMapper.insert(summary);
        } else {
            userBehaviorSummaryMapper.updateById(summary);
        }
        return summary;
    }

    public AggregationResult aggregate(Long userId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<UserBehaviorLog> logs = behaviorLogMapper.selectList(
                new LambdaQueryWrapper<UserBehaviorLog>()
                        .eq(UserBehaviorLog::getUserId, userId)
                        .ge(UserBehaviorLog::getCreatedAt, since)
                        .orderByDesc(UserBehaviorLog::getCreatedAt)
        );

        Map<String, Object> browsePreferences = buildBrowsePreferences(logs);
        Map<String, Object> chatPartnerTraits = buildChatPartnerTraits(userId, since);
        Map<String, Object> matchInterestPattern = buildMatchInterestPattern(logs);
        Map<String, BigDecimal> signalScores = buildSignalScores(userId, since, days, logs, browsePreferences);

        return new AggregationResult(browsePreferences, chatPartnerTraits, matchInterestPattern, signalScores);
    }

    public Map<String, BigDecimal> buildDimensionScores(Long userId, int days) {
        AggregationResult result = aggregate(userId, days);
        Map<String, BigDecimal> dimensionScores = new LinkedHashMap<>();
        for (String dim : OceanBehaviorSignals.DIMENSIONS) {
            Map<String, Double> weights = OceanBehaviorSignals.SIGNAL_WEIGHTS.get(dim);
            if (weights == null || weights.isEmpty()) continue;
            double score = 0d;
            double totalWeight = 0d;
            for (Map.Entry<String, Double> entry : weights.entrySet()) {
                BigDecimal signal = result.signalScores().get(entry.getKey());
                if (signal == null) continue;
                score += signal.doubleValue() * entry.getValue();
                totalWeight += entry.getValue();
            }
            if (totalWeight > 0d) {
                dimensionScores.put(dim, BigDecimal.valueOf(score / totalWeight).setScale(1, RoundingMode.HALF_UP));
            }
        }
        return dimensionScores;
    }

    public boolean hasRecentActivity(Long userId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        Long count = behaviorLogMapper.selectCount(new LambdaQueryWrapper<UserBehaviorLog>()
                .eq(UserBehaviorLog::getUserId, userId)
                .ge(UserBehaviorLog::getCreatedAt, since));
        return count != null && count > 0;
    }

    /**
     * 根据动态浏览（FEED_VIEW）与点赞（FEED_LIKE）汇总用户对内容类目的偏好。
     * 点赞权重高于浏览，便于画像更贴近「明确正向反馈」。
     */
    private Map<String, Object> buildBrowsePreferences(List<UserBehaviorLog> logs) {
        Map<Long, Integer> postWeights = new HashMap<>();
        for (UserBehaviorLog log : logs) {
            if (log.getTargetId() == null) continue;
            String type = log.getBehaviorType();
            if ("FEED_VIEW".equals(type)) {
                postWeights.merge(log.getTargetId(), 1, Integer::sum);
            } else if ("FEED_LIKE".equals(type)) {
                postWeights.merge(log.getTargetId(), 2, Integer::sum);
            }
        }
        if (postWeights.isEmpty()) return Map.of();
        List<FeedPost> posts = feedPostMapper.selectList(
                new LambdaQueryWrapper<FeedPost>().in(FeedPost::getId, postWeights.keySet()));
        Map<String, Long> byCategory = new HashMap<>();
        for (FeedPost post : posts) {
            String cat = post.getPrimaryCategory();
            if (cat == null || cat.isBlank()) continue;
            int w = postWeights.getOrDefault(post.getId(), 1);
            byCategory.merge(cat, (long) w, Long::sum);
        }
        long total = byCategory.values().stream().mapToLong(Long::longValue).sum();
        if (total <= 0) return Map.of();
        Map<String, Object> normalized = new LinkedHashMap<>();
        byCategory.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> normalized.put(entry.getKey(), round(entry.getValue() * 100d / total)));
        return normalized;
    }

    private Map<String, Object> buildChatPartnerTraits(Long userId, LocalDateTime since) {
        List<Message> messages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .ge(Message::getCreatedAt, since)
                        .and(w -> w.eq(Message::getSenderId, userId).or().eq(Message::getReceiverId, userId))
        );
        if (messages.isEmpty()) return Map.of();
        Set<Long> partnerIds = messages.stream()
                .map(msg -> Objects.equals(msg.getSenderId(), userId) ? msg.getReceiverId() : msg.getSenderId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (partnerIds.isEmpty()) return Map.of();
        List<User> partners = userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, partnerIds));
        Map<String, Long> mbtiDist = partners.stream()
                .map(User::getMbti)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        Map<String, Long> majorDist = partners.stream()
                .map(User::getMajor)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("topMbti", topEntries(mbtiDist, 3));
        result.put("topMajor", topEntries(majorDist, 3));
        result.put("partnerCount", partnerIds.size());
        return result;
    }

    private Map<String, Object> buildMatchInterestPattern(List<UserBehaviorLog> logs) {
        List<UserBehaviorLog> cardViews = logs.stream()
                .filter(log -> "MATCH_CARD_VIEW".equals(log.getBehaviorType()))
                .toList();
        if (cardViews.isEmpty()) return Map.of();
        double totalWeight = 0d;
        int viewProfileCount = 0;
        int swipeLeftCount = 0;
        for (UserBehaviorLog log : cardViews) {
            Map<String, Object> meta = parseMap(log.getMetadata());
            totalWeight += asDouble(meta.get("weight"), 0.7d);
            String action = meta.get("postAction") != null ? meta.get("postAction").toString() : "";
            if ("VIEW_PROFILE".equals(action)) viewProfileCount++;
            if ("SWIPE_LEFT".equals(action)) swipeLeftCount++;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("avgWeight", round(totalWeight / cardViews.size()));
        result.put("viewProfileRatio", round(viewProfileCount * 100d / cardViews.size()));
        result.put("swipeLeftRatio", round(swipeLeftCount * 100d / cardViews.size()));
        result.put("sampleSize", cardViews.size());
        return result;
    }

    private Map<String, BigDecimal> buildSignalScores(Long userId, LocalDateTime since, int days, List<UserBehaviorLog> logs, Map<String, Object> browsePreferences) {
        Map<String, BigDecimal> signals = new LinkedHashMap<>();

        UserPortrait portrait = userPortraitService.getPortrait(userId);
        int interestCount = portrait != null ? InterestTagConverter.extractCodesFromNewFormat(portrait.getInterestTags()).size() : 0;
        signals.put("interest_tag_breadth", scaledPercent(interestCount / 12d));

        int browseCategoryCount = browsePreferences.size();
        signals.put("browse_category_diversity", scaledPercent(browseCategoryCount / 6d));

        List<FeedPost> ownPosts = feedPostMapper.selectList(
                new LambdaQueryWrapper<FeedPost>()
                        .eq(FeedPost::getUserId, userId)
                        .ge(FeedPost::getCreatedAt, since)
        );
        signals.put("feed_topic_span", scaledPercent(extractDistinctTagCount(ownPosts) / 12d));
        signals.put("feed_publish_frequency", scaledPercent(ownPosts.size() / Math.max(1d, days / 3d)));

        List<Message> messages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .ge(Message::getCreatedAt, since)
                        .and(w -> w.eq(Message::getSenderId, userId).or().eq(Message::getReceiverId, userId))
        );
        long sentCount = messages.stream().filter(msg -> Objects.equals(msg.getSenderId(), userId)).count();
        signals.put("chat_initiation_ratio", messages.isEmpty() ? null : roundDecimal(sentCount * 100d / messages.size()));
        signals.put("chat_reply_consistency", computeChatReplyConsistency(messages, userId));

        List<Invite> invites = inviteMapper.selectList(
                new LambdaQueryWrapper<Invite>()
                        .eq(Invite::getCreatorId, userId)
                        .ge(Invite::getCreatedAt, since)
        );
        signals.put("invite_initiation_count", scaledPercent(invites.size() / 4d));
        signals.put("invite_completion_rate", computeInviteCompletionRate(invites));

        List<InviteParticipant> participations = inviteParticipantMapper.selectList(
                new LambdaQueryWrapper<InviteParticipant>()
                        .eq(InviteParticipant::getUserId, userId)
                        .ge(InviteParticipant::getJoinAt, since)
        );
        signals.put("invite_acceptance_rate", participations.isEmpty() ? null : scaledPercent(participations.size() / 4d));

        long likes = feedLikeMapper.selectCount(new LambdaQueryWrapper<FeedLike>()
                .eq(FeedLike::getUserId, userId)
                .ge(FeedLike::getCreatedAt, since));
        long comments = feedCommentMapper.selectCount(new LambdaQueryWrapper<FeedComment>()
                .eq(FeedComment::getUserId, userId)
                .ge(FeedComment::getCreatedAt, since));
        signals.put("positive_reaction_ratio", scaledPercent((likes + comments) / 20d));

        signals.put("conflict_recovery_speed", computeConflictRecoverySpeed(messages, userId));

        long reportsAgainst = reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .eq(Report::getTargetType, Report.TARGET_USER)
                .eq(Report::getTargetId, userId)
                .ge(Report::getCreatedAt, since));
        signals.put("block_report_rate", scaledInversePercent(reportsAgainst / 3d));

        signals.put("feed_sentiment_score", computeFeedSentimentScore(ownPosts));
        signals.put("late_night_activity_ratio", computeLateNightActivity(logs, messages, ownPosts));
        signals.put("chat_abandon_rate", computeChatAbandonRate(logs));
        return signals;
    }

    private BigDecimal computeChatReplyConsistency(List<Message> messages, Long userId) {
        if (messages.isEmpty()) return null;
        Map<Integer, Long> hourDist = messages.stream()
                .filter(msg -> Objects.equals(msg.getSenderId(), userId))
                .collect(Collectors.groupingBy(msg -> msg.getCreatedAt().getHour(), Collectors.counting()));
        if (hourDist.isEmpty()) return null;
        long maxBucket = hourDist.values().stream().mapToLong(Long::longValue).max().orElse(0);
        return scaledPercent(1d - (maxBucket / (double) messages.size()) * 0.5d);
    }

    private BigDecimal computeInviteCompletionRate(List<Invite> invites) {
        if (invites.isEmpty()) return null;
        long completed = invites.stream().filter(invite -> "COMPLETED".equalsIgnoreCase(invite.getStatus())).count();
        return roundDecimal(completed * 100d / invites.size());
    }

    private BigDecimal computeConflictRecoverySpeed(List<Message> messages, Long userId) {
        if (messages.size() < 4) return null;
        List<Message> sorted = new ArrayList<>(messages);
        sorted.sort(Comparator.comparing(Message::getCreatedAt));
        double score = 50d;
        for (int i = 1; i < sorted.size(); i++) {
            long gapHours = java.time.Duration.between(sorted.get(i - 1).getCreatedAt(), sorted.get(i).getCreatedAt()).toHours();
            if (gapHours > 24) {
                score -= Math.min(20d, gapHours / 4d);
            } else {
                score += 3d;
            }
        }
        return BigDecimal.valueOf(Math.max(0d, Math.min(100d, score))).setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal computeFeedSentimentScore(List<FeedPost> ownPosts) {
        if (ownPosts.size() < 5) return null;
        double sum = 0d;
        for (FeedPost post : ownPosts) {
            String sentiment = post.getTagSentiment();
            if ("positive".equalsIgnoreCase(sentiment)) sum += 1d;
            else if ("negative".equalsIgnoreCase(sentiment)) sum -= 1d;
        }
        double normalized = ((sum / ownPosts.size()) + 1d) / 2d;
        return scaledPercent(normalized);
    }

    private BigDecimal computeLateNightActivity(List<UserBehaviorLog> logs, List<Message> messages, List<FeedPost> posts) {
        long total = logs.size() + messages.size() + posts.size();
        if (total == 0) return null;
        long lateNight = logs.stream().filter(log -> log.getCreatedAt().getHour() < 5).count()
                + messages.stream().filter(msg -> msg.getCreatedAt().getHour() < 5).count()
                + posts.stream().filter(post -> post.getCreatedAt().getHour() < 5).count();
        return scaledInversePercent(lateNight / (double) total);
    }

    private BigDecimal computeChatAbandonRate(List<UserBehaviorLog> logs) {
        List<UserBehaviorLog> cardViews = logs.stream().filter(log -> "MATCH_CARD_VIEW".equals(log.getBehaviorType())).toList();
        if (cardViews.isEmpty()) return null;
        long negative = cardViews.stream()
                .map(UserBehaviorLog::getMetadata)
                .map(this::parseMap)
                .filter(meta -> "SWIPE_LEFT".equals(meta.get("postAction")))
                .count();
        return scaledInversePercent(negative / (double) cardViews.size());
    }

    private int extractDistinctTagCount(List<FeedPost> posts) {
        return (int) posts.stream()
                .map(FeedPost::getAiTags)
                .filter(Objects::nonNull)
                .flatMap(tags -> List.of(tags.split("[,，、]")).stream())
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .distinct()
                .count();
    }

    private List<String> topEntries(Map<String, Long> dist, int limit) {
        return dist.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }

    private Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("serialize behavior summary failed: {}", e.getMessage());
            return null;
        }
    }

    private double asDouble(Object value, double fallback) {
        if (value == null) return fallback;
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return fallback;
        }
    }

    private BigDecimal scaledPercent(double ratio) {
        double normalized = Math.max(0d, Math.min(1d, ratio));
        return roundDecimal(normalized * 100d);
    }

    private BigDecimal scaledInversePercent(double ratio) {
        double normalized = 1d - Math.max(0d, Math.min(1d, ratio));
        return roundDecimal(normalized * 100d);
    }

    private BigDecimal roundDecimal(double value) {
        return BigDecimal.valueOf(Math.max(0d, Math.min(100d, value))).setScale(1, RoundingMode.HALF_UP);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    public record AggregationResult(
            Map<String, Object> browsePreferences,
            Map<String, Object> chatPartnerTraits,
            Map<String, Object> matchInterestPattern,
            Map<String, BigDecimal> signalScores
    ) {}
}
