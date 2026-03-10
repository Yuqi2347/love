package com.campus.love.auth.service;

import com.campus.love.common.constants.RedisKeyConstants;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerifyService {

    private static final int CODE_LENGTH = 6;
    private static final int EXPIRE_MINUTES = 5;
    private static final int SEND_COOLDOWN_SECONDS = 60;

    private final JavaMailSender mailSender;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${spring.mail.from:${spring.mail.username:}}")
    private String mailFrom;

    private final Random random = new Random();

    /**
     * 发送验证码到指定邮箱（使用 Redis 存储，StringRedisTemplate 避免序列化问题）
     */
    public void sendVerifyCode(String email) {
        String normalized = normalizeEmail(email);
        if (normalized == null || !normalized.contains("@") || normalized.indexOf("@") >= normalized.length() - 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "邮箱格式不正确");
        }

        String cooldownKey = RedisKeyConstants.emailVerifyCode(normalized) + ":cooldown";
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(cooldownKey))) {
            throw new BusinessException(ResultCode.VERIFY_CODE_COOLDOWN);
        }

        String code = generateCode();
        String key = RedisKeyConstants.emailVerifyCode(normalized);
        stringRedisTemplate.opsForValue().set(key, code, EXPIRE_MINUTES, TimeUnit.MINUTES);
        stringRedisTemplate.opsForValue().set(cooldownKey, "1", SEND_COOLDOWN_SECONDS, TimeUnit.SECONDS);

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            if (mailFrom != null && !mailFrom.isEmpty()) {
                msg.setFrom(mailFrom);
            }
            msg.setTo(normalized);
            msg.setSubject("【Campal】终于等到你~");
            msg.setText("Campal 终于等到你~\n\n" +
                    "· 发声：分享想法、表达态度，让更多人听见我们的声音\n" +
                    "· 社交：认识同校伙伴，一起吃饭、自习、狠狠社交！\n" +
                    "· 恋爱：AI 多维度匹配，遇见志趣相投的TA\n\n" +
                    "验证码：" + code + "\n" +
                    "5 分钟有效。");
            mailSender.send(msg);
            log.info("Sent verify code to {} (Redis)", normalized);
        } catch (Exception e) {
            log.error("Failed to send verify code to {}: {}", normalized, e.getMessage());
            stringRedisTemplate.delete(key);
            stringRedisTemplate.delete(cooldownKey);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "验证码发送失败，请稍后重试");
        }
    }

    /**
     * 校验验证码（从 Redis 读取，使用 StringRedisTemplate 避免序列化问题）
     */
    public boolean verifyCode(String email, String code) {
        if (email == null || code == null) return false;
        String normalized = normalizeEmail(email);
        String key = RedisKeyConstants.emailVerifyCode(normalized);
        String stored = stringRedisTemplate.opsForValue().get(key);
        if (stored == null) return false;
        boolean ok = code.trim().equals(stored);
        if (ok) {
            stringRedisTemplate.delete(key);
        }
        return ok;
    }

    /** 统一邮箱格式：trim + 小写，避免 Redis key 不一致 */
    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
