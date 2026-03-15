package com.campus.love.profile.service;

import com.campus.love.common.constants.TagOceanWeights;
import com.campus.love.profile.entity.UserPortrait;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class QuestionnaireOceanMapper {

    private static final BigDecimal CURRENT_WEIGHT = BigDecimal.valueOf(0.7d);
    private static final BigDecimal QUESTIONNAIRE_WEIGHT = BigDecimal.valueOf(0.3d);

    public Map<String, BigDecimal> computeQuestionnaireOcean(UserPortrait portrait) {
        ScoreAccumulator acc = new ScoreAccumulator();
        if (portrait == null) return acc.finish();

        acc.add("E", optionScore(portrait.getSocialStyle(), Map.of("A", 80, "B", 30, "C", 50)), 1.0d);
        acc.add("C", optionScore(portrait.getLifeRhythm(), Map.of("A", 90, "B", 30, "C", 60, "D", 20)), 1.0d);
        acc.add("E", optionScore(portrait.getPersonalityBase(), Map.of("A", 90, "B", 20, "C", 50)), 1.0d);
        acc.add("N", optionScore(portrait.getIntimacyPace(), Map.of("A", 80, "B", 30, "C", 50)), 1.0d);

        acc.add("E", optionScore(portrait.getEmotionStyle(), Map.of("A", 70, "B", 20, "C", 40)), 0.7d);
        acc.add("N", optionScore(portrait.getEmotionStyle(), Map.of("A", 70, "B", 50, "C", 60)), 0.7d);

        acc.add("O", optionScore(portrait.getMajorPreference(), Map.of("A", 40, "B", 80, "C", 50)), 0.7d);
        acc.add("C", optionScore(portrait.getCareerAmbitionPref(), Map.of("A", 80, "B", 20, "C", 60)), 0.7d);

        acc.add("O", optionScore(portrait.getDateStyle(), Map.of("A", 80, "B", 50, "C", 40, "D", 70)), 0.7d);
        acc.add("E", optionScore(portrait.getDateStyle(), Map.of("A", 70, "B", 40, "C", 50, "D", 60)), 0.7d);

        acc.add("O", optionScore(portrait.getPremaritalCohabitation(), Map.of("A", 80, "B", 60, "C", 30, "D", 50)), 0.7d);

        acc.add("A", optionScore(portrait.getRelationshipCoreValue(), Map.of("A", 90, "B", 60, "C", 70, "D", 40)), 0.7d);
        acc.add("N", optionScore(portrait.getRelationshipCoreValue(), Map.of("A", 40, "B", 50, "C", 45, "D", 70)), 0.7d);
        acc.add("O", optionScore(portrait.getRelationshipCoreValue(), Map.of("A", 55, "B", 70, "C", 60, "D", 45)), 0.7d);

        acc.add("A", optionScore(portrait.getConflictStyle(), Map.of("A", 70, "B", 90, "C", 65, "D", 30)), 0.7d);
        acc.add("C", optionScore(portrait.getConflictStyle(), Map.of("A", 70, "B", 65, "C", 80, "D", 20)), 0.7d);
        acc.add("N", optionScore(portrait.getConflictStyle(), Map.of("A", 55, "B", 45, "C", 30, "D", 70)), 0.7d);

        acc.add("O", optionScore(portrait.getFutureLifestyle(), Map.of("A", 55, "B", 40, "C", 90, "D", 45)), 0.7d);
        acc.add("C", optionScore(portrait.getFutureLifestyle(), Map.of("A", 80, "B", 60, "C", 35, "D", 30)), 0.7d);

        acc.add("A", optionScore(portrait.getTemptationResponse(), Map.of("A", 90, "B", 70, "C", 45, "D", 30)), 0.4d);
        acc.add("N", optionScore(portrait.getTemptationResponse(), Map.of("A", 35, "B", 45, "C", 80, "D", 85)), 0.4d);

        acc.add("C", optionScore(portrait.getLifeGoalPriority(), Map.of("A", 90, "B", 55, "C", 35)), 0.4d);
        acc.add("A", optionScore(portrait.getLifeGoalPriority(), Map.of("A", 50, "B", 80, "C", 40)), 0.4d);
        acc.add("O", optionScore(portrait.getLifeGoalPriority(), Map.of("A", 55, "B", 45, "C", 90)), 0.4d);

        acc.add("A", optionScore(portrait.getHonestyLevel(), Map.of("A", 80, "B", 60, "C", 55)), 0.4d);
        return acc.finish();
    }

    public void blendIntoPortrait(UserPortrait portrait) {
        Map<String, BigDecimal> questionnaire = computeQuestionnaireOcean(portrait);
        if (questionnaire.isEmpty()) return;
        portrait.setOceanOLong(blend(portrait.getOceanOLong(), questionnaire.get("O")));
        portrait.setOceanCLong(blend(portrait.getOceanCLong(), questionnaire.get("C")));
        portrait.setOceanELong(blend(portrait.getOceanELong(), questionnaire.get("E")));
        portrait.setOceanALong(blend(portrait.getOceanALong(), questionnaire.get("A")));
        portrait.setOceanNLong(blend(portrait.getOceanNLong(), questionnaire.get("N")));
        portrait.setOceanOShort(blend(portrait.getOceanOShort(), questionnaire.get("O")));
        portrait.setOceanCShort(blend(portrait.getOceanCShort(), questionnaire.get("C")));
        portrait.setOceanEShort(blend(portrait.getOceanEShort(), questionnaire.get("E")));
        portrait.setOceanAShort(blend(portrait.getOceanAShort(), questionnaire.get("A")));
        portrait.setOceanNShort(blend(portrait.getOceanNShort(), questionnaire.get("N")));
    }

    private BigDecimal blend(BigDecimal current, BigDecimal questionnaire) {
        if (questionnaire == null) return current;
        if (current == null) return questionnaire.setScale(1, RoundingMode.HALF_UP);
        return current.multiply(CURRENT_WEIGHT)
                .add(questionnaire.multiply(QUESTIONNAIRE_WEIGHT))
                .setScale(1, RoundingMode.HALF_UP);
    }

    private Integer optionScore(String option, Map<String, Integer> scores) {
        if (option == null || option.isBlank()) return null;
        return scores.get(option);
    }

    private static class ScoreAccumulator {
        private final Map<String, Double> weightedScore = new LinkedHashMap<>();
        private final Map<String, Double> totalWeight = new LinkedHashMap<>();

        private ScoreAccumulator() {
            for (String dim : TagOceanWeights.DIMENSIONS) {
                weightedScore.put(dim, 0d);
                totalWeight.put(dim, 0d);
            }
        }

        private void add(String dim, Integer score, double weight) {
            if (score == null || weight <= 0) return;
            weightedScore.computeIfPresent(dim, (k, v) -> v + score * weight);
            totalWeight.computeIfPresent(dim, (k, v) -> v + weight);
        }

        private Map<String, BigDecimal> finish() {
            Map<String, BigDecimal> result = new LinkedHashMap<>();
            for (String dim : TagOceanWeights.DIMENSIONS) {
                double weight = totalWeight.getOrDefault(dim, 0d);
                if (weight <= 0) continue;
                double value = weightedScore.get(dim) / weight;
                result.put(dim, BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP));
            }
            return result;
        }
    }
}
