package com.campus.love.tracking;

import com.campus.love.tracking.entity.UserBehaviorLog;
import com.campus.love.tracking.mapper.UserBehaviorLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 行为追踪（技术文档 V1.1.0 第 1.3 节）
 * 匹配列表停留异常值处理：双层过滤
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BehaviorTracker {

    private final UserBehaviorLogMapper behaviorLogMapper;
    private final ObjectMapper objectMapper;

    private static final int MIN_STAY_SEC = 3;
    private static final int MAX_STAY_SEC = 60;

    /**
     * 追踪匹配卡片停留
     * @param userId 当前用户
     * @param targetUserId 被浏览用户
     * @param staySeconds 停留秒数
     * @param hasInteraction 是否有点击/滑动
     * @param postAction VIEW_PROFILE / SWIPE_LEFT / 其他
     */
    public void trackMatchCardView(Long userId, Long targetUserId, int staySeconds, boolean hasInteraction, String postAction) {
        if (staySeconds < MIN_STAY_SEC) return;
        int effectiveStay = Math.min(staySeconds, MAX_STAY_SEC);

        double weight;
        if ("VIEW_PROFILE".equals(postAction)) weight = 1.5;
        else if ("SWIPE_LEFT".equals(postAction)) weight = -0.5;
        else if (!hasInteraction && effectiveStay > 30) weight = 0.5;
        else weight = hasInteraction ? 1.0 : 0.7;

        try {
            Map<String, Object> meta = new HashMap<>();
            meta.put("effectiveStay", effectiveStay);
            meta.put("weight", weight);
            meta.put("postAction", postAction);

            UserBehaviorLog entry = new UserBehaviorLog();
            entry.setUserId(userId);
            entry.setTargetId(targetUserId);
            entry.setBehaviorType("MATCH_CARD_VIEW");
            entry.setMetadata(objectMapper.writeValueAsString(meta));
            entry.setCreatedAt(java.time.LocalDateTime.now());
            behaviorLogMapper.insert(entry);
        } catch (Exception e) {
            this.log.warn("Track match card view failed: {}", e.getMessage());
        }
    }

    /**
     * 追踪动态浏览（技术文档 FEED_VIEW）
     * 用于朋友圈浏览偏好、人物画像 OCEAN 更新
     */
    public void trackFeedView(Long userId, Long postId) {
        if (userId == null || postId == null) return;
        try {
            UserBehaviorLog entry = new UserBehaviorLog();
            entry.setUserId(userId);
            entry.setTargetId(postId);
            entry.setBehaviorType("FEED_VIEW");
            behaviorLogMapper.insert(entry);
        } catch (Exception e) {
            this.log.warn("Track feed view failed: {}", e.getMessage());
        }
    }
}
