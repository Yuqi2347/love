package com.campus.love.match.service;

import com.campus.love.common.utils.InterestTagConverter;
import com.campus.love.profile.entity.UserPortrait;
import com.campus.love.profile.mapper.UserPortraitMapper;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 标签权重缓存服务（V2.0）
 *
 * 功能：
 * 1. getIdfWeight() - 获取标签的IDF权重
 * 2. refreshCache() - 刷新缓存
 * 3. batchUpdateWeights() - 批量更新权重
 *
 * IDF（逆文档频率）权重原理：
 * - 热门标签（如"旅游"）被很多人使用，信息量少，权重低
 * - 稀有标签（如"洞箫演奏"）被少数人使用，信息量多，权重高
 *
 * @author Campus Love Team
 * @version 2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagWeightCache {

    private final UserMapper userMapper;
    private final UserPortraitMapper userPortraitMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Redis缓存键前缀
     */
    private static final String TAG_WEIGHT_PREFIX = "match:tag:weight:";
    private static final String TAG_STATS_PREFIX = "match:tag:stats:";

    /**
     * 默认IDF权重
     * 当标签不存在时返回此值
     */
    private static final double DEFAULT_IDF_WEIGHT = 1.0;

    /**
     * IDF权重范围
     */
    private static final double MIN_IDF_WEIGHT = 0.5;
    private static final double MAX_IDF_WEIGHT = 2.0;

    /**
     * 缓存过期时间（24小时）
     */
    private static final Duration CACHE_EXPIRATION = Duration.ofHours(24);

    /**
     * 获取标签的IDF权重
     *
     * @param tag 标签名称
     * @return IDF权重 [0.5, 2.0]
     */
    public double getIdfWeight(String tag) {
        if (tag == null || tag.isEmpty()) {
            return DEFAULT_IDF_WEIGHT;
        }

        String cacheKey = TAG_WEIGHT_PREFIX + tag;

        // 先从Redis缓存获取
        Double cachedWeight = (Double) redisTemplate.opsForValue().get(cacheKey);
        if (cachedWeight != null) {
            return cachedWeight;
        }

        // 缓存未命中，计算IDF权重
        double weight = calculateIdfWeight(tag);

        // 写入缓存
        redisTemplate.opsForValue().set(cacheKey, weight, CACHE_EXPIRATION);

        return weight;
    }

    /**
     * 批量获取标签IDF权重
     *
     * @param tags 标签集合
     * @return 标签 -> 权重映射
     */
    public Map<String, Double> batchGetIdfWeights(Collection<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Double> weights = new HashMap<>();
        for (String tag : tags) {
            weights.put(tag, getIdfWeight(tag));
        }
        return weights;
    }

    /**
     * 刷新单个标签的权重
     */
    public void refreshTagWeight(String tag) {
        if (tag == null || tag.isEmpty()) return;

        double weight = calculateIdfWeight(tag);
        String cacheKey = TAG_WEIGHT_PREFIX + tag;
        redisTemplate.opsForValue().set(cacheKey, weight, CACHE_EXPIRATION);

        log.debug("Refreshed weight for tag: {} = {}", tag, weight);
    }

    /**
     * 刷新所有标签权重缓存
     * 定时任务：每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @CacheEvict(value = "tagWeights", allEntries = true)
    public void refreshCache() {
        log.info("Starting tag weight cache refresh...");

        // 获取所有活跃用户的兴趣标签
        Set<String> allTags = getAllActiveTags();

        // 统计每个标签的使用人数
        Map<String, Integer> tagCounts = countTagUsage(allTags);

        // 计算并更新IDF权重
        int totalUsers = countActiveUsers();

        for (Map.Entry<String, Integer> entry : tagCounts.entrySet()) {
            String tag = entry.getKey();
            int count = entry.getValue();

            double idfWeight = calculateIdf(count, totalUsers);
            String cacheKey = TAG_WEIGHT_PREFIX + tag;
            redisTemplate.opsForValue().set(cacheKey, idfWeight, CACHE_EXPIRATION);
        }

        log.info("Tag weight cache refresh completed. Total tags: {}", allTags.size());
    }

    /**
     * 批量更新标签权重（手动触发）
     */
    public void batchUpdateWeights(Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return;
        }

        int totalUsers = countActiveUsers();
        Map<String, Integer> tagCounts = countTagUsage(tags);

        for (String tag : tags) {
            int count = tagCounts.getOrDefault(tag, 0);
            double idfWeight = calculateIdf(count, totalUsers);
            String cacheKey = TAG_WEIGHT_PREFIX + tag;
            redisTemplate.opsForValue().set(cacheKey, idfWeight, CACHE_EXPIRATION);
        }

        log.info("Batch updated weights for {} tags", tags.size());
    }

    /**
     * 获取热门标签（使用人数最多的前N个）
     *
     * @param limit 返回数量限制
     * @return 热门标签列表
     */
    public List<String> getTopTags(int limit) {
        Set<String> allTags = getAllActiveTags();
        Map<String, Integer> tagCounts = countTagUsage(allTags);

        return tagCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * 获取稀有标签（使用人数最少的标签）
     *
     * @param minUsers 最小用户数（过滤掉太冷门的）
     * @param limit 返回数量限制
     * @return 稀有标签列表
     */
    public List<String> getRareTags(int minUsers, int limit) {
        Set<String> allTags = getAllActiveTags();
        Map<String, Integer> tagCounts = countTagUsage(allTags);

        return tagCounts.entrySet().stream()
                .filter(e -> e.getValue() >= minUsers)
                .sorted(Map.Entry.comparingByValue())
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }

    // ==================== 私有方法 ====================

    /**
     * 计算IDF权重
     * 公式：IDF = log(totalUsers / usersWithTag + 1)
     * 归一化到 [MIN_IDF_WEIGHT, MAX_IDF_WEIGHT]
     */
    private double calculateIdfWeight(String tag) {
        Set<String> allTags = getAllActiveTags();
        Map<String, Integer> tagCounts = countTagUsage(allTags);

        int usersWithTag = tagCounts.getOrDefault(tag, 0);
        int totalUsers = countActiveUsers();

        return calculateIdf(usersWithTag, totalUsers);
    }

    /**
     * IDF计算核心公式
     */
    private double calculateIdf(int usersWithTag, int totalUsers) {
        if (usersWithTag == 0 || totalUsers == 0) {
            return DEFAULT_IDF_WEIGHT;
        }

        // IDF = log(totalUsers / usersWithTag)
        double rawIdf = Math.log((double) totalUsers / usersWithTag);

        // 归一化到 [0, 1]
        double normalized = Math.min(1.0, rawIdf / Math.log(totalUsers));

        // 映射到 [MIN_IDF_WEIGHT, MAX_IDF_WEIGHT]
        return MIN_IDF_WEIGHT + normalized * (MAX_IDF_WEIGHT - MIN_IDF_WEIGHT);
    }

    /**
     * 获取所有活跃用户的兴趣标签（tag code，新格式优先）
     */
    private Set<String> getAllActiveTags() {
        Set<String> allTags = new HashSet<>();
        List<UserPortrait> portraits = userPortraitMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserPortrait>()
                        .isNotNull(UserPortrait::getInterestTags)
                        .ne(UserPortrait::getInterestTags, "")
        );
        for (UserPortrait p : portraits) {
            allTags.addAll(InterestTagConverter.extractCodesFromNewFormat(p.getInterestTags()));
        }
        List<User> activeUsers = userMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getStatus, 1)
                        .isNotNull(User::getInterests)
                        .ne(User::getInterests, "")
        );
        for (User u : activeUsers) {
            String converted = InterestTagConverter.legacyToNewFormat(u.getInterests());
            if (converted != null) allTags.addAll(InterestTagConverter.extractCodesFromNewFormat(converted));
        }
        return allTags;
    }

    /**
     * 统计每个标签的使用人数（按 tag code）
     */
    private Map<String, Integer> countTagUsage(Collection<String> tags) {
        if (tags == null || tags.isEmpty()) return Collections.emptyMap();
        Map<String, Integer> tagCounts = new HashMap<>();
        List<UserPortrait> portraits = userPortraitMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserPortrait>()
                        .isNotNull(UserPortrait::getInterestTags)
                        .ne(UserPortrait::getInterestTags, "")
        );
        for (UserPortrait p : portraits) {
            Set<String> userCodes = InterestTagConverter.extractCodesFromNewFormat(p.getInterestTags());
            for (String tag : userCodes) {
                if (tags.contains(tag)) tagCounts.merge(tag, 1, Integer::sum);
            }
        }
        List<User> activeUsers = userMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getStatus, 1)
                        .isNotNull(User::getInterests)
                        .ne(User::getInterests, "")
        );
        for (User u : activeUsers) {
            String converted = InterestTagConverter.legacyToNewFormat(u.getInterests());
            if (converted == null) continue;
            Set<String> userCodes = InterestTagConverter.extractCodesFromNewFormat(converted);
            for (String tag : userCodes) {
                if (tags.contains(tag)) tagCounts.merge(tag, 1, Integer::sum);
            }
        }
        return tagCounts;
    }

    /**
     * 统计活跃用户总数
     */
    private int countActiveUsers() {
        return Math.toIntExact(userMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getStatus, 1)
        ));
    }

    /**
     * 清除所有标签权重缓存
     */
    public void clearAllCache() {
        Set<String> keys = redisTemplate.keys(TAG_WEIGHT_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Cleared {} tag weight cache entries", keys.size());
        }
    }

    /**
     * 预热标签权重缓存
     */
    public void warmUpCache() {
        log.info("Starting tag weight cache warm-up...");
        refreshCache();
        log.info("Tag weight cache warm-up completed");
    }
}
