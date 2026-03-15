package com.campus.love.moment.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MomentDimensionLabelResolver {

    public List<String> resolveLabels(Map<String, Object> scoreDetail) {
        double personality = resolveRawScore(scoreDetail, "personality", 0.22d);
        double preference = resolveRawScore(scoreDetail, "preference", 0.30d);
        double lifestyle = resolveRawScore(scoreDetail, "lifestyle", 0.20d);
        double coreValue = resolveRawScore(scoreDetail, "coreValue", 0.20d);
        return List.of(
                personalityLabel(personality),
                preferenceLabel(preference),
                lifestyleLabel(lifestyle),
                coreValueLabel(coreValue)
        );
    }

    private double resolveRawScore(Map<String, Object> detail, String key, double weight) {
        if (detail == null || detail.isEmpty()) {
            return 0d;
        }
        double value = number(detail.get(key));
        if (value <= 0) {
            return 0d;
        }
        if (value <= weight * 100 + 0.0001d) {
            return Math.min(100d, Math.round((value / weight) * 10d) / 10d);
        }
        return Math.min(100d, value);
    }

    private double number(Object value) {
        if (value == null) {
            return 0d;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignored) {
            return 0d;
        }
    }

    private String personalityLabel(double score) {
        if (score >= 80d) return "性格：相互吸引";
        if (score >= 65d) return "性格：有些默契";
        if (score >= 50d) return "性格：各有特色";
        return "性格：需要磨合";
    }

    private String preferenceLabel(double score) {
        if (score >= 80d) return "偏好：双向心动";
        if (score >= 65d) return "偏好：基本合拍";
        if (score >= 50d) return "偏好：有所期待";
        return "偏好：惊喜未知";
    }

    private String lifestyleLabel(double score) {
        if (score >= 80d) return "生活：节奏相近";
        if (score >= 65d) return "生活：能够配合";
        if (score >= 50d) return "生活：各有风格";
        return "生活：需要适应";
    }

    private String coreValueLabel(double score) {
        if (score >= 80d) return "价值观：高度同频";
        if (score >= 65d) return "价值观：方向一致";
        if (score >= 50d) return "价值观：求同存异";
        return "价值观：坦诚沟通";
    }
}
