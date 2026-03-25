package com.campus.love.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.auth.dto.AuthResponse;
import com.campus.love.auth.dto.LoginRequest;
import com.campus.love.auth.dto.RegisterRequest;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.common.service.RateLimitService;
import com.campus.love.common.utils.JwtUtil;
import com.campus.love.profile.entity.UserPortrait;
import com.campus.love.profile.service.OceanUpdateService;
import com.campus.love.profile.service.UserPortraitService;
import com.campus.love.profile.service.UserStatsService;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import com.campus.love.user.service.UserIdAllocator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final com.campus.love.auth.service.EmailVerifyService emailVerifyService;
    private final com.campus.love.auth.service.SchoolService schoolService;
    private final RateLimitService rateLimitService;
    private final UserPortraitService userPortraitService;
    private final UserStatsService userStatsService;
    private final OceanUpdateService oceanUpdateService;
    private final UserIdAllocator userIdAllocator;

    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail();
        if (email == null || !email.contains("@") || email.indexOf("@") >= email.length() - 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "邮箱格式不正确");
        }
        if (request.getSchool() != null && !request.getSchool().isBlank()) {
            if (!schoolService.isEmailSuffixMatch(email, request.getSchool())) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "邮箱后缀与所选学校不匹配，请使用该校邮箱注册");
            }
        }
        String normalizedEmail = email.trim().toLowerCase();

        if (!emailVerifyService.verifyCode(email, request.getVerifyCode())) {
            rateLimitService.checkAndIncrement(RateLimitService.LimitType.REGISTER_EMAIL, normalizedEmail);
            throw new BusinessException(ResultCode.VERIFY_CODE_INVALID);
        }

        Long existCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail()));
        if (existCount > 0) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        User user = new User();
        user.setId(userIdAllocator.allocateEightDigitUserId());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setSchool(request.getSchool());
        user.setGender(0);
        user.setProfileComplete(false);
        user.setStatus(1);
        userMapper.insert(user);

        userPortraitService.savePortrait(createEmptyPortrait(user.getId()));
        userStatsService.createStats(user.getId());

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail()));
        if (user == null) {
            throw new BusinessException(ResultCode.INVALID_CREDENTIALS);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.INVALID_CREDENTIALS);
        }

        handleOceanReactivation(user.getId());

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail());

        boolean isAdmin = Boolean.TRUE.equals(user.getIsAdmin());
        if (!isAdmin && user.getId() != null) {
            User refetch = userMapper.selectById(user.getId());
            isAdmin = refetch != null && Boolean.TRUE.equals(refetch.getIsAdmin());
        }

        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .profileComplete(user.getProfileComplete())
                .isAdmin(isAdmin)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    private UserPortrait createEmptyPortrait(Long userId) {
        UserPortrait p = new UserPortrait();
        p.setUserId(userId);
        p.setQuestionnaireVersion(1);
        p.setProfileVersion(1);
        return p;
    }

    private void handleOceanReactivation(Long userId) {
        if (userId == null) return;
        try {
            UserPortrait portrait = userPortraitService.getPortrait(userId);
            if (portrait == null) return;
            if (portrait.getLastShortUpdate() != null
                    && portrait.getLastShortUpdate().isBefore(java.time.LocalDate.now().minusDays(30))) {
                oceanUpdateService.resetShortToLong(userId);
            }
            oceanUpdateService.updateShortOceanAsync(userId);
        } catch (Exception e) {
            // 登录流程不应因画像更新失败而中断
        }
    }
}
