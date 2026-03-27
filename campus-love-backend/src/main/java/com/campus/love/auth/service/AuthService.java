package com.campus.love.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.auth.config.WechatMiniConfig;
import com.campus.love.auth.dto.AuthResponse;
import com.campus.love.auth.dto.LoginRequest;
import com.campus.love.auth.dto.RegisterRequest;
import com.campus.love.auth.dto.WechatCompleteRequest;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;

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
    private final ObjectMapper objectMapper;
    private final WechatMiniConfig wechatMiniConfig;

    private static final HttpClient WECHAT_HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

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

    public AuthResponse loginByWechatCode(String code) {
        WechatSessionResult session = requestWechatSession(code);
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getWechatOpenid, session.openid()));
        if (user == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该微信尚未绑定账号，请先使用邮箱登录后绑定微信");
        }
        handleOceanReactivation(user.getId());
        return buildAuthResponse(user);
    }

    public AuthResponse completeWechatWithEmail(WechatCompleteRequest request) {
        WechatSessionResult session = requestWechatSession(request.getCode());

        User byOpenid = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getWechatOpenid, session.openid()));
        if (byOpenid != null) {
            handleOceanReactivation(byOpenid.getId());
            return buildAuthResponse(byOpenid);
        }

        String email = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase();
        if (!email.contains("@") || email.indexOf("@") >= email.length() - 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "邮箱格式不正确");
        }
        if (!emailVerifyService.verifyCode(email, request.getVerifyCode())) {
            throw new BusinessException(ResultCode.VERIFY_CODE_INVALID);
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            String nickname = request.getNickname() == null ? "" : request.getNickname().trim();
            String password = request.getPassword() == null ? "" : request.getPassword();
            if (!StringUtils.hasText(nickname)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "该邮箱未注册，请填写昵称后完成注册");
            }
            if (password.length() < 6 || password.length() > 32) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "该邮箱未注册，请设置6-32位密码");
            }

            user = new User();
            user.setId(userIdAllocator.allocateEightDigitUserId());
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setNickname(nickname);
            user.setGender(0);
            user.setProfileComplete(false);
            user.setStatus(1);
            user.setWechatOpenid(session.openid());
            user.setWechatUnionid(session.unionid());
            user.setWechatBoundAt(LocalDateTime.now());
            userMapper.insert(user);

            userPortraitService.savePortrait(createEmptyPortrait(user.getId()));
            userStatsService.createStats(user.getId());
            return buildAuthResponse(user);
        }

        User occupied = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getWechatOpenid, session.openid()));
        if (occupied != null && !occupied.getId().equals(user.getId())) {
            throw new BusinessException(ResultCode.CONFLICT, "该微信已绑定其他账号");
        }

        user.setWechatOpenid(session.openid());
        if (StringUtils.hasText(session.unionid())) {
            user.setWechatUnionid(session.unionid());
        }
        if (user.getWechatBoundAt() == null) {
            user.setWechatBoundAt(LocalDateTime.now());
        }
        userMapper.updateById(user);

        handleOceanReactivation(user.getId());
        return buildAuthResponse(user);
    }

    public void bindWechat(Long userId, String code) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录后再绑定微信");
        }
        User currentUser = userMapper.selectById(userId);
        if (currentUser == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        WechatSessionResult session = requestWechatSession(code);

        User occupied = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getWechatOpenid, session.openid()));
        if (occupied != null && !occupied.getId().equals(userId)) {
            throw new BusinessException(ResultCode.CONFLICT, "该微信已绑定其他账号");
        }

        currentUser.setWechatOpenid(session.openid());
        if (StringUtils.hasText(session.unionid())) {
            currentUser.setWechatUnionid(session.unionid());
        }
        if (currentUser.getWechatBoundAt() == null) {
            currentUser.setWechatBoundAt(LocalDateTime.now());
        }
        userMapper.updateById(currentUser);
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
                .wechatBound(StringUtils.hasText(user.getWechatOpenid()))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    private WechatSessionResult requestWechatSession(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "微信登录凭证不能为空");
        }
        try {
            String endpoint = wechatMiniConfig.requiredCode2SessionUrl()
                    + "?appid=" + URLEncoder.encode(wechatMiniConfig.requiredMiniAppId(), StandardCharsets.UTF_8)
                    + "&secret=" + URLEncoder.encode(wechatMiniConfig.requiredMiniAppSecret(), StandardCharsets.UTF_8)
                    + "&js_code=" + URLEncoder.encode(code.trim(), StandardCharsets.UTF_8)
                    + "&grant_type=authorization_code";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> response = WECHAT_HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(ResultCode.INTERNAL_ERROR, "微信登录服务暂时不可用，请稍后重试");
            }

            JsonNode root = objectMapper.readTree(response.body());
            int errCode = root.path("errcode").asInt(0);
            if (errCode != 0) {
                if (errCode == 40029) {
                    throw new BusinessException(ResultCode.BAD_REQUEST, "微信登录凭证已失效，请重试");
                }
                throw new BusinessException(ResultCode.BAD_REQUEST, "微信登录失败，请稍后再试");
            }

            String openid = root.path("openid").asText("");
            if (!StringUtils.hasText(openid)) {
                throw new BusinessException(ResultCode.INTERNAL_ERROR, "微信登录失败，请稍后重试");
            }
            String unionid = root.path("unionid").asText("");
            return new WechatSessionResult(openid.trim(), StringUtils.hasText(unionid) ? unionid.trim() : null);
        } catch (BusinessException e) {
            throw e;
        } catch (IllegalStateException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "微信登录未配置，请联系管理员");
        } catch (Exception e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "微信登录服务暂时不可用，请稍后重试");
        }
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

    private record WechatSessionResult(String openid, String unionid) {}
}
