package com.campus.love.match.service;

import com.campus.love.match.constants.GlobalWeights;
import com.campus.love.user.entity.User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.Map;

@Service
public class AgeGradeMatcher {

    private static final Map<String, Integer> GRADE_ORDER = Map.ofEntries(
            Map.entry("大一", 1),
            Map.entry("大二", 2),
            Map.entry("大三", 3),
            Map.entry("大四", 4),
            Map.entry("大五", 5),
            Map.entry("研一", 7),
            Map.entry("研二", 8),
            Map.entry("研三", 9),
            Map.entry("博一", 10),
            Map.entry("博二", 11),
            Map.entry("博三", 12),
            Map.entry("博士", 10),
            Map.entry("毕业", 13)
    );

    public int calculateAgeGradeScore(User self, User target) {
        int ageScore = calculateAgeScore(self != null ? self.getBirthDate() : null, target != null ? target.getBirthDate() : null);
        int gradeScore = calculateGradeScore(self != null ? self.getGrade() : null, target != null ? target.getGrade() : null);
        return (int) Math.round(ageScore * 0.7 + gradeScore * 0.3);
    }

    private int calculateAgeScore(LocalDate selfBirthDate, LocalDate targetBirthDate) {
        if (selfBirthDate == null || targetBirthDate == null) {
            return 50;
        }
        int diff = Math.abs(Period.between(selfBirthDate, targetBirthDate).getYears());
        if (diff == 0) {
            return 100;
        }
        if (diff >= GlobalWeights.MAX_AGE_DIFF_YEARS) {
            return 20;
        }
        return 100 - (diff * 80 / GlobalWeights.MAX_AGE_DIFF_YEARS);
    }

    private int calculateGradeScore(String selfGrade, String targetGrade) {
        Integer selfOrder = normalizeGrade(selfGrade);
        Integer targetOrder = normalizeGrade(targetGrade);
        if (selfOrder == null || targetOrder == null) {
            return 50;
        }
        int diff = Math.abs(selfOrder - targetOrder);
        return switch (diff) {
            case 0 -> 100;
            case 1 -> 80;
            case 2 -> 60;
            default -> 40;
        };
    }

    private Integer normalizeGrade(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        Integer direct = GRADE_ORDER.get(normalized);
        if (direct != null) {
            return direct;
        }
        if (normalized.startsWith("本科")) {
            return GRADE_ORDER.get(normalized.substring(2));
        }
        if (normalized.startsWith("硕士")) {
            return GRADE_ORDER.get("研" + normalized.substring(2));
        }
        if (normalized.startsWith("研究生")) {
            return GRADE_ORDER.get("研" + normalized.substring(3));
        }
        if (normalized.startsWith("博士")) {
            return GRADE_ORDER.get(normalized);
        }
        return null;
    }
}
