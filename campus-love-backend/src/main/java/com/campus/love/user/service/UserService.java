package com.campus.love.user.service;

import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.common.service.FileUploadService;
import com.campus.love.common.utils.BaziUtil;
import com.campus.love.common.utils.ZodiacUtil;
import com.campus.love.ai.skill.UserProfileSkill;
import com.campus.love.profile.mapper.UserAiProfileMapper;
import com.campus.love.profile.entity.UserAiProfile;
import com.campus.love.user.dto.UserAiProfileResponse;
import com.campus.love.user.dto.UserProfileRequest;
import com.campus.love.user.dto.UserProfileResponse;
import com.campus.love.user.dto.UserSearchItemResponse;
import com.campus.love.user.entity.User;
import com.campus.love.user.entity.UserIceBreakAllow;
import com.campus.love.feed.constants.VisibilityConstants;
import com.campus.love.follow.service.FollowService;
import com.campus.love.user.mapper.UserMapper;
import com.campus.love.user.mapper.UserIceBreakAllowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final FileUploadService fileUploadService;
    private final UserProfileSkill userProfileSkill;
    private final UserAiProfileMapper userAiProfileMapper;
    private final UserIceBreakAllowMapper userIceBreakAllowMapper;
    private final FollowService followService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    /**
     * 获取用户资料（本人或他人）。返回为公开资料，不含 password/email 等敏感字段；若需区分本人与他人展示，在 toProfileResponse 或 Controller 层处理。
     */
    /**
     * 按昵称搜索用户（用于全局搜索框），排除当前用户，最多返回 limit 条。
     */
    public List<UserSearchItemResponse> searchUsers(String keyword, int limit) {
        Long currentUserId = CurrentUser.getId();
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        String k = keyword.trim();
        if (k.length() < 2) {
            return List.of();
        }
        int size = Math.min(Math.max(limit, 1), 20);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .like(User::getNickname, k)
                .ne(currentUserId != null, User::getId, currentUserId)
                .select(User::getId, User::getNickname, User::getAvatarUrl)
                .last("LIMIT " + size);
        return userMapper.selectList(wrapper).stream()
                .map(u -> new UserSearchItemResponse(u.getId(), u.getNickname(), u.getAvatarUrl()))
                .collect(Collectors.toList());
    }

    public UserProfileResponse getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);
        Long currentUserId = CurrentUser.getId();
        boolean isSelf = userId.equals(currentUserId);
        return toProfileResponse(user, isSelf);
    }

    public UserProfileResponse updateProfile(UserProfileRequest request) {
        Long userId = CurrentUser.getId();
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);

        user.setNickname(request.getNickname());
        user.setGender(request.getGender());
        user.setSchool(request.getSchool());
        user.setMajor(request.getMajor());
        user.setGrade(request.getGrade());
        user.setMbti(request.getMbti());
        user.setBio(request.getBio());
        user.setInterests(request.getInterests());
        if (request.getFeedVisibility() != null && !request.getFeedVisibility().isEmpty()) {
            String v = request.getFeedVisibility().toUpperCase();
            if (VisibilityConstants.ALL.equals(v) || VisibilityConstants.FOLLOWING.equals(v)
                    || VisibilityConstants.FOLLOWERS.equals(v) || VisibilityConstants.FRIENDS.equals(v)
                    || VisibilityConstants.SELF.equals(v)) {
                user.setFeedVisibility(v);
            }
        }

        if (request.getBirthDate() != null) {
            LocalDate birthDate = LocalDate.parse(request.getBirthDate());
            user.setBirthDate(birthDate);
            user.setZodiac(ZodiacUtil.getZodiac(birthDate));

            boolean baziUnknown = Boolean.TRUE.equals(request.getBaziUnknown());
            user.setBaziUnknown(baziUnknown);
            LocalTime birthTime = null;
            if (!baziUnknown && request.getBirthTime() != null && !request.getBirthTime().isEmpty()) {
                birthTime = LocalTime.parse(request.getBirthTime());
            }
            user.setBirthTime(birthTime);
            user.setBazi(BaziUtil.getBazi(birthDate, birthTime));
        }

        boolean complete = user.getNickname() != null && user.getGender() != null
                && user.getBirthDate() != null && user.getMbti() != null
                && user.getInterests() != null && !user.getInterests().isEmpty();
        boolean wasIncomplete = !Boolean.TRUE.equals(user.getProfileComplete());
        user.setProfileComplete(complete);

        userMapper.updateById(user);

        if (complete && wasIncomplete && userAiProfileMapper.selectById(userId) == null) {
            userProfileSkill.generateInitialProfile(userId, user);
        }
        return toProfileResponse(user, true);
    }

    public UserProfileResponse updateNickname(String nickname) {
        Long userId = CurrentUser.getId();
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "昵称不能为空");
        }
        user.setNickname(nickname.trim());
        userMapper.updateById(user);
        return toProfileResponse(user, true);
    }

    public UserProfileResponse updateFeedVisibility(String visibility) {
        Long userId = CurrentUser.getId();
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);
        String v = visibility != null ? visibility.trim().toUpperCase() : VisibilityConstants.ALL;
        if (!VisibilityConstants.ALL.equals(v) && !VisibilityConstants.FOLLOWING.equals(v)
                && !VisibilityConstants.FOLLOWERS.equals(v) && !VisibilityConstants.FRIENDS.equals(v)
                && !VisibilityConstants.SELF.equals(v)) {
            v = VisibilityConstants.ALL;
        }
        user.setFeedVisibility(v);
        userMapper.updateById(user);
        return toProfileResponse(user, true);
    }

    public UserProfileResponse updateIceBreakEnabled(boolean enabled) {
        Long userId = CurrentUser.getId();
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);
        user.setIceBreakEnabled(enabled);
        userMapper.updateById(user);
        return toProfileResponse(user, true);
    }

    /** 按好友单独设置：允许/禁止对方使用破冰（需互关） */
    public void updateIceBreakAllow(Long targetUserId, boolean allowed) {
        Long myId = CurrentUser.getId();
        if (myId == null || targetUserId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "参数无效");
        }
        if (!followService.isMutual(myId, targetUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅互关好友可设置");
        }
        if (allowed) {
            UserIceBreakAllow existing = userIceBreakAllowMapper.selectOne(
                    new LambdaQueryWrapper<UserIceBreakAllow>()
                            .eq(UserIceBreakAllow::getUserId, myId)
                            .eq(UserIceBreakAllow::getAllowedUserId, targetUserId));
            if (existing == null) {
                UserIceBreakAllow allow = new UserIceBreakAllow();
                allow.setUserId(myId);
                allow.setAllowedUserId(targetUserId);
                userIceBreakAllowMapper.insert(allow);
            }
        } else {
            userIceBreakAllowMapper.delete(
                    new LambdaQueryWrapper<UserIceBreakAllow>()
                            .eq(UserIceBreakAllow::getUserId, myId)
                            .eq(UserIceBreakAllow::getAllowedUserId, targetUserId));
        }
    }

    public UserProfileResponse updateAiDisclosureSettings(java.util.Map<String, Boolean> settings) {
        Long userId = CurrentUser.getId();
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);
        if (settings == null || settings.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "授权设置不能为空");
        }
        try {
            user.setAiDisclosureSettings(objectMapper.writeValueAsString(settings));
        } catch (Exception e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "授权设置格式错误");
        }
        userMapper.updateById(user);
        return toProfileResponse(user, true);
    }

    public UserProfileResponse updateFeedVisibilityTime(Integer days) {
        Long userId = CurrentUser.getId();
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);
        int d = (days != null && (days == 3 || days == 30 || days == 180 || days == -1)) ? days : -1;
        user.setFeedVisibilityTime(d);
        userMapper.updateById(user);
        return toProfileResponse(user, true);
    }

    public String uploadAvatar(MultipartFile file) throws IOException {
        Long userId = CurrentUser.getId();
        String avatarUrl = fileUploadService.uploadImage(file, "avatar_" + userId + "_", 5L * 1024 * 1024);

        User user = userMapper.selectById(userId);
        user.setAvatarUrl(avatarUrl);
        userMapper.updateById(user);

        return avatarUrl;
    }

    public String uploadCover(MultipartFile file) throws IOException {
        Long userId = CurrentUser.getId();
        String coverUrl = fileUploadService.uploadImage(file, "cover_" + userId + "_", 5L * 1024 * 1024);

        User user = userMapper.selectById(userId);
        user.setCoverImageUrl(coverUrl);
        userMapper.updateById(user);

        return coverUrl;
    }

    public void clearCover() {
        Long userId = CurrentUser.getId();
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setCoverImageUrl(null);
            userMapper.updateById(user);
        }
    }

    /**
     * 获取当前用户的 AI 画像（本人可见，用于性格画像页）
     */
    public UserAiProfileResponse getMyAiProfile() {
        Long userId = CurrentUser.getId();
        UserAiProfile p = userAiProfileMapper.selectById(userId);
        if (p == null) {
            return UserAiProfileResponse.builder()
                    .userId(userId)
                    .hasRealOcean(false)
                    .oceanO(null)
                    .oceanC(null)
                    .oceanE(null)
                    .oceanA(null)
                    .oceanN(null)
                    .naturalLanguageTags(List.of())
                    .build();
        }
        return UserAiProfileResponse.builder()
                .userId(userId)
                .hasRealOcean(Boolean.TRUE.equals(p.getHasRealOcean()))
                .oceanO(p.getOceanOLong() != null ? p.getOceanOLong() : p.getOceanOShort())
                .oceanC(p.getOceanCLong() != null ? p.getOceanCLong() : p.getOceanCShort())
                .oceanE(p.getOceanELong() != null ? p.getOceanELong() : p.getOceanEShort())
                .oceanA(p.getOceanALong() != null ? p.getOceanALong() : p.getOceanAShort())
                .oceanN(p.getOceanNLong() != null ? p.getOceanNLong() : p.getOceanNShort())
                .naturalLanguageTags(parseTags(p.getNaturalLanguageTags()))
                .build();
    }

    private List<String> parseTags(String json) {
        if (json == null || json.trim().isEmpty()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 获取用户邮箱（用于密码修改）
     */
    public String getUserEmail(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null ? user.getEmail() : null;
    }

    /**
     * 更新用户密码
     */
    public void updatePassword(Long userId, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    private UserProfileResponse toProfileResponse(User user, boolean isSelf) {
        String birthDate = null;
        String birthTime = null;
        String bazi = null;
        Integer age = null;
        if (isSelf) {
            birthDate = user.getBirthDate() != null ? user.getBirthDate().toString() : null;
            birthTime = user.getBirthTime() != null ? user.getBirthTime().toString() : null;
            bazi = user.getBazi();
        } else if (user.getBirthDate() != null) {
            age = Period.between(user.getBirthDate(), LocalDate.now()).getYears();
        }
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(isSelf ? user.getEmail() : null)
                .nickname(user.getNickname())
                .gender(user.getGender())
                .birthDate(birthDate)
                .birthTime(birthTime)
                .age(age)
                .school(user.getSchool())
                .major(user.getMajor())
                .grade(user.getGrade())
                .activityScore(user.getActivityScore() != null ? user.getActivityScore() : 0)
                .userLevel(user.getUserLevel() != null ? user.getUserLevel() : 1)
                .isAdmin(isSelf ? (user.getIsAdmin() != null ? user.getIsAdmin() : false) : null)
                .creditScore(user.getCreditScore() != null ? user.getCreditScore() : 100)
                .inviteCount(user.getInviteCount() != null ? user.getInviteCount() : 0)
                .participateCount(user.getParticipateCount() != null ? user.getParticipateCount() : 0)
                .mbti(user.getMbti())
                .zodiac(user.getZodiac())
                .bazi(bazi)
                .baziUnknown(user.getBaziUnknown())
                .avatarUrl(user.getAvatarUrl())
                .coverImageUrl(user.getCoverImageUrl())
                .bio(user.getBio())
                .interests(user.getInterests())
                .profileComplete(user.getProfileComplete())
                .feedVisibility(user.getFeedVisibility() != null ? user.getFeedVisibility() : VisibilityConstants.ALL)
                .feedVisibilityTime(user.getFeedVisibilityTime() != null ? user.getFeedVisibilityTime() : -1)
                .iceBreakEnabled(isSelf ? user.getIceBreakEnabled() : null)
                .aiDisclosureSettings(isSelf ? user.getAiDisclosureSettings() : null)
                .build();
    }
}
