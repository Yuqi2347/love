package com.campus.love.common.filter;

import com.campus.love.common.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * API 限流过滤器：根据路径与用户身份应用不同限流策略。
 * 需在 Security 链中注册，置于 JwtAuthenticationFilter 之后，以便获取当前用户。
 */
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path == null) path = "";

        try {
            if (path.contains("auth/register")) {
                rateLimitService.checkAndIncrement(RateLimitService.LimitType.REGISTER_IP, getClientIp(request));
            } else if (path.contains("auth/send-verify-code")) {
                rateLimitService.checkAndIncrement(RateLimitService.LimitType.VERIFY_CODE_IP, getClientIp(request));
            } else if (path.contains("/feed") && "POST".equals(request.getMethod())) {
                if (path.contains("/comment")) {
                    applyUserLimit(RateLimitService.LimitType.COMMENT_USER, request, filterChain, response);
                    return;
                }
                if (path.contains("/upload/")) {
                    filterChain.doFilter(request, response);
                    return;
                }
                applyUserLimit(RateLimitService.LimitType.POST_USER, request, filterChain, response);
                return;
            } else if (path.contains("/match/action") && "POST".equals(request.getMethod())) {
                applyUserLimit(RateLimitService.LimitType.MATCH_ACTION_USER, request, filterChain, response);
                return;
            } else if (path.contains("yuanfen-analysis") && "POST".equals(request.getMethod())) {
                applyUserLimit(RateLimitService.LimitType.YUANFEN_USER, request, filterChain, response);
                return;
            }
        } catch (Exception e) {
            if (e instanceof com.campus.love.common.exception.BusinessException be) {
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":429,\"message\":\"" + be.getResultCode().getMessage() + "\"}");
                return;
            }
            throw e;
        }

        filterChain.doFilter(request, response);
    }

    private void applyUserLimit(RateLimitService.LimitType type, HttpServletRequest request,
                                FilterChain filterChain, HttpServletResponse response) throws IOException, ServletException {
        Long userId = getCurrentUserId();
        if (userId == null) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            rateLimitService.checkAndIncrement(type, String.valueOf(userId));
        } catch (com.campus.love.common.exception.BusinessException e) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":429,\"message\":\"" + e.getResultCode().getMessage() + "\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long userId) {
            return userId;
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        String ip = request.getRemoteAddr();
        return ip != null ? ip : "unknown";
    }
}
