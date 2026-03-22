package com.campus.love.pairdate.service;

import com.campus.love.pairdate.enums.LocationChoice;
import com.campus.love.pairdate.enums.TimeSlotCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 约会方式 / 时间交集 / 地点决定者（对齐 V1.2.0_INVITATION_MODULE §4）
 */
@Service
@RequiredArgsConstructor
public class PairDateResultService {

    private final ObjectMapper objectMapper;

    public JsonNode pickFinalDateOption(String dateOptionsJson, int excludeA, int excludeB) throws Exception {
        JsonNode root = objectMapper.readTree(dateOptionsJson);
        JsonNode options = root.path("options");
        if (!options.isArray() || options.size() != 3) {
            throw new IllegalArgumentException("date_options");
        }
        Set<Integer> ranks = new HashSet<>();
        for (JsonNode o : options) {
            ranks.add(o.path("rank").asInt());
        }
        if (!ranks.containsAll(Set.of(1, 2, 3))) {
            throw new IllegalArgumentException("date_options.rank");
        }
        if (excludeA < 1 || excludeA > 3 || excludeB < 1 || excludeB > 3) {
            throw new IllegalArgumentException("exclude");
        }
        if (excludeA == excludeB) {
            int keep = Integer.MAX_VALUE;
            for (int r : List.of(1, 2, 3)) {
                if (r != excludeA) {
                    keep = Math.min(keep, r);
                }
            }
            return optionByRank(options, keep);
        }
        int remaining = 1 + 2 + 3 - excludeA - excludeB;
        return optionByRank(options, remaining);
    }

    private static JsonNode optionByRank(JsonNode options, int rank) {
        for (JsonNode o : options) {
            if (o.path("rank").asInt() == rank) {
                return o;
            }
        }
        throw new IllegalStateException("rank " + rank);
    }

    public TimeSlotCode minIntersectSlots(String slotsJsonA, String slotsJsonB) throws Exception {
        Set<TimeSlotCode> a = parseSlotSet(slotsJsonA);
        Set<TimeSlotCode> b = parseSlotSet(slotsJsonB);
        return TimeSlotCode.minIntersect(a, b);
    }

    public Set<TimeSlotCode> parseSlotSet(String json) throws Exception {
        if (json == null || json.isBlank()) {
            return Set.of();
        }
        List<String> codes = objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        EnumSet<TimeSlotCode> set = EnumSet.noneOf(TimeSlotCode.class);
        for (String c : codes) {
            set.add(TimeSlotCode.fromString(c));
        }
        return set;
    }

    public long calcLocationDeciderId(Long userIdA, Long userIdB, LocationChoice a, LocationChoice b) {
        String ca = a.name();
        String cb = b.name();
        if ("SELF".equals(ca) && "PARTNER".equals(cb)) {
            return userIdA;
        }
        if ("PARTNER".equals(ca) && "SELF".equals(cb)) {
            return userIdB;
        }
        if ("SELF".equals(ca) && "EITHER".equals(cb)) {
            return userIdA;
        }
        if ("EITHER".equals(ca) && "SELF".equals(cb)) {
            return userIdB;
        }
        if ("PARTNER".equals(ca) && "EITHER".equals(cb)) {
            return userIdB;
        }
        if ("EITHER".equals(ca) && "PARTNER".equals(cb)) {
            return userIdA;
        }
        return ThreadLocalRandom.current().nextBoolean() ? userIdA : userIdB;
    }

    /**
     * 前端「为什么是 Ta 决定」徽章用（§4.4 扩展覆盖矩阵全部确定性情形）
     */
    public String calcDeciderReasonKey(LocationChoice a, LocationChoice b, long deciderId, long userIdA, long userIdB) {
        if (deciderId == userIdA) {
            if (a == LocationChoice.SELF && b == LocationChoice.PARTNER) {
                return "MUTUAL_BOTH_WANT_A";
            }
            if (a == LocationChoice.SELF && b == LocationChoice.EITHER) {
                return "A_DECIDES_B_FLEX";
            }
            if (a == LocationChoice.EITHER && b == LocationChoice.PARTNER) {
                return "A_DECIDES_B_PICKED_A";
            }
        } else if (deciderId == userIdB) {
            if (a == LocationChoice.PARTNER && b == LocationChoice.SELF) {
                return "MUTUAL_BOTH_WANT_B";
            }
            if (a == LocationChoice.EITHER && b == LocationChoice.SELF) {
                return "B_DECIDES_A_FLEX";
            }
            if (a == LocationChoice.PARTNER && b == LocationChoice.EITHER) {
                return "B_DECIDES_A_PICKED_B";
            }
        }
        return "DICE_ROLL";
    }
}
