package com.campus.love.moment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.moment.dto.MomentDatePrepResponse;
import com.campus.love.moment.entity.MomentActivityWeek;
import com.campus.love.moment.entity.MomentAdminLog;
import com.campus.love.moment.entity.MomentEnrollment;
import com.campus.love.moment.entity.MomentMatchConfig;
import com.campus.love.moment.entity.MomentMatchConfirm;
import com.campus.love.moment.entity.MomentMatchResult;
import com.campus.love.moment.entity.MomentPairScore;
import com.campus.love.moment.entity.MomentProfile;
import com.campus.love.moment.enums.MomentPool;
import com.campus.love.moment.mapper.MomentAdminLogMapper;
import com.campus.love.moment.mapper.MomentEnrollmentMapper;
import com.campus.love.moment.mapper.MomentMatchConfirmMapper;
import com.campus.love.moment.mapper.MomentMatchResultMapper;
import com.campus.love.moment.mapper.MomentPairScoreMapper;
import com.campus.love.moment.mapper.MomentProfileMapper;
import com.campus.love.pairdate.entity.MomentYueIntent;
import com.campus.love.pairdate.entity.PairDateNegotiation;
import com.campus.love.pairdate.mapper.MomentYueIntentMapper;
import com.campus.love.pairdate.mapper.PairDateNegotiationMapper;
import com.campus.love.profile.entity.UserPortrait;
import com.campus.love.profile.service.UserPortraitService;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 心动时刻管理员服务：总控、名单管理、结果中心、操作日志。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MomentAdminService {

    private static final String ACTION_MANUAL_CLOSE = "MANUAL_CLOSE";
    private static final String ACTION_AUTO_CLOSE = "AUTO_CLOSE";
    private static final String ACTION_MANUAL_TRIGGER_MATCH = "MANUAL_TRIGGER_MATCH";
    private static final String ACTION_AUTO_TRIGGER_MATCH = "AUTO_TRIGGER_MATCH";
    private static final String ACTION_MANUAL_REOPEN = "MANUAL_REOPEN";
    private static final String ACTION_RESET_WEEK = "RESET_WEEK";
    private static final String ACTION_REMOVE_ENROLLMENT = "REMOVE_ENROLLMENT";

    private static final String TARGET_WEEK = "WEEK";
    private static final String TARGET_ENROLLMENT = "ENROLLMENT";
    private static final String TARGET_MATCH_RESULT = "MATCH_RESULT";

    private final MomentEnrollmentMapper enrollmentMapper;
    private final MomentMatchResultMapper matchResultMapper;
    private final MomentMatchConfirmMapper matchConfirmMapper;
    private final MomentPairScoreMapper pairScoreMapper;
    private final MomentProfileMapper profileMapper;
    private final UserMapper userMapper;
    private final MomentMatcher matcher;
    private final MomentMatchConfigService matchConfigService;
    private final UserPortraitService userPortraitService;
    private final MomentResultContentService momentResultContentService;
    private final ObjectMapper objectMapper;
    private final MomentActivityWeekService activityWeekService;
    private final MomentAdminLogService momentAdminLogService;
    private final MomentAdminLogMapper adminLogMapper;
    private final PairDateNegotiationMapper pairDateNegotiationMapper;
    private final MomentYueIntentMapper momentYueIntentMapper;

    public MomentAdminOverviewResponse getOverview(String weekTag, String currentWeekTag) {
        String resolvedWeekTag = resolveWeekTag(weekTag, currentWeekTag);
        MomentMatchConfig config = matchConfigService.getConfig();
        MomentActivityWeek week = activityWeekService.getOrCreateWeek(resolvedWeekTag);
        List<MomentEnrollment> enrollments = enrollmentMapper.selectList(
                new LambdaQueryWrapper<MomentEnrollment>()
                        .eq(MomentEnrollment::getWeekTag, resolvedWeekTag)
        );
        List<MomentMatchResult> results = matchResultMapper.selectList(
                new LambdaQueryWrapper<MomentMatchResult>()
                        .eq(MomentMatchResult::getWeekTag, resolvedWeekTag)
        );

        Set<Long> participantIds = enrollments.stream()
                .map(MomentEnrollment::getUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> waitingUserIds = enrollments.stream()
                .filter(enrollment -> MomentEnrollment.STATUS_WAITING.equals(enrollment.getStatus()))
                .map(MomentEnrollment::getUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> matchedUserIds = new LinkedHashSet<>();
        for (MomentMatchResult result : results) {
            matchedUserIds.add(result.getUserIdA());
            matchedUserIds.add(result.getUserIdB());
        }

        Set<Long> unmatchedUserIds = new LinkedHashSet<>(participantIds);
        unmatchedUserIds.removeAll(waitingUserIds);
        unmatchedUserIds.removeAll(matchedUserIds);

        boolean enrollmentOpen = Boolean.TRUE.equals(week.getEnrollmentOpen());
        long participantCount = participantIds.size();
        long matchedPairs = results.size();
        long matchedUsers = matchedUserIds.size();
        long waitingUsers = waitingUserIds.size();
        long unmatchedUsers = unmatchedUserIds.size();
        double successRate = participantCount == 0
                ? 0d
                : BigDecimal.valueOf(matchedUsers * 100d / participantCount).setScale(1, RoundingMode.HALF_UP).doubleValue();

        LocalDateTime lastMatchAt = week.getMatchedAt() != null
                ? week.getMatchedAt()
                : results.stream()
                .map(MomentMatchResult::getCreatedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return new MomentAdminOverviewResponse(
                resolvedWeekTag,
                week.getStatus(),
                participantCount,
                waitingUsers,
                matchedUsers,
                unmatchedUsers,
                matchedPairs,
                successRate,
                enrollmentOpen,
                config.getBaseThreshold(),
                config.getAutoMatchEnabled(),
                config.getAutoMatchDayOfWeek(),
                config.getAutoMatchTime(),
                resolveNextAutoMatchAt(config, LocalDateTime.now()),
                lastMatchAt,
                buildPoolStats(enrollments, results),
                waitingUsers > 0 && !MomentActivityWeek.STATUS_RESULT_READY.equals(week.getStatus()),
                enrollmentOpen,
                !enrollmentOpen && !MomentActivityWeek.STATUS_RESULT_READY.equals(week.getStatus()),
                participantCount > 0 || matchedPairs > 0
        );
    }

    public Map<String, Object> closeEnrollment(String weekTag, String currentWeekTag, Long operatorId) {
        return closeEnrollmentInternal(weekTag, currentWeekTag, operatorId, false);
    }

    public Map<String, Object> closeEnrollmentBySystem(String weekTag, String currentWeekTag) {
        return closeEnrollmentInternal(weekTag, currentWeekTag, null, true);
    }

    public Map<String, Object> reopenEnrollment(String weekTag, String currentWeekTag, Long operatorId) {
        String resolvedWeekTag = resolveWeekTag(weekTag, currentWeekTag);
        MomentActivityWeek week = activityWeekService.reopenEnrollment(resolvedWeekTag);
        logAction(
                resolvedWeekTag,
                operatorId,
                ACTION_MANUAL_REOPEN,
                TARGET_WEEK,
                null,
                "管理员重新开放报名",
                detailJson(Map.of(
                        "weekTag", resolvedWeekTag,
                        "status", week.getStatus(),
                        "enrollmentOpen", week.getEnrollmentOpen()
                ))
        );
        log.info("管理员重新开放报名: weekTag={}", resolvedWeekTag);
        return Map.of("weekTag", resolvedWeekTag, "enrollmentOpen", true);
    }

    public Map<String, Object> triggerMatching(String weekTag, String currentWeekTag, Long operatorId) {
        return triggerMatchingInternal(weekTag, currentWeekTag, operatorId, false);
    }

    public Map<String, Object> triggerMatchingBySystem(String weekTag, String currentWeekTag) {
        return triggerMatchingInternal(weekTag, currentWeekTag, null, true);
    }

    @Transactional
    public Map<String, Object> resetWeek(String weekTag, String currentWeekTag, Long operatorId) {
        String resolvedWeekTag = resolveWeekTag(weekTag, currentWeekTag);
        List<Long> matchResultIds = matchResultMapper.selectList(
                        new LambdaQueryWrapper<MomentMatchResult>()
                                .eq(MomentMatchResult::getWeekTag, resolvedWeekTag))
                .stream()
                .map(MomentMatchResult::getId)
                .toList();

        if (!matchResultIds.isEmpty()) {
            matchConfirmMapper.delete(new LambdaQueryWrapper<MomentMatchConfirm>()
                    .in(MomentMatchConfirm::getMatchResultId, matchResultIds));
            momentYueIntentMapper.delete(new LambdaQueryWrapper<MomentYueIntent>()
                    .in(MomentYueIntent::getMatchResultId, matchResultIds));
        }
        int pairDateDeleted = pairDateNegotiationMapper.delete(
                new LambdaQueryWrapper<PairDateNegotiation>()
                        .eq(PairDateNegotiation::getWeekTag, resolvedWeekTag));
        int resultDeleted = matchResultMapper.delete(
                new LambdaQueryWrapper<MomentMatchResult>()
                        .eq(MomentMatchResult::getWeekTag, resolvedWeekTag)
        );
        int pairScoreDeleted = pairScoreMapper.delete(
                new LambdaQueryWrapper<MomentPairScore>()
                        .eq(MomentPairScore::getWeekTag, resolvedWeekTag)
        );
        int enrollmentDeleted = enrollmentMapper.delete(
                new LambdaQueryWrapper<MomentEnrollment>()
                        .eq(MomentEnrollment::getWeekTag, resolvedWeekTag)
        );

        MomentActivityWeek week = activityWeekService.resetWeek(resolvedWeekTag);
        logAction(
                resolvedWeekTag,
                operatorId,
                ACTION_RESET_WEEK,
                TARGET_WEEK,
                null,
                "已重置本周活动并恢复报名",
                detailJson(Map.of(
                        "weekTag", resolvedWeekTag,
                        "deletedEnrollments", enrollmentDeleted,
                        "deletedResults", resultDeleted,
                        "deletedPairScores", pairScoreDeleted,
                        "deletedPairDateNegotiations", pairDateDeleted,
                        "status", week.getStatus()
                ))
        );

        log.info("管理员重置本周活动: weekTag={}", resolvedWeekTag);
        return Map.of(
                "weekTag", resolvedWeekTag,
                "message", "本周活动已重置（含匹配结果、确认记录、约会三步协商与约一下意向），报名已重新开放",
                "deletedPairDateNegotiations", pairDateDeleted);
    }

    @Transactional
    public void removeEnrollment(Long userId, String weekTag, String currentWeekTag, Long operatorId) {
        String resolvedWeekTag = resolveWeekTag(weekTag, currentWeekTag);
        List<MomentEnrollment> enrollments = enrollmentMapper.selectList(
                new LambdaQueryWrapper<MomentEnrollment>()
                        .eq(MomentEnrollment::getUserId, userId)
                        .eq(MomentEnrollment::getWeekTag, resolvedWeekTag)
        );
        if (enrollments.isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报名记录不存在");
        }
        boolean removable = enrollments.stream().allMatch(item -> MomentEnrollment.STATUS_WAITING.equals(item.getStatus()));
        if (!removable) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅支持移除待匹配中的报名记录");
        }

        int deleted = enrollmentMapper.delete(
                new LambdaQueryWrapper<MomentEnrollment>()
                        .eq(MomentEnrollment::getUserId, userId)
                        .eq(MomentEnrollment::getWeekTag, resolvedWeekTag)
        );
        User user = userMapper.selectById(userId);
        logAction(
                resolvedWeekTag,
                operatorId,
                ACTION_REMOVE_ENROLLMENT,
                TARGET_ENROLLMENT,
                userId,
                "已移除报名用户 " + displayName(user, userId),
                detailJson(Map.of(
                        "weekTag", resolvedWeekTag,
                        "userId", userId,
                        "nickname", displayName(user, userId),
                        "deletedRows", deleted,
                        "pools", enrollments.stream().map(MomentEnrollment::getPool).distinct().toList()
                ))
        );
    }

    public IPage<MomentEnrollmentAdminItem> listEnrollments(int page,
                                                            int size,
                                                            String weekTag,
                                                            String pool,
                                                            String status,
                                                            String keyword) {
        LambdaQueryWrapper<MomentEnrollment> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(weekTag)) {
            wrapper.eq(MomentEnrollment::getWeekTag, weekTag.trim());
        }
        if (StringUtils.hasText(pool)) {
            wrapper.eq(MomentEnrollment::getPool, pool.trim());
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(MomentEnrollment::getStatus, status.trim());
        }
        wrapper.orderByDesc(MomentEnrollment::getCreatedAt);

        List<MomentEnrollment> enrollments = enrollmentMapper.selectList(wrapper);
        if (enrollments.isEmpty()) {
            return emptyPage(page, size);
        }

        Map<String, List<MomentEnrollment>> grouped = new LinkedHashMap<>();
        for (MomentEnrollment enrollment : enrollments) {
            String groupKey = enrollment.getWeekTag() + "_" + enrollment.getUserId();
            grouped.computeIfAbsent(groupKey, key -> new ArrayList<>()).add(enrollment);
        }

        Set<Long> userIds = grouped.values().stream()
                .map(items -> items.get(0).getUserId())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, User> userMap = loadUserMap(userIds);
        Map<Long, MomentProfile> profileMap = loadProfileMap(userIds);

        List<MomentEnrollmentAdminItem> items = grouped.values().stream()
                .map(group -> {
                    MomentEnrollment first = group.get(0);
                    Long userId = first.getUserId();
                    User user = userMap.get(userId);
                    MomentProfile profile = profileMap.get(userId);
                    LocalDateTime createdAt = group.stream()
                            .map(MomentEnrollment::getCreatedAt)
                            .filter(Objects::nonNull)
                            .max(LocalDateTime::compareTo)
                            .orElse(first.getCreatedAt());
                    List<String> pools = group.stream()
                            .map(MomentEnrollment::getPool)
                            .filter(StringUtils::hasText)
                            .distinct()
                            .sorted(Comparator.comparingInt(this::poolOrder))
                            .toList();
                    return new MomentEnrollmentAdminItem(
                            first.getWeekTag(),
                            userId,
                            displayName(user, userId),
                            user != null ? user.getSchool() : null,
                            user != null ? user.getMajor() : null,
                            user != null ? user.getGrade() : null,
                            pools,
                            first.getStatus(),
                            profile != null && Boolean.TRUE.equals(profile.getPrioritizeMatching()),
                            user != null ? user.getMomentPriorityCountOrDefault() : 0,
                            createdAt
                    );
                })
                .filter(item -> matchesEnrollmentKeyword(item, keyword))
                .toList();

        return paginateList(items, page, size);
    }

    public IPage<MomentMatchResultItem> listResults(int page,
                                                    int size,
                                                    String weekTag,
                                                    String pool,
                                                    String keyword) {
        LambdaQueryWrapper<MomentMatchResult> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(weekTag)) {
            wrapper.eq(MomentMatchResult::getWeekTag, weekTag.trim());
        }
        if (StringUtils.hasText(pool)) {
            wrapper.eq(MomentMatchResult::getPool, pool.trim());
        }
        wrapper.orderByDesc(MomentMatchResult::getCreatedAt);

        List<MomentMatchResult> results = matchResultMapper.selectList(wrapper);
        if (results.isEmpty()) {
            return emptyPage(page, size);
        }

        Set<Long> userIds = new LinkedHashSet<>();
        List<Long> resultIds = new ArrayList<>();
        for (MomentMatchResult result : results) {
            userIds.add(result.getUserIdA());
            userIds.add(result.getUserIdB());
            resultIds.add(result.getId());
        }
        Map<Long, User> userMap = loadUserMap(userIds);
        Map<Long, MomentMatchConfirm> confirmMap = loadConfirmMap(resultIds);

        List<MomentMatchResultItem> items = results.stream()
                .map(result -> {
                    User userA = userMap.get(result.getUserIdA());
                    User userB = userMap.get(result.getUserIdB());
                    MomentMatchConfirm confirm = confirmMap.get(result.getId());
                    return new MomentMatchResultItem(
                            result.getId(),
                            result.getWeekTag(),
                            result.getPool(),
                            result.getUserIdA(),
                            displayName(userA, result.getUserIdA()),
                            result.getUserIdB(),
                            displayName(userB, result.getUserIdB()),
                            result.getTotalScore(),
                            result.getYuanfenTitle(),
                            confirm != null ? confirm.getChoiceA() : null,
                            confirm != null ? confirm.getChoiceB() : null,
                            resolveConfirmStatus(confirm),
                            result.getCreatedAt()
                    );
                })
                .filter(item -> matchesResultKeyword(item, keyword))
                .toList();

        return paginateList(items, page, size);
    }

    public MomentMatchResultDetailResponse getResultDetail(Long id) {
        MomentMatchResult result = matchResultMapper.selectById(id);
        if (result == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "匹配结果不存在");
        }

        String rawDatePrepJson = result.getDatePrepJson();
        User userA = userMapper.selectById(result.getUserIdA());
        User userB = userMapper.selectById(result.getUserIdB());
        Map<Long, MomentProfile> profileMap = loadProfileMap(List.of(result.getUserIdA(), result.getUserIdB()));
        MomentProfile profileA = profileMap.get(result.getUserIdA());
        MomentProfile profileB = profileMap.get(result.getUserIdB());
        UserPortrait portraitA = userPortraitService.getPortrait(result.getUserIdA());
        UserPortrait portraitB = userPortraitService.getPortrait(result.getUserIdB());
        MomentMatchConfirm confirm = matchConfirmMapper.selectOne(
                new LambdaQueryWrapper<MomentMatchConfirm>()
                        .eq(MomentMatchConfirm::getMatchResultId, id)
                        .last("limit 1")
        );

        List<String> insightCards = new ArrayList<>();
        if (StringUtils.hasText(result.getInsightCard1())) {
            insightCards.add(result.getInsightCard1());
        }
        if (StringUtils.hasText(result.getInsightCard2())) {
            insightCards.add(result.getInsightCard2());
        }
        if (StringUtils.hasText(result.getInsightCard3())) {
            insightCards.add(result.getInsightCard3());
        }

        MomentDatePrepResponse datePrepA = momentResultContentService.getOrGenerateDatePrep(
                result, result.getUserIdA(), userA, userB, profileA, profileB, portraitA, portraitB
        );
        MomentDatePrepResponse datePrepB = momentResultContentService.getOrGenerateDatePrep(
                result, result.getUserIdB(), userB, userA, profileB, profileA, portraitB, portraitA
        );
        if (!Objects.equals(rawDatePrepJson, result.getDatePrepJson()) && result.getId() != null) {
            matchResultMapper.updateById(result);
        }

        return new MomentMatchResultDetailResponse(
                result.getId(),
                result.getWeekTag(),
                result.getPool(),
                result.getTotalScore(),
                result.getScoreDetail(),
                result.getYuanfenTitle(),
                result.getComplementaryModes(),
                result.getSoftPenaltyReasons(),
                result.getDateSceneType(),
                insightCards,
                result.getGoldenSentence(),
                result.getDimensionLabels(),
                result.getAboutUserA(),
                result.getAboutUserB(),
                datePrepA,
                datePrepB,
                result.getDatePrepJson(),
                buildMatchedUserCard(userA),
                buildMatchedUserCard(userB),
                confirm != null ? confirm.getChoiceA() : null,
                confirm != null ? confirm.getChoiceB() : null,
                confirm != null ? confirm.getChoiceAAt() : null,
                confirm != null ? confirm.getChoiceBAt() : null,
                resolveConfirmStatus(confirm),
                result.getCreatedAt()
        );
    }

    public IPage<MomentOperationLogItem> listLogs(int page, int size, String weekTag, String actionType) {
        LambdaQueryWrapper<MomentAdminLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(weekTag)) {
            wrapper.eq(MomentAdminLog::getWeekTag, weekTag.trim());
        }
        if (StringUtils.hasText(actionType)) {
            wrapper.eq(MomentAdminLog::getActionType, actionType.trim());
        }
        wrapper.orderByDesc(MomentAdminLog::getCreatedAt);

        Page<MomentAdminLog> pageReq = new Page<>(page, size);
        IPage<MomentAdminLog> logPage = adminLogMapper.selectPage(pageReq, wrapper);
        List<MomentAdminLog> records = logPage.getRecords();
        if (records.isEmpty()) {
            Page<MomentOperationLogItem> result = new Page<>(logPage.getCurrent(), logPage.getSize(), logPage.getTotal());
            result.setRecords(List.of());
            return result;
        }

        Set<Long> operatorIds = records.stream()
                .map(MomentAdminLog::getOperatorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, User> userMap = loadUserMap(operatorIds);
        List<MomentOperationLogItem> items = records.stream()
                .map(logItem -> new MomentOperationLogItem(
                        logItem.getId(),
                        logItem.getWeekTag(),
                        logItem.getOperatorId(),
                        logItem.getOperatorId() == null ? "SYSTEM" : displayName(userMap.get(logItem.getOperatorId()), logItem.getOperatorId()),
                        logItem.getActionType(),
                        logItem.getTargetType(),
                        logItem.getTargetId(),
                        logItem.getSummary(),
                        logItem.getDetailJson(),
                        logItem.getCreatedAt()
                ))
                .toList();

        Page<MomentOperationLogItem> result = new Page<>(logPage.getCurrent(), logPage.getSize(), logPage.getTotal());
        result.setRecords(items);
        return result;
    }

    @Transactional
    protected Map<String, Object> closeEnrollmentInternal(String weekTag,
                                                          String currentWeekTag,
                                                          Long operatorId,
                                                          boolean autoTriggered) {
        String resolvedWeekTag = resolveWeekTag(weekTag, currentWeekTag);
        if (autoTriggered) {
            activityWeekService.markAutoProcessed(resolvedWeekTag);
        }
        MomentActivityWeek before = activityWeekService.getOrCreateWeek(resolvedWeekTag);
        MomentActivityWeek after = activityWeekService.closeEnrollment(resolvedWeekTag);
        Long count = enrollmentMapper.selectCount(
                new LambdaQueryWrapper<MomentEnrollment>()
                        .eq(MomentEnrollment::getWeekTag, resolvedWeekTag)
        );
        int participantCount = count != null ? count.intValue() : 0;
        String summary = autoTriggered
                ? "系统自动截止报名"
                : Boolean.TRUE.equals(before.getEnrollmentOpen()) ? "管理员手动截止报名" : "报名已处于截止状态";
        logAction(
                resolvedWeekTag,
                operatorId,
                autoTriggered ? ACTION_AUTO_CLOSE : ACTION_MANUAL_CLOSE,
                TARGET_WEEK,
                null,
                summary,
                detailJson(Map.of(
                        "weekTag", resolvedWeekTag,
                        "participantCount", participantCount,
                        "status", after.getStatus(),
                        "enrollmentOpen", after.getEnrollmentOpen()
                ))
        );
        log.info("{}: weekTag={}", summary, resolvedWeekTag);

        return Map.of(
                "weekTag", resolvedWeekTag,
                "enrollmentOpen", false,
                "participantCount", participantCount
        );
    }

    @Transactional
    protected Map<String, Object> triggerMatchingInternal(String weekTag,
                                                          String currentWeekTag,
                                                          Long operatorId,
                                                          boolean autoTriggered) {
        String resolvedWeekTag = resolveWeekTag(weekTag, currentWeekTag);
        MomentActivityWeek week = activityWeekService.getOrCreateWeek(resolvedWeekTag);
        if (MomentActivityWeek.STATUS_RESULT_READY.equals(week.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "本周匹配结果已生成，如需重新执行请先重置本周活动");
        }
        if (autoTriggered) {
            activityWeekService.markAutoProcessed(resolvedWeekTag);
        }
        activityWeekService.closeEnrollment(resolvedWeekTag);
        log.info("开始触发心动时刻匹配（报名已截止）: weekTag={}, autoTriggered={}", resolvedWeekTag, autoTriggered);

        List<MomentEnrollment> enrollments = enrollmentMapper.selectList(
                new LambdaQueryWrapper<MomentEnrollment>()
                        .eq(MomentEnrollment::getWeekTag, resolvedWeekTag)
                        .eq(MomentEnrollment::getStatus, MomentEnrollment.STATUS_WAITING)
        );
        if (enrollments.isEmpty()) {
            String summary = autoTriggered ? "自动匹配跳过：无待匹配用户" : "本周无待匹配用户";
            logAction(
                    resolvedWeekTag,
                    operatorId,
                    autoTriggered ? ACTION_AUTO_TRIGGER_MATCH : ACTION_MANUAL_TRIGGER_MATCH,
                    TARGET_WEEK,
                    null,
                    summary,
                    detailJson(Map.of(
                            "weekTag", resolvedWeekTag,
                            "matchedPairs", 0,
                            "matchedUsers", 0,
                            "unmatchedUsers", 0
                    ))
            );
            return Map.of("message", "本周无待匹配用户", "weekTag", resolvedWeekTag);
        }

        deleteExistingResults(resolvedWeekTag);
        pairScoreMapper.delete(new LambdaQueryWrapper<MomentPairScore>()
                .eq(MomentPairScore::getWeekTag, resolvedWeekTag));

        Map<Long, User> userCache = new HashMap<>();
        Map<Long, MomentProfile> profileCache = new HashMap<>();
        Map<Long, UserPortrait> portraitCache = new HashMap<>();
        Map<String, List<MomentMatcher.Candidate>> poolCandidates = new HashMap<>();

        for (MomentEnrollment enrollment : enrollments) {
            Long userId = enrollment.getUserId();
            User user = userCache.computeIfAbsent(userId, userMapper::selectById);
            MomentProfile profile = profileCache.computeIfAbsent(userId, id -> profileMapper.selectOne(
                    new LambdaQueryWrapper<MomentProfile>().eq(MomentProfile::getUserId, id).last("limit 1")
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
        Set<Long> globalMatchedUserIds = new HashSet<>();
        Map<String, Map<String, Object>> poolSummary = new LinkedHashMap<>();

        for (String pool : matcher.poolOrder()) {
            List<MomentMatcher.Candidate> candidates = poolCandidates.getOrDefault(pool, List.of()).stream()
                    .filter(candidate -> !globalMatchedUserIds.contains(candidate.user().getId()))
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
                result.setWeekTag(resolvedWeekTag);
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
                pairScore.setWeekTag(resolvedWeekTag);
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
                    .eq(MomentEnrollment::getWeekTag, resolvedWeekTag)
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

        activityWeekService.markMatched(resolvedWeekTag);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("weekTag", resolvedWeekTag);
        response.put("totalParticipants", uniqueUserIds.size());
        response.put("matchedPairs", totalMatchedPairs);
        response.put("matchedUsers", globalMatchedUserIds.size());
        response.put("unmatchedUsers", unmatchedUsers);
        response.put("baseThreshold", config.getBaseThreshold());
        response.put("poolSummary", poolSummary);

        logAction(
                resolvedWeekTag,
                operatorId,
                autoTriggered ? ACTION_AUTO_TRIGGER_MATCH : ACTION_MANUAL_TRIGGER_MATCH,
                TARGET_WEEK,
                null,
                "匹配完成，生成 " + totalMatchedPairs + " 对结果",
                detailJson(response)
        );
        return response;
    }

    private void deleteExistingResults(String weekTag) {
        List<Long> resultIds = matchResultMapper.selectList(
                        new LambdaQueryWrapper<MomentMatchResult>()
                                .eq(MomentMatchResult::getWeekTag, weekTag))
                .stream()
                .map(MomentMatchResult::getId)
                .toList();
        if (!resultIds.isEmpty()) {
            matchConfirmMapper.delete(new LambdaQueryWrapper<MomentMatchConfirm>()
                    .in(MomentMatchConfirm::getMatchResultId, resultIds));
        }
        matchResultMapper.delete(new LambdaQueryWrapper<MomentMatchResult>()
                .eq(MomentMatchResult::getWeekTag, weekTag));
    }

    private Map<Long, User> loadUserMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(User::getId, user -> user, (a, b) -> a));
    }

    private Map<Long, MomentProfile> loadProfileMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return profileMapper.selectList(new LambdaQueryWrapper<MomentProfile>()
                        .in(MomentProfile::getUserId, userIds))
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(MomentProfile::getUserId, profile -> profile, (a, b) -> a));
    }

    private Map<Long, MomentMatchConfirm> loadConfirmMap(Collection<Long> matchResultIds) {
        if (matchResultIds == null || matchResultIds.isEmpty()) {
            return Map.of();
        }
        return matchConfirmMapper.selectList(new LambdaQueryWrapper<MomentMatchConfirm>()
                        .in(MomentMatchConfirm::getMatchResultId, matchResultIds))
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(MomentMatchConfirm::getMatchResultId, confirm -> confirm, (a, b) -> a));
    }

    private boolean matchesEnrollmentKeyword(MomentEnrollmentAdminItem item, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalized = keyword.trim().toLowerCase();
        return contains(String.valueOf(item.userId()), normalized)
                || contains(item.nickname(), normalized)
                || contains(item.school(), normalized)
                || contains(item.major(), normalized)
                || contains(item.grade(), normalized)
                || contains(item.weekTag(), normalized);
    }

    private boolean matchesResultKeyword(MomentMatchResultItem item, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalized = keyword.trim().toLowerCase();
        return contains(String.valueOf(item.id()), normalized)
                || contains(String.valueOf(item.userIdA()), normalized)
                || contains(String.valueOf(item.userIdB()), normalized)
                || contains(item.nicknameA(), normalized)
                || contains(item.nicknameB(), normalized)
                || contains(item.weekTag(), normalized)
                || contains(item.yuanfenTitle(), normalized);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private MatchedUserCard buildMatchedUserCard(User user) {
        if (user == null) {
            return new MatchedUserCard(null, null, null, null, null, null, null, null, null);
        }
        Integer age = null;
        if (user.getBirthDate() != null) {
            age = java.time.Period.between(user.getBirthDate(), LocalDate.now()).getYears();
        }
        return new MatchedUserCard(
                user.getId(),
                displayName(user, user.getId()),
                user.getGender(),
                user.getSchool(),
                user.getMajor(),
                user.getGrade(),
                user.getMbti(),
                user.getZodiac(),
                age
        );
    }

    private String resolveConfirmStatus(MomentMatchConfirm confirm) {
        if (confirm == null) {
            return "待确认";
        }
        boolean aChosen = StringUtils.hasText(confirm.getChoiceA());
        boolean bChosen = StringUtils.hasText(confirm.getChoiceB());
        if (MomentMatchConfirm.CHOICE_YUE.equals(confirm.getChoiceA())
                && MomentMatchConfirm.CHOICE_YUE.equals(confirm.getChoiceB())) {
            return "双向约一下";
        }
        if (MomentMatchConfirm.CHOICE_GUANZHU.equals(confirm.getChoiceA())
                || MomentMatchConfirm.CHOICE_GUANZHU.equals(confirm.getChoiceB())) {
            return "至少一方关注一下";
        }
        if (aChosen && bChosen) {
            return "双方已确认";
        }
        if (aChosen || bChosen) {
            return "等待对方确认";
        }
        return "待确认";
    }

    private String resolveWeekTag(String weekTag, String currentWeekTag) {
        return StringUtils.hasText(weekTag) ? weekTag.trim() : currentWeekTag;
    }

    private int poolOrder(String pool) {
        return switch (pool) {
            case "MF" -> 1;
            case "FF" -> 2;
            case "MM" -> 3;
            default -> 99;
        };
    }

    private String displayName(User user, Long fallbackId) {
        if (user == null) {
            return fallbackId == null ? "未知用户" : "用户" + fallbackId;
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        if (StringUtils.hasText(user.getEmail())) {
            return user.getEmail();
        }
        return user.getId() == null ? "未知用户" : "用户" + user.getId();
    }

    private void logAction(String weekTag,
                           Long operatorId,
                           String actionType,
                           String targetType,
                           Long targetId,
                           String summary,
                           String detailJson) {
        momentAdminLogService.log(weekTag, operatorId, actionType, targetType, targetId, summary, detailJson);
    }

    private String detailJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("序列化心动时刻后台日志详情失败", e);
            return null;
        }
    }

    private <T> Page<T> emptyPage(int page, int size) {
        Page<T> result = new Page<>(page, size, 0);
        result.setRecords(List.of());
        return result;
    }

    private <T> Page<T> paginateList(List<T> items, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int start = Math.min((safePage - 1) * safeSize, items.size());
        int end = Math.min(start + safeSize, items.size());
        Page<T> result = new Page<>(safePage, safeSize, items.size());
        result.setRecords(items.subList(start, end));
        return result;
    }

    private String pairKey(long userIdA, long userIdB) {
        return userIdA < userIdB ? userIdA + "_" + userIdB : userIdB + "_" + userIdA;
    }

    private LocalDateTime resolveNextAutoMatchAt(MomentMatchConfig config, LocalDateTime now) {
        if (config == null || !Boolean.TRUE.equals(config.getAutoMatchEnabled())) {
            return null;
        }
        LocalTime triggerTime = LocalTime.parse(config.getAutoMatchTime());
        int targetDay = config.getAutoMatchDayOfWeek();
        LocalDate nextDate = now.toLocalDate();
        int diff = targetDay - now.getDayOfWeek().getValue();
        if (diff < 0 || (diff == 0 && !now.toLocalTime().isBefore(triggerTime))) {
            diff += 7;
        }
        return LocalDateTime.of(nextDate.plusDays(diff), triggerTime);
    }

    private List<OverviewPoolStat> buildPoolStats(List<MomentEnrollment> enrollments, List<MomentMatchResult> results) {
        Map<String, Long> participants = enrollments.stream()
                .collect(Collectors.groupingBy(
                        MomentEnrollment::getPool,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                MomentEnrollment::getUserId,
                                Collectors.collectingAndThen(Collectors.toSet(), values -> (long) values.size())
                        )
                ));
        Map<String, Long> matchedPairs = results.stream()
                .collect(Collectors.groupingBy(MomentMatchResult::getPool, LinkedHashMap::new, Collectors.counting()));

        List<OverviewPoolStat> stats = new ArrayList<>();
        for (String pool : matcher.poolOrder()) {
            long participantCount = participants.getOrDefault(pool, 0L);
            long pairCount = matchedPairs.getOrDefault(pool, 0L);
            stats.add(new OverviewPoolStat(pool, participantCount, pairCount, Math.max(0L, participantCount - pairCount * 2)));
        }
        for (MomentPool pool : MomentPool.values()) {
            if (stats.stream().noneMatch(item -> item.pool().equals(pool.getCode()))) {
                stats.add(new OverviewPoolStat(pool.getCode(), 0L, 0L, 0L));
            }
        }
        stats.sort(Comparator.comparingInt(item -> poolOrder(item.pool())));
        return stats;
    }

    public record MomentAdminOverviewResponse(
            String weekTag,
            String phase,
            long participantCount,
            long waitingUsers,
            long matchedUsers,
            long unmatchedUsers,
            long matchedPairs,
            double successRate,
            boolean enrollmentOpen,
            int currentThreshold,
            Boolean autoMatchEnabled,
            Integer autoMatchDayOfWeek,
            String autoMatchTime,
            LocalDateTime nextAutoMatchAt,
            LocalDateTime lastMatchAt,
            List<OverviewPoolStat> poolStats,
            boolean canTriggerMatching,
            boolean canCloseEnrollment,
            boolean canReopenEnrollment,
            boolean canResetWeek
    ) {
    }

    public record OverviewPoolStat(
            String pool,
            long participants,
            long matchedPairs,
            long unmatchedUsers
    ) {
    }

    public record MomentEnrollmentAdminItem(
            String weekTag,
            Long userId,
            String nickname,
            String school,
            String major,
            String grade,
            List<String> pools,
            String status,
            boolean prioritizeMatching,
            int priorityCount,
            LocalDateTime createdAt
    ) {
    }

    public record MomentMatchResultItem(
            Long id,
            String weekTag,
            String pool,
            Long userIdA,
            String nicknameA,
            Long userIdB,
            String nicknameB,
            BigDecimal totalScore,
            String yuanfenTitle,
            String choiceA,
            String choiceB,
            String confirmStatus,
            LocalDateTime createdAt
    ) {
    }

    public record MatchedUserCard(
            Long userId,
            String nickname,
            Integer gender,
            String school,
            String major,
            String grade,
            String mbti,
            String zodiac,
            Integer age
    ) {
    }

    public record MomentMatchResultDetailResponse(
            Long id,
            String weekTag,
            String pool,
            BigDecimal totalScore,
            String scoreDetail,
            String yuanfenTitle,
            String complementaryModes,
            String softPenaltyReasons,
            String dateSceneType,
            List<String> insightCards,
            String goldenSentence,
            String dimensionLabels,
            String aboutUserA,
            String aboutUserB,
            MomentDatePrepResponse datePrepA,
            MomentDatePrepResponse datePrepB,
            String datePrepJson,
            MatchedUserCard userA,
            MatchedUserCard userB,
            String choiceA,
            String choiceB,
            LocalDateTime choiceAAt,
            LocalDateTime choiceBAt,
            String confirmStatus,
            LocalDateTime createdAt
    ) {
    }

    public record MomentOperationLogItem(
            Long id,
            String weekTag,
            Long operatorId,
            String operatorName,
            String actionType,
            String targetType,
            Long targetId,
            String summary,
            String detailJson,
            LocalDateTime createdAt
    ) {
    }
}
