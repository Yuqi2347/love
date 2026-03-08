package com.campus.love.user.service;

import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.common.service.FileUploadService;
import com.campus.love.common.utils.BaziUtil;
import com.campus.love.common.utils.ZodiacUtil;
import com.campus.love.user.dto.UserProfileRequest;
import com.campus.love.user.dto.UserProfileResponse;
import com.campus.love.user.dto.UserSearchItemResponse;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final FileUploadService fileUploadService;

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
            if ("ALL".equals(v) || "FOLLOWERS".equals(v) || "SELF".equals(v)) {
                user.setFeedVisibility(v);
            }
        }

        if (request.getBirthDate() != null) {
            LocalDate birthDate = LocalDate.parse(request.getBirthDate());
            user.setBirthDate(birthDate);
            user.setZodiac(ZodiacUtil.getZodiac(birthDate));

            LocalTime birthTime = null;
            if (request.getBirthTime() != null && !request.getBirthTime().isEmpty()) {
                birthTime = LocalTime.parse(request.getBirthTime());
            }
            user.setBirthTime(birthTime);
            user.setBazi(BaziUtil.getBazi(birthDate, birthTime));
        }

        boolean complete = user.getNickname() != null && user.getGender() != null
                && user.getBirthDate() != null && user.getMbti() != null
                && user.getInterests() != null && !user.getInterests().isEmpty();
        user.setProfileComplete(complete);

        userMapper.updateById(user);
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
        String v = visibility != null ? visibility.trim().toUpperCase() : "ALL";
        if (!"ALL".equals(v) && !"FOLLOWERS".equals(v) && !"SELF".equals(v)) {
            v = "ALL";
        }
        user.setFeedVisibility(v);
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

    private UserProfileResponse toProfileResponse(User user, boolean isSelf) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(isSelf ? user.getEmail() : null)
                .nickname(user.getNickname())
                .gender(user.getGender())
                .birthDate(user.getBirthDate() != null ? user.getBirthDate().toString() : null)
                .birthTime(user.getBirthTime() != null ? user.getBirthTime().toString() : null)
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
                .bazi(user.getBazi())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .interests(user.getInterests())
                .profileComplete(user.getProfileComplete())
                .feedVisibility(user.getFeedVisibility() != null ? user.getFeedVisibility() : "ALL")
                .build();
    }
}
