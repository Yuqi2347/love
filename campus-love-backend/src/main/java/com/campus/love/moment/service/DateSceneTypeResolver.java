package com.campus.love.moment.service;

import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class DateSceneTypeResolver {

    public String resolve(String a, String b) {
        if (Objects.equals(a, b) && a != null) {
            return normalize(a);
        }
        if (Objects.equals(a, "D")) {
            return normalize(b);
        }
        if (Objects.equals(b, "D")) {
            return normalize(a);
        }
        if (isPair(a, b, "A", "B")) {
            return "CAMPUS_DAILY";
        }
        if (isPair(a, b, "A", "C")) {
            return "OUTDOOR";
        }
        if (isPair(a, b, "B", "C")) {
            return "INDOOR";
        }
        return "CAMPUS_DAILY";
    }

    private boolean isPair(String left, String right, String expectedLeft, String expectedRight) {
        return (Objects.equals(left, expectedLeft) && Objects.equals(right, expectedRight))
                || (Objects.equals(left, expectedRight) && Objects.equals(right, expectedLeft));
    }

    private String normalize(String code) {
        if (Objects.equals(code, "A")) {
            return "OUTDOOR";
        }
        if (Objects.equals(code, "B")) {
            return "INDOOR";
        }
        if (Objects.equals(code, "D")) {
            return "MIXED";
        }
        return "CAMPUS_DAILY";
    }
}
