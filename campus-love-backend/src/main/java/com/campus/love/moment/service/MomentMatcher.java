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
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Period;
import java.util.*;

/**
 * 心动时刻完整匹配器：
 * 硬筛选 -> 五维得分 -> 软惩罚 -> 阈值过滤 -> 最大权重匹配。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MomentMatcher {

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

    public record PairEvaluation(
            Long userIdA,
            Long userIdB,
            String pool,
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

    public record PoolMatchResult(List<MatchPair> matches, List<PairEvaluation> evaluations, int candidateCount) {}

    public List<String> poolOrder() {
        return POOL_ORDER;
    }

    public PoolMatchResult match(List<Candidate> candidates, String pool, MomentMatchConfig config) {
        if (candidates == null || candidates.size() < 2) {
            return new PoolMatchResult(List.of(), List.of(), candidates == null ? 0 : candidates.size());
        }

        List<PairEvaluation> evaluations = new ArrayList<>();
        List<EligibleEdge> eligibleEdges = new ArrayList<>();

        for (int i = 0; i < candidates.size(); i++) {
            for (int j = i + 1; j < candidates.size(); j++) {
                Candidate a = candidates.get(i);
                Candidate b = candidates.get(j);
                PairComputation computation = evaluatePair(a, b, pool, config);
                evaluations.add(new PairEvaluation(
                        a.user().getId(),
                        b.user().getId(),
                        pool,
                        computation.totalScore(),
                        computation.scoreDetail(),
                        computation.hardFilterPassed(),
                        computation.hardFilterReason(),
                        computation.softPenalty(),
                        computation.softPenaltyReason(),
                        computation.thresholdOffsetA(),
                        computation.thresholdOffsetB(),
                        computation.effectiveThresholdA(),
                        computation.effectiveThresholdB(),
                        computation.thresholdRequired(),
                        computation.includedByThreshold()
                ));
                if (computation.hardFilterPassed() && computation.includedByThreshold() && computation.totalScore() > 0) {
                    eligibleEdges.add(new EligibleEdge(i, j, computation.totalScore(), computation.scoreDetail()));
                }
            }
        }

        List<MatchPair> matches = "MF".equals(pool)
                ? solveMaximumWeightBipartite(candidates, eligibleEdges)
                : solveMaximumWeightGeneralGraph(candidates, eligibleEdges);

        log.info("心动时刻池 {} 匹配完成: {}人参与, {}对成功, {}条候选边通过阈值",
                pool, candidates.size(), matches.size(), eligibleEdges.size());
        return new PoolMatchResult(matches, evaluations, candidates.size());
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
        Penalty penalty = highestPenalty(penalties);
        double total =
                personalityScore * 0.22
                        + preferenceScore * 0.30
                        + lifestyleScore * 0.20
                        + coreValueScore * 0.20
                        + interestOceanScore * 0.08
                        - penalty.score();
        total = Math.max(0d, total);

        Map<String, Object> scoreDetail = new LinkedHashMap<>();
        scoreDetail.put("personality", round(personalityScore));
        scoreDetail.put("preference", round(preferenceScore));
        scoreDetail.put("lifestyle", round(lifestyleScore));
        scoreDetail.put("coreValue", round(coreValueScore));
        scoreDetail.put("interestOcean", round(interestOceanScore));
        scoreDetail.put("personalityWeighted", round(personalityScore * 0.22));
        scoreDetail.put("preferenceWeighted", round(preferenceScore * 0.30));
        scoreDetail.put("lifestyleWeighted", round(lifestyleScore * 0.20));
        scoreDetail.put("coreValueWeighted", round(coreValueScore * 0.20));
        scoreDetail.put("interestOceanWeighted", round(interestOceanScore * 0.08));
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

        if (isConflict(profileA != null ? profileA.getCampusLovePlan() : null,
                profileB != null ? profileB.getCampusLovePlan() : null, "A", "B")) {
            return "校园恋爱规划 A/B 冲突";
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
                + MomentPersonalityMatrix.lifeRhythmScore(a.getLifeRhythm(), b.getLifeRhythm()) * 0.25
                + MomentPersonalityMatrix.personalityBaseScore(a.getPersonalityBase(), b.getPersonalityBase()) * 0.25
                + MomentPersonalityMatrix.emotionStyleScore(a.getEmotionStyle(), b.getEmotionStyle()) * 0.25;
    }

    private double calcPreferenceScore(Candidate a, Candidate b) {
        MomentProfile pa = a.profile();
        MomentProfile pb = b.profile();
        return averageAppearanceMatch(a.user(), b.user(), pa, pb) * 0.20
                + averagePartnerPersonalityMatch(pa, pb) * 0.18
                + calcMajorMatch(pa.getMajorPreference(), pb.getMajorPreference(), a.user().getMajor(), b.user().getMajor()) * 0.12
                + calcAgeCompatibility(pa, a.user(), pb, b.user()) * 0.20
                + calcGradeCompatibility(pa, a.user(), pb, b.user()) * 0.18
                + calcSimilarity(pa.getCareerAmbitionPref(), pb.getCareerAmbitionPref()) * 0.12;
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
            return 88;
        }
        if ("B".equals(requirement)) {
            return targetSelfScore >= 5 ? 92 : 58;
        }
        if ("A".equals(requirement)) {
            return targetSelfScore >= 7 ? 100 : (targetSelfScore >= 5 ? 66 : 30);
        }
        return 60;
    }

    private double calcPersonalityExpect(String expected, String actual) {
        if ("D".equals(expected)) {
            return 85;
        }
        if (expected == null || actual == null) {
            return 60;
        }
        if (Objects.equals(expected, actual)) {
            return 100;
        }
        if ("C".equals(expected) || "C".equals(actual)) {
            return 78;
        }
        return 45;
    }

    private double calcMajorMatch(String prefA, String prefB, String majorA, String majorB) {
        boolean sameMajor = majorA != null && majorB != null && majorA.equalsIgnoreCase(majorB);
        return (calcSingleMajorPref(prefA, sameMajor) + calcSingleMajorPref(prefB, sameMajor)) / 2d;
    }

    private double calcSingleMajorPref(String pref, boolean sameMajor) {
        if ("C".equals(pref)) {
            return 85;
        }
        if ("A".equals(pref)) {
            return sameMajor ? 100 : 40;
        }
        if ("B".equals(pref)) {
            return sameMajor ? 45 : 100;
        }
        return 60;
    }

    private double calcAgeCompatibility(MomentProfile a, User userA, MomentProfile b, User userB) {
        if (userA == null || userB == null) {
            return 55;
        }
        if (ageIsFullyOpen(a) && ageIsFullyOpen(b)) {
            return 100;
        }
        if (userA.getBirthDate() == null || userB.getBirthDate() == null) {
            return 55;
        }
        boolean ab = checkAgeCovered(a, userA, userB);
        boolean ba = checkAgeCovered(b, userB, userA);
        if (ab && ba) {
            return 100;
        }
        if (ab || ba) {
            return 76;
        }
        return 35;
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
            return 100;
        }
        Integer gradeA = normalizeGrade(userA != null ? userA.getGrade() : null);
        Integer gradeB = normalizeGrade(userB != null ? userB.getGrade() : null);
        if (gradeA == null || gradeB == null) {
            return 55;
        }
        boolean ab = checkGradeCovered(a, gradeA, gradeB);
        boolean ba = checkGradeCovered(b, gradeB, gradeA);
        if (ab && ba) {
            return 100;
        }
        if (ab || ba) {
            return 76;
        }
        return 35;
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
        return calcSimilarity(a.getCompanionshipStyle(), b.getCompanionshipStyle()) * 0.18
                + calcSimilarity(a.getDateStyle(), b.getDateStyle()) * 0.20
                + calcSimilarity(a.getIntimacyPace(), b.getIntimacyPace()) * 0.22
                + calcSimilarity(a.getFutureLifestyle(), b.getFutureLifestyle()) * 0.22
                + calcSimilarity(a.getCampusLovePlan(), b.getCampusLovePlan()) * 0.18;
    }

    private double calcCoreValueScore(MomentProfile a, MomentProfile b) {
        return calcSimilarity(a.getRelationshipCoreValue(), b.getRelationshipCoreValue()) * 0.15
                + calcSimilarity(a.getConflictStyle(), b.getConflictStyle()) * 0.12
                + calcSimilarity(a.getSocialBoundary(), b.getSocialBoundary()) * 0.10
                + calcSimilarity(a.getHonestyLevel(), b.getHonestyLevel()) * 0.08
                + calcSimilarity(a.getPremaritalCohabitation(), b.getPremaritalCohabitation()) * 0.08
                + calcSimilarity(a.getTemptationResponse(), b.getTemptationResponse()) * 0.10
                + calcSimilarity(a.getRealityCondition(), b.getRealityCondition()) * 0.08
                + calcSimilarity(a.getHumanNatureView(), b.getHumanNatureView()) * 0.06
                + calcSimilarity(a.getBreakupView(), b.getBreakupView()) * 0.06
                + calcSimilarity(a.getCareerLoveConflict(), b.getCareerLoveConflict()) * 0.07
                + calcSimilarity(a.getEmotionPriority(), b.getEmotionPriority()) * 0.04
                + calcSimilarity(a.getLifeGoalPriority(), b.getLifeGoalPriority()) * 0.04
                + calcSimilarity(a.getIdolRole(), b.getIdolRole()) * 0.02;
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
        return interestScore * (5d / 8d) + oceanScore * (3d / 8d);
    }

    private double calcInterestScore(UserPortrait a, UserPortrait b) {
        Set<String> tagsA = InterestTagConverter.extractCodesFromNewFormat(a != null ? a.getInterestTags() : null);
        Set<String> tagsB = InterestTagConverter.extractCodesFromNewFormat(b != null ? b.getInterestTags() : null);
        if (tagsA.isEmpty() && tagsB.isEmpty()) {
            return 60;
        }
        if (tagsA.isEmpty() || tagsB.isEmpty()) {
            return 45;
        }
        Set<String> union = new HashSet<>(tagsA);
        union.addAll(tagsB);
        Set<String> intersection = new HashSet<>(tagsA);
        intersection.retainAll(tagsB);
        if (union.isEmpty()) {
            return 60;
        }
        return round((intersection.size() * 100d) / union.size());
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
            return 50;
        }
        double similarity = 1d - (diff / count) / 100d;
        return round(Math.max(0d, Math.min(1d, similarity)) * 100d);
    }

    private List<Penalty> collectPenalties(MomentProfile a, MomentProfile b) {
        List<Penalty> penalties = new ArrayList<>();

        int sexA = premaritalSexToTier(a.getPremaritalSex());
        int sexB = premaritalSexToTier(b.getPremaritalSex());
        if (sexA > 0 && sexB > 0 && Math.abs(sexA - sexB) == 2) {
            penalties.add(new Penalty("亲密边界存在明显差异", 15));
        }
        if (!Objects.equals(a.getPremaritalCohabitation(), b.getPremaritalCohabitation())) {
            penalties.add(new Penalty("婚前同居态度不同", 10));
        }
        if (!Objects.equals(a.getCampusLovePlan(), b.getCampusLovePlan())) {
            penalties.add(new Penalty("恋爱推进预期不同", 10));
        }
        penalties.add(temptationPenalty(a.getTemptationResponse(), b.getTemptationResponse()));
        penalties.add(temptationPenalty(b.getTemptationResponse(), a.getTemptationResponse()));
        if (isConflict(a.getFutureLifestyle(), b.getFutureLifestyle(), "A", "B")) {
            penalties.add(new Penalty("未来生活方式方向不同", 10));
        }
        return penalties.stream().filter(Objects::nonNull).toList();
    }

    private Penalty highestPenalty(List<Penalty> penalties) {
        Penalty result = penalties.stream()
                .max(Comparator.comparingInt(Penalty::score))
                .orElse(new Penalty(null, 0));
        return new Penalty(result.reason(), Math.min(result.score(), 30));
    }

    private Penalty temptationPenalty(String self, String other) {
        if (!Objects.equals(self, "D")) {
            return null;
        }
        if (Objects.equals(other, "A")) {
            return new Penalty("面对新诱惑的选择更不一致", 15);
        }
        if (Objects.equals(other, "B") || Objects.equals(other, "C")) {
            return new Penalty("对关系边界的想法并不完全相同", 8);
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
            return 60;
        }
        if (Objects.equals(a, b)) {
            return 100;
        }
        if ("D".equals(a) || "D".equals(b)) {
            return 75;
        }
        if ("C".equals(a) || "C".equals(b)) {
            return 72;
        }
        return 45;
    }

    private List<MatchPair> solveMaximumWeightBipartite(List<Candidate> candidates, List<EligibleEdge> edges) {
        if (edges.isEmpty()) {
            return List.of();
        }
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        Set<Integer> left = new LinkedHashSet<>();
        Set<Integer> right = new LinkedHashSet<>();
        Map<DefaultWeightedEdge, EligibleEdge> edgeLookup = new HashMap<>();

        for (EligibleEdge edge : edges) {
            graph.addVertex(edge.idxA());
            graph.addVertex(edge.idxB());
            Integer genderA = candidates.get(edge.idxA()).user().getGender();
            Integer genderB = candidates.get(edge.idxB()).user().getGender();
            if (genderA != null && genderA == 1) {
                left.add(edge.idxA());
                right.add(edge.idxB());
            } else {
                left.add(edge.idxB());
                right.add(edge.idxA());
            }
            DefaultWeightedEdge graphEdge = graph.addEdge(edge.idxA(), edge.idxB());
            if (graphEdge != null) {
                graph.setEdgeWeight(graphEdge, edge.score());
                edgeLookup.put(graphEdge, edge);
            }
        }

        MatchingAlgorithm.Matching<Integer, DefaultWeightedEdge> matching =
                new MaximumWeightBipartiteMatching<>(graph, left, right).getMatching();
        return toMatchPairs(candidates, matching.getEdges(), edgeLookup);
    }

    private List<MatchPair> solveMaximumWeightGeneralGraph(List<Candidate> candidates, List<EligibleEdge> edges) {
        if (edges.isEmpty()) {
            return List.of();
        }
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        Map<DefaultWeightedEdge, EligibleEdge> edgeLookup = new HashMap<>();
        for (EligibleEdge edge : edges) {
            graph.addVertex(edge.idxA());
            graph.addVertex(edge.idxB());
            DefaultWeightedEdge graphEdge = graph.addEdge(edge.idxA(), edge.idxB());
            if (graphEdge != null) {
                graph.setEdgeWeight(graphEdge, edge.score());
                edgeLookup.put(graphEdge, edge);
            }
        }

        MatchingAlgorithm.Matching<Integer, DefaultWeightedEdge> matching =
                new KolmogorovWeightedMatching<>(graph, ObjectiveSense.MAXIMIZE).getMatching();
        return toMatchPairs(candidates, matching.getEdges(), edgeLookup);
    }

    private List<MatchPair> toMatchPairs(
            List<Candidate> candidates,
            Set<DefaultWeightedEdge> edges,
            Map<DefaultWeightedEdge, EligibleEdge> edgeLookup
    ) {
        List<MatchPair> results = new ArrayList<>();
        for (DefaultWeightedEdge graphEdge : edges) {
            EligibleEdge eligible = edgeLookup.get(graphEdge);
            if (eligible == null) {
                continue;
            }
            Candidate a = candidates.get(eligible.idxA());
            Candidate b = candidates.get(eligible.idxB());
            results.add(new MatchPair(
                    a.user().getId(),
                    b.user().getId(),
                    round(eligible.score()),
                    eligible.detail()
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

    private record EligibleEdge(int idxA, int idxB, double score, Map<String, Object> detail) {}

    private record Penalty(String reason, int score) {}
}
