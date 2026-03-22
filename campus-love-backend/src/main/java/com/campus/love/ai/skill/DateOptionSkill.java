package com.campus.love.ai.skill;

import com.campus.love.ai.service.AiService;
import com.campus.love.common.utils.InterestTagConverter;
import com.campus.love.moment.entity.MomentProfile;
import com.campus.love.profile.entity.UserPortrait;
import com.campus.love.user.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Month;

/**
 * 生成 3 个第一次见面约会方式 JSON（与前端 momentConst FIRST_DATE_CONSTRAINTS 语义对齐，见 V1.2.0_INVITATION_MODULE §1.3 / §8）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DateOptionSkill {

    private final AiService aiService;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM = """
            你是校园恋爱产品的约会顾问。根据两人问卷与标签，生成 3 个适合「第一次见面」的约会方式。
            必须同时满足：公开场合、有人气、时长约 1.5～3 小时、有自然退出机会、有聊天话题引子。
            禁止：看电影（全程难交流）、居家/做饭/游戏（过私密）、深夜酒吧 KTV、长途旅行或高强度户外运动。
            输出只能是严格 JSON，不要 Markdown、不要解释。结构如下：
            {"options":[{"rank":1,"title":"10字内标题","description":"20字内说明","reason":"15字内理由"},{"rank":2,...},{"rank":3,...}]}
            rank 必须为 1、2、3 且与推荐优先级一致（1 最高）。
            """;

    private static final String FALLBACK = """
            {"options":[
            {"rank":1,"title":"校园咖啡馆小坐","description":"选个人气店，边喝边聊，随时可结束","reason":"轻松安全，话题自然"},
            {"rank":2,"title":"书店闲逛","description":"校园或附近书店翻翻书，再散步","reason":"有素材不尬聊"},
            {"rank":3,"title":"食堂轻食约饭","description":"校内食堂或轻食店，边吃边聊","reason":"日常感强压力小"}
            ]}
            """;

    public String generateDateOptionsJson(User userA, User userB, MomentProfile profileA, MomentProfile profileB,
                                          UserPortrait portraitA, UserPortrait portraitB) {
        String userMsg = buildUserMessage(userA, userB, profileA, profileB, portraitA, portraitB);
        try {
            String raw = aiService.chatCompletion(SYSTEM, userMsg, 1024, 45).getContent();
            if (raw == null || raw.isBlank()) {
                return FALLBACK;
            }
            String json = extractJsonObject(raw);
            if (!validateOptionsJson(json)) {
                log.warn("DateOption AI JSON 校验失败，使用降级");
                return FALLBACK;
            }
            return json;
        } catch (Exception e) {
            log.warn("DateOption AI 调用失败，使用降级: {}", e.getMessage());
            return FALLBACK;
        }
    }

    private static String buildUserMessage(User userA, User userB, MomentProfile profileA, MomentProfile profileB,
                                           UserPortrait portraitA, UserPortrait portraitB) {
        String campus = firstNonBlank(userA.getSchool(), userB.getSchool(), "校园");
        String season = seasonCn(LocalDate.now().getMonth());
        String dateA = profileA != null && profileA.getDateStyle() != null ? profileA.getDateStyle() : "未知";
        String dateB = profileB != null && profileB.getDateStyle() != null ? profileB.getDateStyle() : "未知";
        String tagsA = portraitA != null
                ? nullToEmpty(InterestTagConverter.getInterestsForMatching(portraitA.getInterestTags(), null))
                : "";
        String tagsB = portraitB != null
                ? nullToEmpty(InterestTagConverter.getInterestsForMatching(portraitB.getInterestTags(), null))
                : "";
        return """
                A 的约会方式问卷选项：%s
                B 的约会方式问卷选项：%s
                A 的兴趣标签：%s
                B 的兴趣标签：%s
                所在城市/校园：%s
                当前季节（参考）：%s
                """.formatted(dateA, dateB, tagsA, tagsB, campus, season);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String firstNonBlank(String a, String b, String def) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return def;
    }

    private static String seasonCn(Month month) {
        return switch (month) {
            case MARCH, APRIL, MAY -> "春季";
            case JUNE, JULY, AUGUST -> "夏季";
            case SEPTEMBER, OCTOBER, NOVEMBER -> "秋季";
            default -> "冬季";
        };
    }

    private static String extractJsonObject(String raw) {
        int i = raw.indexOf('{');
        int j = raw.lastIndexOf('}');
        if (i < 0 || j <= i) {
            return raw.trim();
        }
        return raw.substring(i, j + 1).trim();
    }

    private boolean validateOptionsJson(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode options = root.path("options");
            if (!options.isArray() || options.size() != 3) {
                return false;
            }
            int mask = 0;
            for (JsonNode o : options) {
                if (!o.path("title").isTextual() || o.path("title").asText().isBlank()) {
                    return false;
                }
                int r = o.path("rank").asInt(-1);
                if (r < 1 || r > 3) {
                    return false;
                }
                mask |= 1 << (r - 1);
            }
            return mask == 7;
        } catch (Exception e) {
            return false;
        }
    }
}
