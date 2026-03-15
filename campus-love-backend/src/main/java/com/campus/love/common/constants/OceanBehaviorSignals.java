package com.campus.love.common.constants;

import java.util.List;
import java.util.Map;

public final class OceanBehaviorSignals {

    private OceanBehaviorSignals() {}

    public static final Map<String, Map<String, Double>> SIGNAL_WEIGHTS = Map.of(
            "O", Map.of(
                    "browse_category_diversity", 0.50d,
                    "feed_topic_span", 0.30d,
                    "interest_tag_breadth", 0.20d
            ),
            "C", Map.of(
                    "active_time_regularity", 0.55d,
                    "chat_reply_consistency", 0.30d,
                    "invite_completion_rate", 0.15d
            ),
            "E", Map.of(
                    "chat_initiation_ratio", 0.50d,
                    "feed_publish_frequency", 0.30d,
                    "invite_initiation_count", 0.20d
            ),
            "A", Map.of(
                    "positive_reaction_ratio", 0.35d,
                    "invite_acceptance_rate", 0.30d,
                    "conflict_recovery_speed", 0.20d,
                    "block_report_rate", 0.15d
            ),
            "N", Map.of(
                    "feed_sentiment_score", 0.55d,
                    "late_night_activity_ratio", 0.25d,
                    "chat_abandon_rate", 0.20d
            )
    );

    public static final List<String> DIMENSIONS = TagOceanWeights.DIMENSIONS;
}
