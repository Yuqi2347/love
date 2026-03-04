package com.campus.love.match.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.common.utils.BaziUtil;
import com.campus.love.follow.entity.Follow;
import com.campus.love.follow.mapper.FollowMapper;
import com.campus.love.match.constants.MatchWeights;
import com.campus.love.match.constants.MbtiCompatibilityMatrix;
import com.campus.love.match.constants.ZodiacCompatibilityTable;
import com.campus.love.match.dto.MatchResultResponse;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final UserMapper userMapper;
    private final FollowMapper followMapper;

    public List<MatchResultResponse> getRecommendations(int page, int size) {
        Long currentUserId = CurrentUser.getId();
        User currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);

        // 查询已关注的用户ID列表
        List<Long> followingIds = followMapper.selectList(
                new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowerId, currentUserId)
                        .select(Follow::getFollowingId)
        ).stream().map(Follow::getFollowingId).toList();

        List<User> candidates = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .ne(User::getId, currentUserId)
                        .eq(User::getStatus, 1)
                        .eq(User::getProfileComplete, true)
                        .notIn(User::getId, followingIds)  // 排除已关注的用户
        );

        List<MatchResultResponse> results = candidates.stream()
                .map(candidate -> calculateMatch(currentUser, candidate))
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
        return calculateMatch(currentUser, targetUser);
    }

    private MatchResultResponse calculateMatch(User self, User target) {
        int interestScore = calculateInterestScore(self.getInterests(), target.getInterests());
        int mbtiScore = MbtiCompatibilityMatrix.getCompatibility(self.getMbti(), target.getMbti());
        int zodiacScore = ZodiacCompatibilityTable.getCompatibility(self.getZodiac(), target.getZodiac());
        int baziScore = calculateBaziScore(self.getBirthDate(), target.getBirthDate());
        int majorScore = calculateMajorScore(self.getMajor(), target.getMajor());
        int ageScore = calculateAgeScore(self.getBirthDate(), target.getBirthDate());

        int totalScore = (int) (
                interestScore * MatchWeights.INTEREST_WEIGHT +
                mbtiScore * MatchWeights.MBTI_WEIGHT +
                zodiacScore * MatchWeights.ZODIAC_WEIGHT +
                baziScore * MatchWeights.BAZI_WEIGHT +
                majorScore * MatchWeights.MAJOR_WEIGHT +
                ageScore * MatchWeights.AGE_WEIGHT
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

    private int calculateInterestScore(String interests1, String interests2) {
        if (interests1 == null || interests2 == null) return 50;
        Set<String> set1 = new HashSet<>(Arrays.asList(interests1.split(",")));
        Set<String> set2 = new HashSet<>(Arrays.asList(interests2.split(",")));

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        if (union.isEmpty()) return 50;
        return (int) (((double) intersection.size() / union.size()) * 100);
    }

    private int calculateBaziScore(LocalDate date1, LocalDate date2) {
        if (date1 == null || date2 == null) return 50;
        return BaziUtil.getWuxingCompatibility(date1, date2);
    }

    private int calculateMajorScore(String major1, String major2) {
        if (major1 == null || major2 == null) return 50;
        return major1.equals(major2) ? 90 : 50;
    }

    private int calculateAgeScore(LocalDate date1, LocalDate date2) {
        if (date1 == null || date2 == null) return 50;
        int diff = Math.abs(Period.between(date1, date2).getYears());
        if (diff == 0) return 100;
        if (diff >= MatchWeights.MAX_AGE_DIFF_YEARS) return 20;
        return 100 - (diff * 80 / MatchWeights.MAX_AGE_DIFF_YEARS);
    }
}
