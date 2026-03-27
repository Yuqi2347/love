package com.campus.love.notification.service;

import com.campus.love.auth.config.WechatMiniConfig;
import com.campus.love.common.enums.NotificationTypeEnum;
import com.campus.love.notification.config.WechatSubscribeConfig;
import com.campus.love.notification.dto.WechatSubscribeConfigResponse;
import com.campus.love.notification.entity.Notification;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 微信小程序订阅消息（站外提醒）。
 * 仅发送白名单三类事件，失败不会影响主流程与站内通知。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatSubscribeMessageService {

    private static final HttpClient WECHAT_HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final WechatMiniConfig wechatMiniConfig;
    private final WechatSubscribeConfig subscribeConfig;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    private final Object tokenLock = new Object();
    private volatile String cachedAccessToken;
    private volatile long cachedTokenExpireAtEpochMs;

    public WechatSubscribeConfigResponse getClientSubscribeConfig() {
        List<String> templateIds = new ArrayList<>(3);
        addIfText(templateIds, subscribeConfig.getTmplPairDateResultReady());
        addIfText(templateIds, subscribeConfig.getTmplInviteNewParticipant());
        addIfText(templateIds, subscribeConfig.getTmplInviteMatchFound());
        boolean enabled = subscribeConfig.isEnabled() && !templateIds.isEmpty();
        return WechatSubscribeConfigResponse.builder()
                .enabled(enabled)
                .templateIds(templateIds)
                .build();
    }

    @Async
    public void pushByNotificationAsync(Notification notification) {
        if (notification == null || !subscribeConfig.isEnabled()) {
            return;
        }
        try {
            NotificationTypeEnum type = parseType(notification.getType());
            if (type == null) {
                return;
            }
            String templateId = resolveTemplateId(type);
            if (!StringUtils.hasText(templateId)) {
                return;
            }

            Long receiverId = notification.getUserId();
            if (receiverId == null) {
                return;
            }
            User receiver = userMapper.selectById(receiverId);
            if (receiver == null || !StringUtils.hasText(receiver.getWechatOpenid())) {
                return;
            }

            String accessToken = getAccessToken();
            if (!StringUtils.hasText(accessToken)) {
                return;
            }
            String page = resolvePage(type, notification);
            Map<String, Object> body = buildPayload(receiver.getWechatOpenid(), templateId, page, notification);
            sendSubscribeMessage(accessToken, body, type, notification.getId(), receiverId);
        } catch (Exception e) {
            log.warn("微信订阅消息发送异常，notificationId={}", notification.getId(), e);
        }
    }

    private void sendSubscribeMessage(String accessToken, Map<String, Object> payload,
                                      NotificationTypeEnum type, Long notificationId, Long receiverId) throws Exception {
        String endpoint = subscribeConfig.getSendUrl()
                + "?access_token=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = WECHAT_HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("微信订阅消息HTTP失败，status={}, type={}, notificationId={}, receiverId={}",
                    response.statusCode(), type, notificationId, receiverId);
            return;
        }
        JsonNode root = objectMapper.readTree(response.body());
        int errCode = root.path("errcode").asInt(-1);
        if (errCode == 0) {
            return;
        }
        String errMsg = root.path("errmsg").asText("");
        if (errCode == 43101) {
            // 用户拒收/未订阅，不作为错误打点
            log.info("微信订阅消息用户未订阅或已拒绝，type={}, notificationId={}, receiverId={}, err={}",
                    type, notificationId, receiverId, errMsg);
            return;
        }
        log.warn("微信订阅消息发送失败，type={}, notificationId={}, receiverId={}, errCode={}, errMsg={}",
                type, notificationId, receiverId, errCode, errMsg);
    }

    private Map<String, Object> buildPayload(String openid, String templateId, String page, Notification notification) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("touser", openid);
        body.put("template_id", templateId);
        if (StringUtils.hasText(page)) {
            body.put("page", page);
        }
        body.put("data", buildTemplateData(notification));
        return body;
    }

    private Map<String, Object> buildTemplateData(Notification notification) {
        Map<String, Object> data = new LinkedHashMap<>();
        putTemplateField(data, subscribeConfig.getDataKeyTitle(), safeThing(notification.getTitle(), "校园提醒"));
        putTemplateField(data, subscribeConfig.getDataKeyTime(), formatTime(notification.getCreatedAt()));
        putTemplateField(data, subscribeConfig.getDataKeyRemark(), safeThing(notification.getContent(), "点击查看详情"));
        return data;
    }

    private static void putTemplateField(Map<String, Object> data, String key, String value) {
        if (!StringUtils.hasText(key) || !StringUtils.hasText(value)) {
            return;
        }
        data.put(key.trim(), Map.of("value", value.trim()));
    }

    private static String safeThing(String raw, String fallback) {
        String text = StringUtils.hasText(raw) ? raw.trim() : fallback;
        // 按 thing 类型常见限制控制长度，避免模板校验失败。
        return text.length() <= 20 ? text : text.substring(0, 20);
    }

    private static String formatTime(LocalDateTime time) {
        LocalDateTime target = time != null ? time : LocalDateTime.now();
        return TIME_FORMATTER.format(target);
    }

    private String resolveTemplateId(NotificationTypeEnum type) {
        return switch (type) {
            case PAIR_DATE_RESULT_READY -> subscribeConfig.getTmplPairDateResultReady();
            case INVITE_NEW_PARTICIPANT -> subscribeConfig.getTmplInviteNewParticipant();
            case INVITE_MATCH_FOUND -> subscribeConfig.getTmplInviteMatchFound();
            default -> null;
        };
    }

    private String resolvePage(NotificationTypeEnum type, Notification notification) {
        return switch (type) {
            case PAIR_DATE_RESULT_READY -> {
                if (notification.getRelatedId() != null) {
                    yield "pages/moment/pair-date/pair-date?negotiationId=" + notification.getRelatedId();
                }
                yield subscribeConfig.getPagePairDateResultReady();
            }
            case INVITE_NEW_PARTICIPANT -> {
                if (notification.getInviteId() != null) {
                    yield "pages/invite/detail/detail?id=" + notification.getInviteId();
                }
                yield subscribeConfig.getPageInviteNewParticipant();
            }
            case INVITE_MATCH_FOUND -> {
                if (notification.getInviteId() != null) {
                    yield "pages/invite/detail/detail?id=" + notification.getInviteId();
                }
                yield subscribeConfig.getPageInviteMatchFound();
            }
            default -> "";
        };
    }

    private NotificationTypeEnum parseType(String rawType) {
        if (!StringUtils.hasText(rawType)) {
            return null;
        }
        try {
            return NotificationTypeEnum.valueOf(rawType.trim());
        } catch (Exception ignore) {
            return null;
        }
    }

    private String getAccessToken() throws Exception {
        long now = System.currentTimeMillis();
        if (StringUtils.hasText(cachedAccessToken) && now < cachedTokenExpireAtEpochMs - 60_000) {
            return cachedAccessToken;
        }
        synchronized (tokenLock) {
            now = System.currentTimeMillis();
            if (StringUtils.hasText(cachedAccessToken) && now < cachedTokenExpireAtEpochMs - 60_000) {
                return cachedAccessToken;
            }
            String endpoint = subscribeConfig.getAccessTokenUrl()
                    + "?grant_type=client_credential"
                    + "&appid=" + URLEncoder.encode(wechatMiniConfig.requiredMiniAppId(), StandardCharsets.UTF_8)
                    + "&secret=" + URLEncoder.encode(wechatMiniConfig.requiredMiniAppSecret(), StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> response = WECHAT_HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("获取微信access_token失败, status=" + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            int errCode = root.path("errcode").asInt(0);
            if (errCode != 0) {
                throw new IllegalStateException("获取微信access_token失败, errCode=" + errCode);
            }
            String token = root.path("access_token").asText("");
            int expiresIn = root.path("expires_in").asInt(7200);
            if (!StringUtils.hasText(token)) {
                throw new IllegalStateException("微信access_token为空");
            }
            cachedAccessToken = token.trim();
            cachedTokenExpireAtEpochMs = System.currentTimeMillis() + Math.max(expiresIn, 300) * 1000L;
            return cachedAccessToken;
        }
    }

    private static void addIfText(List<String> list, String value) {
        if (StringUtils.hasText(value)) {
            list.add(value.trim());
        }
    }
}

