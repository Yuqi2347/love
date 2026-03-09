package com.campus.love.common.config;

import com.campus.love.common.filter.RateLimitFilter;
import com.campus.love.common.service.RateLimitService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfig {

    @Bean
    public RateLimitFilter rateLimitFilter(RateLimitService rateLimitService) {
        return new RateLimitFilter(rateLimitService);
    }
}
