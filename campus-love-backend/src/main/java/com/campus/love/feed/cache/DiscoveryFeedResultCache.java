package com.campus.love.feed.cache;

import com.campus.love.feed.dto.FeedPostResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

/**
 * 探索列表短期本地缓存（按用户+分页+排序+关键词），减轻 DB 与组装压力。多实例部署时每节点独立缓存。
 */
@Slf4j
@Component
public class DiscoveryFeedResultCache {

    @Value("${app.feed.discovery-cache-seconds:20}")
    private int ttlSeconds;

    private Cache<String, List<FeedPostResponse>> cache;

    @PostConstruct
    void init() {
        if (ttlSeconds <= 0) {
            log.info("Discovery feed cache disabled (app.feed.discovery-cache-seconds<=0)");
            cache = null;
            return;
        }
        cache = Caffeine.newBuilder()
                .maximumSize(8_000)
                .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                .build();
        log.info("Discovery feed cache enabled: ttlSeconds={}, maxEntries=8000", ttlSeconds);
    }

    public List<FeedPostResponse> getOrCompute(String key, Function<String, List<FeedPostResponse>> loader) {
        if (cache == null) {
            return loader.apply(key);
        }
        return cache.get(key, loader);
    }

    /** 置顶、点赞/取消点赞等变更后清空探索列表缓存（列表内含有当前用户的 liked 与 likeCount 快照） */
    public void invalidateAll() {
        if (cache != null) {
            cache.invalidateAll();
        }
    }
}
