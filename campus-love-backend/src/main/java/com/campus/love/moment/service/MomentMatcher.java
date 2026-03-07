package com.campus.love.moment.service;

import com.campus.love.moment.entity.MomentProfile;
import com.campus.love.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;

/**
 * 心动一刻匹配算法 — 贪心匹配
 * 四维度打分：性格匹配(25%) + 偏好满足(35%) + 生活方式(20%) + 核心价值(20%)
 */
@Slf4j
@Component
public class MomentMatcher {

    /**
     * 匹配候选数据：用户 + 问卷档案
     */
    public record Candidate(User user, MomentProfile profile) {}

    /**
     * 匹配结果：两个用户 + 综合分
     */
    public record MatchPair(Long userIdA, Long userIdB, double totalScore, Map<String, Object> scoreDetail) {}

    /**
     * 对一组候选人执行贪心匹配
     * @param candidates 同一匹配池的所有候选人
     * @param isMFPool 是否为MF池（需要一男一女约束）
     * @return 匹配结果列表
     */
    public List<MatchPair> match(List<Candidate> candidates, boolean isMFPool) {
        int n = candidates.size();
        if (n < 2) return Collections.emptyList();

        // 1. 计算所有候选对的分数
        List<ScoredPair> allPairs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                Candidate a = candidates.get(i);
                Candidate b = candidates.get(j);

                // MF 池必须一男一女
                if (isMFPool && !isOppositeGender(a.user(), b.user())) continue;

                double score = calcPairScore(a, b);
                if (score > 0) {
                    allPairs.add(new ScoredPair(i, j, score, buildScoreDetail(a, b)));
                }
            }
        }

        // 2. 按分数降序排列
        allPairs.sort((a, b) -> Double.compare(b.score, a.score));

        // 3. 贪心配对
        Set<Integer> matched = new HashSet<>();
        List<MatchPair> results = new ArrayList<>();

        for (ScoredPair pair : allPairs) {
            if (matched.contains(pair.idxA) || matched.contains(pair.idxB)) continue;

            matched.add(pair.idxA);
            matched.add(pair.idxB);

            Candidate a = candidates.get(pair.idxA);
            Candidate b = candidates.get(pair.idxB);

            results.add(new MatchPair(
                    a.user().getId(), b.user().getId(),
                    pair.score, pair.detail
            ));
        }

        log.info("匹配完成: {}人参与, {}对匹配成功, {}人未匹配",
                n, results.size(), n - results.size() * 2);
        return results;
    }

    /**
     * 计算两个候选人之间的综合匹配分（满分100）
     */
    double calcPairScore(Candidate a, Candidate b) {
        MomentProfile pa = a.profile();
        MomentProfile pb = b.profile();

        // 硬筛选：价值观冲突直接排除
        if (!isValueCompatible(pa, pb)) return 0;

        // 四维度打分
        double personality = calcPersonalityScore(pa, pb) * 0.25;
        double preference = calcPreferenceScore(a, b) * 0.35;
        double lifestyle = calcLifestyleScore(pa, pb) * 0.20;
        double coreValue = calcCoreValueScore(pa, pb) * 0.20;

        return personality + preference + lifestyle + coreValue;
    }

    // ==================== 硬筛选 ====================

    boolean isValueCompatible(MomentProfile a, MomentProfile b) {
        // Q13 同居观冲突：一方坚决不接受(C) 另一方支持(A)
        if ("C".equals(a.getPremaritalCohabitation()) && "A".equals(b.getPremaritalCohabitation())) return false;
        if ("A".equals(a.getPremaritalCohabitation()) && "C".equals(b.getPremaritalCohabitation())) return false;
        return true;
    }

    // ==================== 性格匹配 25% ====================

    double calcPersonalityScore(MomentProfile a, MomentProfile b) {
        double score = 0;
        // Q3 社交风格：相同或互补都给分 (权重35%)
        score += calcSimilarity(a.getSocialStyle(), b.getSocialStyle()) * 0.35;
        // Q4 生活节奏 (权重35%)
        score += calcSimilarity(a.getLifeRhythm(), b.getLifeRhythm()) * 0.35;
        // Q5 陪伴方式 (权重30%)
        score += calcSimilarity(a.getCompanionshipStyle(), b.getCompanionshipStyle()) * 0.30;
        return score;
    }

    // ==================== 偏好满足 35% ====================

    double calcPreferenceScore(Candidate a, Candidate b) {
        double score = 0;
        MomentProfile pa = a.profile();
        MomentProfile pb = b.profile();

        // Q6 颜值期望 vs 对方自评分（双向，各12.5%）
        int selfScoreA = a.user().getMomentSelfScore() != null ? a.user().getMomentSelfScore() : 5;
        int selfScoreB = b.user().getMomentSelfScore() != null ? b.user().getMomentSelfScore() : 5;
        score += calcAppearanceMatch(pb.getAppearanceRequirement(), selfScoreA) * 0.125;
        score += calcAppearanceMatch(pa.getAppearanceRequirement(), selfScoreB) * 0.125;

        // Q7 性格期望 vs 对方实际社交风格（双向，各12.5%）
        score += calcPersonalityExpect(pa.getPartnerPersonality(), pb.getSocialStyle()) * 0.125;
        score += calcPersonalityExpect(pb.getPartnerPersonality(), pa.getSocialStyle()) * 0.125;

        // Q8 专业偏好 (25%)
        score += calcMajorMatch(pa.getMajorPreference(), pb.getMajorPreference(),
                a.user().getMajor(), b.user().getMajor()) * 0.25;

        // Q9 年龄偏好（双向，各12.5%）
        score += calcAgeMatch(pa.getAgeRangePreference(), a.user(), b.user()) * 0.125;
        score += calcAgeMatch(pb.getAgeRangePreference(), b.user(), a.user()) * 0.125;

        return score;
    }

    double calcAppearanceMatch(String requirement, int selfScore) {
        // A=非常在意(需7+), B=有一定要求(需5+), C=不太在意(任何分都可)
        if ("C".equals(requirement)) return 80;
        if ("B".equals(requirement)) return selfScore >= 5 ? 90 : 50;
        if ("A".equals(requirement)) return selfScore >= 7 ? 100 : (selfScore >= 5 ? 60 : 30);
        return 60;
    }

    double calcPersonalityExpect(String expected, String actual) {
        // A=活泼开朗(匹配A), B=温柔安静(匹配B), C=相近就好(任何都可)
        if ("C".equals(expected)) return 80;
        if (expected.equals(actual)) return 100;
        if ("C".equals(actual)) return 70; // 对方是"两种都OK"
        return 40;
    }

    double calcMajorMatch(String prefA, String prefB, String majorA, String majorB) {
        // A=同专业, B=不同专业, C=不在意
        boolean sameMajor = majorA != null && majorA.equals(majorB);
        double scoreA = calcSingleMajorPref(prefA, sameMajor);
        double scoreB = calcSingleMajorPref(prefB, sameMajor);
        return (scoreA + scoreB) / 2;
    }

    double calcSingleMajorPref(String pref, boolean sameMajor) {
        if ("C".equals(pref)) return 80;
        if ("A".equals(pref)) return sameMajor ? 100 : 40;
        if ("B".equals(pref)) return sameMajor ? 40 : 100;
        return 60;
    }

    double calcAgeMatch(String agePref, User self, User target) {
        if (agePref == null || agePref.contains("D")) return 80; // 年龄不是问题
        if (self.getBirthDate() == null || target.getBirthDate() == null) return 60;

        int ageDiff = Period.between(target.getBirthDate(), self.getBirthDate()).getYears();
        // ageDiff > 0 means target is older

        boolean accepted = false;
        for (String choice : agePref.split(",")) {
            switch (choice.trim()) {
                case "A" -> { if (ageDiff < -1 && ageDiff >= -3) accepted = true; } // 比我大1-2岁
                case "B" -> { if (Math.abs(ageDiff) <= 1) accepted = true; }         // 同龄±1
                case "C" -> { if (ageDiff > 1 && ageDiff <= 3) accepted = true; }    // 比我小1-2岁
            }
        }
        return accepted ? 90 : 40;
    }

    // ==================== 生活方式 20% ====================

    double calcLifestyleScore(MomentProfile a, MomentProfile b) {
        double score = 0;
        // Q10 约会风格 (权重35%)
        score += calcSimilarity(a.getDateStyle(), b.getDateStyle()) * 0.35;
        // Q11 亲密节奏 (权重35%)
        score += calcSimilarity(a.getIntimacyPace(), b.getIntimacyPace()) * 0.35;
        // Q14 未来规划 (权重30%)
        score += calcSimilarity(a.getFutureLifestyle(), b.getFutureLifestyle()) * 0.30;
        return score;
    }

    // ==================== 核心价值 20% ====================

    double calcCoreValueScore(MomentProfile a, MomentProfile b) {
        // Q15 最重要的事
        if (a.getRelationshipCoreValue() == null || b.getRelationshipCoreValue() == null) return 60;
        if (a.getRelationshipCoreValue().equals(b.getRelationshipCoreValue())) return 100;
        return 50;
    }

    // ==================== 工具方法 ====================

    /**
     * 计算两个选项的相似/互补分数
     * 相同=100, C(灵活型)=70, 不同=40
     */
    double calcSimilarity(String a, String b) {
        if (a == null || b == null) return 60;
        if (a.equals(b)) return 100;
        if ("C".equals(a) || "C".equals(b)) return 70;
        return 40;
    }

    boolean isOppositeGender(User a, User b) {
        if (a.getGender() == null || b.getGender() == null) return false;
        return !a.getGender().equals(b.getGender());
    }

    Map<String, Object> buildScoreDetail(Candidate a, Candidate b) {
        MomentProfile pa = a.profile();
        MomentProfile pb = b.profile();
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("personality", round(calcPersonalityScore(pa, pb) * 0.25));
        detail.put("preference", round(calcPreferenceScore(a, b) * 0.35));
        detail.put("lifestyle", round(calcLifestyleScore(pa, pb) * 0.20));
        detail.put("coreValue", round(calcCoreValueScore(pa, pb) * 0.20));
        return detail;
    }

    double round(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private record ScoredPair(int idxA, int idxB, double score, Map<String, Object> detail) {}
}
