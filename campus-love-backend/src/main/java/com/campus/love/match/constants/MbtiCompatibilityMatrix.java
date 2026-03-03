package com.campus.love.match.constants;

import java.util.Map;

/**
 * MBTI 16×16 相性矩阵。
 * 得分范围 0-100，数值越高越相配。
 * 基于功能栈互补/相似理论简化建模。
 */
public final class MbtiCompatibilityMatrix {

    private MbtiCompatibilityMatrix() {}

    private static final String[] TYPES = {
            "INTJ", "INTP", "ENTJ", "ENTP",
            "INFJ", "INFP", "ENFJ", "ENFP",
            "ISTJ", "ISFJ", "ESTJ", "ESFJ",
            "ISTP", "ISFP", "ESTP", "ESFP"
    };

    // 16x16 compatibility scores
    private static final int[][] SCORES = {
        //  INTJ INTP ENTJ ENTP INFJ INFP ENFJ ENFP ISTJ ISFJ ESTJ ESFJ ISTP ISFP ESTP ESFP
            { 75,  80,  85,  90,  80,  70,  75,  85,  65,  55,  70,  55,  60,  50,  60,  50 }, // INTJ
            { 80,  75,  80,  90,  70,  75,  70,  85,  60,  50,  65,  50,  70,  55,  65,  55 }, // INTP
            { 85,  80,  75,  85,  85,  65,  80,  80,  75,  60,  80,  65,  65,  55,  70,  60 }, // ENTJ
            { 90,  90,  85,  75,  75,  80,  75,  80,  60,  50,  65,  55,  70,  60,  70,  65 }, // ENTP
            { 80,  70,  85,  75,  75,  85,  85,  90,  60,  65,  60,  65,  55,  65,  50,  60 }, // INFJ
            { 70,  75,  65,  80,  85,  75,  80,  90,  50,  60,  50,  60,  55,  70,  55,  65 }, // INFP
            { 75,  70,  80,  75,  85,  80,  75,  85,  65,  70,  65,  75,  55,  65,  60,  70 }, // ENFJ
            { 85,  85,  80,  80,  90,  90,  85,  75,  55,  60,  55,  60,  60,  70,  65,  70 }, // ENFP
            { 65,  60,  75,  60,  60,  50,  65,  55,  75,  80,  85,  80,  70,  65,  75,  65 }, // ISTJ
            { 55,  50,  60,  50,  65,  60,  70,  60,  80,  75,  80,  85,  60,  70,  65,  75 }, // ISFJ
            { 70,  65,  80,  65,  60,  50,  65,  55,  85,  80,  75,  80,  70,  60,  80,  65 }, // ESTJ
            { 55,  50,  65,  55,  65,  60,  75,  60,  80,  85,  80,  75,  60,  70,  65,  80 }, // ESFJ
            { 60,  70,  65,  70,  55,  55,  55,  60,  70,  60,  70,  60,  75,  70,  85,  75 }, // ISTP
            { 50,  55,  55,  60,  65,  70,  65,  70,  65,  70,  60,  70,  70,  75,  75,  85 }, // ISFP
            { 60,  65,  70,  70,  50,  55,  60,  65,  75,  65,  80,  65,  85,  75,  75,  80 }, // ESTP
            { 50,  55,  60,  65,  60,  65,  70,  70,  65,  75,  65,  80,  75,  85,  80,  75 }, // ESFP
    };

    private static final Map<String, Integer> TYPE_INDEX;

    static {
        TYPE_INDEX = new java.util.HashMap<>();
        for (int i = 0; i < TYPES.length; i++) {
            TYPE_INDEX.put(TYPES[i], i);
        }
    }

    public static int getCompatibility(String mbti1, String mbti2) {
        if (mbti1 == null || mbti2 == null) return 50;
        Integer idx1 = TYPE_INDEX.get(mbti1.toUpperCase());
        Integer idx2 = TYPE_INDEX.get(mbti2.toUpperCase());
        if (idx1 == null || idx2 == null) return 50;
        return SCORES[idx1][idx2];
    }
}
