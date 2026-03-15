package com.campus.love.match.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.common.utils.InterestTagConverter;
import com.campus.love.follow.entity.Follow;
import com.campus.love.follow.mapper.FollowMapper;
import com.campus.love.match.constants.GlobalWeights;
import com.campus.love.match.constants.ZodiacCompatibilityTable;
import com.campus.love.match.dto.MatchResultResponse;
import com.campus.love.profile.service.UserPortraitService;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 匹配服务（V2.0）
 *
 * 核心升级：
 * 1. 支持用户个性化动态权重
 * 2. 采用 OCEAN/兴趣/价值观/年龄年级/专业/星座 六维模型
 * 3. 支持真实 OCEAN 与冷启动双权重集
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
    private final UserPortraitService userPortraitService;
    private final UserWeightService userWeightService;
    private final InterestMatcher interestMatcher;
    private final MajorCategoryMatcher majorCategoryMatcher;
    private final OceanMatcher oceanMatcher;
    private final ValuesMatcher valuesMatcher;
    private final AgeGradeMatcher ageGradeMatcher;
    private final MatchSummaryService matchSummaryService;

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
     * 计算匹配度（使用当前用户有效权重）
     */
    private MatchResultResponse calculateMatch(User self, User target, Map<String, Double> weights) {
        var selfPortrait = userPortraitService.getPortrait(self.getId());
        var targetPortrait = userPortraitService.getPortrait(target.getId());
        String selfInterestTags = selfPortrait != null ? selfPortrait.getInterestTags() : null;
        String targetInterestTags = targetPortrait != null ? targetPortrait.getInterestTags() : null;
        String selfForMatch = InterestTagConverter.getInterestsForMatching(selfInterestTags, self.getInterests());
        String targetForMatch = InterestTagConverter.getInterestsForMatching(targetInterestTags, target.getInterests());
        String selfForDisplay = InterestTagConverter.getInterestsForDisplay(selfInterestTags, self.getInterests());
        String targetForDisplay = InterestTagConverter.getInterestsForDisplay(targetInterestTags, target.getInterests());

        int oceanScore = oceanMatcher.calculateOceanScore(selfPortrait, targetPortrait);
        int interestScore = calculateInterestScore(selfForMatch, targetForMatch);
        OptionalInt valuesScoreOptional = valuesMatcher.calculateValuesScore(selfPortrait, targetPortrait);
        Integer valuesScore = valuesScoreOptional.isPresent() ? valuesScoreOptional.getAsInt() : null;
        int ageGradeScore = ageGradeMatcher.calculateAgeGradeScore(self, target);
        int zodiacScore = ZodiacCompatibilityTable.getCompatibility(self.getZodiac(), target.getZodiac());
        int majorScore = calculateMajorScore(self.getMajor(), target.getMajor());

        Map<String, Double> effectiveWeights = new HashMap<>(weights);
        if (valuesScore == null) {
            effectiveWeights.put(
                    "interest",
                    effectiveWeights.getOrDefault("interest", GlobalWeights.getDefaultWeight("interest"))
                            + effectiveWeights.getOrDefault("values", GlobalWeights.getDefaultWeight("values"))
            );
            effectiveWeights.put("values", 0.0);
        }
        effectiveWeights = GlobalWeights.normalizeWeights(effectiveWeights);

        int totalScore = (int) Math.round(
                oceanScore * effectiveWeights.getOrDefault("ocean", GlobalWeights.getDefaultWeight("ocean")) +
                interestScore * effectiveWeights.getOrDefault("interest", GlobalWeights.getDefaultWeight("interest")) +
                (valuesScore != null ? valuesScore : 50) * effectiveWeights.getOrDefault("values", GlobalWeights.getDefaultWeight("values")) +
                ageGradeScore * effectiveWeights.getOrDefault("age_grade", GlobalWeights.getDefaultWeight("age_grade")) +
                zodiacScore * effectiveWeights.getOrDefault("zodiac", GlobalWeights.getDefaultWeight("zodiac")) +
                majorScore * effectiveWeights.getOrDefault("major", GlobalWeights.getDefaultWeight("major"))
        );

        MatchResultResponse.MatchDetail detail = MatchResultResponse.MatchDetail.builder()
                .oceanScore(oceanScore)
                .interestScore(interestScore)
                .valuesScore(valuesScore)
                .ageGradeScore(ageGradeScore)
                .zodiacScore(zodiacScore)
                .majorScore(majorScore)
                .build();
        String aiSummary = matchSummaryService.generateOneLiner(detail, selfForDisplay, targetForDisplay);

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
                .interests(targetForDisplay)
                .matchScore(totalScore)
                .aiSummary(aiSummary)
                .detail(detail)
                .build();
    }

    /**
     * 计算兴趣匹配度（使用InterestMatcher）
     */
    private int calculateInterestScore(String interests1, String interests2) {
        return interestMatcher.calculateInterestScore(interests1, interests2);
    }

    /**
     * 计算专业匹配度（使用MajorCategoryMatcher 7档评分）
     */
    private int calculateMajorScore(String major1, String major2) {
        return majorCategoryMatcher.calculateMajorScore(major1, major2);
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
        return calculateMatch(currentUser, targetUser, userWeightService.getBaseWeightsForUser(currentUserId));
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
