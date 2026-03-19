package com.campus.love.moment.service;

import com.campus.love.common.utils.InterestTagConverter;
import com.campus.love.moment.entity.MomentProfile;
import com.campus.love.profile.entity.UserPortrait;
import com.campus.love.user.entity.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class MomentPromptHelper {

    private MomentPromptHelper() {
    }

    public static String socialStyle(String code) {
        return switch (code) {
            case "A" -> "热闹充电型";
            case "B" -> "深度小圈型";
            case "C" -> "灵活切换型";
            default -> "社交风格未填";
        };
    }

    public static String lifeRhythm(String code) {
        return switch (code) {
            case "A" -> "计划型";
            case "B" -> "随性型";
            case "C" -> "半规半随型";
            case "D" -> "佛系摆烂型";
            default -> "生活节奏未填";
        };
    }

    public static String personalityBase(String code) {
        return switch (code) {
            case "A" -> "外向活泼";
            case "B" -> "内向安静";
            case "C" -> "多变切换";
            default -> "性格底色未填";
        };
    }

    public static String campusFocus(String code) {
        return switch (code) {
            case "A" -> "学业至上";
            case "B" -> "社交为主";
            case "C" -> "兴趣优先";
            case "D" -> "多元平衡";
            default -> "校园重心未填";
        };
    }

    public static String emotionStyle(String code) {
        return switch (code) {
            case "A" -> "外放型";
            case "B" -> "内收型";
            case "C" -> "选择性表达";
            default -> "情绪表达未填";
        };
    }

    public static String companionshipStyle(String code) {
        return switch (code) {
            case "A" -> "形影不离";
            case "B" -> "各自空间";
            case "C" -> "视情况而定";
            default -> "陪伴方式未填";
        };
    }

    public static String dateStyle(String code) {
        return switch (code) {
            case "A" -> "户外探索型";
            case "B" -> "室内舒适型";
            case "C" -> "校园日常型";
            case "D" -> "多元尝试型";
            default -> "约会方式未填";
        };
    }

    public static String intimacyPace(String code) {
        return switch (code) {
            case "A" -> "迅速升温型";
            case "B" -> "慢热型";
            case "C" -> "随缘配合型";
            default -> "亲密节奏未填";
        };
    }

    public static String futureLifestyle(String code) {
        return switch (code) {
            case "A" -> "安稳陪伴型";
            case "B" -> "自由探索型";
            case "C" -> "边走边看型";
            default -> "未来生活方式未填";
        };
    }

    public static String dateSceneTypeLabel(String sceneType) {
        return switch (sceneType) {
            case "OUTDOOR" -> "轻互动熟悉型";
            case "INDOOR" -> "轻松聊天型";
            case "CAMPUS_DAILY" -> "校园安全感型";
            case "MIXED" -> "低压力陪伴型";
            default -> "校园安全感型";
        };
    }

    public static String genderLabel(Integer gender) {
        if (gender == null) {
            return "未知";
        }
        return gender == 1 ? "男生" : "女生";
    }

    public static List<String> interestNames(UserPortrait portrait, User user, int limit) {
        String display = InterestTagConverter.getInterestsForDisplay(
                portrait != null ? portrait.getInterestTags() : null,
                user != null ? user.getInterests() : null
        );
        if (display == null || display.isBlank()) {
            return List.of();
        }
        return Arrays.stream(display.split("[,，、]"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .limit(Math.max(limit, 0))
                .collect(Collectors.toList());
    }

    public static List<String> commonInterestNames(UserPortrait portraitA, User userA, UserPortrait portraitB, User userB, int limit) {
        List<String> interestsA = interestNames(portraitA, userA, 12);
        List<String> interestsB = interestNames(portraitB, userB, 12);
        if (interestsA.isEmpty() || interestsB.isEmpty()) {
            return List.of();
        }
        Set<String> setB = new LinkedHashSet<>(interestsB);
        List<String> result = new ArrayList<>();
        for (String item : interestsA) {
            if (setB.contains(item) && !result.contains(item)) {
                result.add(item);
            }
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    public static String joinOrDefault(List<String> items, String fallback) {
        if (items == null || items.isEmpty()) {
            return fallback;
        }
        return items.stream()
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining("、"));
    }

    public static String summarizeProfile(MomentProfile profile) {
        if (profile == null) {
            return "资料不足";
        }
        return String.format("社交:%s；节奏:%s；性格:%s；校园重心:%s；情绪:%s；陪伴:%s；约会:%s；亲密:%s",
                socialStyle(profile.getSocialStyle()),
                lifeRhythm(profile.getLifeRhythm()),
                personalityBase(profile.getPersonalityBase()),
                campusFocus(profile.getCampusFocus()),
                emotionStyle(profile.getEmotionStyle()),
                companionshipStyle(profile.getCompanionshipStyle()),
                dateStyle(profile.getDateStyle()),
                intimacyPace(profile.getIntimacyPace()));
    }
}
