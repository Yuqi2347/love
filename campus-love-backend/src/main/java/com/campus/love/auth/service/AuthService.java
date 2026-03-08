package com.campus.love.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.auth.dto.AuthResponse;
import com.campus.love.auth.dto.LoginRequest;
import com.campus.love.auth.dto.RegisterRequest;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.common.utils.JwtUtil;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
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

    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail();
        if (email == null || !email.contains("@")) {
            throw new BusinessException(ResultCode.INVALID_CAMPUS_EMAIL);
        }
        String domain = email.substring(email.indexOf("@") + 1);
        if (!schoolService.isSupportedDomain(domain)) {
            throw new BusinessException(ResultCode.INVALID_CAMPUS_EMAIL);
        }

        if (!emailVerifyService.verifyCode(email, request.getVerifyCode())) {
            throw new BusinessException(ResultCode.VERIFY_CODE_INVALID);
        }

        Long existCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail()));
        if (existCount > 0) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setGender(0);
        user.setProfileComplete(false);
        user.setStatus(1);
        userMapper.insert(user);

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
}
