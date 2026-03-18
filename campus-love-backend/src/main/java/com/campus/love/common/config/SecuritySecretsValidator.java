package com.campus.love.common.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class SecuritySecretsValidator {

    @Value("${app.security.enforce-secrets:true}")
    private boolean enforceSecrets;

    @Value("${spring.datasource.username:}")
    private String dbUsername;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${ai.api-key:}")
    private String aiApiKey;

    @PostConstruct
    public void validate() {
        if (!enforceSecrets) {
            return;
        }

        List<String> missing = new ArrayList<>();
        checkRequired("spring.datasource.username / DB_USERNAME", dbUsername, missing);
        checkRequired("spring.datasource.password / DB_PASSWORD", dbPassword, missing);
        checkRequired("spring.mail.username / MAIL_USERNAME", mailUsername, missing);
        checkRequired("spring.mail.password / MAIL_PASSWORD", mailPassword, missing);
        checkRequired("app.jwt.secret / JWT_SECRET", jwtSecret, missing);
        checkRequired("ai.api-key / AI_API_KEY", aiApiKey, missing);

        if (!missing.isEmpty()) {
            throw new IllegalStateException("敏感配置缺失，请设置环境变量: " + String.join(", ", missing));
        }

        int jwtBytes = jwtSecret.getBytes(StandardCharsets.UTF_8).length;
        if (jwtBytes < 32) {
            throw new IllegalStateException("JWT_SECRET 强度不足，至少 32 字节");
        }
    }

    private void checkRequired(String field, String value, List<String> missing) {
        if (!StringUtils.hasText(value)) {
            missing.add(field);
        }
    }
}
