package com.campus.love.profile.service;

import com.campus.love.common.constants.TagOceanWeights;
import com.campus.love.common.utils.InterestTagConverter;
import com.campus.love.profile.entity.UserPortrait;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OceanConfidenceService {

    private static final BigDecimal MERGE_LONG_WEIGHT = BigDecimal.valueOf(0.7d);
    private static final BigDecimal MERGE_SHORT_WEIGHT = BigDecimal.valueOf(0.3d);
    private static final BigDecimal MIDPOINT = BigDecimal.valueOf(50d);
    private static final BigDecimal CONFIDENCE_CAP = BigDecimal.valueOf(0.75d);

    private final ObjectMapper objectMapper;

    public Map<String, BigDecimal> computeOceanConfidence(String mbti, Collection<String> selectedTags, boolean questionnaireFilled) {
        Map<String, BigDecimal> base = buildBaseConfidence(mbti, questionnaireFilled);
        Map<String, Integer> dimTagCount = new LinkedHashMap<>();
        for (String dim : TagOceanWeights.DIMENSIONS) {
            dimTagCount.put(dim, 0);
        }
        if (selectedTags != null) {
            for (String tagCode : selectedTags) {
                for (String dim : TagOceanWeights.weightsFor(tagCode).keySet()) {
                    dimTagCount.computeIfPresent(dim, (k, v) -> v + 1);
                }
            }
        }

        Map<String, BigDecimal> confidence = new LinkedHashMap<>();
        for (String dim : TagOceanWeights.DIMENSIONS) {
            BigDecimal raw = base.get(dim).add(BigDecimal.valueOf(dimTagCount.getOrDefault(dim, 0) * 0.05d));
            confidence.put(dim, raw.min(CONFIDENCE_CAP).setScale(2, RoundingMode.HALF_UP));
        }
        return confidence;
    }

    public Map<String, BigDecimal> getEffectiveOcean(UserPortrait portrait) {
        return getEffectiveOcean(portrait, getMergedOcean(portrait));
    }

    public Map<String, BigDecimal> getMergedOcean(UserPortrait portrait) {
        Map<String, BigDecimal> merged = new LinkedHashMap<>();
        if (portrait == null) {
            return merged;
        }
        merged.put("O", mergeValue(portrait.getOceanOLong(), portrait.getOceanOShort()));
        merged.put("C", mergeValue(portrait.getOceanCLong(), portrait.getOceanCShort()));
        merged.put("E", mergeValue(portrait.getOceanELong(), portrait.getOceanEShort()));
        merged.put("A", mergeValue(portrait.getOceanALong(), portrait.getOceanAShort()));
        merged.put("N", mergeValue(portrait.getOceanNLong(), portrait.getOceanNShort()));
        return merged;
    }

    public Map<String, BigDecimal> getEffectiveOcean(UserPortrait portrait, Map<String, BigDecimal> mergedOcean) {
        Map<String, BigDecimal> source = mergedOcean != null ? mergedOcean : getMergedOcean(portrait);
        if (portrait == null) return source;
        if (Boolean.TRUE.equals(portrait.getHasRealOcean())) {
            return roundOceanMap(source);
        }
        Map<String, BigDecimal> confidence = getOrComputeConfidence(portrait);
        Map<String, BigDecimal> effective = new LinkedHashMap<>();
        for (String dim : TagOceanWeights.DIMENSIONS) {
            BigDecimal raw = source.get(dim);
            if (raw == null) {
                effective.put(dim, null);
                continue;
            }
            BigDecimal conf = confidence.getOrDefault(dim, BigDecimal.valueOf(0.25d));
            BigDecimal value = raw.multiply(conf).add(MIDPOINT.multiply(BigDecimal.ONE.subtract(conf)));
            effective.put(dim, value.setScale(1, RoundingMode.HALF_UP));
        }
        return effective;
    }

    public Map<String, BigDecimal> getOrComputeConfidence(UserPortrait portrait) {
        if (portrait == null) return Map.of();
        Map<String, BigDecimal> parsed = parseConfidence(portrait.getOceanConfidence());
        if (!parsed.isEmpty()) return parsed;
        Set<String> tags = InterestTagConverter.extractCodesFromNewFormat(portrait.getInterestTags());
        return computeOceanConfidence(portrait.getMbti(), tags, isQuestionnaireFilled(portrait));
    }

    public Map<String, BigDecimal> parseConfidence(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            Map<String, BigDecimal> raw = objectMapper.readValue(json, new TypeReference<Map<String, BigDecimal>>() {});
            Map<String, BigDecimal> parsed = new LinkedHashMap<>();
            for (String dim : TagOceanWeights.DIMENSIONS) {
                BigDecimal value = raw.get(dim);
                if (value != null) parsed.put(dim, value.setScale(2, RoundingMode.HALF_UP));
            }
            return parsed;
        } catch (Exception e) {
            log.debug("parseConfidence failed: {}", e.getMessage());
            return Map.of();
        }
    }

    public String toJson(Map<String, BigDecimal> confidence) {
        if (confidence == null || confidence.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(confidence);
        } catch (Exception e) {
            log.warn("serialize confidence failed: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, BigDecimal> buildBaseConfidence(String mbti, boolean questionnaireFilled) {
        boolean hasMbti = mbti != null && mbti.length() >= 4;
        Map<String, BigDecimal> base = new LinkedHashMap<>();
        if (hasMbti) {
            base.put("O", BigDecimal.valueOf(0.55d));
            base.put("C", BigDecimal.valueOf(0.55d));
            base.put("E", BigDecimal.valueOf(0.65d));
            base.put("A", BigDecimal.valueOf(0.50d));
            base.put("N", BigDecimal.valueOf(questionnaireFilled ? 0.45d : 0.25d));
        } else {
            base.put("O", BigDecimal.valueOf(0.25d));
            base.put("C", BigDecimal.valueOf(0.25d));
            base.put("E", BigDecimal.valueOf(0.25d));
            base.put("A", BigDecimal.valueOf(0.20d));
            base.put("N", BigDecimal.valueOf(questionnaireFilled ? 0.45d : 0.15d));
        }
        return base;
    }

    private boolean isQuestionnaireFilled(UserPortrait portrait) {
        return portrait.getSocialStyle() != null
                || portrait.getLifeRhythm() != null
                || portrait.getPersonalityBase() != null
                || portrait.getIntimacyPace() != null
                || (portrait.getQuestionnaireSnapshot() != null && !portrait.getQuestionnaireSnapshot().isBlank());
    }

    private BigDecimal mergeValue(BigDecimal longValue, BigDecimal shortValue) {
        if (longValue == null && shortValue == null) return null;
        if (longValue == null) return shortValue.setScale(1, RoundingMode.HALF_UP);
        if (shortValue == null) return longValue.setScale(1, RoundingMode.HALF_UP);
        return longValue.multiply(MERGE_LONG_WEIGHT)
                .add(shortValue.multiply(MERGE_SHORT_WEIGHT))
                .setScale(1, RoundingMode.HALF_UP);
    }

    private Map<String, BigDecimal> roundOceanMap(Map<String, BigDecimal> source) {
        Map<String, BigDecimal> rounded = new LinkedHashMap<>();
        for (String dim : TagOceanWeights.DIMENSIONS) {
            BigDecimal value = source.get(dim);
            rounded.put(dim, value != null ? value.setScale(1, RoundingMode.HALF_UP) : null);
        }
        return rounded;
    }
}
