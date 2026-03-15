package com.campus.love.profile.service;

import com.campus.love.ai.rag.EmbeddingService;
import com.campus.love.profile.entity.UserAiProfile;
import com.campus.love.profile.entity.UserPortrait;
import com.campus.love.profile.entity.UserProfileVector;
import com.campus.love.profile.mapper.UserAiProfileMapper;
import com.campus.love.profile.mapper.UserProfileVectorMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OceanUpdateService {

    private static final BigDecimal MIDPOINT = BigDecimal.valueOf(50d);

    private final BehaviorAggregationService behaviorAggregationService;
    private final UserPortraitService userPortraitService;
    private final UserAiProfileMapper userAiProfileMapper;
    private final UserProfileVectorMapper userProfileVectorMapper;
    private final EmbeddingService embeddingService;

    public void updateShortOcean(Long userId) {
        UserPortrait portrait = userPortraitService.getPortrait(userId);
        if (portrait == null) return;
        behaviorAggregationService.aggregateAndSave(userId, 14);
        Map<String, BigDecimal> signals = behaviorAggregationService.buildDimensionScores(userId, 14);
        applyToShortPortrait(portrait, signals, BigDecimal.valueOf(3.0d));
        portrait.setHasRealOcean(true);
        portrait.setLastShortUpdate(LocalDate.now());
        userPortraitService.savePortrait(portrait);
        syncLegacyAiProfile(portrait);
    }

    public void updateLongOcean(Long userId) {
        UserPortrait portrait = userPortraitService.getPortrait(userId);
        if (portrait == null) return;
        behaviorAggregationService.aggregateAndSave(userId, 180);
        Map<String, BigDecimal> signals = behaviorAggregationService.buildDimensionScores(userId, 180);
        applyToLongPortrait(portrait, signals, BigDecimal.valueOf(1.5d));
        portrait.setLastLongUpdate(LocalDate.now());
        userPortraitService.savePortrait(portrait);
        syncLegacyAiProfile(portrait);
        reembedProfile(portrait);
    }

    @Async
    public void updateShortOceanAsync(Long userId) {
        try {
            updateShortOcean(userId);
        } catch (Exception e) {
            log.warn("Async short ocean update failed for {}: {}", userId, e.getMessage());
        }
    }

    public void resetShortToLong(Long userId) {
        UserPortrait portrait = userPortraitService.getPortrait(userId);
        if (portrait == null) return;
        portrait.setOceanOShort(copy(portrait.getOceanOLong()));
        portrait.setOceanCShort(copy(portrait.getOceanCLong()));
        portrait.setOceanEShort(copy(portrait.getOceanELong()));
        portrait.setOceanAShort(copy(portrait.getOceanALong()));
        portrait.setOceanNShort(copy(portrait.getOceanNLong()));
        portrait.setLastShortUpdate(LocalDate.now());
        userPortraitService.savePortrait(portrait);
        syncLegacyAiProfile(portrait);
    }

    private void applyToShortPortrait(UserPortrait portrait, Map<String, BigDecimal> signals, BigDecimal maxDelta) {
        Map<String, BigDecimal> current = currentShort(portrait);
        Map<String, BigDecimal> longBaseline = currentLong(portrait);
        portrait.setOceanOShort(updateValue(current.get("O"), longBaseline.get("O"), signals.get("O"), maxDelta));
        portrait.setOceanCShort(updateValue(current.get("C"), longBaseline.get("C"), signals.get("C"), maxDelta));
        portrait.setOceanEShort(updateValue(current.get("E"), longBaseline.get("E"), signals.get("E"), maxDelta));
        portrait.setOceanAShort(updateValue(current.get("A"), longBaseline.get("A"), signals.get("A"), maxDelta));
        portrait.setOceanNShort(updateValue(current.get("N"), longBaseline.get("N"), signals.get("N"), maxDelta));
    }

    private void applyToLongPortrait(UserPortrait portrait, Map<String, BigDecimal> signals, BigDecimal maxDelta) {
        Map<String, BigDecimal> current = currentLong(portrait);
        portrait.setOceanOLong(updateValue(current.get("O"), null, signals.get("O"), maxDelta));
        portrait.setOceanCLong(updateValue(current.get("C"), null, signals.get("C"), maxDelta));
        portrait.setOceanELong(updateValue(current.get("E"), null, signals.get("E"), maxDelta));
        portrait.setOceanALong(updateValue(current.get("A"), null, signals.get("A"), maxDelta));
        portrait.setOceanNLong(updateValue(current.get("N"), null, signals.get("N"), maxDelta));
    }

    private BigDecimal updateValue(BigDecimal current, BigDecimal baseline, BigDecimal signal, BigDecimal maxDelta) {
        BigDecimal base = current != null ? current : (baseline != null ? baseline : MIDPOINT);
        if (signal == null) return base.setScale(1, RoundingMode.HALF_UP);
        BigDecimal delta = signal.subtract(base);
        if (delta.compareTo(maxDelta) > 0) delta = maxDelta;
        if (delta.compareTo(maxDelta.negate()) < 0) delta = maxDelta.negate();
        return base.add(delta).setScale(1, RoundingMode.HALF_UP);
    }

    private Map<String, BigDecimal> currentShort(UserPortrait portrait) {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        values.put("O", portrait.getOceanOShort());
        values.put("C", portrait.getOceanCShort());
        values.put("E", portrait.getOceanEShort());
        values.put("A", portrait.getOceanAShort());
        values.put("N", portrait.getOceanNShort());
        return values;
    }

    private Map<String, BigDecimal> currentLong(UserPortrait portrait) {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        values.put("O", portrait.getOceanOLong());
        values.put("C", portrait.getOceanCLong());
        values.put("E", portrait.getOceanELong());
        values.put("A", portrait.getOceanALong());
        values.put("N", portrait.getOceanNLong());
        return values;
    }

    private void syncLegacyAiProfile(UserPortrait portrait) {
        UserAiProfile profile = userAiProfileMapper.selectById(portrait.getUserId());
        if (profile == null) return;
        profile.setOceanOLong(portrait.getOceanOLong());
        profile.setOceanCLong(portrait.getOceanCLong());
        profile.setOceanELong(portrait.getOceanELong());
        profile.setOceanALong(portrait.getOceanALong());
        profile.setOceanNLong(portrait.getOceanNLong());
        profile.setOceanOShort(portrait.getOceanOShort());
        profile.setOceanCShort(portrait.getOceanCShort());
        profile.setOceanEShort(portrait.getOceanEShort());
        profile.setOceanAShort(portrait.getOceanAShort());
        profile.setOceanNShort(portrait.getOceanNShort());
        profile.setOceanConfidence(portrait.getOceanConfidence());
        profile.setHasRealOcean(portrait.getHasRealOcean());
        profile.setLastLongUpdate(portrait.getLastLongUpdate());
        profile.setLastShortUpdate(portrait.getLastShortUpdate());
        userAiProfileMapper.updateById(profile);
    }

    private void reembedProfile(UserPortrait portrait) {
        try {
            String text = buildEmbeddingText(portrait);
            String vectorJson = embeddingService.embedAsJson(text);
            if (vectorJson == null) return;
            UserProfileVector vector = userProfileVectorMapper.selectById(portrait.getUserId());
            if (vector == null) {
                vector = new UserProfileVector();
                vector.setUserId(portrait.getUserId());
                vector.setProfileVector(vectorJson);
                userProfileVectorMapper.insert(vector);
            } else {
                vector.setProfileVector(vectorJson);
                userProfileVectorMapper.updateById(vector);
            }
        } catch (Exception e) {
            log.warn("Re-embed profile failed for {}: {}", portrait.getUserId(), e.getMessage());
        }
    }

    private String buildEmbeddingText(UserPortrait portrait) {
        return String.format("mbti=%s interests=%s oceanLong=%s,%s,%s,%s,%s oceanShort=%s,%s,%s,%s,%s",
                portrait.getMbti(),
                portrait.getInterestTags(),
                val(portrait.getOceanOLong()), val(portrait.getOceanCLong()), val(portrait.getOceanELong()), val(portrait.getOceanALong()), val(portrait.getOceanNLong()),
                val(portrait.getOceanOShort()), val(portrait.getOceanCShort()), val(portrait.getOceanEShort()), val(portrait.getOceanAShort()), val(portrait.getOceanNShort()));
    }

    private String val(BigDecimal value) {
        return value == null ? "null" : value.toPlainString();
    }

    private BigDecimal copy(BigDecimal value) {
        return value == null ? null : value.setScale(1, RoundingMode.HALF_UP);
    }
}
