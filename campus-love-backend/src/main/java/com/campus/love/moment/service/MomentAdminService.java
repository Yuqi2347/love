package com.campus.love.moment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.love.moment.entity.MomentEnrollment;
import com.campus.love.moment.enums.MomentPool;
import com.campus.love.moment.entity.MomentMatchResult;
import com.campus.love.moment.entity.MomentProfile;
import com.campus.love.moment.mapper.MomentEnrollmentMapper;
import com.campus.love.moment.mapper.MomentMatchResultMapper;
import com.campus.love.moment.mapper.MomentProfileMapper;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * 心动时刻管理员服务：截止/开放报名、触发匹配、重置。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MomentAdminService {

    private final MomentEnrollmentMapper enrollmentMapper;
    private final MomentMatchResultMapper matchResultMapper;
    private final MomentProfileMapper profileMapper;
    private final UserMapper userMapper;
    private final MomentMatcher matcher;
    private final MomentEnrollmentState enrollmentState;
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

        Map<String, List<MomentMatcher.Candidate>> poolCandidates = new HashMap<>();

        for (MomentEnrollment e : enrollments) {
            User user = userMapper.selectById(e.getUserId());
            MomentProfile profile = profileMapper.selectOne(
                    new LambdaQueryWrapper<MomentProfile>()
                            .eq(MomentProfile::getUserId, e.getUserId())
            );

            if (user == null || profile == null) continue;

            String pool = e.getPool();
            poolCandidates.computeIfAbsent(pool, k -> new ArrayList<>())
                    .add(new MomentMatcher.Candidate(user, profile));
        }

        int totalMatched = 0;
        int totalUnmatched = 0;
        String finalWeekTag = weekTag;
        Set<Long> globalMatchedUserIds = new HashSet<>();

        for (Map.Entry<String, List<MomentMatcher.Candidate>> entry : poolCandidates.entrySet()) {
            String pool = entry.getKey();
            List<MomentMatcher.Candidate> candidates = entry.getValue();
            // 排除已在其他池匹配成功的用户，避免重复匹配
            candidates = candidates.stream()
                    .filter(c -> !globalMatchedUserIds.contains(c.user().getId()))
                    .toList();
            boolean isMFPool = MomentPool.MF.getCode().equals(pool);

            List<MomentMatcher.MatchPair> pairs = matcher.match(candidates, isMFPool);

            Set<Long> matchedUserIds = new HashSet<>();
            for (MomentMatcher.MatchPair pair : pairs) {
                MomentMatchResult result = new MomentMatchResult();
                result.setWeekTag(finalWeekTag);
                result.setPool(pool);
                result.setUserIdA(pair.userIdA());
                result.setUserIdB(pair.userIdB());
                result.setTotalScore(BigDecimal.valueOf(pair.totalScore()));
                try {
                    result.setScoreDetail(objectMapper.writeValueAsString(pair.scoreDetail()));
                } catch (Exception ex) {
                    log.warn("序列化分数明细失败", ex);
                }
                matchResultMapper.insert(result);

                matchedUserIds.add(pair.userIdA());
                matchedUserIds.add(pair.userIdB());
            }
            globalMatchedUserIds.addAll(matchedUserIds);

            for (MomentMatcher.Candidate c : candidates) {
                Long uid = c.user().getId();
                String newStatus = matchedUserIds.contains(uid)
                        ? MomentEnrollment.STATUS_MATCHED
                        : MomentEnrollment.STATUS_UNMATCHED;

                enrollmentMapper.update(null, new LambdaUpdateWrapper<MomentEnrollment>()
                        .eq(MomentEnrollment::getUserId, uid)
                        .eq(MomentEnrollment::getWeekTag, finalWeekTag)
                        .set(MomentEnrollment::getStatus, newStatus)
                );

                if (matchedUserIds.contains(uid)) totalMatched++;
                else totalUnmatched++;
            }

            log.info("池子 {} 匹配完成: {}人参与, {}对成功", pool, candidates.size(), pairs.size());
        }

        return Map.of(
                "weekTag", weekTag,
                "totalParticipants", enrollments.size(),
                "matched", totalMatched,
                "unmatched", totalUnmatched
        );
    }

    @Transactional
    public Map<String, Object> resetWeek(String weekTag, String currentWeekTag) {
        if (weekTag == null || weekTag.isEmpty()) {
            weekTag = currentWeekTag;
        }

        matchResultMapper.delete(
                new LambdaQueryWrapper<MomentMatchResult>()
                        .eq(MomentMatchResult::getWeekTag, weekTag)
        );

        enrollmentMapper.delete(
                new LambdaQueryWrapper<MomentEnrollment>()
                        .eq(MomentEnrollment::getWeekTag, weekTag)
        );

        enrollmentState.reopen(weekTag);

        log.info("管理员重置本周活动: weekTag={}", weekTag);
        return Map.of("weekTag", weekTag, "message", "本周活动已重置，所有数据已清除，报名已重新开放");
    }
}
