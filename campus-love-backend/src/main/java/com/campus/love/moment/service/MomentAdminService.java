package com.campus.love.moment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.moment.dto.MomentDatePrepResponse;
import com.campus.love.moment.entity.MomentActivityWeek;
import com.campus.love.moment.entity.MomentAiAnalysisTask;
import com.campus.love.moment.entity.MomentAdminLog;
import com.campus.love.moment.entity.MomentEnrollment;
import com.campus.love.moment.entity.MomentMatchConfig;
import com.campus.love.moment.entity.MomentMatchConfirm;
import com.campus.love.moment.entity.MomentMatchResult;
import com.campus.love.moment.entity.MomentMatchResultContent;
import com.campus.love.moment.entity.MomentPairScore;
import com.campus.love.moment.entity.MomentProfile;
import com.campus.love.moment.enums.MomentPool;
import com.campus.love.moment.mapper.MomentAiAnalysisTaskMapper;
import com.campus.love.moment.mapper.MomentAdminLogMapper;
import com.campus.love.moment.mapper.MomentEnrollmentMapper;
import com.campus.love.moment.mapper.MomentMatchConfirmMapper;
import com.campus.love.moment.mapper.MomentMatchResultContentMapper;
import com.campus.love.moment.mapper.MomentMatchResultMapper;
import com.campus.love.moment.mapper.MomentPairScoreMapper;
import com.campus.love.moment.mapper.MomentProfileMapper;
import com.campus.love.pairdate.util.PairDateTimeUtils;
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
import java.time.ZoneId;
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 心动时刻管理员服务：总控、名单管理、结果中心、操作日志。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MomentAdminService {

    private static final ZoneId ZONE_SHANGHAI = ZoneId.of("Asia/Shanghai");

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
    private final MomentMatchResultContentMapper matchResultContentMapper;
    private final MomentMatchConfirmMapper matchConfirmMapper;
    private final MomentPairScoreMapper pairScoreMapper;
    private final MomentProfileMapper profileMapper;
    private final UserMapper userMapper;
    private final MomentMatcher matcher;
    private final MomentMatchPipelineService momentMatchPipelineService;
    private final MomentWeekDataService momentWeekDataService;
    private final MomentMatchResetArchiveService momentMatchResetArchiveService;
    private final MomentMatchProgressRegistry momentMatchProgressRegistry;
    private final MomentAiAnalysisTaskMapper aiAnalysisTaskMapper;
    private final MomentMatchConfigService matchConfigService;
    private final UserPortraitService userPortraitService;
    private final MomentResultContentService momentResultContentService;
    private final ObjectMapper objectMapper;
    private final MomentActivityWeekService activityWeekService;
    private final MomentAdminLogService momentAdminLogService;
    private final MomentAdminLogMapper adminLogMapper;
    public MomentAdminOverviewResponse getOverview(String weekTag, String currentWeekTag) {
        String resolvedWeekTag = resolveWeekTag(weekTag, currentWeekTag);
        MomentMatchConfig config = matchConfigService.getConfig();
        MomentActivityWeek week = activityWeekService.getOrCreateWeek(resolvedWeekTag);
        long participantCount = nzLong(enrollmentMapper.countDistinctUsersByWeekTag(resolvedWeekTag));
        long waitingUsers = nzLong(enrollmentMapper.countDistinctWaitingByWeekTag(resolvedWeekTag));
        long matchedPairs = nzLong(matchResultMapper.countRowsByWeekTag(resolvedWeekTag));
        long matchedUsers = nzLong(matchResultMapper.countDistinctMatchedUsersByWeekTag(resolvedWeekTag));
        long unmatchedUsers = nzLong(enrollmentMapper.countDistinctUnmatchedByWeekTag(resolvedWeekTag));

        boolean enrollmentOpen = Boolean.TRUE.equals(week.getEnrollmentOpen());
        double successRate = participantCount == 0
                ? 0d
                : BigDecimal.valueOf(matchedUsers * 100d / participantCount).setScale(1, RoundingMode.HALF_UP).doubleValue();

        LocalDateTime lastMatchAt = week.getMatchedAt() != null
                ? week.getMatchedAt()
                : matchResultMapper.maxCreatedAtByWeekTag(resolvedWeekTag);

        LocalDateTime shNow = LocalDateTime.now(ZONE_SHANGHAI);

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
                resolveNextAutoMatchAt(config, shNow),
                lastMatchAt,
                buildPoolStatsAggregated(resolvedWeekTag),
                waitingUsers > 0 && !MomentWeekStatusPolicy.weekHasCompletedMatchingWork(week.getStatus()),
                enrollmentOpen,
                !enrollmentOpen && !MomentWeekStatusPolicy.blocksReopenEnrollment(week.getStatus()),
                participantCount > 0
                        || matchedPairs > 0
                        || !MomentActivityWeek.STATUS_ENROLLING.equals(week.getStatus()),
                MomentActivityWeek.STATUS_RESULT_READY.equals(week.getStatus()),
                config.getAutoPublishEnabled(),
                config.getAutoPublishDayOfWeek(),
                config.getAutoPublishTime(),
                resolveNextAutoPublishAt(config, shNow, resolvedWeekTag, week.getStatus())
        );
    }

    public Map<String, Object> closeEnrollment(String weekTag, String currentWeekTag, Long operatorId) {
        return closeEnrollmentInternal(weekTag, currentWeekTag, operatorId, false);
    }

    public Map<String, Object> closeEnrollmentBySystem(String weekTag, String currentWeekTag) {
        return closeEnrollmentInternal(weekTag, currentWeekTag, null, true);
    }

    public Map<String, Object> publishResult(String weekTag, String currentWeekTag, Long operatorId) {
        String resolvedWeekTag = resolveWeekTag(weekTag, currentWeekTag);
        MomentActivityWeek week = activityWeekService.getOrCreateWeek(resolvedWeekTag);
        if (!MomentActivityWeek.STATUS_RESULT_READY.equals(week.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅当状态为 RESULT_READY（管理员已可预览）时可公布");
        }
        activityWeekService.markPublished(resolvedWeekTag);
        logAction(
                resolvedWeekTag,
                operatorId,
                "MANUAL_PUBLISH",
                TARGET_WEEK,
                null,
                "已公布本周匹配结果",
                detailJson(Map.of("weekTag", resolvedWeekTag, "status", MomentActivityWeek.STATUS_PUBLISHED))
        );
        return Map.of("weekTag", resolvedWeekTag, "status", MomentActivityWeek.STATUS_PUBLISHED);
    }

    /**
     * 系统自动公布（定时任务）；状态不符时静默跳过，不抛业务异常。
     */
    @Transactional
    public Map<String, Object> publishResultBySystem(String weekTag, String currentWeekTag) {
        String resolvedWeekTag = resolveWeekTag(weekTag, currentWeekTag);
        MomentActivityWeek week = activityWeekService.getOrCreateWeek(resolvedWeekTag);
        if (!MomentActivityWeek.STATUS_RESULT_READY.equals(week.getStatus())) {
            return Map.of("skipped", true, "reason", "not_result_ready", "weekTag", resolvedWeekTag);
        }
        activityWeekService.markPublished(resolvedWeekTag);
        logAction(
                resolvedWeekTag,
                null,
                "AUTO_PUBLISH",
                TARGET_WEEK,
                null,
                "系统自动公布本周匹配结果",
                detailJson(Map.of("weekTag", resolvedWeekTag, "status", MomentActivityWeek.STATUS_PUBLISHED))
        );
        return Map.of("weekTag", resolvedWeekTag, "status", MomentActivityWeek.STATUS_PUBLISHED);
    }

    public Map<String, Object> getMatchProgress(String weekTag, String currentWeekTag) {
        String resolvedWeekTag = resolveWeekTag(weekTag, currentWeekTag);
        MomentActivityWeek week = activityWeekService.getOrCreateWeek(resolvedWeekTag);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", week.getStatus());
        // 仅 FAILED 时展示 error_message；成功流水线会切换状态但旧错误可能未清库，避免进度区误报历史异常
        body.put(
                "errorMessage",
                MomentActivityWeek.STATUS_FAILED.equals(week.getStatus()) ? week.getErrorMessage() : null);

        MomentMatchProgressRegistry.Snapshot snap = momentMatchProgressRegistry.getSnapshot();
        Map<String, Object> matchProgress = new LinkedHashMap<>();
        if (snap != null && resolvedWeekTag.equals(snap.weekTag())) {
            matchProgress.put("currentPool", snap.currentPool());
            matchProgress.put("processedPairs", snap.progressContext().getProcessedPairs());
            matchProgress.put("totalEstimatedPairs", snap.totalEstimatedPairs());
            matchProgress.put("matchedPairs", snap.matchedPairsSoFar());
        } else {
            matchProgress.put("currentPool", "");
            matchProgress.put("processedPairs", 0L);
            matchProgress.put("totalEstimatedPairs", 0L);
            matchProgress.put("matchedPairs", 0);
        }
        body.put("matchProgress", matchProgress);

        long aiTotal = aiAnalysisTaskMapper.selectCount(
                new LambdaQueryWrapper<MomentAiAnalysisTask>().eq(MomentAiAnalysisTask::getWeekTag, resolvedWeekTag));
        long aiDone = aiAnalysisTaskMapper.selectCount(
                new LambdaQueryWrapper<MomentAiAnalysisTask>()
                        .eq(MomentAiAnalysisTask::getWeekTag, resolvedWeekTag)
                        .eq(MomentAiAnalysisTask::getStatus, MomentAiAnalysisTask.STATUS_DONE));
        long aiFailed = aiAnalysisTaskMapper.selectCount(
                new LambdaQueryWrapper<MomentAiAnalysisTask>()
                        .eq(MomentAiAnalysisTask::getWeekTag, resolvedWeekTag)
                        .eq(MomentAiAnalysisTask::getStatus, MomentAiAnalysisTask.STATUS_FAILED));
        Map<String, Object> ai = new LinkedHashMap<>();
        ai.put("total", aiTotal);
        ai.put("done", aiDone);
        ai.put("failed", aiFailed);
        body.put("aiProgress", ai);
        return body;
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
        Optional<String> archiveBatch =
                momentMatchResetArchiveService.archiveWeekMatchResultsBeforeDelete(resolvedWeekTag, operatorId);

        momentWeekDataService.deletePipelineDataForWeek(resolvedWeekTag);

        // 删除本周报名记录：若仅把状态改回 WAITING，用户端仍会显示「正在为你寻找」
        int enrollmentsDeleted = enrollmentMapper.delete(
                new LambdaQueryWrapper<MomentEnrollment>()
                        .eq(MomentEnrollment::getWeekTag, resolvedWeekTag)
        );

        MomentActivityWeek week = activityWeekService.resetWeekToWaitingMatch(resolvedWeekTag);
        Map<String, Object> logDetail = new LinkedHashMap<>();
        logDetail.put("weekTag", resolvedWeekTag);
        logDetail.put("enrollmentRowsDeleted", enrollmentsDeleted);
        logDetail.put("status", week.getStatus());
        archiveBatch.ifPresent(b -> logDetail.put("archiveSnapshotBatchId", b));
        logAction(
                resolvedWeekTag,
                operatorId,
                ACTION_RESET_WEEK,
                TARGET_WEEK,
                null,
                "已重置本周匹配数据，本周报名记录已清空（用户需重新报名）",
                detailJson(logDetail)
        );

        log.info("管理员重置本周心动时刻: weekTag={}", resolvedWeekTag);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("weekTag", resolvedWeekTag);
        body.put("message", "本周匹配数据已清空，用户需重新报名参加本周活动");
        body.put("enrollmentRowsDeleted", enrollmentsDeleted);
        archiveBatch.ifPresent(b -> body.put("archiveSnapshotBatchId", b));
        return body;
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
                                                            Integer gender,
                                                            String status,
                                                            String keyword) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        long offset = (long) (safePage - 1) * safeSize;
        String keywordLike = keywordLike(keyword);
        String weekTagFilter = normalizeFilter(weekTag);
        String poolFilter = normalizeFilter(pool);
        Integer genderFilter = (gender != null && (gender == 1 || gender == 2)) ? gender : null;
        String statusFilter = normalizeFilter(status);

        long total = nzLong(enrollmentMapper.countAdminEnrollmentGroups(
                weekTagFilter, poolFilter, genderFilter, statusFilter, keywordLike
        ));
        if (total == 0) {
            return emptyPage(safePage, safeSize);
        }

        List<Map<String, Object>> rows = enrollmentMapper.selectAdminEnrollmentGroups(
                weekTagFilter, poolFilter, genderFilter, statusFilter, keywordLike, offset, safeSize
        );
        if (rows.isEmpty()) {
            return emptyPage(safePage, safeSize);
        }

        Set<Long> userIds = rows.stream()
                .map(row -> toLong(row.get("userId")))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MomentProfile> profileMap = loadProfileMap(userIds);

        List<MomentEnrollmentAdminItem> items = rows.stream().map(row -> {
            Long userId = toLong(row.get("userId"));
            MomentProfile profile = userId != null ? profileMap.get(userId) : null;
            return new MomentEnrollmentAdminItem(
                    stringValue(row.get("weekTag")),
                    userId,
                    stringValue(row.get("nickname")),
                    toNullableInt(row.get("gender")),
                    stringValue(row.get("school")),
                    stringValue(row.get("major")),
                    stringValue(row.get("grade")),
                    parsePools(row.get("pools")),
                    stringValue(row.get("status")),
                    profile != null && Boolean.TRUE.equals(profile.getPrioritizeMatching()),
                    toInt(row.get("priorityCount")),
                    toLocalDateTime(row.get("createdAt"))
            );
        }).toList();

        Page<MomentEnrollmentAdminItem> result = new Page<>(safePage, safeSize, total);
        result.setRecords(items);
        return result;
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
        wrapper.orderByDesc(MomentMatchResult::getTotalScore)
                .orderByDesc(MomentMatchResult::getCreatedAt);

        if (!StringUtils.hasText(keyword)) {
            Page<MomentMatchResult> pageReq = new Page<>(Math.max(page, 1), Math.max(size, 1));
            IPage<MomentMatchResult> resultPage = matchResultMapper.selectPage(pageReq, wrapper);
            List<MomentMatchResult> results = resultPage.getRecords();
            if (results.isEmpty()) {
                Page<MomentMatchResultItem> empty = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
                empty.setRecords(List.of());
                return empty;
            }
            return buildMatchResultItemPage(results, resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        }
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        long offset = (long) (safePage - 1) * safeSize;
        String keywordLike = keywordLike(keyword);
        String weekTagFilter = normalizeFilter(weekTag);
        String poolFilter = normalizeFilter(pool);

        long total = nzLong(matchResultMapper.countAdminResultIdsFiltered(weekTagFilter, poolFilter, keywordLike));
        if (total == 0) {
            return emptyPage(safePage, safeSize);
        }

        List<Long> ids = matchResultMapper.selectAdminResultIdsFiltered(
                weekTagFilter, poolFilter, keywordLike, offset, safeSize
        );
        if (ids.isEmpty()) {
            return emptyPage(safePage, safeSize);
        }
        List<MomentMatchResult> results = matchResultMapper.selectList(
                new LambdaQueryWrapper<MomentMatchResult>()
                        .in(MomentMatchResult::getId, ids)
                        .orderByDesc(MomentMatchResult::getTotalScore)
                        .orderByDesc(MomentMatchResult::getCreatedAt)
        );
        return buildMatchResultItemPage(results, safePage, safeSize, total);
    }

    private List<MomentMatchResultItem> buildMatchResultItems(List<MomentMatchResult> results) {
        Set<Long> userIds = new LinkedHashSet<>();
        List<Long> resultIds = new ArrayList<>();
        for (MomentMatchResult result : results) {
            userIds.add(result.getUserIdA());
            userIds.add(result.getUserIdB());
            resultIds.add(result.getId());
        }
        Map<Long, MomentMatchResultContent> contentByResultId = matchResultContentMapper.selectList(
                        new LambdaQueryWrapper<MomentMatchResultContent>()
                                .in(MomentMatchResultContent::getMatchResultId, resultIds))
                .stream()
                .collect(Collectors.toMap(MomentMatchResultContent::getMatchResultId, c -> c, (a, b) -> a));
        Map<Long, User> userMap = loadUserMap(userIds);
        Map<Long, MomentMatchConfirm> confirmMap = loadConfirmMap(resultIds);

        return results.stream()
                .map(result -> {
                    User userA = userMap.get(result.getUserIdA());
                    User userB = userMap.get(result.getUserIdB());
                    MomentMatchConfirm confirm = confirmMap.get(result.getId());
                    MomentMatchResultContent content = contentByResultId.get(result.getId());
                    return new MomentMatchResultItem(
                            result.getId(),
                            result.getWeekTag(),
                            result.getPool(),
                            result.getUserIdA(),
                            displayName(userA, result.getUserIdA()),
                            result.getUserIdB(),
                            displayName(userB, result.getUserIdB()),
                            result.getTotalScore(),
                            content != null ? content.getYuanfenTitle() : null,
                            confirm != null ? confirm.getChoiceA() : null,
                            confirm != null ? confirm.getChoiceB() : null,
                            resolveConfirmStatus(confirm),
                            result.getCreatedAt()
                    );
                })
                .toList();
    }

    private IPage<MomentMatchResultItem> buildMatchResultItemPage(
            List<MomentMatchResult> results, long current, long pageSize, long total) {
        List<MomentMatchResultItem> items = buildMatchResultItems(results);
        Page<MomentMatchResultItem> out = new Page<>(current, pageSize, total);
        out.setRecords(items);
        return out;
    }

    public MomentMatchResultDetailResponse getResultDetail(Long id) {
        MomentMatchResult result = matchResultMapper.selectById(id);
        if (result == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "匹配结果不存在");
        }
        MomentMatchResultContent content = matchResultContentMapper.selectOne(
                new LambdaQueryWrapper<MomentMatchResultContent>()
                        .eq(MomentMatchResultContent::getMatchResultId, id)
                        .last("limit 1")
        );
        if (content == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "匹配结果内容不存在");
        }

        String rawDatePrepJson = content.getDatePrepJson();
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
        if (StringUtils.hasText(content.getInsightCard1())) {
            insightCards.add(content.getInsightCard1());
        }
        if (StringUtils.hasText(content.getInsightCard2())) {
            insightCards.add(content.getInsightCard2());
        }
        if (StringUtils.hasText(content.getInsightCard3())) {
            insightCards.add(content.getInsightCard3());
        }

        MomentDatePrepResponse datePrepA = momentResultContentService.getOrGenerateDatePrep(
                result, content, result.getUserIdA(), userA, userB, profileA, profileB, portraitA, portraitB
        );
        MomentDatePrepResponse datePrepB = momentResultContentService.getOrGenerateDatePrep(
                result, content, result.getUserIdB(), userB, userA, profileB, profileA, portraitB, portraitA
        );
        if (!Objects.equals(rawDatePrepJson, content.getDatePrepJson()) && content.getId() != null) {
            matchResultContentMapper.updateById(content);
        }

        return new MomentMatchResultDetailResponse(
                result.getId(),
                result.getWeekTag(),
                result.getPool(),
                result.getTotalScore(),
                content.getScoreDetail(),
                content.getYuanfenTitle(),
                content.getComplementaryModes(),
                content.getSoftPenaltyReasons(),
                content.getDateSceneType(),
                insightCards,
                content.getGoldenSentence(),
                content.getDimensionLabels(),
                content.getAboutUserA(),
                content.getAboutUserB(),
                datePrepA,
                datePrepB,
                content.getDatePrepJson(),
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

    protected Map<String, Object> triggerMatchingInternal(String weekTag,
                                                          String currentWeekTag,
                                                          Long operatorId,
                                                          boolean autoTriggered) {
        String resolvedWeekTag = resolveWeekTag(weekTag, currentWeekTag);
        MomentActivityWeek week = activityWeekService.getOrCreateWeek(resolvedWeekTag);
        if (MomentWeekStatusPolicy.blocksTriggerMatching(week.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "本周匹配进行中或已有结果，请先重置后再试");
        }
        if (autoTriggered) {
            activityWeekService.markAutoProcessed(resolvedWeekTag);
        }
        activityWeekService.closeEnrollment(resolvedWeekTag);
        log.info("触发心动时刻异步匹配: weekTag={}, autoTriggered={}", resolvedWeekTag, autoTriggered);

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

        activityWeekService.setMatching(resolvedWeekTag);
        momentMatchPipelineService.submitMatchJob(resolvedWeekTag, operatorId, autoTriggered);

        logAction(
                resolvedWeekTag,
                operatorId,
                autoTriggered ? ACTION_AUTO_TRIGGER_MATCH : ACTION_MANUAL_TRIGGER_MATCH,
                TARGET_WEEK,
                null,
                "匹配任务已启动（异步执行）",
                detailJson(Map.of("weekTag", resolvedWeekTag, "async", true))
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("weekTag", resolvedWeekTag);
        response.put("message", "匹配任务已启动");
        response.put("async", true);
        return response;
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

    private String normalizeFilter(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String keywordLike(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String normalized = keyword.trim().replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + normalized + "%";
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private Integer toNullableInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime time) {
            return time;
        }
        if (value instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime();
        }
        try {
            return LocalDateTime.parse(String.valueOf(value).replace(" ", "T"));
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<String> parsePools(Object value) {
        if (value == null) {
            return List.of();
        }
        return List.of(String.valueOf(value).split(",")).stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted(Comparator.comparingInt(this::poolOrder))
                .toList();
    }

    private int poolOrder(String pool) {
        if (pool == null || pool.isBlank()) {
            return 99;
        }
        return switch (pool.trim()) {
            case "MF" -> 1;
            case "FF" -> 2;
            case "MM" -> 3;
            default -> 99;
        };
    }

    private static long nzLong(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 按周聚合各池人数/对数，避免大数据量下全表载入 enrollment/result。
     */
    private List<OverviewPoolStat> buildPoolStatsAggregated(String weekTag) {
        Map<String, Long> participants = new LinkedHashMap<>();
        QueryWrapper<MomentEnrollment> we = new QueryWrapper<>();
        we.select("pool", "COUNT(DISTINCT user_id) AS cnt");
        we.eq("week_tag", weekTag);
        we.groupBy("pool");
        for (Map<String, Object> row : enrollmentMapper.selectMaps(we)) {
            Object poolObj = row.get("pool");
            Object cntObj = row.get("cnt");
            String poolKey = poolObj != null ? String.valueOf(poolObj) : "UNKNOWN";
            long cnt = cntObj instanceof Number ? ((Number) cntObj).longValue() : 0L;
            participants.merge(poolKey, cnt, Long::sum);
        }

        Map<String, Long> pairCountByPool = new LinkedHashMap<>();
        QueryWrapper<MomentMatchResult> wr = new QueryWrapper<>();
        wr.select("pool", "COUNT(*) AS cnt");
        wr.eq("week_tag", weekTag);
        wr.groupBy("pool");
        for (Map<String, Object> row : matchResultMapper.selectMaps(wr)) {
            Object poolObj = row.get("pool");
            Object cntObj = row.get("cnt");
            String poolKey = poolObj != null ? String.valueOf(poolObj) : "UNKNOWN";
            long cnt = cntObj instanceof Number ? ((Number) cntObj).longValue() : 0L;
            pairCountByPool.merge(poolKey, cnt, Long::sum);
        }

        List<OverviewPoolStat> stats = new ArrayList<>();
        for (String pool : matcher.poolOrder()) {
            long pCount = participants.getOrDefault(pool, 0L);
            long pairs = pairCountByPool.getOrDefault(pool, 0L);
            stats.add(new OverviewPoolStat(pool, pCount, pairs, Math.max(0L, pCount - pairs * 2)));
        }
        for (MomentPool pool : MomentPool.values()) {
            if (stats.stream().noneMatch(item -> item.pool().equals(pool.getCode()))) {
                stats.add(new OverviewPoolStat(pool.getCode(), 0L, 0L, 0L));
            }
        }
        stats.sort(Comparator.comparingInt(item -> poolOrder(item.pool())));
        return stats;
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
        String timeStr = config.getAutoMatchTime();
        if (timeStr == null || timeStr.isBlank()) {
            return null;
        }
        LocalTime triggerTime;
        try {
            triggerTime = LocalTime.parse(timeStr.trim());
        } catch (Exception e) {
            log.warn("自动匹配时间解析失败 autoMatchTime={}，总览不展示下次执行时间", timeStr);
            return null;
        }
        int targetDay = config.getAutoMatchDayOfWeek();
        LocalDate nextDate = now.toLocalDate();
        int diff = targetDay - now.getDayOfWeek().getValue();
        if (diff < 0 || (diff == 0 && !now.toLocalTime().isBefore(triggerTime))) {
            diff += 7;
        }
        return LocalDateTime.of(nextDate.plusDays(diff), triggerTime);
    }

    /** 仍为 RESULT_READY 且未到配置的自动公布时刻时，返回下一次公布时刻（北京时间）；已过期则返回 null（将由调度立即公布） */
    private LocalDateTime resolveNextAutoPublishAt(
            MomentMatchConfig config,
            LocalDateTime shNow,
            String weekTag,
            String weekStatus) {
        if (config == null || !Boolean.TRUE.equals(config.getAutoPublishEnabled())) {
            return null;
        }
        if (!MomentActivityWeek.STATUS_RESULT_READY.equals(weekStatus)) {
            return null;
        }
        try {
            String timeStr = config.getAutoPublishTime();
            if (timeStr == null || timeStr.isBlank()) {
                return null;
            }
            LocalTime tt = LocalTime.parse(timeStr.trim());
            Integer dow = config.getAutoPublishDayOfWeek();
            if (dow == null || dow < 1 || dow > 7) {
                return null;
            }
            LocalDate monday = PairDateTimeUtils.mondayOfIsoWeek(weekTag);
            LocalDate day = monday.plusDays(dow - 1L);
            LocalDateTime slot = LocalDateTime.of(day, tt);
            if (shNow.isBefore(slot)) {
                return slot;
            }
            return null;
        } catch (Exception e) {
            log.warn("解析下次自动公布时间失败 weekTag={}", weekTag, e);
            return null;
        }
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
            boolean canResetWeek,
            boolean canPublishResult,
            Boolean autoPublishEnabled,
            Integer autoPublishDayOfWeek,
            String autoPublishTime,
            LocalDateTime nextAutoPublishAt
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
            Integer gender,
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
