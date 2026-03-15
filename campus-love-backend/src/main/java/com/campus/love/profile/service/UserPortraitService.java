package com.campus.love.profile.service;

import com.campus.love.common.utils.InterestTagConverter;
import com.campus.love.profile.entity.UserPortrait;
import com.campus.love.profile.mapper.UserPortraitMapper;
import com.campus.love.profile.mapper.UserAiProfileMapper;
import com.campus.love.profile.entity.UserAiProfile;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import com.campus.love.moment.entity.MomentProfile;
import com.campus.love.moment.mapper.MomentProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户画像统一读写
 * 读取优先从 t_user_portrait，缺失时回退旧表（User + MomentProfile + UserAiProfile）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserPortraitService {

    private final UserPortraitMapper userPortraitMapper;
    private final UserMapper userMapper;
    private final MomentProfileMapper momentProfileMapper;
    private final UserAiProfileMapper userAiProfileMapper;

    /**
     * 获取画像，优先 t_user_portrait，缺失时从旧表组装
     */
    public UserPortrait getPortrait(Long userId) {
        UserPortrait p = userPortraitMapper.selectById(userId);
        if (p != null) return p;
        return buildFromLegacy(userId);
    }

    /**
     * 从旧表组装画像（双写过渡期兼容）
     */
    private UserPortrait buildFromLegacy(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return null;
        MomentProfile mp = momentProfileMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MomentProfile>()
                        .eq(MomentProfile::getUserId, userId));
        UserAiProfile aip = userAiProfileMapper.selectById(userId);

        UserPortrait p = new UserPortrait();
        p.setUserId(userId);
        p.setMbti(user.getMbti());
        p.setZodiac(user.getZodiac());
        p.setBazi(user.getBazi());
        p.setBio(user.getBio());
        p.setInterestTags(InterestTagConverter.legacyToNewFormat(user.getInterests()));
        p.setQuestionnaireVersion(1);

        if (mp != null) {
            p.setTargetGender(mp.getTargetGender());
            p.setSocialStyle(mp.getSocialStyle());
            p.setLifeRhythm(mp.getLifeRhythm());
            p.setCompanionshipStyle(mp.getCompanionshipStyle());
            p.setAppearanceRequirement(mp.getAppearanceRequirement());
            p.setPartnerPersonality(mp.getPartnerPersonality());
            p.setMajorPreference(mp.getMajorPreference());
            p.setAgeRangePreference(mp.getAgeRangePreference());
            p.setAgePreferenceMin(mp.getAgePreferenceMin());
            p.setAgePreferenceMax(mp.getAgePreferenceMax());
            p.setGradeRangeMin(mp.getGradeRangeMin());
            p.setGradeRangeMax(mp.getGradeRangeMax());
            p.setGradeRangePreference(mp.getGradeRangePreference());
            p.setPrioritizeMatching(mp.getPrioritizeMatching());
            p.setDateStyle(mp.getDateStyle());
            p.setIntimacyPace(mp.getIntimacyPace());
            p.setPremaritalCohabitation(mp.getPremaritalCohabitation());
            p.setFutureLifestyle(mp.getFutureLifestyle());
            p.setRelationshipCoreValue(mp.getRelationshipCoreValue());
            p.setAppearanceScore(mp.getAppearanceScore());
            p.setPersonalityBase(mp.getPersonalityBase());
            p.setCampusFocus(mp.getCampusFocus());
            p.setEmotionStyle(mp.getEmotionStyle());
            p.setCareerAmbitionPref(mp.getCareerAmbitionPref());
            p.setHonestyLevel(mp.getHonestyLevel());
            p.setPremaritalSex(mp.getPremaritalSex());
            p.setConflictStyle(mp.getConflictStyle());
            p.setSocialBoundary(mp.getSocialBoundary());
            p.setCampusLovePlan(mp.getCampusLovePlan());
            p.setIdolRole(mp.getIdolRole());
            p.setTemptationResponse(mp.getTemptationResponse());
            p.setRealityCondition(mp.getRealityCondition());
            p.setHumanNatureView(mp.getHumanNatureView());
            p.setBreakupView(mp.getBreakupView());
            p.setCareerLoveConflict(mp.getCareerLoveConflict());
            p.setEmotionPriority(mp.getEmotionPriority());
            p.setLifeGoalPriority(mp.getLifeGoalPriority());
        }

        if (aip != null) {
            p.setOceanOLong(aip.getOceanOLong());
            p.setOceanCLong(aip.getOceanCLong());
            p.setOceanELong(aip.getOceanELong());
            p.setOceanALong(aip.getOceanALong());
            p.setOceanNLong(aip.getOceanNLong());
            p.setOceanOShort(aip.getOceanOShort());
            p.setOceanCShort(aip.getOceanCShort());
            p.setOceanEShort(aip.getOceanEShort());
            p.setOceanAShort(aip.getOceanAShort());
            p.setOceanNShort(aip.getOceanNShort());
            p.setOceanConfidence(aip.getOceanConfidence());
            p.setHasRealOcean(aip.getHasRealOcean());
            p.setNaturalLanguageTags(aip.getNaturalLanguageTags());
            p.setLoveAttachmentType(aip.getLoveAttachmentType());
            p.setAttractedToTraits(aip.getAttractedToTraits());
            p.setFrictionPoints(aip.getFrictionPoints());
            p.setProfileVersion(aip.getProfileVersion());
            p.setLastLongUpdate(aip.getLastLongUpdate());
            p.setLastShortUpdate(aip.getLastShortUpdate());
        }
        return p;
    }

    @Transactional(rollbackFor = Exception.class)
    public void savePortrait(UserPortrait portrait) {
        if (portrait == null || portrait.getUserId() == null) return;
        UserPortrait existing = userPortraitMapper.selectById(portrait.getUserId());
        if (existing != null) {
            userPortraitMapper.updateById(portrait);
        } else {
            userPortraitMapper.insert(portrait);
        }
    }

    /**
     * 清空画像（用户注销/删除时）
     */
    @Transactional(rollbackFor = Exception.class)
    public void clearPortrait(Long userId) {
        userPortraitMapper.deleteById(userId);
    }

    /**
     * 获取 UserAiProfile 兼容视图（供现有消费者使用）
     */
    public UserAiProfile getAiProfileView(Long userId) {
        UserPortrait p = getPortrait(userId);
        if (p == null) return null;
        UserAiProfile aip = new UserAiProfile();
        aip.setUserId(userId);
        aip.setOceanOLong(p.getOceanOLong());
        aip.setOceanCLong(p.getOceanCLong());
        aip.setOceanELong(p.getOceanELong());
        aip.setOceanALong(p.getOceanALong());
        aip.setOceanNLong(p.getOceanNLong());
        aip.setOceanOShort(p.getOceanOShort());
        aip.setOceanCShort(p.getOceanCShort());
        aip.setOceanEShort(p.getOceanEShort());
        aip.setOceanAShort(p.getOceanAShort());
        aip.setOceanNShort(p.getOceanNShort());
        aip.setOceanConfidence(p.getOceanConfidence());
        aip.setHasRealOcean(p.getHasRealOcean());
        aip.setNaturalLanguageTags(p.getNaturalLanguageTags());
        aip.setLoveAttachmentType(p.getLoveAttachmentType());
        aip.setAttractedToTraits(p.getAttractedToTraits());
        aip.setFrictionPoints(p.getFrictionPoints());
        aip.setInterestTags(p.getInterestTags());
        aip.setProfileVersion(p.getProfileVersion());
        aip.setLastLongUpdate(p.getLastLongUpdate());
        aip.setLastShortUpdate(p.getLastShortUpdate());
        return aip;
    }
}
