package com.campus.love.moment.service;

import com.campus.love.common.utils.InterestTagConverter;
import com.campus.love.moment.entity.MomentMatchConfig;
import com.campus.love.moment.entity.MomentProfile;
import com.campus.love.profile.entity.UserPortrait;
import com.campus.love.profile.service.OceanConfidenceService;
import com.campus.love.user.entity.User;
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
import java.time.Period;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 心动时刻完整匹配器：
 * 硬筛选 -> 五维得分 -> 软惩罚 -> 阈值过滤 -> 最大权重匹配。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MomentMatcher {

    private static final double WEIGHT_VALUE = 0.35d;
    private static final double WEIGHT_RHYTHM = 0.27d;
    private static final double WEIGHT_PREFERENCE = 0.18d;
    private static final double WEIGHT_PERSONALITY = 0.12d;
    private static final double WEIGHT_INTEREST = 0.08d;
    /** 软惩罚折算系数：降低多项轻度差异的累计惩罚强度 */
    private static final double SOFT_PENALTY_MULTIPLIER = 0.55d;
    /** 软惩罚上限：避免轻度多项差异把总分“打穿” */
    private static final int SOFT_PENALTY_CAP = 18;

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

    private record HeapEntry(int otherIdx, double score) {}

    private static String pairKey(long userIdA, long userIdB) {
        long min = Math.min(userIdA, userIdB);
        long max = Math.max(userIdA, userIdB);
        return min + ":" + max;
    }

    private PairComputation evaluatePair(Candidate a, Candidate b, String pool, MomentMatchConfig config) {
        int thresholdOffsetA = matchConfigService.calculateThresholdOffset(a.user(), a.profile(), config);
        int thresholdOffsetB = matchConfigService.calculateThresholdOffset(b.user(), b.profile(), config);
        int effectiveThresholdA = matchConfigService.calculateEffectiveThreshold(a.user(), a.profile(), config);
        int effectiveThresholdB = matchConfigService.calculateEffectiveThreshold(b.user(), b.profile(), config);
        int thresholdRequired = Math.max(effectiveThresholdA, effectiveThresholdB);

        String hardFilterReason = detectHardFilterReason(a, b, pool);
        if (hardFilterReason != null) {
            return new PairComputation(
                    0d,
                    zeroScoreDetail(thresholdRequired, hardFilterReason),
                    false,
                    hardFilterReason,
                    0,
                    null,
                    thresholdOffsetA,
                    thresholdOffsetB,
                    effectiveThresholdA,
                    effectiveThresholdB,
                    thresholdRequired,
                    false
            );
        }

        MomentProfile pa = a.profile();
        MomentProfile pb = b.profile();

        double personalityScore = calcPersonalityScore(pa, pb);
        double preferenceScore = calcPreferenceScore(a, b);
        double lifestyleScore = calcLifestyleScore(pa, pb);
        double coreValueScore = calcCoreValueScore(pa, pb);
        double interestOceanScore = calcInterestOceanScore(a, b);

        List<Penalty> penalties = collectPenalties(pa, pb);
        Penalty penalty = summarizePenalty(penalties);
        double rawBaseScore =
                coreValueScore * WEIGHT_VALUE
                        + lifestyleScore * WEIGHT_RHYTHM
                        + preferenceScore * WEIGHT_PREFERENCE
                        + personalityScore * WEIGHT_PERSONALITY
                        + interestOceanScore * WEIGHT_INTEREST;
        double calibratedBaseScore = calibrateBaseScore(rawBaseScore);
        double total = calibratedBaseScore - penalty.score();
        total = Math.max(0d, total);

        Map<String, Object> scoreDetail = new LinkedHashMap<>();
        scoreDetail.put("personality", round(personalityScore));
        scoreDetail.put("preference", round(preferenceScore));
        scoreDetail.put("lifestyle", round(lifestyleScore));
        scoreDetail.put("coreValue", round(coreValueScore));
        scoreDetail.put("interestOcean", round(interestOceanScore));
        scoreDetail.put("personalityWeighted", round(personalityScore * WEIGHT_PERSONALITY));
        scoreDetail.put("preferenceWeighted", round(preferenceScore * WEIGHT_PREFERENCE));
        scoreDetail.put("lifestyleWeighted", round(lifestyleScore * WEIGHT_RHYTHM));
        scoreDetail.put("coreValueWeighted", round(coreValueScore * WEIGHT_VALUE));
        scoreDetail.put("interestOceanWeighted", round(interestOceanScore * WEIGHT_INTEREST));
        scoreDetail.put("rawBaseScore", round(rawBaseScore));
        scoreDetail.put("calibratedBaseScore", round(calibratedBaseScore));
        scoreDetail.put("softPenalty", penalty.score());
        scoreDetail.put("softPenaltyReason", penalty.reason());
        scoreDetail.put("softPenaltyReasons", penalties.stream().map(Penalty::reason).filter(Objects::nonNull).distinct().toList());
        scoreDetail.put("thresholdRequired", thresholdRequired);
        scoreDetail.put("finalScore", round(total));

        return new PairComputation(
                round(total),
                scoreDetail,
                true,
                null,
                penalty.score(),
                penalty.reason(),
                thresholdOffsetA,
                thresholdOffsetB,
                effectiveThresholdA,
                effectiveThresholdB,
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
        if (!acceptsGender(profileA != null ? profileA.getTargetGender() : null, userB.getGender())
                || !acceptsGender(profileB != null ? profileB.getTargetGender() : null, userA.getGender())) {
            return "性别偏好不匹配";
        }
        if (isConflict(profileA != null ? profileA.getPremaritalCohabitation() : null,
                profileB != null ? profileB.getPremaritalCohabitation() : null, "A", "C")) {
            return "婚前同居 A/C 冲突";
        }

        int sexA = premaritalSexToTier(profileA != null ? profileA.getPremaritalSex() : null);
        int sexB = premaritalSexToTier(profileB != null ? profileB.getPremaritalSex() : null);
        if (sexA > 0 && sexB > 0 && Math.abs(sexA - sexB) >= 3) {
            return "婚前性行为 A/D 冲突";
        }

        if (isConflict(profileA != null ? profileA.getFutureLifestyle() : null,
                profileB != null ? profileB.getFutureLifestyle() : null, "A", "B")) {
            return "未来生活方式 A/B 冲突";
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

    private boolean isConflict(String a, String b, String left, String right) {
        return (Objects.equals(a, left) && Objects.equals(b, right))
                || (Objects.equals(a, right) && Objects.equals(b, left));
    }

    private double calcPersonalityScore(MomentProfile a, MomentProfile b) {
        return MomentPersonalityMatrix.socialStyleScore(a.getSocialStyle(), b.getSocialStyle()) * 0.25
                + MomentPersonalityMatrix.personalityBaseScore(a.getPersonalityBase(), b.getPersonalityBase()) * 0.30
                + MomentPersonalityMatrix.emotionStyleScore(a.getEmotionStyle(), b.getEmotionStyle()) * 0.25
                + scoreCampusFocus(a.getCampusFocus(), b.getCampusFocus()) * 0.20;
    }

    private double calcPreferenceScore(Candidate a, Candidate b) {
        MomentProfile pa = a.profile();
        MomentProfile pb = b.profile();
        return averageAppearanceMatch(a.user(), b.user(), pa, pb) * 0.15
                + averagePartnerPersonalityMatch(pa, pb) * 0.20
                + calcMajorMatch(pa.getMajorPreference(), pb.getMajorPreference(), a.user().getMajor(), b.user().getMajor()) * 0.05
                + calcAgeCompatibility(pa, a.user(), pb, b.user()) * 0.30
                + calcGradeCompatibility(pa, a.user(), pb, b.user()) * 0.20
                + scorePreferenceChoice(pa.getCareerAmbitionPref(), pb.getCareerAmbitionPref()) * 0.10;
    }

    private double averageAppearanceMatch(User a, User b, MomentProfile pa, MomentProfile pb) {
        int selfScoreA = a.getMomentSelfScore() != null ? a.getMomentSelfScore() : 5;
        int selfScoreB = b.getMomentSelfScore() != null ? b.getMomentSelfScore() : 5;
        return (calcAppearanceMatch(pa.getAppearanceRequirement(), selfScoreB)
                + calcAppearanceMatch(pb.getAppearanceRequirement(), selfScoreA)) / 2d;
    }

    private double averagePartnerPersonalityMatch(MomentProfile a, MomentProfile b) {
        return (calcPersonalityExpect(a.getPartnerPersonality(), b.getPersonalityBase())
                + calcPersonalityExpect(b.getPartnerPersonality(), a.getPersonalityBase())) / 2d;
    }

    private double calcAppearanceMatch(String requirement, int targetSelfScore) {
        if ("C".equals(requirement)) {
            return 70;
        }
        if ("B".equals(requirement)) {
            return targetSelfScore >= 6 ? 85 : 55;
        }
        if ("A".equals(requirement)) {
            return targetSelfScore >= 7 ? 96 : (targetSelfScore >= 6 ? 52 : 28);
        }
        return 55;
    }

    private double calcPersonalityExpect(String expected, String actual) {
        if ("D".equals(expected)) {
            return 72;
        }
        if (expected == null || actual == null) {
            return 50;
        }
        if (Objects.equals(expected, actual)) {
            return 96;
        }
        if ("C".equals(expected) || "C".equals(actual)) {
            return 68;
        }
        return 38;
    }

    private double calcMajorMatch(String prefA, String prefB, String majorA, String majorB) {
        boolean sameMajor = majorA != null && majorB != null && majorA.equalsIgnoreCase(majorB);
        return (calcSingleMajorPref(prefA, sameMajor) + calcSingleMajorPref(prefB, sameMajor)) / 2d;
    }

    private double calcSingleMajorPref(String pref, boolean sameMajor) {
        if ("C".equals(pref)) {
            return 62;
        }
        if ("A".equals(pref)) {
            return sameMajor ? 88 : 32;
        }
        if ("B".equals(pref)) {
            return sameMajor ? 38 : 86;
        }
        return 50;
    }

    private double calcAgeCompatibility(MomentProfile a, User userA, MomentProfile b, User userB) {
        if (userA == null || userB == null) {
            return 42;
        }
        if (ageIsFullyOpen(a) && ageIsFullyOpen(b)) {
            return 62;
        }
        if (userA.getBirthDate() == null || userB.getBirthDate() == null) {
            return 42;
        }
        boolean ab = checkAgeCovered(a, userA, userB);
        boolean ba = checkAgeCovered(b, userB, userA);
        if (ab && ba) {
            return oneSideIsOpen(a, b, true) ? 80 : 94;
        }
        if (ab || ba) {
            return 50;
        }
        return 14;
    }

    private boolean checkAgeCovered(MomentProfile selfProfile, User self, User target) {
        if (selfProfile == null) {
            return true;
        }
        if (ageIsFullyOpen(selfProfile)) {
            return true;
        }
        if (self == null || target == null || self.getBirthDate() == null || target.getBirthDate() == null) {
            return false;
        }
        Integer min = selfProfile.getAgePreferenceMin();
        Integer max = selfProfile.getAgePreferenceMax();
        int ageDiff = Period.between(target.getBirthDate(), self.getBirthDate()).getYears();
        if (min != null && max != null) {
            return ageDiff >= min && ageDiff <= max;
        }
        String agePref = selfProfile.getAgeRangePreference();
        if (agePref == null || agePref.isBlank()) {
            return true;
        }
        for (String choice : agePref.split(",")) {
            switch (choice.trim()) {
                case "A" -> {
                    if (ageDiff >= 1 && ageDiff <= 2) {
                        return true;
                    }
                }
                case "B" -> {
                    if (Math.abs(ageDiff) <= 1) {
                        return true;
                    }
                }
                case "C" -> {
                    if (ageDiff >= -2 && ageDiff <= -1) {
                        return true;
                    }
                }
                case "D" -> {
                    return true;
                }
                default -> {
                }
            }
        }
        return false;
    }

    private boolean ageIsFullyOpen(MomentProfile profile) {
        if (profile == null) {
            return true;
        }
        if (profile.getAgePreferenceMin() != null && profile.getAgePreferenceMax() != null) {
            return profile.getAgePreferenceMin() <= -10 && profile.getAgePreferenceMax() >= 10;
        }
        return profile.getAgeRangePreference() == null
                || profile.getAgeRangePreference().isBlank()
                || profile.getAgeRangePreference().contains("D");
    }

    private double calcGradeCompatibility(MomentProfile a, User userA, MomentProfile b, User userB) {
        if (gradeIsFullyOpen(a) && gradeIsFullyOpen(b)) {
            return 62;
        }
        Integer gradeA = normalizeGrade(userA != null ? userA.getGrade() : null);
        Integer gradeB = normalizeGrade(userB != null ? userB.getGrade() : null);
        if (gradeA == null || gradeB == null) {
            return 42;
        }
        boolean ab = checkGradeCovered(a, gradeA, gradeB);
        boolean ba = checkGradeCovered(b, gradeB, gradeA);
        if (ab && ba) {
            return oneSideIsOpen(a, b, false) ? 79 : 93;
        }
        if (ab || ba) {
            return 48;
        }
        return 14;
    }

    private boolean checkGradeCovered(MomentProfile profile, Integer selfGradeOrder, Integer targetGradeOrder) {
        if (profile == null) {
            return true;
        }
        if (gradeIsFullyOpen(profile)) {
            return true;
        }
        if (selfGradeOrder == null || targetGradeOrder == null) {
            return false;
        }
        Integer min = profile.getGradeRangeMin();
        Integer max = profile.getGradeRangeMax();
        if (min != null && max != null && !(min == 1 && max >= 11)) {
            return targetGradeOrder >= min && targetGradeOrder <= max;
        }
        String preference = profile.getGradeRangePreference();
        if (preference == null || preference.isBlank()) {
            return true;
        }
        return switch (preference) {
            case "A" -> targetGradeOrder >= selfGradeOrder + 1 && targetGradeOrder <= selfGradeOrder + 2;
            case "B" -> Objects.equals(targetGradeOrder, selfGradeOrder);
            case "C" -> targetGradeOrder >= selfGradeOrder - 2 && targetGradeOrder <= selfGradeOrder - 1;
            case "D" -> true;
            default -> false;
        };
    }

    private boolean gradeIsFullyOpen(MomentProfile profile) {
        if (profile == null) {
            return true;
        }
        if ("D".equals(profile.getGradeRangePreference())) {
            return true;
        }
        return profile.getGradeRangeMin() != null
                && profile.getGradeRangeMax() != null
                && profile.getGradeRangeMin() <= 1
                && profile.getGradeRangeMax() >= 11;
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

    private double calcLifestyleScore(MomentProfile a, MomentProfile b) {
        return MomentPersonalityMatrix.lifeRhythmScore(a.getLifeRhythm(), b.getLifeRhythm()) * 0.30
                + scoreRhythmChoice(a.getCompanionshipStyle(), b.getCompanionshipStyle()) * 0.20
                + scoreRhythmChoice(a.getDateStyle(), b.getDateStyle()) * 0.15
                + scoreIntimacyPace(a.getIntimacyPace(), b.getIntimacyPace()) * 0.15
                + scoreLifestyleDirection(a.getFutureLifestyle(), b.getFutureLifestyle()) * 0.10
                + scoreLifestyleDirection(a.getCampusLovePlan(), b.getCampusLovePlan()) * 0.10;
    }

    private double calcCoreValueScore(MomentProfile a, MomentProfile b) {
        return scoreValueChoice(a.getRelationshipCoreValue(), b.getRelationshipCoreValue()) * 0.18
                + scoreValueChoice(a.getHonestyLevel(), b.getHonestyLevel()) * 0.12
                + scoreValueChoice(a.getPremaritalCohabitation(), b.getPremaritalCohabitation()) * 0.10
                + scoreValueChoice(a.getConflictStyle(), b.getConflictStyle()) * 0.10
                + scoreValueChoice(a.getSocialBoundary(), b.getSocialBoundary()) * 0.10
                + scoreValueChoice(a.getTemptationResponse(), b.getTemptationResponse()) * 0.10
                + scoreValueChoice(a.getRealityCondition(), b.getRealityCondition()) * 0.08
                + scoreValueChoice(a.getHumanNatureView(), b.getHumanNatureView()) * 0.05
                + scoreValueChoice(a.getBreakupView(), b.getBreakupView()) * 0.05
                + scoreValueChoice(a.getCareerLoveConflict(), b.getCareerLoveConflict()) * 0.05
                + scoreValueChoice(a.getEmotionPriority(), b.getEmotionPriority()) * 0.04
                + scoreValueChoice(a.getLifeGoalPriority(), b.getLifeGoalPriority()) * 0.03;
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

    private List<Penalty> collectPenalties(MomentProfile a, MomentProfile b) {
        List<Penalty> penalties = new ArrayList<>();

        int sexA = premaritalSexToTier(a.getPremaritalSex());
        int sexB = premaritalSexToTier(b.getPremaritalSex());
        if (sexA > 0 && sexB > 0 && Math.abs(sexA - sexB) == 2) {
            penalties.add(new Penalty("亲密边界存在明显差异", 12));
        }
        if (!Objects.equals(a.getPremaritalCohabitation(), b.getPremaritalCohabitation())) {
            penalties.add(new Penalty("婚前同居态度不同", 8));
        }
        if (!Objects.equals(a.getCampusLovePlan(), b.getCampusLovePlan())) {
            penalties.add(new Penalty("恋爱推进预期不同", 6));
        }
        if (isConflict(a.getCampusLovePlan(), b.getCampusLovePlan(), "A", "B")) {
            penalties.add(new Penalty("校园恋爱规划 A/B 冲突", 6));
        }
        if (!Objects.equals(a.getRelationshipCoreValue(), b.getRelationshipCoreValue())) {
            penalties.add(new Penalty("关系核心价值不一致", 6));
        }
        if (!Objects.equals(a.getHonestyLevel(), b.getHonestyLevel())) {
            penalties.add(new Penalty("坦诚观念存在差异", 5));
        }
        if (!Objects.equals(a.getConflictStyle(), b.getConflictStyle())) {
            penalties.add(new Penalty("冲突处理方式不同", 4));
        }
        if (!Objects.equals(a.getSocialBoundary(), b.getSocialBoundary())) {
            penalties.add(new Penalty("社交边界存在差异", 4));
        }
        if (!Objects.equals(a.getCareerLoveConflict(), b.getCareerLoveConflict())) {
            penalties.add(new Penalty("事业与恋爱优先级不同", 3));
        }
        if (!Objects.equals(a.getEmotionPriority(), b.getEmotionPriority())) {
            penalties.add(new Penalty("情绪需求排序不同", 3));
        }
        if (!Objects.equals(a.getLifeGoalPriority(), b.getLifeGoalPriority())) {
            penalties.add(new Penalty("人生目标排序不同", 3));
        }
        penalties.add(temptationPenalty(a.getTemptationResponse(), b.getTemptationResponse()));
        penalties.add(temptationPenalty(b.getTemptationResponse(), a.getTemptationResponse()));
        if (isConflict(a.getFutureLifestyle(), b.getFutureLifestyle(), "A", "B")) {
            penalties.add(new Penalty("未来生活方式方向不同", 6));
        }
        if (severePaceMismatch(a.getIntimacyPace(), b.getIntimacyPace())) {
            penalties.add(new Penalty("亲密节奏差异过大", 6));
        }
        return penalties.stream().filter(Objects::nonNull).toList();
    }

    private Penalty summarizePenalty(List<Penalty> penalties) {
        if (penalties == null || penalties.isEmpty()) {
            return new Penalty(null, 0);
        }
        int total = penalties.stream().mapToInt(Penalty::score).sum();
        int scaled = (int) Math.round(total * SOFT_PENALTY_MULTIPLIER);
        String reason = penalties.stream()
                .map(Penalty::reason)
                .filter(Objects::nonNull)
                .distinct()
                .findFirst()
                .orElse(null);
        return new Penalty(reason, Math.min(scaled, SOFT_PENALTY_CAP));
    }

    /**
     * 对基础综合分做温和校准，提升中段分数分辨率，减少“集中卡在阈值下”的情况。
     */
    private double calibrateBaseScore(double rawBaseScore) {
        double clamped = Math.max(0d, Math.min(100d, rawBaseScore));
        double uplift;
        if (clamped < 60d) {
            uplift = 8d;
        } else if (clamped < 75d) {
            uplift = 5d;
        } else if (clamped < 85d) {
            uplift = 3d;
        } else {
            uplift = 1d;
        }
        return Math.min(100d, clamped + uplift);
    }

    private Penalty temptationPenalty(String self, String other) {
        if (!Objects.equals(self, "D")) {
            return null;
        }
        if (Objects.equals(other, "A")) {
            return new Penalty("面对新诱惑的选择更不一致", 10);
        }
        if (Objects.equals(other, "B") || Objects.equals(other, "C")) {
            return new Penalty("对关系边界的想法并不完全相同", 6);
        }
        return null;
    }

    private int premaritalSexToTier(String value) {
        if (value == null) {
            return 0;
        }
        return switch (value) {
            case "A" -> 1;
            case "B" -> 2;
            case "C" -> 3;
            case "D" -> 4;
            default -> 0;
        };
    }

    private double calcSimilarity(String a, String b) {
        if (a == null || b == null) {
            return 45;
        }
        if (Objects.equals(a, b)) {
            return 100;
        }
        if ("D".equals(a) || "D".equals(b)) {
            return 62;
        }
        if ("C".equals(a) || "C".equals(b)) {
            return 58;
        }
        return 28;
    }

    private double scoreCampusFocus(String a, String b) {
        if (a == null || b == null) {
            return 42;
        }
        if (Objects.equals(a, b)) {
            return 92;
        }
        return "C".equals(a) || "C".equals(b) ? 56 : 34;
    }

    private double scorePreferenceChoice(String a, String b) {
        if (a == null || b == null) {
            return 42;
        }
        if (Objects.equals(a, b)) {
            return 92;
        }
        return "C".equals(a) || "C".equals(b) || "D".equals(a) || "D".equals(b) ? 58 : 34;
    }

    private double scoreRhythmChoice(String a, String b) {
        if (a == null || b == null) {
            return 42;
        }
        if (Objects.equals(a, b)) {
            return 94;
        }
        if ("C".equals(a) || "C".equals(b) || "D".equals(a) || "D".equals(b)) {
            return 54;
        }
        return 30;
    }

    private double scoreIntimacyPace(String a, String b) {
        if (a == null || b == null) {
            return 40;
        }
        if (Objects.equals(a, b)) {
            return 95;
        }
        if (("B".equals(a) && "C".equals(b)) || ("C".equals(a) && "B".equals(b))) {
            return 68;
        }
        if ("C".equals(a) || "C".equals(b) || "D".equals(a) || "D".equals(b)) {
            return 52;
        }
        return 24;
    }

    private double scoreLifestyleDirection(String a, String b) {
        if (a == null || b == null) {
            return 42;
        }
        if (Objects.equals(a, b)) {
            return 94;
        }
        if ("C".equals(a) || "C".equals(b) || "D".equals(a) || "D".equals(b)) {
            return 54;
        }
        return 26;
    }

    private double scoreValueChoice(String a, String b) {
        if (a == null || b == null) {
            return 42;
        }
        if (Objects.equals(a, b)) {
            return 100;
        }
        if ("D".equals(a) || "D".equals(b)) {
            return 46;
        }
        if ("C".equals(a) || "C".equals(b)) {
            return 40;
        }
        return 24;
    }

    private boolean oneSideIsOpen(MomentProfile a, MomentProfile b, boolean age) {
        return age ? ageIsFullyOpen(a) || ageIsFullyOpen(b) : gradeIsFullyOpen(a) || gradeIsFullyOpen(b);
    }

    private boolean severePaceMismatch(String a, String b) {
        if (a == null || b == null || Objects.equals(a, b)) {
            return false;
        }
        return ("A".equals(a) && ("C".equals(b) || "D".equals(b)))
                || ("A".equals(b) && ("C".equals(a) || "D".equals(a)));
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
        // JGraphT 1.5.2 在内部复制图时会调用无参 addVertex()。
        // 若 vertexSupplier 生成的值与我们手动塞入的 Integer 顶点重合，会在 blossom/copy 阶段触发 loops not allowed。
        // 因此这里统一使用业务顶点键 + 不冲突的 aux supplier。
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
        detail.put("personality", 0d);
        detail.put("preference", 0d);
        detail.put("lifestyle", 0d);
        detail.put("coreValue", 0d);
        detail.put("interestOcean", 0d);
        detail.put("personalityWeighted", 0d);
        detail.put("preferenceWeighted", 0d);
        detail.put("lifestyleWeighted", 0d);
        detail.put("coreValueWeighted", 0d);
        detail.put("interestOceanWeighted", 0d);
        detail.put("rawBaseScore", 0d);
        detail.put("calibratedBaseScore", 0d);
        detail.put("softPenalty", 0);
        detail.put("softPenaltyReason", null);
        detail.put("softPenaltyReasons", List.of());
        detail.put("thresholdRequired", thresholdRequired);
        detail.put("hardFilterReason", hardFilterReason);
        detail.put("finalScore", 0d);
        return detail;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
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

    private record Penalty(String reason, int score) {}
}
