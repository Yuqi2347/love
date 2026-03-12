package com.campus.love.match.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 兴趣标签匹配服务（V2.0）
 *
 * 使用加权Jaccard相似度计算兴趣匹配分
 * 支持IDF（逆文档频率）权重，热门标签权重低，稀有标签权重高
 *
 * @author Campus Love Team
 * @version 2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterestMatcher {

    private final TagWeightCache tagWeightCache;

    /**
     * 计算兴趣匹配度（基础版本，无IDF权重）
     *
     * @param interests1 用户1的兴趣标签（逗号分隔）
     * @param interests2 用户2的兴趣标签（逗号分隔）
     * @return 匹配分数 [0, 100]
     */
    public int calculateInterestScore(String interests1, String interests2) {
        if (interests1 == null || interests1.isEmpty() ||
            interests2 == null || interests2.isEmpty()) {
            return 50;  // 数据缺失时返回中等分
        }

        Set<String> set1 = parseInterests(interests1);
        Set<String> set2 = parseInterests(interests2);

        if (set1.isEmpty() || set2.isEmpty()) return 50;

        // Jaccard相似度：交集大小 / 并集大小
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        if (union.isEmpty()) return 50;

        return (int) Math.round((double) intersection.size() / union.size() * 100);
    }

    /**
     * 计算兴趣匹配度（加权Jaccard，带IDF权重）
     *
     * @param interests1 用户1的兴趣标签
     * @param interests2 用户2的兴趣标签
     * @return 匹配分数 [0, 100]
     */
    public int calculateWeightedInterestScore(String interests1, String interests2) {
        if (interests1 == null || interests1.isEmpty() ||
            interests2 == null || interests2.isEmpty()) {
            return 50;
        }

        Map<String, Double> weights1 = toWeightMap(parseInterests(interests1));
        Map<String, Double> weights2 = toWeightMap(parseInterests(interests2));

        if (weights1.isEmpty() || weights2.isEmpty()) return 50;

        // 交集加权和（取两者中较小的权重）
        double intersectionWeight = weights1.entrySet().stream()
                .filter(e -> weights2.containsKey(e.getKey()))
                .mapToDouble(e -> Math.min(e.getValue(), weights2.get(e.getKey())))
                .sum();

        // 并集加权和（取两者中较大的权重）
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(weights1.keySet());
        allKeys.addAll(weights2.keySet());

        double unionWeight = allKeys.stream()
                .mapToDouble(k -> Math.max(
                        weights1.getOrDefault(k, 0.0),
                        weights2.getOrDefault(k, 0.0)))
                .sum();

        if (unionWeight == 0) return 50;

        return (int) Math.round((intersectionWeight / unionWeight) * 100);
    }

    /**
     * 解析兴趣标签字符串
     *
     * @param interests 逗号分隔的兴趣标签
     * @return 标签集合
     */
    private Set<String> parseInterests(String interests) {
        if (interests == null || interests.isEmpty()) {
            return Collections.emptySet();
        }

        return Arrays.stream(interests.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * 将兴趣标签转换为权重映射（带IDF权重）
     *
     * @param tags 兴趣标签集合
     * @return 标签 -> 权重映射
     */
    private Map<String, Double> toWeightMap(Set<String> tags) {
        if (tags == null || tags.isEmpty()) return Map.of();
        return tagWeightCache.batchGetIdfWeights(tags);
    }

    /**
     * 计算两个兴趣标签集合的重叠度
     *
     * @param tags1 标签集合1
     * @param tags2 标签集合2
     * @return 重叠分数 [0, 100]
     */
    public int calculateOverlapScore(Set<String> tags1, Set<String> tags2) {
        if (tags1 == null || tags1.isEmpty() ||
            tags2 == null || tags2.isEmpty()) {
            return 50;
        }

        Set<String> intersection = new HashSet<>(tags1);
        intersection.retainAll(tags2);

        Set<String> union = new HashSet<>(tags1);
        union.addAll(tags2);

        if (union.isEmpty()) return 50;

        return (int) Math.round((double) intersection.size() / union.size() * 100);
    }

    /**
     * 获取推荐标签（基于用户兴趣）
     *
     * @param userInterests 用户当前兴趣
     * @param allAvailableTags 所有可用标签
     * @param limit 推荐数量限制
     * @return 推荐标签列表
     */
    public List<String> getRecommendedTags(String userInterests, List<String> allAvailableTags, int limit) {
        Set<String> currentTags = parseInterests(userInterests);

        return allAvailableTags.stream()
                .filter(tag -> !currentTags.contains(tag))
                .limit(limit)
                .toList();
    }

    /**
     * 判断两个用户的兴趣是否高度相关
     *
     * @param interests1 兴趣1
     * @param interests2 兴趣2
     * @param threshold 阈值（默认70）
     * @return 是否高度相关
     */
    public boolean isHighlyRelevant(String interests1, String interests2, int threshold) {
        return calculateInterestScore(interests1, interests2) >= threshold;
    }

    public boolean isHighlyRelevant(String interests1, String interests2) {
        return isHighlyRelevant(interests1, interests2, 70);
    }
}
