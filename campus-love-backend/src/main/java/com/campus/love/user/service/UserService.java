package com.campus.love.user.service;

import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.common.utils.BaziUtil;
import com.campus.love.common.utils.ZodiacUtil;
import com.campus.love.user.dto.UserProfileRequest;
import com.campus.love.user.dto.UserProfileResponse;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    @Value("${app.upload.path}")
    private String uploadPath;

    /**
     * 获取用户资料（本人或他人）。返回为公开资料，不含 password/email 等敏感字段；若需区分本人与他人展示，在 toProfileResponse 或 Controller 层处理。
     */
    public UserProfileResponse getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);
        return toProfileResponse(user);
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
        return toProfileResponse(user);
    }

    public String uploadAvatar(MultipartFile file) throws IOException {
        Long userId = CurrentUser.getId();
        String ext = getFileExtension(file.getOriginalFilename());
        String filename = "avatar_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;

        File dir = new File(uploadPath);
        if (!dir.exists()) dir.mkdirs();

        File dest = new File(dir, filename);
        file.transferTo(dest);

        String avatarUrl = "/uploads/" + filename;
        User user = userMapper.selectById(userId);
        user.setAvatarUrl(avatarUrl);
        userMapper.updateById(user);

        return avatarUrl;
    }

    private String getFileExtension(String filename) {
        if (filename == null) return ".jpg";
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 ? filename.substring(dotIndex) : ".jpg";
    }

    private UserProfileResponse toProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .gender(user.getGender())
                .birthDate(user.getBirthDate() != null ? user.getBirthDate().toString() : null)
                .birthTime(user.getBirthTime() != null ? user.getBirthTime().toString() : null)
                .school(user.getSchool())
                .major(user.getMajor())
                .grade(user.getGrade())
                .activityScore(user.getActivityScore() != null ? user.getActivityScore() : 0)
                .userLevel(user.getUserLevel() != null ? user.getUserLevel() : 1)
                .isAdmin(user.getIsAdmin() != null ? user.getIsAdmin() : false)
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
                .build();
    }
}
