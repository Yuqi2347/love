package com.campus.love.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;
import org.springframework.util.StringUtils;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiConfig {

    /** API Key（Bearer token） */
    private String apiKey;

    /** OpenAI 兼容 API 基础 URL */
    private String baseUrl;

    /** 模型名称 */
    private String model;

    /** 单次请求最大 token */
    private int maxTokens = 1000;

    /** 调用 AI API 超时秒数（缘分解析等长文本生成需更长时间） */
    private int timeoutSeconds = 90;

    /**
     * 缘分解析冷却（小时）：-1=永久按「用户对」读库缓存；0=不读缓存、每次调 AI（调试用）；
     * &gt;0=仅使用该时间窗内的记录作「按对缓存」+「每用户每窗最多对一位新互关触发一次 AI」。
     */
    private long yuanfenCooldownHours = 1;

    /**
     * 缘分解析专用 max_tokens（通常略低于 {@link #maxTokens}，缩短模型生成长度与耗时）。
     * 配置项：ai.yuanfen-max-tokens / YUANFEN_MAX_TOKENS
     */
    private int yuanfenMaxTokens = 3072;

    /**
     * 缘分解析调用厂商 API 的超时（秒），与前端 axios 一致；默认 60。
     * 配置项：ai.yuanfen-timeout-seconds / YUANFEN_TIMEOUT
     */
    private int yuanfenTimeoutSeconds = 60;

    /**
     * 同对用户「进行中」互斥锁 TTL（秒），应 ≥ AI 超时 + 余量；用于防止对同一互关连点并发打 AI。
     */
    private int yuanfenInflightTtlSeconds = 120;

    /**
     * 每用户在滑动窗口内允许「进入 AI 流程」的最大次数（缓存命中不计入）。
     * 未完成即断开时，只要请求已打到本段逻辑通常会计入 1 次，用于防止反复取消再点刷接口。
     */
    private int yuanfenStartBurstMax = 8;

    /** 上述次数统计的窗口长度（秒），默认 15 分钟 */
    private int yuanfenStartBurstWindowSeconds = 900;

    /**
     * 全站当前自然小时（上海时区）内最多「真正调用厂商 AI」的缘分分析次数（Redis 计数）。
     * 缓存命中不计入；0 表示不限制。
     */
    private int yuanfenGlobalAiHourlyMax = 0;

    // ---------- 火山方舟图生图（与 DeepSeek 文本 AI 分离，见 ARK_* 环境变量）----------

    /** 火山方舟 API Key，用于 {@code /images/generations} */
    private String arkApiKey;

    /** 如 https://ark.cn-beijing.volces.com/api/v3 */
    private String arkBaseUrl = "https://ark.cn-beijing.volces.com/api/v3";

    /** 图生图模型，如 doubao-seedream-5-0-260128 */
    private String arkImageModel = "doubao-seedream-5-0-260128";

    /**
     * AI 头像工作室每人免费生成次数（成功调用图生图后计一次）。
     * 配置项：ai.avatar-studio-free-quota / AVATAR_STUDIO_FREE_QUOTA
     */
    private int avatarStudioFreeQuota = 2;

    /**
     * 图生图 HTTP 超时（秒，含下载结果图）。
     * 配置项：ai.avatar-studio-timeout-seconds / AVATAR_STUDIO_TIMEOUT_SECONDS
     */
    private int avatarStudioTimeoutSeconds = 180;

    public String requiredApiKey() {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("AI_API_KEY 未配置");
        }
        return apiKey.trim();
    }

    public String requiredBaseUrl() {
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException("AI_BASE_URL 未配置");
        }
        return trimTrailingSlash(baseUrl.trim());
    }

    public String requiredModel() {
        if (!StringUtils.hasText(model)) {
            throw new IllegalStateException("AI_MODEL 未配置");
        }
        return model.trim();
    }

    private String trimTrailingSlash(String url) {
        int end = url.length();
        while (end > 0 && url.charAt(end - 1) == '/') {
            end--;
        }
        return url.substring(0, end);
    }
}
