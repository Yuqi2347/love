package com.campus.love.moment.service;

import com.campus.love.common.utils.InterestTagConverter;
import com.campus.love.moment.entity.MomentMatchConfig;
import com.campus.love.moment.entity.MomentProfile;
import com.campus.love.profile.entity.UserPortrait;
import com.campus.love.user.entity.User;
import com.campus.love.profile.service.OceanConfidenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.alg.interfaces.MatchingAlgorithm;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;
import org.jgrapht.alg.matching.blossom.v5.KolmogorovWeightedMatching;
import org.jgrapht.alg.matching.blossom.v5.ObjectiveSense;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.util.SupplierUtil;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 心动时刻完整匹配器：硬筛选 → 五维得分 → 阈值过滤 → 最大权重匹配。
 *
 * <p>硬筛选（不进软分）：池内性别合法；性别期待双向覆盖；年龄双向覆盖（问卷 min/max 为相对本人岁差，与前端滑块一致）。
 *
 * <p>五维加权总分不做事后校准；有效门槛由 MomentMatchConfigService.calculateEffectiveThreshold 提供，
 * 为运营层个人动态筛选，仅决定是否进入 eligible，不参与分数变形。
 *
 * <p>年级偏好 gradeRangePreference 为问卷 2.4 的 A/B/C/D 枚举；normalizeGrade 仅解析 User.grade 等实际年级字符串。
 *
 * <p>降权题（核心价值观子分内各 2.5%）：idolRole、humanNatureView、emotionPriority、lifeGoalPriority。
 *
 * <p>事业心分：当前为双方 careerAmbitionPref 线性相近度；若未来增加 selfCareerAmbition 可改为双向覆盖。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MomentMatcher {

    /** 矩阵题或线性题任一侧选项缺失时的默认分（统一口径） */
    private static final double DEFAULT_MISSING_OPTION_SCORE = 70d;

    private static final double WEIGHT_CORE_VALUE = 0.32d;
    private static final double WEIGHT_LIFESTYLE = 0.22d;
    private static final double WEIGHT_PREFERENCE = 0.20d;
    private static final double WEIGHT_PERSONALITY = 0.18d;
    private static final double WEIGHT_INTEREST = 0.08d;

    private static final List<String> POOL_ORDER = List.of("MF", "FF", "MM");
    private static final Map<String, Integer> GRADE_ORDER = Map.ofEntries(
            Map.entry("大一", 1),
            Map.entry("大二", 2),
            Map.entry("大三", 3),
            Map.entry("大四", 4),
            Map.entry("大五", 5),
            Map.entry("研一", 7),
            Map.entry("研二", 8),
            Map.entry("研三", 9),
            Map.entry("博一", 10),
            Map.entry("博二", 11),
            Map.entry("博三", 12),
            Map.entry("博士", 10),
            Map.entry("毕业", 13)
    );

    // 社交风格矩阵
    private static final Map<String, Map<String, Integer>> SOCIAL_STYLE_MATRIX = Map.of(
        "A", Map.of("A", 100, "B", 82, "C", 52, "D", 78),
        "B", Map.of("A", 82, "B", 100, "C", 75, "D", 85),
        "C", Map.of("A", 52, "B", 75, "C", 100, "D", 80),
        "D", Map.of("A", 78, "B", 85, "C", 80, "D", 95)
    );

    // 生活节奏矩阵
    private static final Map<String, Map<String, Integer>> LIFE_RHYTHM_MATRIX = Map.of(
        "A", Map.of("A", 100, "B", 38, "C", 78, "D", 80),
        "B", Map.of("A", 38, "B", 100, "C", 78, "D", 80),
        "C", Map.of("A", 78, "B", 78, "C", 100, "D", 92),
        "D", Map.of("A", 80, "B", 80, "C", 92, "D", 100)
    );

    // 性格底色矩阵
    private static final Map<String, Map<String, Integer>> PERSONALITY_BASE_MATRIX = Map.of(
        "A", Map.of("A", 100, "B", 88, "C", 85, "D", 72),
        "B", Map.of("A", 88, "B", 100, "C", 82, "D", 68),
        "C", Map.of("A", 85, "B", 82, "C", 100, "D", 75),
        "D", Map.of("A", 72, "B", 68, "C", 75, "D", 100)
    );

    // 情绪表达习惯矩阵
    private static final Map<String, Map<String, Integer>> EMOTION_STYLE_MATRIX = Map.of(
        "A", Map.of("A", 100, "B", 78, "C", 45, "D", 82),
        "B", Map.of("A", 78, "B", 100, "C", 68, "D", 80),
        "C", Map.of("A", 45, "B", 68, "C", 100, "D", 65),
        "D", Map.of("A", 82, "B", 80, "C", 65, "D", 95)
    );

    // 校园重心矩阵
    private static final Map<String, Map<String, Integer>> CAMPUS_FOCUS_MATRIX = Map.of(
        "A", Map.of("A", 100, "B", 80, "C", 60, "D", 75),
        "B", Map.of("A", 80, "B", 100, "C", 78, "D", 82),
        "C", Map.of("A", 60, "B", 78, "C", 100, "D", 70),
        "D", Map.of("A", 75, "B", 82, "C", 70, "D", 100)
    );

    // 同居观矩阵
    private static final Map<String, Map<String, Integer>> COHABITATION_MATRIX = Map.of(
        "A", Map.of("A", 100, "B", 78, "C", 45, "D", 12),
        "B", Map.of("A", 78, "B", 100, "C", 68, "D", 35),
        "C", Map.of("A", 45, "B", 68, "C", 100, "D", 82),
        "D", Map.of("A", 12, "B", 35, "C", 82, "D", 100)
    );

    // 婚前性行为矩阵
    private static final Map<String, Map<String, Integer>> PREMARITAL_SEX_MATRIX = Map.of(
        "A", Map.of("A", 100, "B", 80, "C", 42, "D", 10),
        "B", Map.of("A", 80, "B", 100, "C", 65, "D", 32),
        "C", Map.of("A", 42, "B", 65, "C", 100, "D", 80),
        "D", Map.of("A", 10, "B", 32, "C", 80, "D", 100)
    );

    // 关系最重要的是矩阵
    private static final Map<String, Map<String, Integer>> RELATIONSHIP_IMPORTANCE_MATRIX = Map.of(
        "A", Map.of("A", 100, "B", 75, "C", 48, "D", 88),
        "B", Map.of("A", 75, "B", 100, "C", 70, "D", 78),
        "C", Map.of("A", 48, "B", 70, "C", 100, "D", 60),
        "D", Map.of("A", 88, "B", 78, "C", 60, "D", 100)
    );

    // 矛盾处理矩阵
    private static final Map<String, Map<String, Integer>> CONFLICT_STYLE_MATRIX = Map.of(
        "A", Map.of("A", 100, "B", 85, "C", 42, "D", 88),
        "B", Map.of("A", 85, "B", 100, "C", 60, "D", 90),
        "C", Map.of("A", 42, "B", 60, "C", 100, "D", 62),
        "D", Map.of("A", 88, "B", 90, "C", 62, "D", 100)
    );

    // 分手观矩阵
    private static final Map<String, Map<String, Integer>> BREAKUP_VIEW_MATRIX = Map.of(
        "A", Map.of("A", 100, "B", 80, "C", 82, "D", 65),
        "B", Map.of("A", 80, "B", 100, "C", 78, "D", 72),
        "C", Map.of("A", 82, "B", 78, "C", 100, "D", 68),
        "D", Map.of("A", 65, "B", 72, "C", 68, "D", 100)
    );

    private final MomentMatchConfigService matchConfigService;
    private final OceanConfidenceService oceanConfidenceService;

    public record Candidate(User user, MomentProfile profile, UserPortrait portrait) {}

    public record MatchPair(Long userIdA, Long userIdB, double totalScore, Map<String, Object> scoreDetail) {}

    /** 双层循环进度：每次 evaluatePair 完成 +1，由调用方传入，避免单例 Matcher 上的并发覆盖。 */
    public static final class MatchProgressContext {
        private final AtomicLong processedPairs = new AtomicLong();

        public void recordPairEvaluated() {
            processedPairs.incrementAndGet();
        }

        public long getProcessedPairs() {
            return processedPairs.get();
        }
    }

    /** Tier 回调：eligible 仅传分数，scoreDetail 由 Matcher 内部缓存直至图算法结束。 */
    public interface MomentEvaluationCallback {
        void onEligible(long userIdA, long userIdB, double score);

        void onHardFiltered(long userIdA, long userIdB, String reason);

        void onBelowThreshold(long userIdA, long userIdB, double score, String softPenaltyReason);
    }

    public record EligiblePairView(long userIdA, long userIdB, double score) {}

    /** 匹配过程可观测指标（Top-K 图边集与全量 eligible 计数） */
    public record PoolMatchDiagnostics(
            int eligibleRawCount,
            int mergedGraphEdgeCount,
            double totalMatchingWeightSum,
            int maxUserRawEligibleDegree,
            int truncatedUserCount
    ) {
        public static final PoolMatchDiagnostics EMPTY =
                new PoolMatchDiagnostics(0, 0, 0d, 0, 0);
    }

    public record PoolMatchResult(
            List<MatchPair> matches,
            List<EligiblePairView> eligibleEdges,
            int candidateCount,
            PoolMatchDiagnostics diagnostics
    ) {
        public PoolMatchResult(List<MatchPair> matches, List<EligiblePairView> eligibleEdges, int candidateCount) {
            this(matches, eligibleEdges, candidateCount, PoolMatchDiagnostics.EMPTY);
        }
    }

    public List<String> poolOrder() {
        return POOL_ORDER;
    }

    public PoolMatchResult match(
            List<Candidate> candidates,
            String pool,
            MomentMatchConfig config,
            MatchProgressContext progress,
            MomentEvaluationCallback callback
    ) {
        if (candidates == null || candidates.size() < 2) {
            int n = candidates == null ? 0 : candidates.size();
            return new PoolMatchResult(List.of(), List.of(), n, PoolMatchDiagnostics.EMPTY);
        }

        int k = resolveEligibleTopK(config);
        int n = candidates.size();
        MatchProgressContext ctx = progress != null ? progress : new MatchProgressContext();
        List<PriorityQueue<HeapEntry>> heaps = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            heaps.add(new PriorityQueue<>(Comparator.comparingDouble(HeapEntry::score)));
        }
        int[] perUserRawEligible = new int[n];
        int eligibleRawCount = 0;

        int pairCounter = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                Candidate a = candidates.get(i);
                Candidate b = candidates.get(j);
                long uidA = a.user().getId();
                long uidB = b.user().getId();
                PairComputation computation = evaluatePair(a, b, pool, config);
                ctx.recordPairEvaluated();
                pairCounter++;
                if (pairCounter % 20_000 == 0 && Thread.currentThread().isInterrupted()) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("心动时刻匹配任务被中断");
                }

                if (!computation.hardFilterPassed()) {
                    if (callback != null) {
                        callback.onHardFiltered(Math.min(uidA, uidB), Math.max(uidA, uidB), computation.hardFilterReason());
                    }
                    continue;
                }
                if (!computation.includedByThreshold() || computation.totalScore() <= 0) {
                    if (callback != null) {
                        callback.onBelowThreshold(
                                Math.min(uidA, uidB),
                                Math.max(uidA, uidB),
                                computation.totalScore(),
                                computation.softPenaltyReason()
                        );
                    }
                    continue;
                }

                eligibleRawCount++;
                double sc = computation.totalScore();
                perUserRawEligible[i]++;
                perUserRawEligible[j]++;
                offerTopK(heaps.get(i), k, j, sc);
                offerTopK(heaps.get(j), k, i, sc);
                if (callback != null) {
                    callback.onEligible(Math.min(uidA, uidB), Math.max(uidA, uidB), sc);
                }
            }
        }

        Map<Long, EligibleEdge> mergedByIdxPair = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            for (HeapEntry he : heaps.get(i)) {
                int ia = Math.min(i, he.otherIdx());
                int ib = Math.max(i, he.otherIdx());
                long pkey = idxPairKey(ia, ib);
                EligibleEdge cur = mergedByIdxPair.get(pkey);
                if (cur == null || he.score() > cur.score()) {
                    mergedByIdxPair.put(pkey, new EligibleEdge(ia, ib, he.score()));
                }
            }
        }
        List<EligibleEdge> graphEdges = new ArrayList<>(mergedByIdxPair.values());
        int mergedGraphEdgeCount = graphEdges.size();

        List<EligiblePairView> eligibleViews = new ArrayList<>(mergedGraphEdgeCount);
        for (EligibleEdge e : graphEdges) {
            Candidate ca = candidates.get(e.idxA());
            Candidate cb = candidates.get(e.idxB());
            long ua = ca.user().getId();
            long ub = cb.user().getId();
            eligibleViews.add(new EligiblePairView(Math.min(ua, ub), Math.max(ua, ub), round(e.score())));
        }

        List<MatchPair> matches = "MF".equals(pool)
                ? solveMaximumWeightBipartite(candidates, graphEdges, pool, config)
                : solveMaximumWeightGeneralGraph(candidates, graphEdges, pool, config);

        int maxUserRaw = 0;
        int truncatedUsers = 0;
        for (int v : perUserRawEligible) {
            if (v > maxUserRaw) {
                maxUserRaw = v;
            }
            if (v > k) {
                truncatedUsers++;
            }
        }
        double totalWeight = matches.stream().mapToDouble(MatchPair::totalScore).sum();

        PoolMatchDiagnostics diagnostics = new PoolMatchDiagnostics(
                eligibleRawCount,
                mergedGraphEdgeCount,
                totalWeight,
                maxUserRaw,
                truncatedUsers
        );

        log.info(
                "心动时刻池 {} 匹配完成: n={} K={} eligibleRaw={} mergedEdges={} matchedPairs={} totalWeight={} maxUserRawDeg={} truncatedUsers={}",
                pool,
                n,
                k,
                eligibleRawCount,
                mergedGraphEdgeCount,
                matches.size(),
                round(totalWeight),
                maxUserRaw,
                truncatedUsers
        );
        return new PoolMatchResult(matches, eligibleViews, n, diagnostics);
    }

    private static int resolveEligibleTopK(MomentMatchConfig config) {
        Integer v = config.getEligibleTopK();
        if (v == null || v < 1) {
            return MomentMatchConfig.DEFAULT_ELIGIBLE_TOP_K;
        }
        return Math.min(10_000, v);
    }

    private static void offerTopK(PriorityQueue<HeapEntry> pq, int k, int otherIdx, double score) {
        if (k <= 0) {
            return;
        }
        if (pq.size() < k) {
            pq.offer(new HeapEntry(otherIdx, score));
            return;
        }
        HeapEntry smallest = pq.peek();
        if (smallest != null && score > smallest.score()) {
            pq.poll();
            pq.offer(new HeapEntry(otherIdx, score));
        }
    }

    private static long idxPairKey(int idxA, int idxB) {
        int a = Math.min(idxA, idxB);
        int b = Math.max(idxA, idxB);
        return (((long) a) << 32) | (b & 0xffffffffL);
    }

    private static String pairKey(long userIdA, long userIdB) {
        long min = Math.min(userIdA, userIdB);
        long max = Math.max(userIdA, userIdB);
        return min + ":" + max;
    }

    private PairComputation evaluatePair(Candidate a, Candidate b, String pool, MomentMatchConfig config) {
        int thresholdRequired = matchConfigService.calculateEffectiveThreshold(a.user(), a.profile(), config);
        
        String hardFilterReason = detectHardFilterReason(a, b, pool);
        if (hardFilterReason != null) {
            return new PairComputation(
                    0d,
                    zeroScoreDetail(thresholdRequired, hardFilterReason),
                    false,
                    hardFilterReason,
                    0,
                    null,
                    0,
                    0,
                    thresholdRequired,
                    thresholdRequired,
                    thresholdRequired,
                    false
            );
        }

        // 计算五维度得分
        double coreValueScore = calcCoreValueScore(a.profile(), b.profile());
        double lifestyleScore = calcLifestyleScore(a.profile(), b.profile());
        double preferenceScore = calcPreferenceScore(a, b);
        double personalityScore = calcPersonalityScore(a.profile(), b.profile());
        double interestOceanScore = calcInterestOceanScore(a, b);

        // 加权总分
        double total = coreValueScore * WEIGHT_CORE_VALUE
                + lifestyleScore * WEIGHT_LIFESTYLE
                + preferenceScore * WEIGHT_PREFERENCE
                + personalityScore * WEIGHT_PERSONALITY
                + interestOceanScore * WEIGHT_INTEREST;

        Map<String, Object> scoreDetail = new LinkedHashMap<>();
        scoreDetail.put("coreValue", round(coreValueScore));
        scoreDetail.put("lifestyle", round(lifestyleScore));
        scoreDetail.put("preference", round(preferenceScore));
        scoreDetail.put("personality", round(personalityScore));
        scoreDetail.put("interestOcean", round(interestOceanScore));
        scoreDetail.put("coreValueWeighted", round(coreValueScore * WEIGHT_CORE_VALUE));
        scoreDetail.put("lifestyleWeighted", round(lifestyleScore * WEIGHT_LIFESTYLE));
        scoreDetail.put("preferenceWeighted", round(preferenceScore * WEIGHT_PREFERENCE));
        scoreDetail.put("personalityWeighted", round(personalityScore * WEIGHT_PERSONALITY));
        scoreDetail.put("interestOceanWeighted", round(interestOceanScore * WEIGHT_INTEREST));
        scoreDetail.put("thresholdRequired", thresholdRequired);
        scoreDetail.put("finalScore", round(total));

        return new PairComputation(
                round(total),
                scoreDetail,
                true,
                null,
                0,
                null,
                0,
                0,
                thresholdRequired,
                thresholdRequired,
                thresholdRequired,
                total >= thresholdRequired
        );
    }

    private String detectHardFilterReason(Candidate a, Candidate b, String pool) {
        User userA = a.user();
        User userB = b.user();
        MomentProfile profileA = a.profile();
        MomentProfile profileB = b.profile();

        if (!poolCompatible(pool, userA, userB)) {
            return "匹配池性别不满足";
        }
        if (!genderPreferenceMutualCover(profileA, profileB, userA, userB)) {
            return "性别偏好不满足双向覆盖";
        }
        if (!ageRangeMutualCover(profileA, profileB, userA, userB)) {
            return "年龄范围不满足双向覆盖";
        }

        return null;
    }

    private boolean poolCompatible(String pool, User a, User b) {
        if (a == null || b == null || a.getGender() == null || b.getGender() == null) {
            return false;
        }
        return switch (pool) {
            case "MF" -> !Objects.equals(a.getGender(), b.getGender());
            case "FF" -> a.getGender() == 2 && b.getGender() == 2;
            case "MM" -> a.getGender() == 1 && b.getGender() == 1;
            default -> false;
        };
    }

    private boolean genderPreferenceMutualCover(MomentProfile a, MomentProfile b, User userA, User userB) {
        boolean aCoversB = acceptsGender(a != null ? a.getTargetGender() : null, userB.getGender());
        boolean bCoversA = acceptsGender(b != null ? b.getTargetGender() : null, userA.getGender());
        return aCoversB && bCoversA;
    }

    /**
     * 年龄双向覆盖。问卷中的 agePreferenceMin/Max 与前端滑块一致，为相对<strong>本人</strong>周岁的偏移（通常 ∈ [-10,10]），
     * 而非绝对年龄；历史数据若 min/max 超出该区间则仍按绝对周岁理解。
     */
    private boolean ageRangeMutualCover(MomentProfile a, MomentProfile b, User userA, User userB) {
        if (userA == null || userB == null) {
            return false;
        }
        Integer ageA = userAgeYears(userA);
        Integer ageB = userAgeYears(userB);
        if (ageA == null || ageB == null) {
            return false;
        }
        boolean aAcceptedByB = partnerAgeAcceptedByRefPreference(ageA, ageB, b);
        boolean bAcceptedByA = partnerAgeAcceptedByRefPreference(ageB, ageA, a);
        return aAcceptedByB && bAcceptedByA;
    }

    /**
     * 判断对方年龄 {@code partnerAge} 是否落在 ref 问卷中声明的接受范围内。
     */
    private boolean partnerAgeAcceptedByRefPreference(Integer partnerAge, Integer refAge, MomentProfile ref) {
        Integer min = ref != null ? ref.getAgePreferenceMin() : null;
        Integer max = ref != null ? ref.getAgePreferenceMax() : null;
        if (min == null && max == null) {
            return true;
        }
        if (useRelativeAgePreferenceOffsets(min, max)) {
            int o1 = min != null ? min : -10;
            int o2 = max != null ? max : 10;
            int absLo = refAge + Math.min(o1, o2);
            int absHi = refAge + Math.max(o1, o2);
            return isAgeInRange(partnerAge, absLo, absHi);
        }
        return isAgeInRange(partnerAge, min, max);
    }

    /** 与 {@code MomentEnrollView} 滑块一致：岁差落在 [-10,10] 时按相对偏移；否则视为历史「绝对年龄」区间。 */
    private static boolean useRelativeAgePreferenceOffsets(Integer min, Integer max) {
        if (min == null || max == null) {
            return true;
        }
        return min >= -10 && min <= 10 && max >= -10 && max <= 10;
    }

    /** 由出生日期推算周岁，供年龄偏好硬筛使用 */
    private static Integer userAgeYears(User user) {
        if (user == null || user.getBirthDate() == null) {
            return null;
        }
        return Period.between(user.getBirthDate(), LocalDate.now()).getYears();
    }

    private boolean acceptsGender(String targetGender, Integer actualGender) {
        if (actualGender == null) {
            return false;
        }
        if (targetGender == null || targetGender.isBlank() || "any".equalsIgnoreCase(targetGender)) {
            return true;
        }
        if ("male".equalsIgnoreCase(targetGender)) {
            return actualGender == 1;
        }
        if ("female".equalsIgnoreCase(targetGender)) {
            return actualGender == 2;
        }
        return true;
    }

    private boolean isAgeInRange(Integer age, Integer min, Integer max) {
        if (age == null) {
            return false;
        }
        if (min == null && max == null) {
            return true;
        }
        if (min != null && age < min) {
            return false;
        }
        if (max != null && age > max) {
            return false;
        }
        return true;
    }

    private double calcPersonalityScore(MomentProfile a, MomentProfile b) {
        double socialStyleScore = matrixScore(a.getSocialStyle(), b.getSocialStyle(), SOCIAL_STYLE_MATRIX) * 0.25;
        double lifeRhythmScore = matrixScore(a.getLifeRhythm(), b.getLifeRhythm(), LIFE_RHYTHM_MATRIX) * 0.30;
        double personalityBaseScore = matrixScore(a.getPersonalityBase(), b.getPersonalityBase(), PERSONALITY_BASE_MATRIX) * 0.25;
        double emotionStyleScore = matrixScore(a.getEmotionStyle(), b.getEmotionStyle(), EMOTION_STYLE_MATRIX) * 0.20;
        
        return socialStyleScore + lifeRhythmScore + personalityBaseScore + emotionStyleScore;
    }

    private double calcPreferenceScore(Candidate a, Candidate b) {
        MomentProfile pa = a.profile();
        MomentProfile pb = b.profile();
        User ua = a.user();
        User ub = b.user();
        
        double appearanceScore = calcAppearanceMatchScore(pa, pb, ua, ub) * 0.20;
        double personalityExpectScore = calcPersonalityExpectScore(pa, pb) * 0.20;
        double gradeScore = calcGradePreferenceScore(pa, pb, ua, ub) * 0.15;
        double majorScore = calcMajorPreferenceScore(pa, pb, ua, ub) * 0.10;
        double careerScore = calcCareerAmbitionScore(pa, pb) * 0.15;
        double distanceScore = calcDistanceExpectationScore(pa.getIntimacyPace(), pb.getIntimacyPace()) * 0.20;
        
        return appearanceScore + personalityExpectScore + gradeScore + majorScore + careerScore + distanceScore;
    }

    private double calcAppearanceMatchScore(MomentProfile a, MomentProfile b, User userA, User userB) {
        // 获取A对B的覆盖分数
        int requireScoreA = getAppearanceThreshold(a.getAppearanceRequirement()); // 0,4,6,8,9
        int selfScoreB = userB.getMomentSelfScore() != null ? userB.getMomentSelfScore() : 5;
        
        double aCoversB = selfScoreB >= requireScoreA ? 100 : Math.max(10, 100 - (requireScoreA - selfScoreB) * 20);
        
        // 获取B对A的覆盖分数
        int requireScoreB = getAppearanceThreshold(b.getAppearanceRequirement());
        int selfScoreA = userA.getMomentSelfScore() != null ? userA.getMomentSelfScore() : 5;
        
        double bCoversA = selfScoreA >= requireScoreB ? 100 : Math.max(10, 100 - (requireScoreB - selfScoreA) * 20);
        
        return Math.min(aCoversB, bCoversA);
    }

    private int getAppearanceThreshold(String requirement) {
        if ("A".equals(requirement)) return 0; // 无要求
        if ("B".equals(requirement)) return 4; // 顺眼即可
        if ("C".equals(requirement)) return 6; // 中等以上
        if ("D".equals(requirement)) return 8; // 较高要求
        if ("E".equals(requirement)) return 9; // 很高要求
        return 0;
    }

    private double calcPersonalityExpectScore(MomentProfile a, MomentProfile b) {
        // A期待的性格 vs B实际性格
        double aCoversB = personalityMatchScore(a.getPartnerPersonality(), b.getPersonalityBase());
        // B期待的性格 vs A实际性格
        double bCoversA = personalityMatchScore(b.getPartnerPersonality(), a.getPersonalityBase());
        
        return Math.min(aCoversB, bCoversA);
    }

    private double personalityMatchScore(String expected, String actual) {
        if (expected == null || actual == null) return 85; // 默认"都可以"
        if (Objects.equals(expected, actual)) return 100; // 命中
        if ("D".equals(expected)) return 85; // 都可以
        
        // 相邻性格判断
        if (isAdjacentPersonality(expected, actual)) return 70;
        return 40; // 差距较大
    }

    private boolean isAdjacentPersonality(String expected, String actual) {
        // 定义相邻关系: A=理性冷静, B=感性热烈, C=外冷内热, D=佛系随缘
        if ("A".equals(expected) && "C".equals(actual)) return true; // 理性 vs 外冷内热
        if ("C".equals(expected) && "A".equals(actual)) return true;
        if ("B".equals(expected) && "C".equals(actual)) return true; // 感性 vs 外冷内热
        if ("C".equals(expected) && "B".equals(actual)) return true;
        return false;
    }

    private double calcGradePreferenceScore(MomentProfile a, MomentProfile b, User userA, User userB) {
        double aCoversB = gradePreferenceCoverScore(a.getGradeRangePreference(), userA.getGrade(), userB.getGrade());
        double bCoversA = gradePreferenceCoverScore(b.getGradeRangePreference(), userB.getGrade(), userA.getGrade());
        return Math.min(aCoversB, bCoversA);
    }

    /**
     * 问卷 2.4 年级偏好（A 学长学姐型 / B 同龄 / C 学弟学妹型 / D 无龄感）与「我的实际年级 + 对方实际年级」比对。
     * {@link #normalizeGrade} 仅用于解析实际年级（如 User.grade），不可传入 preference 枚举。
     */
    private double gradePreferenceCoverScore(String gradeRangePreference, String myGrade, String targetGrade) {
        if (gradeRangePreference == null || gradeRangePreference.isBlank()) {
            return 50d;
        }
        if ("D".equals(gradeRangePreference)) {
            return 90d;
        }
        Integer o = normalizeGrade(myGrade);
        Integer t = normalizeGrade(targetGrade);
        if (o == null || t == null) {
            return 50d;
        }
        int delta = t - o;
        return switch (gradeRangePreference) {
            case "A" -> scoreSeniorPreference(delta);
            case "B" -> scorePeerPreference(o, t);
            case "C" -> scoreJuniorPreference(delta);
            default -> 50d;
        };
    }

    /** A：希望对方为高 1～3 个年级序（含本硕衔接等较大跨度） */
    private static double scoreSeniorPreference(int delta) {
        if (delta >= 1 && delta <= 3) {
            return 100d;
        }
        if (delta > 3) {
            return 72d;
        }
        if (delta == 0) {
            return 45d;
        }
        return 20d;
    }

    /** C：希望对方为低 1～3 个年级序 */
    private static double scoreJuniorPreference(int delta) {
        if (delta <= -1 && delta >= -3) {
            return 100d;
        }
        if (delta < -3) {
            return 72d;
        }
        if (delta == 0) {
            return 45d;
        }
        return 20d;
    }

    /** B：同龄合拍 */
    private static double scorePeerPreference(int o, int t) {
        int ad = Math.abs(t - o);
        if (ad == 0) {
            return 100d;
        }
        if (ad == 1) {
            return 72d;
        }
        if (ad == 2) {
            return 45d;
        }
        return 20d;
    }

    private double calcMajorPreferenceScore(MomentProfile a, MomentProfile b, User userA, User userB) {
        if (isMajorPreferenceIndifferent(a.getMajorPreference()) || isMajorPreferenceIndifferent(b.getMajorPreference())) {
            return 85d;
        }
        boolean aMatchesB = majorPreferenceSatisfied(a.getMajorPreference(), userA.getMajor(), userB.getMajor());
        boolean bMatchesA = majorPreferenceSatisfied(b.getMajorPreference(), userB.getMajor(), userA.getMajor());
        if (aMatchesB && bMatchesA) {
            return 100d;
        }
        if (aMatchesB || bMatchesA) {
            return 75d;
        }
        return 50d;
    }

    private static boolean isMajorPreferenceIndifferent(String preference) {
        return preference == null || preference.isBlank()
                || "any".equalsIgnoreCase(preference)
                || "C".equalsIgnoreCase(preference);
    }

    /**
     * A=希望同专业；B=希望专业完全不同；C 由调用方作为「无所谓」处理。
     */
    private static boolean majorPreferenceSatisfied(String preference, String myMajor, String theirMajor) {
        if (preference == null || myMajor == null || theirMajor == null) {
            return false;
        }
        String m = myMajor.trim();
        String t = theirMajor.trim();
        if (m.isEmpty() || t.isEmpty()) {
            return false;
        }
        boolean same = m.equalsIgnoreCase(t);
        if ("A".equals(preference)) {
            return same;
        }
        if ("B".equals(preference)) {
            return !same;
        }
        return false;
    }

    /**
     * 双方 careerAmbitionPref 线性相近度（妥协方案：尚无 selfCareerAmbition 时无法做「期待 vs 对方实际」覆盖）。
     */
    private double calcCareerAmbitionScore(MomentProfile a, MomentProfile b) {
        Map<String, Integer> careerValues = Map.of("A", 1, "B", 2, "C", 3, "D", 4);
        Integer valA = careerValues.get(a.getCareerAmbitionPref());
        Integer valB = careerValues.get(b.getCareerAmbitionPref());
        if (valA == null || valB == null) {
            return DEFAULT_MISSING_OPTION_SCORE;
        }
        return linearScore(valA, valB, 3.0);
    }

    private double calcDistanceExpectationScore(String a, String b) {
        if (a == null || b == null) {
            return DEFAULT_MISSING_OPTION_SCORE;
        }
        // 选项: A 形影不离 / B 保持适当空间 / C 独立为主 / D 随缘（intimacyPace）
        Map<String, Double> values = Map.of("A", 4.0, "B", 3.0, "C", 2.0, "D", 2.5);
        double va = values.getOrDefault(a, 2.5);
        double vb = values.getOrDefault(b, 2.5);
        return linearScore(va, vb, 3.0);
    }

    private double calcLifestyleScore(MomentProfile a, MomentProfile b) {
        double campusFocusScore = matrixScore(a.getCampusFocus(), b.getCampusFocus(), CAMPUS_FOCUS_MATRIX) * 0.20;
        double campusLovePlanScore = calcCampusLovePlanScore(a.getCampusLovePlan(), b.getCampusLovePlan()) * 0.15;
        double cohabitationScore = matrixScore(a.getPremaritalCohabitation(), b.getPremaritalCohabitation(), COHABITATION_MATRIX) * 0.20;
        double premaritalSexScore = matrixScore(a.getPremaritalSex(), b.getPremaritalSex(), PREMARITAL_SEX_MATRIX) * 0.20;
        double companionshipScore = calcCompanionshipStyleScore(a.getCompanionshipStyle(), b.getCompanionshipStyle()) * 0.125;
        double dateStyleScore = calcDateStyleScore(a.getDateStyle(), b.getDateStyle()) * 0.125;
        
        return campusFocusScore + campusLovePlanScore + cohabitationScore + 
               premaritalSexScore + companionshipScore + dateStyleScore;
    }

    private double calcCampusLovePlanScore(String a, String b) {
        if (a == null || b == null) {
            return DEFAULT_MISSING_OPTION_SCORE;
        }
        // 选项: A 认真经营希望走到最后 / B 顺其自然 / C 体验为主 / D 暂时不考虑
        Map<String, Double> values = Map.of("A", 4.0, "B", 3.0, "C", 2.0, "D", 1.0);
        double va = values.getOrDefault(a, 2.5);
        double vb = values.getOrDefault(b, 2.5);
        return linearScore(va, vb, 3.0);
    }

    private double calcCompanionshipStyleScore(String a, String b) {
        if (a == null || b == null) {
            return DEFAULT_MISSING_OPTION_SCORE;
        }
        // 选项: A 随时在线高频互动 / B 每天固定联系 / C 各自空间为主 / D 看心情随缘
        Map<String, Double> values = Map.of("A", 4.0, "B", 3.0, "C", 2.0, "D", 2.5);
        double va = values.getOrDefault(a, 2.5);
        double vb = values.getOrDefault(b, 2.5);
        return linearScore(va, vb, 3.0);
    }

    private double calcDateStyleScore(String a, String b) {
        if (a == null || b == null) {
            return DEFAULT_MISSING_OPTION_SCORE;
        }
        // 选项: A 宅家居家 / B 附近探店 / C 出游探险 / D 都行
        Map<String, Double> values = Map.of("A", 4.0, "B", 3.0, "C", 2.0, "D", 2.5);
        double va = values.getOrDefault(a, 2.5);
        double vb = values.getOrDefault(b, 2.5);
        return linearScore(va, vb, 3.0);
    }

    private double calcCoreValueScore(MomentProfile a, MomentProfile b) {
        double relationshipImportanceScore = matrixScore(a.getRelationshipCoreValue(), b.getRelationshipCoreValue(), RELATIONSHIP_IMPORTANCE_MATRIX) * 0.16;
        double honestyScore = calcHonestyScore(a.getHonestyLevel(), b.getHonestyLevel()) * 0.13;
        double conflictStyleScore = matrixScore(a.getConflictStyle(), b.getConflictStyle(), CONFLICT_STYLE_MATRIX) * 0.12;
        double socialBoundaryScore = calcSocialBoundaryScore(a.getSocialBoundary(), b.getSocialBoundary()) * 0.11;
        double futureLifestyleScore = calcFutureLifestyleScore(a.getFutureLifestyle(), b.getFutureLifestyle()) * 0.10;
        double realityConditionScore = calcRealityConditionScore(a.getRealityCondition(), b.getRealityCondition()) * 0.10;
        double breakupViewScore = matrixScore(a.getBreakupView(), b.getBreakupView(), BREAKUP_VIEW_MATRIX) * 0.10;
        double careerLoveConflictScore = calcCareerLoveConflictScore(a.getCareerLoveConflict(), b.getCareerLoveConflict()) * 0.08;
        
        // 降权题
        double idolRoleScore = calcDowngradedQuestionScore(a.getIdolRole(), b.getIdolRole()) * 0.025; // 2.5% of total core value
        double humanNatureViewScore = calcDowngradedQuestionScore(a.getHumanNatureView(), b.getHumanNatureView()) * 0.025; // 2.5%
        double emotionPriorityScore = calcDowngradedQuestionScore(a.getEmotionPriority(), b.getEmotionPriority()) * 0.025; // 2.5%
        double lifeGoalPriorityScore = calcDowngradedQuestionScore(a.getLifeGoalPriority(), b.getLifeGoalPriority()) * 0.025; // 2.5%
        
        return relationshipImportanceScore + honestyScore + conflictStyleScore + socialBoundaryScore + 
               futureLifestyleScore + realityConditionScore + breakupViewScore + careerLoveConflictScore +
               idolRoleScore + humanNatureViewScore + emotionPriorityScore + lifeGoalPriorityScore;
    }

    private double calcHonestyScore(String a, String b) {
        if (a == null || b == null) {
            return DEFAULT_MISSING_OPTION_SCORE;
        }
        // 选项: A 完全坦诚 / B 保留隐私可接受 / C 各有秘密正常 / D 不在乎
        Map<String, Double> values = Map.of("A", 4.0, "B", 3.0, "C", 2.0, "D", 2.5);
        double va = values.getOrDefault(a, 2.5);
        double vb = values.getOrDefault(b, 2.5);
        return linearScore(va, vb, 3.0);
    }

    private double calcSocialBoundaryScore(String a, String b) {
        if (a == null || b == null) {
            return DEFAULT_MISSING_OPTION_SCORE;
        }
        // 选项: A 完全信任无边界 / B 保持适度边界 / C 严格边界 / D 看情况
        Map<String, Double> values = Map.of("A", 4.0, "B", 3.0, "C", 2.0, "D", 2.5);
        double va = values.getOrDefault(a, 2.5);
        double vb = values.getOrDefault(b, 2.5);
        return linearScore(va, vb, 3.0);
    }

    private double calcFutureLifestyleScore(String a, String b) {
        if (a == null || b == null) {
            return DEFAULT_MISSING_OPTION_SCORE;
        }
        // 选项: A 留在大城市 / B 回到家乡 / C 出国 / D 随缘
        Map<String, Double> values = Map.of("A", 4.0, "B", 3.0, "C", 2.0, "D", 2.5);
        double va = values.getOrDefault(a, 2.5);
        double vb = values.getOrDefault(b, 2.5);
        return linearScore(va, vb, 3.0);
    }

    private double calcRealityConditionScore(String a, String b) {
        if (a == null || b == null) {
            return DEFAULT_MISSING_OPTION_SCORE;
        }
        // 选项: A 完全不在意 / B 有点在意但不决定性 / C 比较在意 / D 现实优先
        Map<String, Double> values = Map.of("A", 4.0, "B", 3.0, "C", 2.0, "D", 1.0);
        double va = values.getOrDefault(a, 2.5);
        double vb = values.getOrDefault(b, 2.5);
        return linearScore(va, vb, 3.0);
    }

    private double calcCareerLoveConflictScore(String a, String b) {
        if (a == null || b == null) {
            return DEFAULT_MISSING_OPTION_SCORE;
        }
        // 选项: A 事业优先 / B 视情况 / C 感情优先 / D 平衡
        Map<String, Double> values = Map.of("A", 4.0, "B", 3.0, "C", 2.0, "D", 2.5);
        double va = values.getOrDefault(a, 2.5);
        double vb = values.getOrDefault(b, 2.5);
        return linearScore(va, vb, 3.0);
    }

    private double calcDowngradedQuestionScore(String a, String b) {
        // 相同给100，不同给72
        return Objects.equals(a, b) ? 100 : 72;
    }

    private double calcInterestOceanScore(Candidate a, Candidate b) {
        double interestScore = calcInterestScore(a.portrait(), b.portrait());
        boolean canUseOcean = a.portrait() != null
                && b.portrait() != null
                && Boolean.TRUE.equals(a.portrait().getHasRealOcean())
                && Boolean.TRUE.equals(b.portrait().getHasRealOcean());
        if (!canUseOcean) {
            return interestScore;
        }
        double oceanScore = calcOceanScore(a.portrait(), b.portrait());
        return interestScore * 0.75d + oceanScore * 0.25d;
    }

    private double calcInterestScore(UserPortrait a, UserPortrait b) {
        Set<String> tagsA = InterestTagConverter.extractCodesFromNewFormat(a != null ? a.getInterestTags() : null);
        Set<String> tagsB = InterestTagConverter.extractCodesFromNewFormat(b != null ? b.getInterestTags() : null);
        if (tagsA.isEmpty() && tagsB.isEmpty()) {
            return 45;
        }
        if (tagsA.isEmpty() || tagsB.isEmpty()) {
            return 30;
        }
        Set<String> union = new HashSet<>(tagsA);
        union.addAll(tagsB);
        Set<String> intersection = new HashSet<>(tagsA);
        intersection.retainAll(tagsB);
        if (union.isEmpty()) {
            return 45;
        }
        double jaccard = (intersection.size() * 1d) / union.size();
        return round(25d + 75d * jaccard);
    }

    private double calcOceanScore(UserPortrait a, UserPortrait b) {
        Map<String, BigDecimal> oceanA = oceanConfidenceService.getEffectiveOcean(a);
        Map<String, BigDecimal> oceanB = oceanConfidenceService.getEffectiveOcean(b);
        double diff = 0d;
        int count = 0;
        for (String dim : List.of("O", "C", "E", "A", "N")) {
            BigDecimal valueA = oceanA.get(dim);
            BigDecimal valueB = oceanB.get(dim);
            if (valueA == null || valueB == null) {
                continue;
            }
            diff += Math.abs(valueA.doubleValue() - valueB.doubleValue());
            count++;
        }
        if (count == 0) {
            return 45;
        }
        double similarity = 1d - (diff / count) / 100d;
        return round(20d + Math.max(0d, Math.min(1d, similarity)) * 80d);
    }

    /**
     * 线性距离：两端完全相同时为 100；相差 maxDiff 时约为 26（非 0），有意保留底线分，
     * 与矩阵题中强冲突组合（可低至 10）在语义上区分。
     */
    private double linearScore(double va, double vb, double maxDiff) {
        double diff = Math.abs(va - vb);
        return Math.max(0, Math.min(100, Math.round(100 - (diff / maxDiff) * 74)));
    }

    private double matrixScore(String a, String b, Map<String, Map<String, Integer>> matrix) {
        if (a == null || b == null) {
            return DEFAULT_MISSING_OPTION_SCORE;
        }
        Map<String, Integer> row = matrix.get(a);
        return row != null ? row.getOrDefault(b, (int) DEFAULT_MISSING_OPTION_SCORE) : DEFAULT_MISSING_OPTION_SCORE;
    }

    private Integer normalizeGrade(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        Integer direct = GRADE_ORDER.get(normalized);
        if (direct != null) {
            return direct;
        }
        if (normalized.startsWith("本科")) {
            return GRADE_ORDER.get(normalized.substring(2));
        }
        if (normalized.startsWith("硕士")) {
            return GRADE_ORDER.get("研" + normalized.substring(2));
        }
        if (normalized.startsWith("研究生")) {
            return GRADE_ORDER.get("研" + normalized.substring(3));
        }
        if (normalized.startsWith("博士")) {
            return GRADE_ORDER.get(normalized);
        }
        return null;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private List<MatchPair> solveMaximumWeightBipartite(
            List<Candidate> candidates,
            List<EligibleEdge> edges,
            String pool,
            MomentMatchConfig config
    ) {
        if (edges.isEmpty()) {
            return List.of();
        }
        // JGraphT 在内部复制图时会调用无参 addVertex()；使用业务顶点键 + 独立 aux supplier 避免与辅助顶点冲突。
        SimpleWeightedGraph<String, DefaultWeightedEdge> graph = newAuxSafeGraph();
        List<String> vertices = buildGraphVertexKeys(candidates.size());
        Set<String> left = new LinkedHashSet<>();
        Set<String> right = new LinkedHashSet<>();
        Map<DefaultWeightedEdge, EligibleEdge> edgeLookup = new HashMap<>();

        for (EligibleEdge edge : edges) {
            String vertexA = vertices.get(edge.idxA());
            String vertexB = vertices.get(edge.idxB());
            graph.addVertex(vertexA);
            graph.addVertex(vertexB);
            Integer genderA = candidates.get(edge.idxA()).user().getGender();
            Integer genderB = candidates.get(edge.idxB()).user().getGender();
            if (genderA != null && genderA == 1) {
                left.add(vertexA);
                right.add(vertexB);
            } else {
                left.add(vertexB);
                right.add(vertexA);
            }
            DefaultWeightedEdge graphEdge = graph.addEdge(vertexA, vertexB);
            if (graphEdge != null) {
                graph.setEdgeWeight(graphEdge, edge.score());
                edgeLookup.put(graphEdge, edge);
            }
        }

        MatchingAlgorithm.Matching<String, DefaultWeightedEdge> matching =
                new MaximumWeightBipartiteMatching<>(graph, left, right).getMatching();
        return toMatchPairs(candidates, matching.getEdges(), edgeLookup, pool, config);
    }

    private List<MatchPair> solveMaximumWeightGeneralGraph(
            List<Candidate> candidates,
            List<EligibleEdge> edges,
            String pool,
            MomentMatchConfig config
    ) {
        if (edges.isEmpty()) {
            return List.of();
        }
        SimpleWeightedGraph<String, DefaultWeightedEdge> graph = newAuxSafeGraph();
        List<String> vertices = buildGraphVertexKeys(candidates.size());
        Map<DefaultWeightedEdge, EligibleEdge> edgeLookup = new HashMap<>();
        for (EligibleEdge edge : edges) {
            String vertexA = vertices.get(edge.idxA());
            String vertexB = vertices.get(edge.idxB());
            graph.addVertex(vertexA);
            graph.addVertex(vertexB);
            DefaultWeightedEdge graphEdge = graph.addEdge(vertexA, vertexB);
            if (graphEdge != null) {
                graph.setEdgeWeight(graphEdge, edge.score());
                edgeLookup.put(graphEdge, edge);
            }
        }

        MatchingAlgorithm.Matching<String, DefaultWeightedEdge> matching =
                new KolmogorovWeightedMatching<>(graph, ObjectiveSense.MAXIMIZE).getMatching();
        return toMatchPairs(candidates, matching.getEdges(), edgeLookup, pool, config);
    }

    private static SimpleWeightedGraph<String, DefaultWeightedEdge> newAuxSafeGraph() {
        AtomicLong seq = new AtomicLong();
        return new SimpleWeightedGraph<>(
                () -> "__aux__" + seq.getAndIncrement(),
                SupplierUtil.createDefaultWeightedEdgeSupplier()
        );
    }

    private static List<String> buildGraphVertexKeys(int size) {
        List<String> keys = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            keys.add("candidate:" + i);
        }
        return keys;
    }

    private List<MatchPair> toMatchPairs(
            List<Candidate> candidates,
            Set<DefaultWeightedEdge> edges,
            Map<DefaultWeightedEdge, EligibleEdge> edgeLookup,
            String pool,
            MomentMatchConfig config
    ) {
        List<MatchPair> results = new ArrayList<>();
        for (DefaultWeightedEdge graphEdge : edges) {
            EligibleEdge eligible = edgeLookup.get(graphEdge);
            if (eligible == null) {
                continue;
            }
            Candidate a = candidates.get(eligible.idxA());
            Candidate b = candidates.get(eligible.idxB());
            PairComputation pc = evaluatePair(a, b, pool, config);
            Map<String, Object> detail = pc.scoreDetail() != null ? pc.scoreDetail() : Map.of();
            long idA = a.user().getId();
            long idB = b.user().getId();
            results.add(new MatchPair(
                    idA,
                    idB,
                    round(pc.totalScore()),
                    detail
            ));
        }
        return results;
    }

    private Map<String, Object> zeroScoreDetail(int thresholdRequired, String hardFilterReason) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("coreValue", 0d);
        detail.put("lifestyle", 0d);
        detail.put("preference", 0d);
        detail.put("personality", 0d);
        detail.put("interestOcean", 0d);
        detail.put("coreValueWeighted", 0d);
        detail.put("lifestyleWeighted", 0d);
        detail.put("preferenceWeighted", 0d);
        detail.put("personalityWeighted", 0d);
        detail.put("interestOceanWeighted", 0d);
        detail.put("thresholdRequired", thresholdRequired);
        detail.put("hardFilterReason", hardFilterReason);
        detail.put("finalScore", 0d);
        return detail;
    }

    private record PairComputation(
            double totalScore,
            Map<String, Object> scoreDetail,
            boolean hardFilterPassed,
            String hardFilterReason,
            int softPenalty,
            String softPenaltyReason,
            int thresholdOffsetA,
            int thresholdOffsetB,
            int effectiveThresholdA,
            int effectiveThresholdB,
            int thresholdRequired,
            boolean includedByThreshold
    ) {}

    private record EligibleEdge(int idxA, int idxB, double score) {}

    private record HeapEntry(int otherIdx, double score) {}
}
