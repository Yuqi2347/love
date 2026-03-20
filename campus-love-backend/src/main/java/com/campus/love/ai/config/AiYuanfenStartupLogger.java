package com.campus.love.ai.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 启动时打印缘分相关配置，便于确认是否误设 YUANFEN_COOLDOWN=0（将导致完全不读库缓存）。
 */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
public class AiYuanfenStartupLogger implements ApplicationRunner {

    private final AiConfig aiConfig;

    @Override
    public void run(ApplicationArguments args) {
        long cd = aiConfig.getYuanfenCooldownHours();
        log.info(
                "缘分配置: yuanfenCooldownHours={} (0=不读库缓存每次调AI; >0=按小时窗读缓存+全局冷却; -1=永久按对读缓存), yuanfenTimeoutSeconds={}, yuanfenGlobalAiHourlyMax={}",
                cd,
                aiConfig.getYuanfenTimeoutSeconds(),
                aiConfig.getYuanfenGlobalAiHourlyMax());
        if (cd == 0) {
            log.warn("YUANFEN_COOLDOWN/ai.yuanfen-cooldown-hours 为 0：缘分解析不会使用 t_yuanfen_analysis_log 缓存，每次点击都会调 AI。");
        }
    }
}
