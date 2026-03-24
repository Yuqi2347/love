package com.campus.love.moment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.moment.entity.MomentEnrollment;
import com.campus.love.moment.entity.MomentMatchConfig;
import com.campus.love.moment.entity.MomentMatchResult;
import com.campus.love.moment.entity.MomentPairScore;
import com.campus.love.moment.entity.MomentProfile;
import com.campus.love.moment.entity.MomentRejectSummary;
import com.campus.love.moment.entity.MomentUserPoolBest;
import com.campus.love.moment.mapper.MomentEnrollmentMapper;
import com.campus.love.moment.mapper.MomentMatchResultMapper;
import com.campus.love.moment.mapper.MomentPairScoreMapper;
import com.campus.love.moment.mapper.MomentProfileMapper;
import com.campus.love.moment.mapper.MomentRejectSummaryMapper;
import com.campus.love.moment.mapper.MomentUserPoolBestMapper;
import com.campus.love.profile.service.UserPortraitService;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MomentDashboardService {

    private static final String STRUCTURAL_POOL_GENDER_REASON = "匹配池性别不满足";

    private final MomentEnrollmentMapper enrollmentMapper;
    private final MomentMatchResultMapper matchResultMapper;
    private final MomentPairScoreMapper pairScoreMapper;
    private final MomentProfileMapper profileMapper;
    private final UserMapper userMapper;
    private final MomentMatchConfigService matchConfigService;
    private final MomentMatcher matcher;
    private final UserPortraitService userPortraitService;
    private final Cache<String, SimulationResponse> momentSimulateCache;
    private final MomentRejectSummaryMapper rejectSummaryMapper;
    private final MomentUserPoolBestMapper userPoolBestMapper;
    private final ObjectMapper objectMapper;

    public MomentDashboardResponse getDashboard(String weekTag) {
        MomentMatchConfig config = matchConfigService.getConfig();
        List<MomentEnrollment> enrollments = listEnrollments(weekTag);
        Set<Long> participantIds = enrollments.stream()
                .map(MomentEnrollment::getUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<MomentMatchResult> results = listMatchResults(weekTag);
        List<MomentRejectSummary> summaries = listRejectSummaries(weekTag);
        Map<String, MomentUserPoolBest> poolBestByKey = listPoolBest(weekTag).stream()
                .collect(Collectors.toMap(
                        row -> row.getPool() + ":" + row.getUserId(),
                        row -> row,
                        (a, b) -> a
                ));
        UnmatchedMetrics unmatchedMetrics = buildUnmatchedMetrics(enrollments, results, poolBestByKey);

        Set<Long> matchedUserIds = matchedUserIds(results);
        long participantCount = participantIds.size();
        long matchedUsers = matchedUserIds.size();
        long unmatchedCount = Math.max(0, participantCount - matchedUsers);
        double successRate = participantCount == 0 ? 0d : round(matchedUsers * 100d / participantCount);

        List<HistogramBucket> matchedHistogram = bucketScores(
                results.stream()
                        .map(MomentMatchResult::getTotalScore)
                        .filter(Objects::nonNull)
                        .map(BigDecimal::doubleValue)
                        .toList()
        );
        List<HistogramBucket> unmatchedBestHistogram = bucketScores(unmatchedMetrics.unmatchedBestScores());

        List<ReasonStat> hardFilterStats = List.of();
        List<ReasonStat> softPenaltyStats = List.of();
        long filteredPairCount = 0L;
        long structuralPoolGenderFiltered = 0L;
        if (!summaries.isEmpty()) {
            filteredPairCount = summaries.stream()
                    .mapToLong(s -> s.getBelowThresholdCount() != null ? s.getBelowThresholdCount() : 0L)
                    .sum();
            structuralPoolGenderFiltered = sumReasonCountFromJson(summaries, MomentRejectSummary::getHardFilterReasonDist, STRUCTURAL_POOL_GENDER_REASON);
            hardFilterStats = mergeReasonsFromJson(summaries, MomentRejectSummary::getHardFilterReasonDist).stream()
                    .filter(item -> !STRUCTURAL_POOL_GENDER_REASON.equals(item.reason()))
                    .toList();
            softPenaltyStats = mergeReasonsFromJson(summaries, MomentRejectSummary::getSoftPenaltyReasonDist);
        }

        List<PoolStat> poolStats = buildPoolStats(enrollments, results);
        List<ReasonStat> unmatchedReasonStats = groupReasons(unmatchedMetrics.unmatchedReasons());
        List<FilteredPairSample> filteredPairSamples = List.of();

        int topK = config.getEligibleTopK() != null ? config.getEligibleTopK() : MomentMatchConfig.DEFAULT_ELIGIBLE_TOP_K;
        String statsNote = summaries.isEmpty()
                ? "当前周尚未生成硬筛/阈值聚合快照，已返回轻量统计结果。建议先触发匹配流水线后再查看细分。"
                : "当前看板以最终匹配结果 + reject_summary 聚合 + user_pool_best 为主；Top-K=" + topK
                + " 仅影响图匹配候选边截断，不再阻塞正式匹配流水线。"
                + (structuralPoolGenderFiltered > 0
                ? " 已将“匹配池性别不满足”视为结构性过滤（非问卷冲突）从硬筛榜中剔除："
                + structuralPoolGenderFiltered + " 对。"
                : "");

        return new MomentDashboardResponse(
                weekTag,
                participantCount,
                matchedUsers,
                unmatchedCount,
                successRate,
                config.getBaseThreshold(),
                matchedHistogram,
                unmatchedBestHistogram,
                filteredPairCount,
                poolStats,
                hardFilterStats,
                softPenaltyStats,
                unmatchedReasonStats,
                filteredPairSamples,
                topK,
                statsNote
        );
    }

    public List<UnmatchedUserResponse> getUnmatchedUsers(String weekTag) {
        List<MomentEnrollment> enrollments = listEnrollments(weekTag);
        if (enrollments.isEmpty()) {
            return List.of();
        }
        Set<Long> matchedUsers = matchedUserIds(listMatchResults(weekTag));
        Map<String, MomentUserPoolBest> poolBestByKey = listPoolBest(weekTag).stream()
                .collect(Collectors.toMap(
                        row -> row.getPool() + ":" + row.getUserId(),
                        row -> row,
                        (a, b) -> a,
                        HashMap::new
                ));

        Map<Long, User> users = loadUsers(enrollments.stream().map(MomentEnrollment::getUserId).collect(Collectors.toSet()));
        Map<Long, MomentProfile> profiles = loadProfiles(enrollments.stream().map(MomentEnrollment::getUserId).collect(Collectors.toSet()));
        Map<String, Long> poolParticipants = enrollments.stream()
                .collect(Collectors.groupingBy(MomentEnrollment::getPool, LinkedHashMap::new,
                        Collectors.mapping(MomentEnrollment::getUserId, Collectors.collectingAndThen(Collectors.toSet(), v -> (long) v.size()))));

        List<UnmatchedUserResponse> rows = new ArrayList<>();
        for (MomentEnrollment enrollment : enrollments) {
            Long userId = enrollment.getUserId();
            if (matchedUsers.contains(userId)) {
                continue;
            }

            MomentUserPoolBest pb = poolBestByKey.get(enrollment.getPool() + ":" + userId);
            Double highestAvailableScore;
            String reason;

            if (pb != null) {
                highestAvailableScore = pb.getMaxEligibleScore() != null
                        ? round(pb.getMaxEligibleScore().doubleValue())
                        : null;
                reason = reasonFromPoolBest(pb);
            } else {
                highestAvailableScore = null;
                reason = poolParticipants.getOrDefault(enrollment.getPool(), 0L) <= 1
                        ? "池内人数不足"
                        : "等待匹配聚合结果";
            }

            User user = users.get(userId);
            MomentProfile profile = profiles.get(userId);
            rows.add(new UnmatchedUserResponse(
                    userId,
                    user != null ? user.getNickname() : null,
                    enrollment.getPool(),
                    highestAvailableScore,
                    reason,
                    user != null ? user.getMomentPriorityCountOrDefault() : 0,
                    profile != null && Boolean.TRUE.equals(profile.getPrioritizeMatching())
            ));
        }

        rows.sort(Comparator.comparing(UnmatchedUserResponse::pool).thenComparing(UnmatchedUserResponse::userId));
        return rows;
    }

    private UnmatchedMetrics buildUnmatchedMetrics(
            List<MomentEnrollment> enrollments,
            List<MomentMatchResult> results,
            Map<String, MomentUserPoolBest> poolBestByKey
    ) {
        if (enrollments == null || enrollments.isEmpty()) {
            return new UnmatchedMetrics(List.of(), List.of());
        }
        Set<Long> matchedUsers = matchedUserIds(results);
        Map<String, Long> poolParticipants = enrollments.stream()
                .collect(Collectors.groupingBy(MomentEnrollment::getPool, LinkedHashMap::new,
                        Collectors.mapping(MomentEnrollment::getUserId, Collectors.collectingAndThen(Collectors.toSet(), v -> (long) v.size()))));

        List<Double> unmatchedBestScores = new ArrayList<>();
        List<String> unmatchedReasons = new ArrayList<>();
        for (MomentEnrollment enrollment : enrollments) {
            Long userId = enrollment.getUserId();
            if (matchedUsers.contains(userId)) {
                continue;
            }
            MomentUserPoolBest pb = poolBestByKey.get(enrollment.getPool() + ":" + userId);
            if (pb != null && pb.getMaxEligibleScore() != null) {
                unmatchedBestScores.add(pb.getMaxEligibleScore().doubleValue());
            }
            String reason = pb != null
                    ? reasonFromPoolBest(pb)
                    : (poolParticipants.getOrDefault(enrollment.getPool(), 0L) <= 1 ? "池内人数不足" : "等待匹配聚合结果");
            unmatchedReasons.add(reason);
        }
        return new UnmatchedMetrics(unmatchedBestScores, unmatchedReasons);
    }

    private static String reasonFromPoolBest(MomentUserPoolBest pb) {
        if (!Boolean.TRUE.equals(pb.getHasAnyEligible())) {
            return "无通过硬筛选候选";
        }
        if (Boolean.TRUE.equals(pb.getTier2Truncated())) {
            return "全局最优未选中（候选截断）";
        }
        return "全局最优未选中";
    }

    private String reasonFromPairScores(
            List<MomentPairScore> userPoolScores,
            Map<String, Long> poolParticipants,
            String pool
    ) {
        if (userPoolScores.isEmpty()) {
            return poolParticipants.getOrDefault(pool, 0L) <= 1 ? "池内人数不足" : "无候选对";
        }
        if (userPoolScores.stream().noneMatch(score -> Boolean.TRUE.equals(score.getHardFilterPassed()))) {
            return "无通过硬筛选候选";
        }
        if (userPoolScores.stream().noneMatch(score -> Boolean.TRUE.equals(score.getIncludedByThreshold()))) {
            return "全部被阈值过滤";
        }
        return "全局最优未选中";
    }

    public SimulationResponse simulate(String weekTag, int threshold) {
        int normalized = Math.min(100, Math.max(0, threshold));
        String cacheKey = "simulate:" + weekTag + ":" + normalized;
        SimulationResponse hit = momentSimulateCache.getIfPresent(cacheKey);
        if (hit != null) {
            return hit;
        }

        List<MomentEnrollment> enrollments = listEnrollments(weekTag);
        if (enrollments.isEmpty()) {
            SimulationResponse empty = new SimulationResponse(weekTag, normalized, 0, 0, 0, 0d, 0);
            momentSimulateCache.put(cacheKey, empty);
            return empty;
        }

        MomentMatchConfig base = matchConfigService.getConfig();
        MomentMatchConfig simConfig = new MomentMatchConfig();
        BeanUtils.copyProperties(base, simConfig);
        simConfig.setBaseThreshold(normalized);

        Map<Long, User> userCache = new HashMap<>();
        Map<Long, MomentProfile> profileCache = new HashMap<>();
        Map<String, List<MomentMatcher.Candidate>> poolCandidates = new LinkedHashMap<>();
        for (MomentEnrollment enrollment : enrollments) {
            Long userId = enrollment.getUserId();
            User user = userCache.computeIfAbsent(userId, userMapper::selectById);
            MomentProfile profile = profileCache.computeIfAbsent(userId, id -> profileMapper.selectOne(
                    new LambdaQueryWrapper<MomentProfile>().eq(MomentProfile::getUserId, id).last("limit 1")
            ));
            if (user == null || profile == null) {
                continue;
            }
            poolCandidates.computeIfAbsent(enrollment.getPool(), k -> new ArrayList<>())
                    .add(new MomentMatcher.Candidate(user, profile, userPortraitService.getPortrait(userId)));
        }

        Set<Long> globalMatchedUserIds = new HashSet<>();
        int matchedPairs = 0;
        MomentMatcher.MatchProgressContext ctx = new MomentMatcher.MatchProgressContext();
        for (String pool : matcher.poolOrder()) {
            List<MomentMatcher.Candidate> candidates = poolCandidates.getOrDefault(pool, List.of()).stream()
                    .filter(c -> !globalMatchedUserIds.contains(c.user().getId()))
                    .toList();
            if (candidates.size() < 2) {
                continue;
            }
            MomentMatcher.PoolMatchResult poolResult = matcher.match(candidates, pool, simConfig, ctx, null);
            matchedPairs += poolResult.matches().size();
            for (MomentMatcher.MatchPair p : poolResult.matches()) {
                globalMatchedUserIds.add(p.userIdA());
                globalMatchedUserIds.add(p.userIdB());
            }
        }

        int participantCount = (int) enrollments.stream().map(MomentEnrollment::getUserId).distinct().count();
        int matchedUsers = globalMatchedUserIds.size();
        int unmatchedUsers = Math.max(0, participantCount - matchedUsers);
        int currentPairs = listMatchResults(weekTag).size();
        double successRate = participantCount == 0 ? 0d : round(matchedUsers * 100d / participantCount);

        SimulationResponse response = new SimulationResponse(
                weekTag,
                normalized,
                matchedPairs,
                matchedUsers,
                unmatchedUsers,
                successRate,
                matchedPairs - currentPairs
        );
        momentSimulateCache.put(cacheKey, response);
        return response;
    }

    private List<MomentRejectSummary> listRejectSummaries(String weekTag) {
        return rejectSummaryMapper.selectList(
                new LambdaQueryWrapper<MomentRejectSummary>().eq(MomentRejectSummary::getWeekTag, weekTag));
    }

    private List<ReasonStat> mergeReasonsFromJson(
            List<MomentRejectSummary> summaries,
            java.util.function.Function<MomentRejectSummary, String> jsonGetter
    ) {
        Map<String, Long> agg = new LinkedHashMap<>();
        for (MomentRejectSummary s : summaries) {
            mergeJsonCounts(agg, jsonGetter.apply(s));
        }
        return agg.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> new ReasonStat(e.getKey(), e.getValue()))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private void mergeJsonCounts(Map<String, Long> agg, String json) {
        if (json == null || json.isBlank()) {
            return;
        }
        try {
            Map<String, Object> raw = objectMapper.readValue(json, Map.class);
            for (Map.Entry<String, Object> e : raw.entrySet()) {
                Object v = e.getValue();
                long n = v instanceof Number ? ((Number) v).longValue() : Long.parseLong(String.valueOf(v));
                agg.merge(e.getKey(), n, Long::sum);
            }
        } catch (Exception ignored) {
            // 忽略损坏 JSON
        }
    }

    private long sumReasonCountFromJson(
            List<MomentRejectSummary> summaries,
            java.util.function.Function<MomentRejectSummary, String> jsonGetter,
            String reason
    ) {
        if (reason == null || reason.isBlank() || summaries == null || summaries.isEmpty()) {
            return 0L;
        }
        long total = 0L;
        for (MomentRejectSummary s : summaries) {
            String json = jsonGetter.apply(s);
            if (json == null || json.isBlank()) {
                continue;
            }
            try {
                Map<String, Object> raw = objectMapper.readValue(json, Map.class);
                Object v = raw.get(reason);
                if (v instanceof Number n) {
                    total += n.longValue();
                } else if (v != null) {
                    total += Long.parseLong(String.valueOf(v));
                }
            } catch (Exception ignored) {
                // ignore broken json
            }
        }
        return total;
    }

    private List<PoolStat> buildPoolStats(List<MomentEnrollment> enrollments, List<MomentMatchResult> results) {
        Map<String, Long> participants = enrollments.stream()
                .collect(Collectors.groupingBy(MomentEnrollment::getPool, LinkedHashMap::new,
                        Collectors.mapping(MomentEnrollment::getUserId, Collectors.collectingAndThen(Collectors.toSet(), v -> (long) v.size()))));
        Map<String, Long> matchedPairs = results.stream()
                .collect(Collectors.groupingBy(MomentMatchResult::getPool, LinkedHashMap::new, Collectors.counting()));

        List<PoolStat> stats = new ArrayList<>();
        for (String pool : matcher.poolOrder()) {
            long participantCount = participants.getOrDefault(pool, 0L);
            long pairCount = matchedPairs.getOrDefault(pool, 0L);
            stats.add(new PoolStat(pool, participantCount, pairCount, Math.max(0L, participantCount - pairCount * 2)));
        }
        return stats;
    }

    private List<ReasonStat> groupReasons(List<String> reasons) {
        Map<String, Long> counts = reasons.stream()
                .collect(Collectors.groupingBy(value -> value, LinkedHashMap::new, Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> new ReasonStat(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<HistogramBucket> bucketScores(List<Double> scores) {
        if (scores == null || scores.isEmpty()) {
            return List.of();
        }
        Map<String, Long> buckets = new LinkedHashMap<>();
        for (int score = 0; score <= 100; score++) {
            buckets.put(String.valueOf(score), 0L);
        }
        for (Double score : scores) {
            if (score == null) {
                continue;
            }
            int rounded = Math.max(0, Math.min(100, (int) Math.floor(score)));
            String label = String.valueOf(rounded);
            buckets.computeIfPresent(label, (k, value) -> value + 1);
        }
        return buckets.entrySet().stream()
                .map(entry -> new HistogramBucket(entry.getKey(), entry.getValue()))
                .toList();
    }

    private Set<Long> matchedUserIds(List<MomentMatchResult> results) {
        Set<Long> matched = new HashSet<>();
        for (MomentMatchResult result : results) {
            matched.add(result.getUserIdA());
            matched.add(result.getUserIdB());
        }
        return matched;
    }

    private Map<Long, User> loadUsers(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(User::getId, user -> user, (a, b) -> a));
    }

    private Map<Long, MomentProfile> loadProfiles(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return profileMapper.selectList(new LambdaQueryWrapper<MomentProfile>().in(MomentProfile::getUserId, userIds)).stream()
                .collect(Collectors.toMap(MomentProfile::getUserId, profile -> profile, (a, b) -> a));
    }

    private List<MomentEnrollment> listEnrollments(String weekTag) {
        return enrollmentMapper.selectList(new LambdaQueryWrapper<MomentEnrollment>()
                .eq(MomentEnrollment::getWeekTag, weekTag));
    }

    private List<MomentMatchResult> listMatchResults(String weekTag) {
        return matchResultMapper.selectList(new LambdaQueryWrapper<MomentMatchResult>()
                .eq(MomentMatchResult::getWeekTag, weekTag));
    }

    private List<MomentUserPoolBest> listPoolBest(String weekTag) {
        return userPoolBestMapper.selectList(
                new LambdaQueryWrapper<MomentUserPoolBest>().eq(MomentUserPoolBest::getWeekTag, weekTag)
        );
    }

    private List<MomentPairScore> listPairScores(String weekTag) {
        return pairScoreMapper.selectList(new LambdaQueryWrapper<MomentPairScore>()
                .eq(MomentPairScore::getWeekTag, weekTag));
    }

    private List<MomentPairScore> listPairScoresLimited(String weekTag, int maxRows) {
        return pairScoreMapper.selectList(new LambdaQueryWrapper<MomentPairScore>()
                .eq(MomentPairScore::getWeekTag, weekTag)
                .last("LIMIT " + maxRows));
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    public record MomentDashboardResponse(
            String weekTag,
            long participantCount,
            long matchedUsers,
            long unmatchedUsers,
            double successRate,
            int currentThreshold,
            List<HistogramBucket> matchedScoreHistogram,
            List<HistogramBucket> unmatchedBestScoreHistogram,
            long filteredPairCount,
            List<PoolStat> poolStats,
            List<ReasonStat> hardFilterStats,
            List<ReasonStat> softPenaltyStats,
            List<ReasonStat> unmatchedReasonStats,
            List<FilteredPairSample> filteredPairSamples,
            int eligibleTopK,
            String statsNote
    ) {}

    public record HistogramBucket(String label, long count) {}

    public record PoolStat(String pool, long participants, long matchedPairs, long unmatchedUsers) {}

    public record ReasonStat(String reason, long count) {}

    public record FilteredPairSample(String pool, Long userIdA, Long userIdB, double score, Integer thresholdRequired) {}

    public record UnmatchedUserResponse(
            Long userId,
            String nickname,
            String pool,
            Double highestAvailableScore,
            String reason,
            int priorityCount,
            boolean prioritizeMatching
    ) {}

    public record SimulationResponse(
            String weekTag,
            int threshold,
            int matchedPairs,
            int matchedUsers,
            int unmatchedUsers,
            double successRate,
            int deltaPairs
    ) {}

    private record UnmatchedMetrics(
            List<Double> unmatchedBestScores,
            List<String> unmatchedReasons
    ) {}
}
