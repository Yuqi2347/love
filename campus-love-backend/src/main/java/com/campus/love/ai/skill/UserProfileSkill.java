package com.campus.love.ai.skill;

import com.campus.love.ai.service.AiService;
import com.campus.love.profile.entity.UserAiProfile;
import com.campus.love.profile.mapper.UserAiProfileMapper;
import com.campus.love.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 画像生成/增量更新（技术文档 V1.1.0）
 * 注册后异步初次画像、双窗口定时更新
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserProfileSkill {

    private final AiService aiService;
    private final UserAiProfileMapper userAiProfileMapper;

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
            mapMbtiToOcean(user.getMbti(), profile);
            if (existing != null) {
                userAiProfileMapper.updateById(profile);
            } else {
                userAiProfileMapper.insert(profile);
            }
            log.info("AI profile {} for user {}", existing != null ? "updated" : "created", userId);
        } catch (Exception e) {
            log.warn("Profile generation failed for user {}: {}", userId, e.getMessage());
        }
    }

    private void mapMbtiToOcean(String mbti, UserAiProfile profile) {
        if (mbti == null || mbti.length() < 4) return;
        double e = mbti.charAt(0) == 'E' ? 7 : 3;
        double o = mbti.contains("N") ? 6 : 5;
        double a = mbti.contains("F") ? 6 : 5;
        double c = mbti.charAt(3) == 'J' ? 6 : 4;
        double n = 5;
        profile.setOceanELong(java.math.BigDecimal.valueOf(e));
        profile.setOceanOLong(java.math.BigDecimal.valueOf(o));
        profile.setOceanALong(java.math.BigDecimal.valueOf(a));
        profile.setOceanCLong(java.math.BigDecimal.valueOf(c));
        profile.setOceanNLong(java.math.BigDecimal.valueOf(n));
        profile.setOceanEShort(profile.getOceanELong());
        profile.setOceanOShort(profile.getOceanOLong());
        profile.setOceanAShort(profile.getOceanALong());
        profile.setOceanCShort(profile.getOceanCLong());
        profile.setOceanNShort(profile.getOceanNLong());
    }
}
