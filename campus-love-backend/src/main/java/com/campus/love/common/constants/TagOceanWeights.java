package com.campus.love.common.constants;

import java.util.List;
import java.util.Map;

/**
 * 兴趣标签到 OCEAN 的修正权重。
 * 数值范围为经验型增量，后续可基于真实行为数据再校准。
 */
public final class TagOceanWeights {

    private TagOceanWeights() {}

    public static final List<String> DIMENSIONS = List.of("O", "C", "E", "A", "N");

    public static final Map<String, Map<String, Integer>> TAG_OCEAN_WEIGHTS = Map.ofEntries(
            Map.entry("tag_fitness", weights("O", -3, "C", 7, "N", -5)),
            Map.entry("tag_ball_sports", weights("E", 6, "A", 2, "N", -2)),
            Map.entry("tag_running_cycling", weights("O", 2, "C", 6, "E", -1, "N", -4)),
            Map.entry("tag_outdoor", weights("O", 7, "C", 2, "E", 4, "N", -3)),
            Map.entry("tag_yoga_meditation", weights("O", 4, "C", 4, "E", -4, "N", -5)),
            Map.entry("tag_dance", weights("O", 4, "E", 8, "A", 1, "N", -1)),
            Map.entry("tag_extreme_sports", weights("O", 9, "C", -2, "E", 6, "N", 2)),
            Map.entry("tag_martial_arts", weights("O", 1, "C", 7, "A", -1, "N", -3)),

            Map.entry("tag_photography", weights("O", 6, "C", 2, "E", -1, "N", 1)),
            Map.entry("tag_drawing", weights("O", 7, "E", -2, "N", 2)),
            Map.entry("tag_writing", weights("O", 7, "C", 5, "E", -4, "N", 4)),
            Map.entry("tag_music_creation", weights("O", 8, "E", 2, "N", 2)),
            Map.entry("tag_handicraft", weights("O", 3, "C", 5, "E", -3, "N", -1)),
            Map.entry("tag_fashion", weights("O", 5, "E", 5, "N", 2)),
            Map.entry("tag_interior_design", weights("O", 4, "C", 6, "A", 1)),
            Map.entry("tag_cooking", weights("O", 3, "C", 4, "A", 4, "N", -2)),

            Map.entry("tag_literature", weights("O", 7, "A", 2, "N", 2)),
            Map.entry("tag_indie_film", weights("O", 8, "E", -2, "N", 1)),
            Map.entry("tag_blockbuster", weights("O", 1, "E", 2, "N", -1)),
            Map.entry("tag_drama", weights("O", 3, "E", -1, "N", 1)),
            Map.entry("tag_anime", weights("O", 4, "E", -1, "N", 1)),
            Map.entry("tag_gaming", weights("O", 2, "C", -2, "A", -1, "N", 1)),
            Map.entry("tag_board_game", weights("O", 4, "C", 2, "E", 4, "A", 1)),
            Map.entry("tag_mystery", weights("O", 6, "C", 2, "N", 1)),

            Map.entry("tag_philosophy", weights("O", 8, "N", 3)),
            Map.entry("tag_psychology", weights("O", 7, "A", 3, "N", 2)),
            Map.entry("tag_history", weights("O", 5, "C", 1)),
            Map.entry("tag_science", weights("O", 6, "C", 3, "N", -1)),
            Map.entry("tag_social_issues", weights("O", 6, "A", 5, "N", 2)),
            Map.entry("tag_business", weights("O", 3, "C", 5, "E", 3)),
            Map.entry("tag_tech", weights("O", 5, "C", 4, "E", -1, "A", -1)),
            Map.entry("tag_spirituality", weights("O", 7, "A", 2, "N", 3)),

            Map.entry("tag_food_dining", weights("O", 4, "E", 3, "A", 2, "N", -1)),
            Map.entry("tag_cooking_home", weights("O", 3, "C", 4, "A", 4, "N", -2)),
            Map.entry("tag_coffee_tea", weights("O", 4, "E", -2, "N", -1)),
            Map.entry("tag_street_food", weights("O", 2, "E", 3, "A", 1)),
            Map.entry("tag_healthy_eating", weights("O", 1, "C", 6, "N", -3)),
            Map.entry("tag_novelty_food", weights("O", 7, "E", 1)),
            Map.entry("tag_dessert", weights("O", 3, "A", 2, "N", -1)),
            Map.entry("tag_beverage", weights("O", 2, "E", 4, "N", 1)),

            Map.entry("tag_live_music", weights("O", 5, "E", 6, "N", 1)),
            Map.entry("tag_study_buddy", weights("C", 6, "E", 1, "A", 2)),
            Map.entry("tag_movie_companion", weights("O", 3, "E", -1, "A", 2)),
            Map.entry("tag_script_killing", weights("O", 5, "C", 1, "E", 5, "N", 1)),
            Map.entry("tag_volunteer", weights("E", 5, "A", 8, "C", 2)),
            Map.entry("tag_pet", weights("A", 4, "N", -2)),
            Map.entry("tag_exhibition", weights("O", 6, "E", -1)),
            Map.entry("tag_night_chat", weights("E", 1, "A", 2, "N", 4))
    );

    public static Map<String, Integer> weightsFor(String tagCode) {
        return TAG_OCEAN_WEIGHTS.getOrDefault(tagCode, Map.of());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Integer> weights(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("weights keyValues length must be even");
        }
        Map.Entry<String, Integer>[] entries = new Map.Entry[keyValues.length / 2];
        for (int i = 0; i < keyValues.length; i += 2) {
            entries[i / 2] = Map.entry((String) keyValues[i], (Integer) keyValues[i + 1]);
        }
        return Map.ofEntries(entries);
    }
}
