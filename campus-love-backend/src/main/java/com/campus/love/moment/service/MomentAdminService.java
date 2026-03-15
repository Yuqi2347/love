package com.campus.love.moment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.love.moment.entity.MomentEnrollment;
import com.campus.love.moment.enums.MomentPool;
import com.campus.love.moment.entity.MomentMatchConfig;
import com.campus.love.moment.entity.MomentMatchConfirm;
import com.campus.love.moment.entity.MomentMatchResult;
import com.campus.love.moment.entity.MomentPairScore;
import com.campus.love.moment.entity.MomentProfile;
import com.campus.love.moment.mapper.MomentEnrollmentMapper;
import com.campus.love.moment.mapper.MomentMatchConfirmMapper;
import com.campus.love.moment.mapper.MomentPairScoreMapper;
import com.campus.love.moment.mapper.MomentMatchResultMapper;
import com.campus.love.moment.mapper.MomentProfileMapper;
import com.campus.love.profile.entity.UserPortrait;
import com.campus.love.profile.service.UserPortraitService;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 心动时刻管理员服务：截止/开放报名、触发匹配、重置。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MomentAdminService {

    private final MomentEnrollmentMapper enrollmentMapper;
    private final MomentMatchResultMapper matchResultMapper;
    private final MomentMatchConfirmMapper matchConfirmMapper;
    private final MomentPairScoreMapper pairScoreMapper;
    private final MomentProfileMapper profileMapper;
    private final UserMapper userMapper;
    private final MomentMatcher matcher;
    private final MomentEnrollmentState enrollmentState;
    private final MomentMatchConfigService matchConfigService;
    private final UserPortraitService userPortraitService;
    private final MomentResultContentService momentResultContentService;
    private final ObjectMapper objectMapper;

    public Map<String, Object> closeEnrollment(String weekTag, String currentWeekTag) {
        if (weekTag == null || weekTag.isEmpty()) {
            weekTag = currentWeekTag;
        }
        enrollmentState.close(weekTag);
        log.info("管理员手动截止报名: weekTag={}", weekTag);

        int participantCount = 0;
        try {
            Long count = enrollmentMapper.selectCount(
                    new LambdaQueryWrapper<MomentEnrollment>()
                            .eq(MomentEnrollment::getWeekTag, weekTag)
            );
            participantCount = count != null ? count.intValue() : 0;
        } catch (Exception e) {
            log.warn("查询报名人数失败", e);
        }

        return Map.of(
                "weekTag", weekTag,
                "enrollmentOpen", false,
                "participantCount", participantCount
        );
    }

    public Map<String, Object> reopenEnrollment(String weekTag, String currentWeekTag) {
        if (weekTag == null || weekTag.isEmpty()) {
            weekTag = currentWeekTag;
        }
        enrollmentState.reopen(weekTag);
        log.info("管理员重新开放报名: weekTag={}", weekTag);
        return Map.of("weekTag", weekTag, "enrollmentOpen", true);
    }

    @Transactional
    public Map<String, Object> triggerMatching(String weekTag, String currentWeekTag) {
        if (weekTag == null || weekTag.isEmpty()) {
            weekTag = currentWeekTag;
        }

        enrollmentState.close(weekTag);
        log.info("开始触发心动时刻匹配（报名已自动截止）: weekTag={}", weekTag);

        List<MomentEnrollment> enrollments = enrollmentMapper.selectList(
                new LambdaQueryWrapper<MomentEnrollment>()
                        .eq(MomentEnrollment::getWeekTag, weekTag)
                        .eq(MomentEnrollment::getStatus, MomentEnrollment.STATUS_WAITING)
        );

        if (enrollments.isEmpty()) {
            return Map.of("message", "本周无待匹配用户", "weekTag", weekTag);
        }

        matchResultMapper.delete(new LambdaQueryWrapper<MomentMatchResult>()
                .eq(MomentMatchResult::getWeekTag, weekTag));
        pairScoreMapper.delete(new LambdaQueryWrapper<MomentPairScore>()
                .eq(MomentPairScore::getWeekTag, weekTag));

        Map<Long, User> userCache = new HashMap<>();
        Map<Long, MomentProfile> profileCache = new HashMap<>();
        Map<Long, UserPortrait> portraitCache = new HashMap<>();
        Map<String, List<MomentMatcher.Candidate>> poolCandidates = new HashMap<>();

        for (MomentEnrollment enrollment : enrollments) {
            Long userId = enrollment.getUserId();
            User user = userCache.computeIfAbsent(userId, userMapper::selectById);
            MomentProfile profile = profileCache.computeIfAbsent(userId, id -> profileMapper.selectOne(
                    new LambdaQueryWrapper<MomentProfile>().eq(MomentProfile::getUserId, id)
            ));
            UserPortrait portrait = portraitCache.computeIfAbsent(userId, userPortraitService::getPortrait);
            if (user == null || profile == null) {
                continue;
            }
            poolCandidates.computeIfAbsent(enrollment.getPool(), key -> new ArrayList<>())
                    .add(new MomentMatcher.Candidate(user, profile, portrait));
        }

        MomentMatchConfig config = matchConfigService.getConfig();
        int totalMatchedPairs = 0;
        String finalWeekTag = weekTag;
        Set<Long> globalMatchedUserIds = new HashSet<>();
        Map<String, Map<String, Object>> poolSummary = new LinkedHashMap<>();

        for (String pool : matcher.poolOrder()) {
            List<MomentMatcher.Candidate> candidates = poolCandidates.getOrDefault(pool, List.of()).stream()
                    .filter(c -> !globalMatchedUserIds.contains(c.user().getId()))
                    .toList();
            if (candidates.isEmpty()) {
                continue;
            }

            MomentMatcher.PoolMatchResult poolResult = matcher.match(candidates, pool, config);
            List<MomentMatcher.MatchPair> pairs = poolResult.matches();
            Set<Long> matchedUserIds = new HashSet<>();
            Set<String> matchedPairKeys = new HashSet<>();
            for (MomentMatcher.MatchPair pair : pairs) {
                long userIdA = Math.min(pair.userIdA(), pair.userIdB());
                long userIdB = Math.max(pair.userIdA(), pair.userIdB());
                User userA = userCache.get(userIdA);
                User userB = userCache.get(userIdB);
                MomentProfile profileA = profileCache.get(userIdA);
                MomentProfile profileB = profileCache.get(userIdB);
                UserPortrait portraitA = portraitCache.get(userIdA);
                UserPortrait portraitB = portraitCache.get(userIdB);
                MomentMatchResult result = new MomentMatchResult();
                result.setWeekTag(finalWeekTag);
                result.setPool(pool);
                result.setUserIdA(userIdA);
                result.setUserIdB(userIdB);
                result.setTotalScore(BigDecimal.valueOf(pair.totalScore()));
                try {
                    result.setScoreDetail(objectMapper.writeValueAsString(pair.scoreDetail()));
                } catch (Exception ex) {
                    log.warn("序列化分数明细失败", ex);
                }
                momentResultContentService.fillPrecomputedContent(
                        result, userA, userB, profileA, profileB, portraitA, portraitB, pair.scoreDetail()
                );
                matchResultMapper.insert(result);

                MomentMatchConfirm confirm = new MomentMatchConfirm();
                confirm.setMatchResultId(result.getId());
                confirm.setUserIdA(userIdA);
                confirm.setUserIdB(userIdB);
                matchConfirmMapper.insert(confirm);

                matchedUserIds.add(userIdA);
                matchedUserIds.add(userIdB);
                matchedPairKeys.add(pairKey(userIdA, userIdB));
            }

            for (MomentMatcher.PairEvaluation evaluation : poolResult.evaluations()) {
                long userIdA = Math.min(evaluation.userIdA(), evaluation.userIdB());
                long userIdB = Math.max(evaluation.userIdA(), evaluation.userIdB());
                MomentPairScore pairScore = new MomentPairScore();
                pairScore.setWeekTag(finalWeekTag);
                pairScore.setPool(pool);
                pairScore.setUserIdA(userIdA);
                pairScore.setUserIdB(userIdB);
                pairScore.setScore(BigDecimal.valueOf(evaluation.totalScore()));
                try {
                    pairScore.setScoreDetail(objectMapper.writeValueAsString(evaluation.scoreDetail()));
                } catch (Exception ex) {
                    log.warn("序列化候选对分数明细失败", ex);
                }
                pairScore.setHardFilterPassed(evaluation.hardFilterPassed());
                pairScore.setHardFilterReason(evaluation.hardFilterReason());
                pairScore.setSoftPenalty(evaluation.softPenalty());
                pairScore.setSoftPenaltyReason(evaluation.softPenaltyReason());
                pairScore.setThresholdOffsetA(evaluation.thresholdOffsetA());
                pairScore.setThresholdOffsetB(evaluation.thresholdOffsetB());
                pairScore.setEffectiveThresholdA(evaluation.effectiveThresholdA());
                pairScore.setEffectiveThresholdB(evaluation.effectiveThresholdB());
                pairScore.setThresholdRequired(evaluation.thresholdRequired());
                pairScore.setIncludedByThreshold(evaluation.includedByThreshold());
                pairScore.setMatched(matchedPairKeys.contains(pairKey(userIdA, userIdB)));
                pairScoreMapper.insert(pairScore);
            }
            globalMatchedUserIds.addAll(matchedUserIds);
            totalMatchedPairs += pairs.size();

            poolSummary.put(pool, Map.of(
                    "participants", candidates.size(),
                    "matchedPairs", pairs.size(),
                    "unmatchedUsers", Math.max(0, candidates.size() - pairs.size() * 2)
            ));
            log.info("池子 {} 匹配完成: {}人参与, {}对成功", pool, candidates.size(), pairs.size());
        }

        Set<Long> uniqueUserIds = enrollments.stream()
                .map(MomentEnrollment::getUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        int unmatchedUsers = 0;
        int maxStack = config.getPriorityMaxStack() != null ? config.getPriorityMaxStack() : MomentMatchConfig.DEFAULT_PRIORITY_MAX_STACK;
        for (Long userId : uniqueUserIds) {
            boolean matched = globalMatchedUserIds.contains(userId);
            enrollmentMapper.update(null, new LambdaUpdateWrapper<MomentEnrollment>()
                    .eq(MomentEnrollment::getUserId, userId)
                    .eq(MomentEnrollment::getWeekTag, finalWeekTag)
                    .set(MomentEnrollment::getStatus, matched ? MomentEnrollment.STATUS_MATCHED : MomentEnrollment.STATUS_UNMATCHED)
            );

            User user = userCache.get(userId);
            if (user != null) {
                user.setMomentPriorityCount(matched
                        ? 0
                        : Math.min(user.getMomentPriorityCountOrDefault() + 1, maxStack));
                userMapper.updateById(user);
            }
            if (!matched) {
                unmatchedUsers++;
            }
        }

        return Map.of(
                "weekTag", weekTag,
                "totalParticipants", uniqueUserIds.size(),
                "matchedPairs", totalMatchedPairs,
                "matchedUsers", globalMatchedUserIds.size(),
                "unmatchedUsers", unmatchedUsers,
                "baseThreshold", config.getBaseThreshold(),
                "poolSummary", poolSummary
        );
    }

    @Transactional
    public Map<String, Object> resetWeek(String weekTag, String currentWeekTag) {
        if (weekTag == null || weekTag.isEmpty()) {
            weekTag = currentWeekTag;
        }

        List<Long> matchResultIds = matchResultMapper.selectList(
                        new LambdaQueryWrapper<MomentMatchResult>()
                                .eq(MomentMatchResult::getWeekTag, weekTag))
                .stream()
                .map(MomentMatchResult::getId)
                .toList();

        if (!matchResultIds.isEmpty()) {
            matchConfirmMapper.delete(new LambdaQueryWrapper<MomentMatchConfirm>()
                    .in(MomentMatchConfirm::getMatchResultId, matchResultIds));
        }

        matchResultMapper.delete(
                new LambdaQueryWrapper<MomentMatchResult>()
                        .eq(MomentMatchResult::getWeekTag, weekTag)
        );

        pairScoreMapper.delete(
                new LambdaQueryWrapper<MomentPairScore>()
                        .eq(MomentPairScore::getWeekTag, weekTag)
        );

        enrollmentMapper.delete(
                new LambdaQueryWrapper<MomentEnrollment>()
                        .eq(MomentEnrollment::getWeekTag, weekTag)
        );

        enrollmentState.reopen(weekTag);

        log.info("管理员重置本周活动: weekTag={}", weekTag);
        return Map.of("weekTag", weekTag, "message", "本周活动已重置，所有数据已清除，报名已重新开放");
    }

    private String pairKey(long userIdA, long userIdB) {
        return userIdA < userIdB ? userIdA + "_" + userIdB : userIdB + "_" + userIdA;
    }
}
