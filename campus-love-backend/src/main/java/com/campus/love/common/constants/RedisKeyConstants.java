package com.campus.love.common.constants;

public final class RedisKeyConstants {

    private RedisKeyConstants() {}

    public static final String USER_PROFILE_PREFIX = "user:profile:";
    public static final String FOLLOW_MUTUAL_PREFIX = "follow:mutual:";
    public static final String CHAT_DAILY_COUNT_PREFIX = "chat:daily:count:";
    public static final String MATCH_RECOMMEND_PREFIX = "match:recommend:";
    public static final String FEED_TIMELINE_PREFIX = "feed:timeline:";
    public static final String EMAIL_VERIFY_CODE_PREFIX = "auth:email:verify:";
    public static final String RATE_LIMIT_PREFIX = "ratelimit:";

    public static String rateLimit(String type, String identifier) {
        return RATE_LIMIT_PREFIX + type + ":" + identifier;
    }

    public static String emailVerifyCode(String email) {
        return EMAIL_VERIFY_CODE_PREFIX + email;
    }

    public static String userProfile(Long userId) {
        return USER_PROFILE_PREFIX + userId;
    }

    public static String followMutual(Long userId) {
        return FOLLOW_MUTUAL_PREFIX + userId;
    }

    public static String chatDailyCount(Long senderId, Long receiverId, String date) {
        return CHAT_DAILY_COUNT_PREFIX + senderId + ":" + receiverId + ":" + date;
    }

    /** 未互关时 A 发给 B 的待回复标记，B 回复后清除 */
    public static final String CHAT_NONMUTUAL_SENT_PREFIX = "chat:nonmutual:sent:";

    public static String chatNonMutualSent(Long fromId, Long toId) {
        return CHAT_NONMUTUAL_SENT_PREFIX + fromId + ":" + toId;
    }

    public static String matchRecommend(Long userId) {
        return MATCH_RECOMMEND_PREFIX + userId;
    }

    public static String feedTimeline(Long userId) {
        return FEED_TIMELINE_PREFIX + userId;
    }

    private static final String PAIR_DATE_SUBMIT_PREFIX = "pairdate:submit:";

    public static String pairDateSubmit(Long userId, Long negotiationId) {
        return PAIR_DATE_SUBMIT_PREFIX + userId + ":" + negotiationId;
    }
}
