package com.campus.love.common.service;

import com.campus.love.common.constants.RedisKeyConstants;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的 API 限流服务（固定窗口）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    public enum LimitType {
        /** 注册：5 次/小时/邮箱（校园网共享 IP 时按邮箱限流） */
        REGISTER_EMAIL(5, 1, TimeUnit.HOURS),
        /** 验证码：10 次/小时/邮箱（校园网共享 IP 时按邮箱限流） */
        VERIFY_CODE_EMAIL(10, 1, TimeUnit.HOURS),
        /** 发帖：20 次/小时/用户 */
        POST_USER(20, 1, TimeUnit.HOURS),
        /** 评论：60 次/小时/用户 */
        COMMENT_USER(60, 1, TimeUnit.HOURS),
        /** 匹配行为：100 次/小时/用户 */
        MATCH_ACTION_USER(100, 1, TimeUnit.HOURS),
        /** 缘分解析：10 次/天/用户 */
        YUANFEN_USER(10, 1, TimeUnit.DAYS);

        private final int limit;
        private final long window;
        private final TimeUnit unit;

        LimitType(int limit, long window, TimeUnit unit) {
            this.limit = limit;
            this.window = window;
            this.unit = unit;
        }
    }

    /**
     * 检查并增加计数，超限则抛出 BusinessException
     */
    public void checkAndIncrement(LimitType type, String identifier) {
        String key = RedisKeyConstants.rateLimit(type.name(), identifier);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            count = 1L;
        }

        if (count == 1) {
            redisTemplate.expire(key, type.window, type.unit);
        }

        if (count > type.limit) {
            log.warn("Rate limit exceeded: type={} identifier={} count={} limit={}", type, identifier, count, type.limit);
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS);
        }
    }
}
