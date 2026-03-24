package com.campus.love.moment.service;

import java.util.Map;

/**
 * 心动时刻题目矩阵：
 * 1.2 / 1.4 偏相似+互补；
 * 1.3 偏纯相似；
 * 1.6 允许方向性差异。
 */
public final class MomentPersonalityMatrix {

    private static final int DEFAULT_SCORE = 60;

    private static final Map<String, Map<String, Integer>> SOCIAL_STYLE = Map.of(
            "A", Map.of("A", 100, "B", 78, "C", 46),
            "B", Map.of("A", 78, "B", 100, "C", 54),
            "C", Map.of("A", 46, "B", 54, "C", 100)
    );

    private static final Map<String, Map<String, Integer>> LIFE_RHYTHM = Map.of(
            "A", Map.of("A", 100, "B", 28, "C", 58, "D", 12),
            "B", Map.of("A", 28, "B", 100, "C", 62, "D", 18),
            "C", Map.of("A", 58, "B", 62, "C", 100, "D", 54),
            "D", Map.of("A", 12, "B", 18, "C", 54, "D", 100)
    );

    private static final Map<String, Map<String, Integer>> PERSONALITY_BASE = Map.of(
            "A", Map.of("A", 100, "B", 80, "C", 48),
            "B", Map.of("A", 80, "B", 100, "C", 56),
            "C", Map.of("A", 48, "B", 56, "C", 100)
    );

    private static final Map<String, Map<String, Integer>> EMOTION_STYLE = Map.of(
            "A", Map.of("A", 100, "B", 48, "C", 68),
            "B", Map.of("A", 58, "B", 100, "C", 74),
            "C", Map.of("A", 68, "B", 74, "C", 100)
    );

    private MomentPersonalityMatrix() {
    }

    public static int socialStyleScore(String a, String b) {
        return symmetricLookup(SOCIAL_STYLE, a, b);
    }

    public static int lifeRhythmScore(String a, String b) {
        return symmetricLookup(LIFE_RHYTHM, a, b);
    }

    public static int personalityBaseScore(String a, String b) {
        return symmetricLookup(PERSONALITY_BASE, a, b);
    }

    public static int emotionStyleScore(String a, String b) {
        return directionalAverage(EMOTION_STYLE, a, b);
    }

    private static int symmetricLookup(Map<String, Map<String, Integer>> matrix, String a, String b) {
        return lookup(matrix, a, b);
    }

    private static int directionalAverage(Map<String, Map<String, Integer>> matrix, String a, String b) {
        int forward = lookup(matrix, a, b);
        int reverse = lookup(matrix, b, a);
        return Math.round((forward + reverse) / 2f);
    }

    private static int lookup(Map<String, Map<String, Integer>> matrix, String a, String b) {
        if (a == null || b == null) {
            return DEFAULT_SCORE;
        }
        Map<String, Integer> row = matrix.get(a);
        if (row == null) {
            return DEFAULT_SCORE;
        }
        return row.getOrDefault(b, DEFAULT_SCORE);
    }
}
