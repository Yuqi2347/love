package com.campus.love.common.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 兴趣标签格式转换：旧格式（逗号分隔）<-> 新格式 JSON
 * 新格式: {"body_space":[{"code":"tag_fitness","sharing":0.5,"intensity":0.5}]}
 */
public final class InterestTagConverter {

    public static final double DEFAULT_SHARING = 0.5d;
    public static final double DEFAULT_INTENSITY = 0.5d;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Map<String, String> LEGACY_TO_CODE = Map.ofEntries(
            Map.entry("读书", "tag_literature"), Map.entry("音乐", "tag_music_creation"), Map.entry("电影", "tag_blockbuster"),
            Map.entry("摄影", "tag_photography"), Map.entry("旅行", "tag_outdoor"), Map.entry("美食", "tag_food_dining"),
            Map.entry("健身", "tag_fitness"), Map.entry("瑜伽", "tag_yoga_meditation"), Map.entry("篮球", "tag_ball_sports"),
            Map.entry("足球", "tag_ball_sports"), Map.entry("羽毛球", "tag_ball_sports"), Map.entry("游泳", "tag_outdoor"),
            Map.entry("绘画", "tag_drawing"), Map.entry("书法", "tag_handicraft"), Map.entry("吉他", "tag_music_creation"),
            Map.entry("钢琴", "tag_music_creation"), Map.entry("舞蹈", "tag_dance"), Map.entry("唱歌", "tag_music_creation"),
            Map.entry("编程", "tag_tech"), Map.entry("游戏", "tag_gaming"), Map.entry("动漫", "tag_anime"),
            Map.entry("追剧", "tag_drama"), Map.entry("Cosplay", "tag_anime"), Map.entry("手账", "tag_handicraft"),
            Map.entry("烘焙", "tag_dessert"), Map.entry("咖啡", "tag_coffee_tea"), Map.entry("宠物", "tag_pet"),
            Map.entry("植物", "tag_handicraft"), Map.entry("天文", "tag_science"), Map.entry("心理学", "tag_psychology"),
            Map.entry("哲学", "tag_philosophy"), Map.entry("历史", "tag_history"), Map.entry("写作", "tag_writing"),
            Map.entry("辩论", "tag_board_game"), Map.entry("志愿者", "tag_volunteer"), Map.entry("创业", "tag_business")
    );

    private static final Map<String, String> CODE_TO_NAME = Map.ofEntries(
            Map.entry("tag_fitness", "健身塑形"), Map.entry("tag_ball_sports", "球类运动"), Map.entry("tag_running_cycling", "跑步骑行"),
            Map.entry("tag_outdoor", "户外探索"), Map.entry("tag_yoga_meditation", "瑜伽冥想"), Map.entry("tag_dance", "舞蹈"),
            Map.entry("tag_extreme_sports", "极限运动"), Map.entry("tag_martial_arts", "武术格斗"),
            Map.entry("tag_photography", "摄影影像"), Map.entry("tag_drawing", "绘画插画"), Map.entry("tag_writing", "写作诗歌"),
            Map.entry("tag_music_creation", "音乐创作"), Map.entry("tag_handicraft", "手工制作"), Map.entry("tag_fashion", "时尚穿搭"),
            Map.entry("tag_interior_design", "室内设计"), Map.entry("tag_cooking", "烹饪料理"),
            Map.entry("tag_literature", "文学小说"), Map.entry("tag_indie_film", "独立电影"), Map.entry("tag_blockbuster", "商业大片"),
            Map.entry("tag_drama", "剧集追番"), Map.entry("tag_anime", "动漫二次元"), Map.entry("tag_gaming", "游戏玩家"),
            Map.entry("tag_board_game", "桌游策略"), Map.entry("tag_mystery", "推理悬疑"),
            Map.entry("tag_philosophy", "哲学思辨"), Map.entry("tag_psychology", "心理学"), Map.entry("tag_history", "历史人文"),
            Map.entry("tag_science", "科学科普"), Map.entry("tag_social_issues", "社会议题"), Map.entry("tag_business", "商业经济"),
            Map.entry("tag_tech", "技术极客"), Map.entry("tag_spirituality", "灵性信仰"),
            Map.entry("tag_food_dining", "探店美食"), Map.entry("tag_cooking_home", "自己下厨"), Map.entry("tag_coffee_tea", "咖啡茶道"),
            Map.entry("tag_street_food", "夜市小吃"), Map.entry("tag_healthy_eating", "健康饮食"), Map.entry("tag_novelty_food", "猎奇尝鲜"),
            Map.entry("tag_dessert", "甜品烘焙"), Map.entry("tag_beverage", "微醺酒饮"),
            Map.entry("tag_live_music", "现场音乐演出"), Map.entry("tag_study_buddy", "学习搭子"), Map.entry("tag_movie_companion", "观影同伴"),
            Map.entry("tag_script_killing", "剧本杀推理"), Map.entry("tag_volunteer", "志愿公益"), Map.entry("tag_pet", "宠物同好"),
            Map.entry("tag_exhibition", "逛展览馆"), Map.entry("tag_night_chat", "深夜聊天")
    );

    private static final Map<String, String> CODE_TO_DIMENSION = Map.ofEntries(
            Map.entry("tag_fitness", "body_space"), Map.entry("tag_ball_sports", "body_space"), Map.entry("tag_running_cycling", "body_space"),
            Map.entry("tag_outdoor", "body_space"), Map.entry("tag_yoga_meditation", "body_space"), Map.entry("tag_dance", "body_space"),
            Map.entry("tag_extreme_sports", "body_space"), Map.entry("tag_martial_arts", "body_space"),
            Map.entry("tag_photography", "aesthetics_creation"), Map.entry("tag_drawing", "aesthetics_creation"), Map.entry("tag_writing", "aesthetics_creation"),
            Map.entry("tag_music_creation", "aesthetics_creation"), Map.entry("tag_handicraft", "aesthetics_creation"), Map.entry("tag_fashion", "aesthetics_creation"),
            Map.entry("tag_interior_design", "aesthetics_creation"), Map.entry("tag_cooking", "aesthetics_creation"),
            Map.entry("tag_literature", "narrative_fiction"), Map.entry("tag_indie_film", "narrative_fiction"), Map.entry("tag_blockbuster", "narrative_fiction"),
            Map.entry("tag_drama", "narrative_fiction"), Map.entry("tag_anime", "narrative_fiction"), Map.entry("tag_gaming", "narrative_fiction"),
            Map.entry("tag_board_game", "narrative_fiction"), Map.entry("tag_mystery", "narrative_fiction"),
            Map.entry("tag_philosophy", "thought_exploration"), Map.entry("tag_psychology", "thought_exploration"), Map.entry("tag_history", "thought_exploration"),
            Map.entry("tag_science", "thought_exploration"), Map.entry("tag_social_issues", "thought_exploration"), Map.entry("tag_business", "thought_exploration"),
            Map.entry("tag_tech", "thought_exploration"), Map.entry("tag_spirituality", "thought_exploration"),
            Map.entry("tag_food_dining", "food_sensory"), Map.entry("tag_cooking_home", "food_sensory"), Map.entry("tag_coffee_tea", "food_sensory"),
            Map.entry("tag_street_food", "food_sensory"), Map.entry("tag_healthy_eating", "food_sensory"), Map.entry("tag_novelty_food", "food_sensory"),
            Map.entry("tag_dessert", "food_sensory"), Map.entry("tag_beverage", "food_sensory"),
            Map.entry("tag_live_music", "scene_community"), Map.entry("tag_study_buddy", "scene_community"), Map.entry("tag_movie_companion", "scene_community"),
            Map.entry("tag_script_killing", "scene_community"), Map.entry("tag_volunteer", "scene_community"), Map.entry("tag_pet", "scene_community"),
            Map.entry("tag_exhibition", "scene_community"), Map.entry("tag_night_chat", "scene_community")
    );

    /**
     * 旧格式（逗号分隔）转新格式 JSON 字符串
     */
    public static String legacyToNewFormat(String legacyInterests) {
        if (legacyInterests == null || legacyInterests.isBlank()) return null;
        Set<String> codes = new LinkedHashSet<>();
        for (String s : legacyInterests.split("[,，、;；]")) {
            String t = s.trim();
            if (t.isEmpty()) continue;
            String code = LEGACY_TO_CODE.get(t);
            if (code != null) codes.add(code);
        }
        if (codes.isEmpty()) return null;
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        for (String code : codes) {
            String dim = CODE_TO_DIMENSION.get(code);
            if (dim == null) dim = "other";
            result.computeIfAbsent(dim, k -> new ArrayList<>())
                    .add(Map.of("code", code, "sharing", 0.5, "intensity", 0.5));
        }
        try {
            return MAPPER.writeValueAsString(result);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从新格式 JSON 提取所有 tag code 集合
     */
    public static Set<String> extractCodesFromNewFormat(String interestTagsJson) {
        if (interestTagsJson == null || interestTagsJson.isBlank()) return Set.of();
        try {
            Map<String, List<Map<String, Object>>> parsed = MAPPER.readValue(interestTagsJson,
                    new TypeReference<Map<String, List<Map<String, Object>>>>() {});
            Set<String> codes = new HashSet<>();
            for (List<Map<String, Object>> list : parsed.values()) {
                if (list == null) continue;
                for (Map<String, Object> item : list) {
                    Object c = item.get("code");
                    if (c != null && !c.toString().isBlank()) codes.add(c.toString());
                }
            }
            return codes;
        } catch (Exception e) {
            return Set.of();
        }
    }

    public static List<TagSelection> extractSelectionsFromNewFormat(String interestTagsJson) {
        if (interestTagsJson == null || interestTagsJson.isBlank()) return List.of();
        try {
            Map<String, List<Map<String, Object>>> parsed = MAPPER.readValue(interestTagsJson,
                    new TypeReference<Map<String, List<Map<String, Object>>>>() {});
            List<TagSelection> items = new ArrayList<>();
            for (List<Map<String, Object>> list : parsed.values()) {
                if (list == null) continue;
                for (Map<String, Object> item : list) {
                    Object code = item.get("code");
                    if (code == null || code.toString().isBlank()) continue;
                    items.add(new TagSelection(
                            code.toString(),
                            toNormalizedDouble(item.get("sharing"), DEFAULT_SHARING),
                            toNormalizedDouble(item.get("intensity"), DEFAULT_INTENSITY)
                    ));
                }
            }
            return items;
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 从新格式 JSON 提取标签名列表（用于展示），需配合 code->name 映射
     */
    public static List<String> extractNamesFromNewFormat(String interestTagsJson, Map<String, String> codeToName) {
        Set<String> codes = extractCodesFromNewFormat(interestTagsJson);
        return codes.stream()
                .map(code -> codeToName != null ? codeToName.getOrDefault(code, code) : code)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 校验新格式 JSON 是否有效且非空
     */
    public static boolean isValidNonEmpty(String interestTagsJson) {
        if (interestTagsJson == null || interestTagsJson.isBlank()) return false;
        return !extractCodesFromNewFormat(interestTagsJson).isEmpty();
    }

    /**
     * 获取用于匹配的逗号分隔 tag code 字符串（从 portrait 或 legacy user.interests）
     */
    public static String getInterestsForMatching(String interestTagsJson, String legacyInterests) {
        if (interestTagsJson != null && isValidNonEmpty(interestTagsJson)) {
            return String.join(",", extractCodesFromNewFormat(interestTagsJson));
        }
        if (legacyInterests != null && !legacyInterests.isBlank()) {
            String converted = legacyToNewFormat(legacyInterests);
            if (converted != null) return String.join(",", extractCodesFromNewFormat(converted));
        }
        return null;
    }

    /**
     * 获取用于展示的逗号分隔标签名
     */
    public static String getInterestsForDisplay(String interestTagsJson, String legacyInterests) {
        if (interestTagsJson != null && isValidNonEmpty(interestTagsJson)) {
            List<String> names = extractNamesFromNewFormat(interestTagsJson, CODE_TO_NAME);
            return names.isEmpty() ? null : String.join(",", names);
        }
        return legacyInterests;
    }

    private static double toNormalizedDouble(Object value, double defaultValue) {
        if (value == null) return defaultValue;
        try {
            double parsed = Double.parseDouble(value.toString());
            if (Double.isNaN(parsed) || Double.isInfinite(parsed)) return defaultValue;
            return Math.max(0d, Math.min(1d, parsed));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public record TagSelection(String code, double sharing, double intensity) {}
}
