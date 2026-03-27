package com.campus.love.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 微信小程序订阅消息配置（仅用于站外微信提醒）。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.wechat.subscribe")
public class WechatSubscribeConfig {

    /** 是否启用订阅消息推送 */
    private boolean enabled = false;

    /** 获取 access_token 接口 */
    private String accessTokenUrl = "https://api.weixin.qq.com/cgi-bin/token";

    /** 发送订阅消息接口 */
    private String sendUrl = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send";

    /** 模板ID：心动协商结果 */
    private String tmplPairDateResultReady;
    /** 模板ID：邀约有人加入 */
    private String tmplInviteNewParticipant;
    /** 模板ID：等待邀约匹配成功 */
    private String tmplInviteMatchFound;

    /** 页面路径：心动协商结果 */
    private String pagePairDateResultReady = "pages/moment/result/result";
    /** 页面路径：邀约有人加入 */
    private String pageInviteNewParticipant = "pages/invite/index/index";
    /** 页面路径：等待邀约匹配成功 */
    private String pageInviteMatchFound = "pages/invite/wait/wait";

    /**
     * 模板字段 key（默认按 thing/time/thing 组合）。
     * 若你的模板字段不同，请在环境变量覆盖这三个 key。
     */
    private String dataKeyTitle = "thing1";
    private String dataKeyTime = "time2";
    private String dataKeyRemark = "thing3";
}

