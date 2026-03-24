package com.campus.love.moment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.love.moment.entity.MomentAiAnalysisTask;
import com.campus.love.moment.entity.MomentEnrollment;
import com.campus.love.moment.entity.MomentMatchConfig;
import com.campus.love.moment.entity.MomentMatchConfirm;
import com.campus.love.moment.entity.MomentMatchResult;
import com.campus.love.moment.entity.MomentMatchResultContent;
import com.campus.love.moment.entity.MomentPairScore;
import com.campus.love.moment.entity.MomentProfile;
import com.campus.love.moment.entity.MomentRejectSummary;
import com.campus.love.moment.entity.MomentUserPoolBest;
import com.campus.love.moment.mapper.MomentAiAnalysisTaskMapper;
import com.campus.love.moment.mapper.MomentEnrollmentMapper;
import com.campus.love.moment.mapper.MomentMatchConfirmMapper;
import com.campus.love.moment.mapper.MomentMatchResultContentMapper;
import com.campus.love.moment.mapper.MomentMatchResultMapper;
import com.campus.love.moment.mapper.MomentPairScoreMapper;
import com.campus.love.moment.mapper.MomentProfileMapper;
import com.campus.love.moment.mapper.MomentRejectSummaryMapper;
import com.campus.love.moment.mapper.MomentUserPoolBestMapper;
import com.campus.love.profile.entity.UserPortrait;
import com.campus.love.profile.service.UserPortraitService;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * 异步匹配流水线：Matcher + 分级落库 + AI 任务入队。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MomentMatchPipelineService {

    private static final int PAIR_SCORE_BATCH = 500;

    private final MomentMatcher matcher;
    private final MomentMatchConfigService matchConfigService;
    private final MomentResultContentService momentResultContentService;
    private final ObjectMapper objectMapper;
    private final MomentActivityWeekService activityWeekService;
    private final MomentWeekDataService momentWeekDataService;
    private final MomentMatchProgressRegistry progressRegistry;
    private final ExecutorService momentMatchExecutor;
    private final TransactionTemplate transactionTemplate;

    private final MomentEnrollmentMapper enrollmentMapper;
    private final MomentMatchResultMapper matchResultMapper;
    private final MomentMatchResultContentMapper matchResultContentMapper;
    private final MomentMatchConfirmMapper matchConfirmMapper;
    private final MomentPairScoreMapper pairScoreMapper;
    private final MomentRejectSummaryMapper rejectSummaryMapper;
    private final MomentUserPoolBestMapper userPoolBestMapper;
    private final MomentAiAnalysisTaskMapper aiAnalysisTaskMapper;
    private final MomentProfileMapper profileMapper;
    private final UserMapper userMapper;
    private final UserPortraitService userPortraitService;

    public void submitMatchJob(String weekTag, Long operatorId, boolean autoTriggered) {
        momentMatchExecutor.submit(() -> {
            try {
                runPipeline(weekTag, operatorId, autoTriggered);
            } catch (Exception e) {
                log.error("心动时刻匹配流水线失败 weekTag={}", weekTag, e);
                transactionTemplate.executeWithoutResult(status -> {
                    momentWeekDataService.deletePipelineDataForWeek(weekTag);
                    enrollmentMapper.update(
                            null,
                            new LambdaUpdateWrapper<MomentEnrollment>()
                                    .eq(MomentEnrollment::getWeekTag, weekTag)
                                    .set(MomentEnrollment::getStatus, MomentEnrollment.STATUS_WAITING)
                    );
                    activityWeekService.markFailed(weekTag, e.getMessage() != null ? e.getMessage() : "UNKNOWN");
                });
            } finally {
                progressRegistry.clear();
            }
        });
    }

    private void runPipeline(String weekTag, Long operatorId, boolean autoTriggered) {
        List<MomentEnrollment> enrollments = enrollmentMapper.selectList(
                new LambdaQueryWrapper<MomentEnrollment>()
                        .eq(MomentEnrollment::getWeekTag, weekTag)
                        .eq(MomentEnrollment::getStatus, MomentEnrollment.STATUS_WAITING)
        );
        if (enrollments.isEmpty()) {
            return;
        }

        Map<Long, User> userCache = new HashMap<>();
        Map<Long, MomentProfile> profileCache = new HashMap<>();
        Map<Long, UserPortrait> portraitCache = new HashMap<>();
        Map<String, List<MomentMatcher.Candidate>> poolCandidates = new LinkedHashMap<>();

        for (MomentEnrollment enrollment : enrollments) {
            Long userId = enrollment.getUserId();
            User user = userCache.computeIfAbsent(userId, userMapper::selectById);
            MomentProfile profile = profileCache.computeIfAbsent(userId, id -> profileMapper.selectOne(
                    new LambdaQueryWrapper<MomentProfile>().eq(MomentProfile::getUserId, id).last("limit 1")
            ));
            UserPortrait portrait = portraitCache.computeIfAbsent(userId, userPortraitService::getPortrait);
            if (user == null || profile == null) {
                continue;
            }
            poolCandidates.computeIfAbsent(enrollment.getPool(), key -> new ArrayList<>())
                    .add(new MomentMatcher.Candidate(user, profile, portrait));
        }

        MomentMatcher.MatchProgressContext progressCtx = new MomentMatcher.MatchProgressContext();
        progressRegistry.start(weekTag, progressCtx, 0L);

        transactionTemplate.executeWithoutResult(status -> momentWeekDataService.deletePipelineDataForWeek(weekTag));

        MomentMatchConfig config = matchConfigService.getConfig();
        Set<Long> globalMatchedUserIds = new HashSet<>();
        int totalMatchedPairs = 0;
        List<Long> allMatchResultIds = new ArrayList<>();

        for (String pool : matcher.poolOrder()) {
            List<MomentMatcher.Candidate> candidates = poolCandidates.getOrDefault(pool, List.of()).stream()
                    .filter(candidate -> !globalMatchedUserIds.contains(candidate.user().getId()))
                    .toList();
            progressRegistry.setCurrentPool(pool);
            progressRegistry.addEstimatedPairs(estimatePairCount(candidates.size()));
            log.info("心动时刻开始处理池 {}: candidates={}", pool, candidates.size());
            if (candidates.size() < 2) {
                log.info("心动时刻跳过池 {}: 可匹配人数不足", pool);
                continue;
            }

            Tier3Aggregator t3 = new Tier3Aggregator();
            Map<Long, UserPoolTracker> userTrack = new HashMap<>();
            for (MomentMatcher.Candidate c : candidates) {
                userTrack.putIfAbsent(c.user().getId(), new UserPoolTracker());
            }

            MomentMatcher.MomentEvaluationCallback callback = new MomentMatcher.MomentEvaluationCallback() {
                @Override
                public void onEligible(long userIdA, long userIdB, double score) {
                    userTrack.computeIfAbsent(userIdA, id -> new UserPoolTracker()).noteEligible(score);
                    userTrack.computeIfAbsent(userIdB, id -> new UserPoolTracker()).noteEligible(score);
                }

                @Override
                public void onHardFiltered(long userIdA, long userIdB, String reason) {
                    t3.hardFilterCount++;
                    if (reason != null) {
                        t3.hardReason.merge(reason, 1L, Long::sum);
                    }
                }

                @Override
                public void onBelowThreshold(long userIdA, long userIdB, double score, String softPenaltyReason) {
                    t3.belowThresholdCount++;
                    int b = scoreBucket(score);
                    if (b >= 0 && b < t3.scoreBuckets.length) {
                        t3.scoreBuckets[b]++;
                    }
                    String r = softPenaltyReason != null ? softPenaltyReason : "UNKNOWN";
                    t3.softReason.merge(r, 1L, Long::sum);
                }
            };

            MomentMatcher.PoolMatchResult poolResult = matcher.match(candidates, pool, config, progressCtx, callback);
            int poolN = candidates.size();
            int topK = resolveEligibleTopK(config);
            MomentMatcher.PoolMatchDiagnostics diag = poolResult.diagnostics();
            if (diag != null) {
                double avgDegree = poolN > 0 ? (2.0 * diag.eligibleRawCount() / poolN) : 0d;
                log.info(
                        "momentMatchPoolTopK weekTag={} pool={} n={} K={} eligibleRaw={} avgDegree={} mergedEdges={} matchedPairs={} totalWeight={} maxUserRawDeg={} truncatedUsers={}",
                        weekTag,
                        pool,
                        poolN,
                        topK,
                        diag.eligibleRawCount(),
                        round1(avgDegree),
                        diag.mergedGraphEdgeCount(),
                        poolResult.matches().size(),
                        round1(diag.totalMatchingWeightSum()),
                        diag.maxUserRawEligibleDegree(),
                        diag.truncatedUserCount()
                );
            }
            List<MomentMatcher.MatchPair> pairs = poolResult.matches();
            Set<String> matchedPairKeys = new HashSet<>();
            Set<Long> matchedUserIds = new HashSet<>();

            for (MomentMatcher.MatchPair pair : pairs) {
                long userIdA = Math.min(pair.userIdA(), pair.userIdB());
                long userIdB = Math.max(pair.userIdA(), pair.userIdB());
                matchedUserIds.add(userIdA);
                matchedUserIds.add(userIdB);
                matchedPairKeys.add(pairKey(userIdA, userIdB));
                totalMatchedPairs++;
            }

            List<MomentMatcher.EligiblePairView> losers = poolResult.eligibleEdges().stream()
                    .filter(e -> !matchedPairKeys.contains(pairKey(e.userIdA(), e.userIdB())))
                    .toList();

            List<Long> poolMatchResultIds = new ArrayList<>();
            transactionTemplate.executeWithoutResult(status -> {
                for (MomentMatcher.MatchPair pair : pairs) {
                    long userIdA = Math.min(pair.userIdA(), pair.userIdB());
                    long userIdB = Math.max(pair.userIdA(), pair.userIdB());
                    User userA = userCache.get(userIdA);
                    User userB = userCache.get(userIdB);
                    MomentProfile profileA = profileCache.get(userIdA);
                    MomentProfile profileB = profileCache.get(userIdB);
                    UserPortrait portraitA = portraitCache.get(userIdA);
                    UserPortrait portraitB = portraitCache.get(userIdB);

                    MomentMatchResult result = new MomentMatchResult();
                    result.setWeekTag(weekTag);
                    result.setPool(pool);
                    result.setUserIdA(userIdA);
                    result.setUserIdB(userIdB);
                    result.setTotalScore(BigDecimal.valueOf(pair.totalScore()));
                    matchResultMapper.insert(result);

                    MomentMatchResultContent content = new MomentMatchResultContent();
                    content.setMatchResultId(result.getId());
                    try {
                        content.setScoreDetail(objectMapper.writeValueAsString(pair.scoreDetail()));
                    } catch (Exception ex) {
                        log.warn("序列化 score_detail 失败", ex);
                    }
                    momentResultContentService.fillRuleBasedContent(
                            content, userA, userB, profileA, profileB, portraitA, portraitB, pair.scoreDetail());
                    matchResultContentMapper.insert(content);

                    MomentMatchConfirm confirm = new MomentMatchConfirm();
                    confirm.setMatchResultId(result.getId());
                    confirm.setUserIdA(userIdA);
                    confirm.setUserIdB(userIdB);
                    matchConfirmMapper.insert(confirm);

                    poolMatchResultIds.add(result.getId());
                }
                flushRejectSummary(weekTag, pool, t3);
                flushUserPoolBest(weekTag, pool, userTrack, config);
            });

            allMatchResultIds.addAll(poolMatchResultIds);
            globalMatchedUserIds.addAll(matchedUserIds);
            progressRegistry.setMatchedPairsSoFar(totalMatchedPairs);

            log.info(
                    "池子 {} 完成: {} 对匹配, {} 条 loser 边（未落库，仅保留聚合统计）",
                    pool,
                    pairs.size(),
                    losers.size()
            );
        }

        progressRegistry.setCurrentPool(allMatchResultIds.isEmpty() ? "" : "AI");

        transactionTemplate.executeWithoutResult(status -> {
            activityWeekService.markAiAnalyzing(weekTag);
            if (!allMatchResultIds.isEmpty()) {
                for (Long id : allMatchResultIds) {
                    MomentAiAnalysisTask task = new MomentAiAnalysisTask();
                    task.setWeekTag(weekTag);
                    task.setMatchResultId(id);
                    task.setStatus(MomentAiAnalysisTask.STATUS_PENDING);
                    task.setRetryCount(0);
                    aiAnalysisTaskMapper.insert(task);
                }
            }
        });

        Set<Long> uniqueUserIds = enrollments.stream()
                .map(MomentEnrollment::getUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        int maxStack = config.getPriorityMaxStack() != null ? config.getPriorityMaxStack() : MomentMatchConfig.DEFAULT_PRIORITY_MAX_STACK;

        transactionTemplate.executeWithoutResult(status -> {
            int unmatchedUsers = 0;
            for (Long userId : uniqueUserIds) {
                boolean matched = globalMatchedUserIds.contains(userId);
                enrollmentMapper.update(null, new LambdaUpdateWrapper<MomentEnrollment>()
                        .eq(MomentEnrollment::getUserId, userId)
                        .eq(MomentEnrollment::getWeekTag, weekTag)
                        .set(MomentEnrollment::getStatus, matched ? MomentEnrollment.STATUS_MATCHED : MomentEnrollment.STATUS_UNMATCHED));
                User user = userCache.get(userId);
                if (user != null) {
                    user.setMomentPriorityCount(matched
                            ? 0
                            : Math.min(user.getMomentPriorityCountOrDefault() + 1, maxStack));
                    userMapper.updateById(user);
                }
                if (!matched) {
                    unmatchedUsers++;
                }
            }
            if (allMatchResultIds.isEmpty()) {
                activityWeekService.markResultReady(weekTag);
            }
        });

        log.info("心动时刻匹配流水线结束 weekTag={}, pairs={}, aiTasks={}", weekTag, totalMatchedPairs, allMatchResultIds.size());
    }

    private static long estimatePairCount(int candidateCount) {
        return candidateCount < 2 ? 0L : (long) candidateCount * (candidateCount - 1) / 2;
    }

    private int flushTier2TopK(
            String weekTag,
            String pool,
            List<MomentMatcher.EligiblePairView> losers,
            Map<Long, UserPoolTracker> userTrack,
            MomentMatchConfig config
    ) {
        if (losers == null || losers.isEmpty()) {
            return 0;
        }
        int tier2Cap = resolveEligibleTopK(config);
        Map<Long, PriorityQueue<MomentPairScore>> heaps = new HashMap<>();

        for (MomentMatcher.EligiblePairView e : losers) {
            MomentPairScore row = new MomentPairScore();
            row.setWeekTag(weekTag);
            row.setPool(pool);
            row.setUserIdA(e.userIdA());
            row.setUserIdB(e.userIdB());
            row.setScore(BigDecimal.valueOf(e.score()));
            row.setHardFilterPassed(true);
            row.setIncludedByThreshold(true);
            row.setMatched(false);
            pushHeap(heaps, e.userIdA(), row, tier2Cap);
            pushHeap(heaps, e.userIdB(), row, tier2Cap);
        }

        Set<String> dedupe = new LinkedHashSet<>();
        List<MomentPairScore> buffer = new ArrayList<>();
        for (PriorityQueue<MomentPairScore> heap : heaps.values()) {
            for (MomentPairScore s : heap) {
                String pk = pairKey(s.getUserIdA(), s.getUserIdB());
                if (dedupe.add(pk)) {
                    buffer.add(s);
                    if (buffer.size() >= PAIR_SCORE_BATCH) {
                        flushPairScoreBuffer(buffer);
                        buffer.clear();
                    }
                }
            }
        }
        if (!buffer.isEmpty()) {
            flushPairScoreBuffer(buffer);
        }
        return dedupe.size();
    }

    private void pushHeap(
            Map<Long, PriorityQueue<MomentPairScore>> heaps,
            long userId,
            MomentPairScore row,
            int cap
    ) {
        PriorityQueue<MomentPairScore> pq = heaps.computeIfAbsent(
                userId,
                u -> new PriorityQueue<>(Comparator.comparing(
                        s -> s.getScore() != null ? s.getScore().doubleValue() : 0d
                ))
        );
        pq.offer(row);
        while (pq.size() > cap) {
            pq.poll();
        }
    }

    private void flushPairScoreBuffer(List<MomentPairScore> buffer) {
        if (buffer == null || buffer.isEmpty()) {
            return;
        }
        pairScoreMapper.batchInsert(buffer);
    }

    private void flushRejectSummary(String weekTag, String pool, Tier3Aggregator t3) {
        MomentRejectSummary row = new MomentRejectSummary();
        row.setWeekTag(weekTag);
        row.setPool(pool);
        row.setHardFilterCount(t3.hardFilterCount);
        row.setBelowThresholdCount(t3.belowThresholdCount);
        try {
            row.setHardFilterReasonDist(objectMapper.writeValueAsString(t3.hardReason));
            row.setScoreDistribution(objectMapper.writeValueAsString(t3.scoreBuckets));
            row.setSoftPenaltyReasonDist(objectMapper.writeValueAsString(t3.softReason));
        } catch (Exception e) {
            log.warn("reject_summary JSON 失败", e);
        }
        rejectSummaryMapper.insert(row);
    }

    private void flushUserPoolBest(
            String weekTag,
            String pool,
            Map<Long, UserPoolTracker> userTrack,
            MomentMatchConfig config
    ) {
        int k = resolveEligibleTopK(config);
        for (Map.Entry<Long, UserPoolTracker> en : userTrack.entrySet()) {
            MomentUserPoolBest row = new MomentUserPoolBest();
            row.setWeekTag(weekTag);
            row.setPool(pool);
            row.setUserId(en.getKey());
            UserPoolTracker t = en.getValue();
            row.setHasAnyEligible(t.rawEligibleDegree > 0);
            row.setMaxEligibleScore(t.maxScore >= 0 ? BigDecimal.valueOf(t.maxScore) : null);
            row.setTier2Truncated(t.rawEligibleDegree > k);
            userPoolBestMapper.insert(row);
        }
    }

    private static int resolveEligibleTopK(MomentMatchConfig config) {
        Integer v = config.getEligibleTopK();
        if (v == null || v < 1) {
            return MomentMatchConfig.DEFAULT_ELIGIBLE_TOP_K;
        }
        return Math.min(10_000, v);
    }

    private static double round1(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private static int scoreBucket(double score) {
        return (int) Math.min(10, Math.max(0, Math.floor(score / 10.0)));
    }

    private static String pairKey(long a, long b) {
        long min = Math.min(a, b);
        long max = Math.max(a, b);
        return min + ":" + max;
    }

    private static final class Tier3Aggregator {
        int hardFilterCount;
        int belowThresholdCount;
        final Map<String, Long> hardReason = new HashMap<>();
        final int[] scoreBuckets = new int[11];
        final Map<String, Long> softReason = new HashMap<>();
    }

    private static final class UserPoolTracker {
        double maxScore = -1;
        /** 扫描阶段该用户参与的 eligible 无向边次数（与 Matcher 内 per-user raw 一致） */
        int rawEligibleDegree;

        void noteEligible(double score) {
            rawEligibleDegree++;
            if (score > maxScore) {
                maxScore = score;
            }
        }
    }
}
