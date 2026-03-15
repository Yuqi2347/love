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
            "A", Map.of("A", 100, "B", 88, "C", 76),
            "B", Map.of("A", 88, "B", 100, "C", 78),
            "C", Map.of("A", 76, "B", 78, "C", 100)
    );

    private static final Map<String, Map<String, Integer>> LIFE_RHYTHM = Map.of(
            "A", Map.of("A", 100, "B", 35, "C", 72, "D", 20),
            "B", Map.of("A", 35, "B", 100, "C", 72, "D", 30),
            "C", Map.of("A", 72, "B", 72, "C", 100, "D", 68),
            "D", Map.of("A", 20, "B", 30, "C", 68, "D", 100)
    );

    private static final Map<String, Map<String, Integer>> PERSONALITY_BASE = Map.of(
            "A", Map.of("A", 100, "B", 86, "C", 72),
            "B", Map.of("A", 86, "B", 100, "C", 74),
            "C", Map.of("A", 72, "B", 74, "C", 100)
    );

    private static final Map<String, Map<String, Integer>> EMOTION_STYLE = Map.of(
            "A", Map.of("A", 100, "B", 62, "C", 82),
            "B", Map.of("A", 75, "B", 100, "C", 88),
            "C", Map.of("A", 80, "B", 86, "C", 100)
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
