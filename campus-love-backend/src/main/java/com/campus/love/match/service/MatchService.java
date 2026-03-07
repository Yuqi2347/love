package com.campus.love.match.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.common.utils.BaziCalculator;
import com.campus.love.follow.entity.Follow;
import com.campus.love.follow.mapper.FollowMapper;
import com.campus.love.match.constants.GlobalWeights;
import com.campus.love.match.constants.MbtiCompatibilityMatrix;
import com.campus.love.match.constants.ZodiacCompatibilityTable;
import com.campus.love.match.dto.MatchResultResponse;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 匹配服务（V2.0）
 *
 * 核心升级：
 * 1. 支持用户个性化动态权重
 * 2. 使用完整四柱八字算法
 * 3. 使用7档专业匹配
 * 4. 支持权重来源标识（默认/个性化）
 *
 * @author Campus Love Team
 * @version 2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchService {

    private final UserMapper userMapper;
    private final FollowMapper followMapper;
    private final UserWeightService userWeightService;
    private final InterestMatcher interestMatcher;
    private final MajorCategoryMatcher majorCategoryMatcher;

    /**
     * 获取推荐用户列表
     * @param page 页码
     * @param size 每页大小
     * @param genderFilter 性别筛选: all-全部, same-同性, opposite-异性
     */
    public List<MatchResultResponse> getRecommendations(int page, int size, String genderFilter) {
        Long currentUserId = CurrentUser.getId();
        User currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);

        // 查询已关注的用户ID列表
        List<Long> followingIds = followMapper.selectList(
                new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowerId, currentUserId)
                        .select(Follow::getFollowingId)
        ).stream().map(Follow::getFollowingId).toList();

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .ne(User::getId, currentUserId)
                .eq(User::getStatus, 1)
                .eq(User::getProfileComplete, true);

        // 性别筛选 (1=男, 2=女)
        if ("same".equals(genderFilter) && currentUser.getGender() != null) {
            wrapper.eq(User::getGender, currentUser.getGender());
        } else if ("opposite".equals(genderFilter) && currentUser.getGender() != null) {
            // 异性：男(1)找女(2)，女(2)找男(1)
            int oppositeGender = currentUser.getGender() == 1 ? 2 : 1;
            wrapper.eq(User::getGender, oppositeGender);
        }

        // 仅在已关注列表非空时排除，避免生成 `NOT IN ()` 语法错误
        if (!followingIds.isEmpty()) {
            wrapper.notIn(User::getId, followingIds);
        }

        List<User> candidates = userMapper.selectList(wrapper);

        // 获取当前用户的个性化权重
        Map<String, Double> weights = userWeightService.getEffectiveWeights(currentUserId);

        List<MatchResultResponse> results = candidates.stream()
                .map(candidate -> calculateMatch(currentUser, candidate, weights))
                .sorted(Comparator.comparingInt(MatchResultResponse::getMatchScore).reversed())
                .collect(Collectors.toList());

        int start = page * size;
        int end = Math.min(start + size, results.size());
        if (start >= results.size()) return Collections.emptyList();
        return results.subList(start, end);
    }

    public MatchResultResponse getMatchDetail(Long targetUserId) {
        Long currentUserId = CurrentUser.getId();
        User currentUser = userMapper.selectById(currentUserId);
        User targetUser = userMapper.selectById(targetUserId);
        if (currentUser == null || targetUser == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 获取当前用户的个性化权重
        Map<String, Double> weights = userWeightService.getEffectiveWeights(currentUserId);

        return calculateMatch(currentUser, targetUser, weights);
    }

    /**
     * 计算匹配度（使用个性化权重）
     */
    private MatchResultResponse calculateMatch(User self, User target, Map<String, Double> weights) {
        int interestScore = calculateInterestScore(self.getInterests(), target.getInterests());
        int mbtiScore = MbtiCompatibilityMatrix.getCompatibility(self.getMbti(), target.getMbti());
        int zodiacScore = ZodiacCompatibilityTable.getCompatibility(self.getZodiac(), target.getZodiac());
        // 八字合婚：同性交友时使用中性分数（不参与合婚评分）
        boolean isSameSex = self.getGender() != null && self.getGender().equals(target.getGender());
        int baziScore = isSameSex ? 50 : calculateBaziScore(self.getBirthDate(), self.getBirthTime(), target.getBirthDate(), target.getBirthTime());
        int majorScore = calculateMajorScore(self.getMajor(), target.getMajor());
        int ageScore = calculateAgeScore(self.getBirthDate(), target.getBirthDate());

        // 使用个性化权重计算总分
        int totalScore = (int) (
                interestScore * weights.getOrDefault("interest", GlobalWeights.getDefaultWeight("interest")) +
                mbtiScore * weights.getOrDefault("mbti", GlobalWeights.getDefaultWeight("mbti")) +
                zodiacScore * weights.getOrDefault("zodiac", GlobalWeights.getDefaultWeight("zodiac")) +
                baziScore * weights.getOrDefault("bazi", GlobalWeights.getDefaultWeight("bazi")) +
                majorScore * weights.getOrDefault("major", GlobalWeights.getDefaultWeight("major")) +
                ageScore * weights.getOrDefault("age", GlobalWeights.getDefaultWeight("age"))
        );

        return MatchResultResponse.builder()
                .userId(target.getId())
                .nickname(target.getNickname())
                .avatarUrl(target.getAvatarUrl())
                .gender(target.getGender())
                .school(target.getSchool())
                .major(target.getMajor())
                .grade(target.getGrade())
                .mbti(target.getMbti())
                .zodiac(target.getZodiac())
                .bio(target.getBio())
                .interests(target.getInterests())
                .matchScore(totalScore)
                .detail(MatchResultResponse.MatchDetail.builder()
                        .interestScore(interestScore)
                        .mbtiScore(mbtiScore)
                        .zodiacScore(zodiacScore)
                        .baziScore(baziScore)
                        .majorScore(majorScore)
                        .ageScore(ageScore)
                        .build())
                .build();
    }

    /**
     * 计算兴趣匹配度（使用InterestMatcher）
     */
    private int calculateInterestScore(String interests1, String interests2) {
        return interestMatcher.calculateInterestScore(interests1, interests2);
    }

    /**
     * 计算八字匹配度（使用完整四柱算法）
     */
    private int calculateBaziScore(LocalDate date1, LocalTime time1, LocalDate date2, LocalTime time2) {
        LocalDateTime birth1 = null;
        LocalDateTime birth2 = null;

        if (date1 != null) {
            birth1 = LocalDateTime.of(date1, time1 != null ? time1 : LocalTime.NOON);
        }
        if (date2 != null) {
            birth2 = LocalDateTime.of(date2, time2 != null ? time2 : LocalTime.NOON);
        }

        return BaziCalculator.calculateHunYinScore(birth1, birth2);
    }

    /**
     * 计算专业匹配度（使用MajorCategoryMatcher 7档评分）
     */
    private int calculateMajorScore(String major1, String major2) {
        return majorCategoryMatcher.calculateMajorScore(major1, major2);
    }

    /**
     * 计算年龄匹配度
     */
    private int calculateAgeScore(LocalDate date1, LocalDate date2) {
        if (date1 == null || date2 == null) return 50;
        int diff = Math.abs(Period.between(date1, date2).getYears());
        if (diff == 0) return 100;
        if (diff >= GlobalWeights.MAX_AGE_DIFF_YEARS) return 20;
        return 100 - (diff * 80 / GlobalWeights.MAX_AGE_DIFF_YEARS);
    }

    // ==================== 兼容性方法 ====================

    /**
     * 兼容旧版API：不使用个性化权重
     */
    public MatchResultResponse getMatchDetailLegacy(Long targetUserId) {
        Long currentUserId = CurrentUser.getId();
        User currentUser = userMapper.selectById(currentUserId);
        User targetUser = userMapper.selectById(targetUserId);
        if (currentUser == null || targetUser == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return calculateMatch(currentUser, targetUser, GlobalWeights.DEFAULT_WEIGHTS);
    }

    /**
     * 批量计算用户之间的匹配度
     */
    public Map<Long, Integer> batchCalculateMatchScores(Long userId, List<Long> targetUserIds) {
        User currentUser = userMapper.selectById(userId);
        if (currentUser == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        Map<String, Double> weights = userWeightService.getEffectiveWeights(userId);
        Map<Long, Integer> scores = new HashMap<>();

        for (Long targetId : targetUserIds) {
            User targetUser = userMapper.selectById(targetId);
            if (targetUser != null) {
                MatchResultResponse result = calculateMatch(currentUser, targetUser, weights);
                scores.put(targetId, result.getMatchScore());
            }
        }

        return scores;
    }

    /**
     * 获取高匹配用户列表
     */
    public List<Long> getHighMatchUsers(Long userId, int limit) {
        List<Long> allUserIds = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .ne(User::getId, userId)
                        .eq(User::getStatus, 1)
                        .eq(User::getProfileComplete, true)
                        .select(User::getId)
        ).stream().map(User::getId).toList();

        Map<Long, Integer> scores = batchCalculateMatchScores(userId, allUserIds);

        return scores.entrySet().stream()
                .filter(e -> e.getValue() >= GlobalWeights.HIGH_MATCH_THRESHOLD)
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }
}
