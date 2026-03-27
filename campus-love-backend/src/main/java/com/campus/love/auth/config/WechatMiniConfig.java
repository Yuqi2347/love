package com.campus.love.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.wechat")
public class WechatMiniConfig {

    /** 微信小程序 AppID */
    private String miniAppId;

    /** 微信小程序 AppSecret */
    private String miniAppSecret;

    /** code2session 接口地址 */
    private String code2SessionUrl = "https://api.weixin.qq.com/sns/jscode2session";

    public String requiredMiniAppId() {
        if (!StringUtils.hasText(miniAppId)) {
            throw new IllegalStateException("WECHAT_MINI_APP_ID 未配置");
        }
        return miniAppId.trim();
    }

    public String requiredMiniAppSecret() {
        if (!StringUtils.hasText(miniAppSecret)) {
            throw new IllegalStateException("WECHAT_MINI_APP_SECRET 未配置");
        }
        return miniAppSecret.trim();
    }

    public String requiredCode2SessionUrl() {
        if (!StringUtils.hasText(code2SessionUrl)) {
            throw new IllegalStateException("WECHAT_CODE2SESSION_URL 未配置");
        }
        return code2SessionUrl.trim();
    }
}
