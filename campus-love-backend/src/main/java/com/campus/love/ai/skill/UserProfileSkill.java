package com.campus.love.ai.skill;

import com.campus.love.common.constants.TagOceanWeights;
import com.campus.love.common.utils.InterestTagConverter;
import com.campus.love.profile.entity.UserAiProfile;
import com.campus.love.profile.entity.UserPortrait;
import com.campus.love.profile.mapper.UserAiProfileMapper;
import com.campus.love.profile.service.OceanConfidenceService;
import com.campus.love.profile.service.UserPortraitService;
import com.campus.love.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 画像生成/增量更新（技术文档 V1.1.0）
 * 注册后异步初次画像、双窗口定时更新
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserProfileSkill {

    private final UserAiProfileMapper userAiProfileMapper;
    private final UserPortraitService userPortraitService;
    private final OceanConfidenceService oceanConfidenceService;

    /**
     * 注册后异步初次画像生成（基于 MBTI + 注册信息 → OCEAN 初始值 + 自然语言标签）
     */
    @Async
    public void generateInitialProfile(Long userId, User user) {
        doGenerateProfile(userId, user, false);
    }

    /**
     * 管理员手动触发画像生成（支持 force 覆盖已有画像）
     */
    @Async
    public void regenerateProfile(Long userId, User user, boolean force) {
        doGenerateProfile(userId, user, force);
    }

    private void doGenerateProfile(Long userId, User user, boolean force) {
        try {
            UserAiProfile existing = userAiProfileMapper.selectById(userId);
            if (existing != null && !force) {
                log.info("User {} already has AI profile, skip", userId);
                return;
            }
            UserAiProfile profile = existing != null ? existing : new UserAiProfile();
            if (existing == null) {
                profile.setUserId(userId);
                profile.setHasRealOcean(false);
                profile.setProfileVersion(1);
            } else {
                profile.setProfileVersion(profile.getProfileVersion() != null ? profile.getProfileVersion() + 1 : 1);
            }
            UserPortrait portrait = userPortraitService.getPortrait(userId);
            if (portrait == null) portrait = new UserPortrait();
            portrait.setUserId(userId);
            portrait.setMbti(user.getMbti());
            portrait.setZodiac(user.getZodiac());
            portrait.setBazi(user.getBazi());
            portrait.setBio(user.getBio());
            if (portrait.getInterestTags() == null || portrait.getInterestTags().isBlank()) {
                portrait.setInterestTags(InterestTagConverter.legacyToNewFormat(user.getInterests()));
            }

            Map<String, BigDecimal> ocean = mapMbtiToOcean(user.getMbti());
            applyTagCorrections(ocean, portrait.getInterestTags());
            Map<String, BigDecimal> confidence = oceanConfidenceService.computeOceanConfidence(
                    user.getMbti(),
                    InterestTagConverter.extractCodesFromNewFormat(portrait.getInterestTags()),
                    false
            );
            fillProfile(profile, ocean, confidence);

            if (existing != null) {
                userAiProfileMapper.updateById(profile);
            } else {
                userAiProfileMapper.insert(profile);
            }
            portrait.setOceanOLong(profile.getOceanOLong());
            portrait.setOceanCLong(profile.getOceanCLong());
            portrait.setOceanELong(profile.getOceanELong());
            portrait.setOceanALong(profile.getOceanALong());
            portrait.setOceanNLong(profile.getOceanNLong());
            portrait.setOceanOShort(profile.getOceanOShort());
            portrait.setOceanCShort(profile.getOceanCShort());
            portrait.setOceanEShort(profile.getOceanEShort());
            portrait.setOceanAShort(profile.getOceanAShort());
            portrait.setOceanNShort(profile.getOceanNShort());
            portrait.setOceanConfidence(profile.getOceanConfidence());
            portrait.setHasRealOcean(profile.getHasRealOcean());
            portrait.setNaturalLanguageTags(profile.getNaturalLanguageTags());
            portrait.setProfileVersion(profile.getProfileVersion());
            portrait.setLastLongUpdate(profile.getLastLongUpdate());
            portrait.setLastShortUpdate(profile.getLastShortUpdate());
            userPortraitService.savePortrait(portrait);
            log.info("AI profile {} for user {}", existing != null ? "updated" : "created", userId);
        } catch (Exception e) {
            log.warn("Profile generation failed for user {}: {}", userId, e.getMessage());
        }
    }

    private Map<String, BigDecimal> mapMbtiToOcean(String mbti) {
        Map<String, BigDecimal> ocean = new LinkedHashMap<>();
        ocean.put("O", BigDecimal.valueOf(50d));
        ocean.put("C", BigDecimal.valueOf(50d));
        ocean.put("E", BigDecimal.valueOf(50d));
        ocean.put("A", BigDecimal.valueOf(50d));
        ocean.put("N", BigDecimal.valueOf(50d));
        if (mbti == null || mbti.length() < 4) {
            return ocean;
        }
        double ei = mbti.charAt(0) == 'E' ? 0.6d : -0.6d;
        double sn = mbti.charAt(1) == 'N' ? 0.6d : -0.6d;
        double tf = mbti.charAt(2) == 'F' ? 0.6d : -0.6d;
        double jp = mbti.charAt(3) == 'P' ? 0.6d : -0.6d;
        ocean.put("O", scaled(50d + sn * 25d));
        ocean.put("C", scaled(50d + (-jp) * 20d));
        ocean.put("E", scaled(50d + ei * 25d));
        ocean.put("A", scaled(50d + tf * 18d));
        return ocean;
    }

    private void applyTagCorrections(Map<String, BigDecimal> ocean, String interestTagsJson) {
        List<InterestTagConverter.TagSelection> selections = InterestTagConverter.extractSelectionsFromNewFormat(interestTagsJson);
        for (InterestTagConverter.TagSelection selection : selections) {
            Map<String, Integer> weights = TagOceanWeights.weightsFor(selection.code());
            if (weights.isEmpty()) continue;
            double intensity = selection.intensity();
            for (Map.Entry<String, Integer> entry : weights.entrySet()) {
                BigDecimal current = ocean.getOrDefault(entry.getKey(), BigDecimal.valueOf(50d));
                BigDecimal next = current.add(BigDecimal.valueOf(entry.getValue() * intensity));
                ocean.put(entry.getKey(), scaled(next.doubleValue()));
            }
        }
    }

    private void fillProfile(UserAiProfile profile, Map<String, BigDecimal> ocean, Map<String, BigDecimal> confidence) {
        profile.setOceanOLong(ocean.get("O"));
        profile.setOceanCLong(ocean.get("C"));
        profile.setOceanELong(ocean.get("E"));
        profile.setOceanALong(ocean.get("A"));
        profile.setOceanNLong(ocean.get("N"));
        profile.setOceanOShort(ocean.get("O"));
        profile.setOceanCShort(ocean.get("C"));
        profile.setOceanEShort(ocean.get("E"));
        profile.setOceanAShort(ocean.get("A"));
        profile.setOceanNShort(ocean.get("N"));
        profile.setOceanConfidence(oceanConfidenceService.toJson(confidence));
    }

    private BigDecimal scaled(double value) {
        double clamped = Math.max(0d, Math.min(100d, value));
        return BigDecimal.valueOf(clamped).setScale(1, RoundingMode.HALF_UP);
    }
}
