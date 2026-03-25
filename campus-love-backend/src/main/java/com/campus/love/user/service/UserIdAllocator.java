package com.campus.love.user.service;

import com.campus.love.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 分配八位数字用户主键：区间 [10_000_000, 99_999_999]，与历史小整数 id 不重叠。
 */
@Service
@RequiredArgsConstructor
public class UserIdAllocator {

    private static final long EIGHT_DIGIT_MIN = 10_000_000L;
    /** inclusive upper bound */
    private static final long EIGHT_DIGIT_MAX = 99_999_999L;

    private static final int MAX_ATTEMPTS = 64;

    private final UserMapper userMapper;

    public long allocateEightDigitUserId() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            long candidate = rnd.nextLong(EIGHT_DIGIT_MIN, EIGHT_DIGIT_MAX + 1);
            if (userMapper.selectById(candidate) == null) {
                return candidate;
            }
        }
        throw new IllegalStateException("无法分配唯一用户ID，请稍后重试注册");
    }
}
