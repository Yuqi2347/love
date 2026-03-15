package com.campus.love.moment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.moment.entity.MomentEnrollment;
import com.campus.love.moment.entity.MomentMatchConfig;
import com.campus.love.moment.entity.MomentMatchResult;
import com.campus.love.moment.entity.MomentPairScore;
import com.campus.love.moment.entity.MomentProfile;
import com.campus.love.moment.mapper.MomentEnrollmentMapper;
import com.campus.love.moment.mapper.MomentMatchResultMapper;
import com.campus.love.moment.mapper.MomentPairScoreMapper;
import com.campus.love.moment.mapper.MomentProfileMapper;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.jgrapht.alg.interfaces.MatchingAlgorithm;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;
import org.jgrapht.alg.matching.blossom.v5.KolmogorovWeightedMatching;
import org.jgrapht.alg.matching.blossom.v5.ObjectiveSense;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MomentDashboardService {

    private final MomentEnrollmentMapper enrollmentMapper;
    private final MomentMatchResultMapper matchResultMapper;
    private final MomentPairScoreMapper pairScoreMapper;
    private final MomentProfileMapper profileMapper;
    private final UserMapper userMapper;
    private final MomentMatchConfigService matchConfigService;
    private final MomentMatcher matcher;

    public MomentDashboardResponse getDashboard(String weekTag) {
        MomentMatchConfig config = matchConfigService.getConfig();
        List<MomentEnrollment> enrollments = listEnrollments(weekTag);
        Set<Long> participantIds = enrollments.stream()
                .map(MomentEnrollment::getUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<MomentMatchResult> results = listMatchResults(weekTag);
        List<MomentPairScore> pairScores = listPairScores(weekTag);
        List<UnmatchedUserResponse> unmatchedUsers = getUnmatchedUsers(weekTag);

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
        List<HistogramBucket> unmatchedBestHistogram = bucketScores(
                unmatchedUsers.stream()
                        .map(UnmatchedUserResponse::highestAvailableScore)
                        .filter(Objects::nonNull)
                        .toList()
        );

        long filteredPairCount = pairScores.stream()
                .filter(score -> Boolean.TRUE.equals(score.getHardFilterPassed()))
                .filter(score -> !Boolean.TRUE.equals(score.getIncludedByThreshold()))
                .count();

        List<PoolStat> poolStats = buildPoolStats(enrollments, results);
        List<ReasonStat> hardFilterStats = groupReasons(
                pairScores.stream()
                        .filter(score -> !Boolean.TRUE.equals(score.getHardFilterPassed()))
                        .map(MomentPairScore::getHardFilterReason)
                        .filter(Objects::nonNull)
                        .toList()
        );
        List<ReasonStat> softPenaltyStats = groupReasons(
                pairScores.stream()
                        .filter(score -> score.getSoftPenalty() != null && score.getSoftPenalty() > 0)
                        .map(MomentPairScore::getSoftPenaltyReason)
                        .filter(Objects::nonNull)
                        .toList()
        );
        List<ReasonStat> unmatchedReasonStats = groupReasons(
                unmatchedUsers.stream()
                        .map(UnmatchedUserResponse::reason)
                        .filter(Objects::nonNull)
                        .toList()
        );
        List<FilteredPairSample> filteredPairSamples = pairScores.stream()
                .filter(score -> Boolean.TRUE.equals(score.getHardFilterPassed()))
                .filter(score -> !Boolean.TRUE.equals(score.getIncludedByThreshold()))
                .sorted(Comparator.comparing(MomentPairScore::getScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(20)
                .map(score -> new FilteredPairSample(
                        score.getPool(),
                        score.getUserIdA(),
                        score.getUserIdB(),
                        score.getScore() != null ? score.getScore().doubleValue() : 0d,
                        score.getThresholdRequired()
                ))
                .toList();

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
                filteredPairSamples
        );
    }

    public List<UnmatchedUserResponse> getUnmatchedUsers(String weekTag) {
        List<MomentEnrollment> enrollments = listEnrollments(weekTag);
        if (enrollments.isEmpty()) {
            return List.of();
        }
        Set<Long> matchedUsers = matchedUserIds(listMatchResults(weekTag));
        List<MomentPairScore> pairScores = listPairScores(weekTag);

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
            List<MomentPairScore> userPoolScores = pairScores.stream()
                    .filter(score -> Objects.equals(score.getPool(), enrollment.getPool()))
                    .filter(score -> Objects.equals(score.getUserIdA(), userId) || Objects.equals(score.getUserIdB(), userId))
                    .toList();
            Double highestAvailableScore = userPoolScores.stream()
                    .filter(score -> Boolean.TRUE.equals(score.getHardFilterPassed()))
                    .map(MomentPairScore::getScore)
                    .filter(Objects::nonNull)
                    .map(BigDecimal::doubleValue)
                    .max(Double::compareTo)
                    .map(this::round)
                    .orElse(null);

            String reason;
            if (userPoolScores.isEmpty()) {
                reason = poolParticipants.getOrDefault(enrollment.getPool(), 0L) <= 1 ? "池内人数不足" : "无候选对";
            } else if (userPoolScores.stream().noneMatch(score -> Boolean.TRUE.equals(score.getHardFilterPassed()))) {
                reason = "无通过硬筛选候选";
            } else if (userPoolScores.stream().noneMatch(score -> Boolean.TRUE.equals(score.getIncludedByThreshold()))) {
                reason = "全部被阈值过滤";
            } else {
                reason = "全局最优未选中";
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

    public SimulationResponse simulate(String weekTag, int threshold) {
        List<MomentEnrollment> enrollments = listEnrollments(weekTag);
        if (enrollments.isEmpty()) {
            return new SimulationResponse(weekTag, threshold, 0, 0, 0, 0d, 0);
        }

        Map<String, Set<Long>> poolUsers = enrollments.stream()
                .collect(Collectors.groupingBy(MomentEnrollment::getPool, LinkedHashMap::new,
                        Collectors.mapping(MomentEnrollment::getUserId, Collectors.toCollection(LinkedHashSet::new))));
        Map<String, List<MomentPairScore>> poolScores = listPairScores(weekTag).stream()
                .collect(Collectors.groupingBy(MomentPairScore::getPool, LinkedHashMap::new, Collectors.toList()));
        Map<Long, User> users = loadUsers(enrollments.stream().map(MomentEnrollment::getUserId).collect(Collectors.toSet()));

        Set<Long> globallyMatched = new HashSet<>();
        int matchedPairs = 0;
        for (String pool : matcher.poolOrder()) {
            Set<Long> activeUsers = new LinkedHashSet<>(poolUsers.getOrDefault(pool, Set.of()));
            activeUsers.removeAll(globallyMatched);
            if (activeUsers.size() < 2) {
                continue;
            }
            List<MomentPairScore> eligible = poolScores.getOrDefault(pool, List.of()).stream()
                    .filter(score -> Boolean.TRUE.equals(score.getHardFilterPassed()))
                    .filter(score -> activeUsers.contains(score.getUserIdA()) && activeUsers.contains(score.getUserIdB()))
                    .filter(score -> score.getScore() != null && score.getScore().doubleValue() >= requiredThreshold(score, threshold))
                    .toList();
            Set<String> matchedPairsInPool = "MF".equals(pool)
                    ? solveBipartite(eligible, users)
                    : solveGeneralGraph(eligible);
            matchedPairs += matchedPairsInPool.size();
            for (String key : matchedPairsInPool) {
                String[] parts = key.split("_");
                globallyMatched.add(Long.parseLong(parts[0]));
                globallyMatched.add(Long.parseLong(parts[1]));
            }
        }

        int participantCount = (int) enrollments.stream().map(MomentEnrollment::getUserId).distinct().count();
        int matchedUsers = globallyMatched.size();
        int unmatchedUsers = Math.max(0, participantCount - matchedUsers);
        int currentPairs = listMatchResults(weekTag).size();
        double successRate = participantCount == 0 ? 0d : round(matchedUsers * 100d / participantCount);

        return new SimulationResponse(
                weekTag,
                threshold,
                matchedPairs,
                matchedUsers,
                unmatchedUsers,
                successRate,
                matchedPairs - currentPairs
        );
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
        int[] boundaries = {0, 40, 50, 60, 70, 80, 90, 101};
        Map<String, Long> buckets = new LinkedHashMap<>();
        for (int i = 0; i < boundaries.length - 1; i++) {
            int start = boundaries[i];
            int end = boundaries[i + 1] - 1;
            buckets.put(start + "-" + end, 0L);
        }
        for (Double score : scores) {
            if (score == null) {
                continue;
            }
            int rounded = Math.max(0, Math.min(100, (int) Math.floor(score)));
            for (int i = 0; i < boundaries.length - 1; i++) {
                if (rounded >= boundaries[i] && rounded < boundaries[i + 1]) {
                    String label = boundaries[i] + "-" + (boundaries[i + 1] - 1);
                    buckets.computeIfPresent(label, (k, value) -> value + 1);
                    break;
                }
            }
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

    private List<MomentPairScore> listPairScores(String weekTag) {
        return pairScoreMapper.selectList(new LambdaQueryWrapper<MomentPairScore>()
                .eq(MomentPairScore::getWeekTag, weekTag));
    }

    private double requiredThreshold(MomentPairScore score, int baseThreshold) {
        int thresholdA = Math.max(0, baseThreshold - safeInt(score.getThresholdOffsetA()));
        int thresholdB = Math.max(0, baseThreshold - safeInt(score.getThresholdOffsetB()));
        return Math.max(thresholdA, thresholdB);
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private Set<String> solveBipartite(List<MomentPairScore> scores, Map<Long, User> users) {
        if (scores.isEmpty()) {
            return Set.of();
        }
        SimpleWeightedGraph<Long, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        Set<Long> left = new LinkedHashSet<>();
        Set<Long> right = new LinkedHashSet<>();
        Map<DefaultWeightedEdge, String> edgeLookup = new HashMap<>();

        for (MomentPairScore score : scores) {
            graph.addVertex(score.getUserIdA());
            graph.addVertex(score.getUserIdB());
            User userA = users.get(score.getUserIdA());
            User userB = users.get(score.getUserIdB());
            if (userA != null && userA.getGender() != null && userA.getGender() == 1) {
                left.add(score.getUserIdA());
                right.add(score.getUserIdB());
            } else {
                left.add(score.getUserIdB());
                right.add(score.getUserIdA());
            }
            DefaultWeightedEdge edge = graph.addEdge(score.getUserIdA(), score.getUserIdB());
            if (edge != null) {
                graph.setEdgeWeight(edge, score.getScore().doubleValue());
                edgeLookup.put(edge, pairKey(score.getUserIdA(), score.getUserIdB()));
            }
        }

        MatchingAlgorithm.Matching<Long, DefaultWeightedEdge> matching =
                new MaximumWeightBipartiteMatching<>(graph, left, right).getMatching();
        return matching.getEdges().stream()
                .map(edgeLookup::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> solveGeneralGraph(List<MomentPairScore> scores) {
        if (scores.isEmpty()) {
            return Set.of();
        }
        SimpleWeightedGraph<Long, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        Map<DefaultWeightedEdge, String> edgeLookup = new HashMap<>();
        for (MomentPairScore score : scores) {
            graph.addVertex(score.getUserIdA());
            graph.addVertex(score.getUserIdB());
            DefaultWeightedEdge edge = graph.addEdge(score.getUserIdA(), score.getUserIdB());
            if (edge != null) {
                graph.setEdgeWeight(edge, score.getScore().doubleValue());
                edgeLookup.put(edge, pairKey(score.getUserIdA(), score.getUserIdB()));
            }
        }

        MatchingAlgorithm.Matching<Long, DefaultWeightedEdge> matching =
                new KolmogorovWeightedMatching<>(graph, ObjectiveSense.MAXIMIZE).getMatching();
        return matching.getEdges().stream()
                .map(edgeLookup::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String pairKey(Long a, Long b) {
        long left = Math.min(a, b);
        long right = Math.max(a, b);
        return left + "_" + right;
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
            List<FilteredPairSample> filteredPairSamples
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
}
