package com.campus.love.moment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.common.service.FileUploadService;
import com.campus.love.follow.service.FollowService;
import com.campus.love.pairdate.service.PairDateService;
import com.campus.love.pairdate.util.PairDateTimeUtils;
import com.campus.love.moment.dto.MomentConfirmRequest;
import com.campus.love.moment.dto.MomentDatePrepResponse;
import com.campus.love.moment.dto.MomentEnrollRequest;
import com.campus.love.moment.dto.MomentResultResponse;
import com.campus.love.moment.dto.MomentStatusResponse;
import com.campus.love.moment.entity.MomentEnrollment;
import com.campus.love.moment.entity.MomentMatchConfirm;
import com.campus.love.moment.entity.MomentActivityWeek;
import com.campus.love.moment.entity.MomentMatchResult;
import com.campus.love.moment.entity.MomentMatchResultContent;
import com.campus.love.moment.entity.MomentMatchResetSnapshot;
import com.campus.love.moment.entity.MomentProfile;
import com.campus.love.moment.enums.MomentPool;
import com.campus.love.moment.mapper.MomentEnrollmentMapper;
import com.campus.love.moment.mapper.MomentMatchConfirmMapper;
import com.campus.love.moment.mapper.MomentMatchResultContentMapper;
import com.campus.love.moment.mapper.MomentMatchResultMapper;
import com.campus.love.moment.mapper.MomentMatchResetSnapshotMapper;
import com.campus.love.moment.mapper.MomentProfileMapper;
import com.campus.love.profile.entity.UserPortrait;
import com.campus.love.profile.service.OceanConfidenceService;
import com.campus.love.profile.service.QuestionnaireOceanMapper;
import com.campus.love.profile.service.UserPortraitService;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MomentService {

    /** 与 PairDateTimeUtils、揭晓时间等业务约定一致，避免服务器默认时区与境内用户日历差一天导致周标签错位 */
    private static final ZoneId MOMENT_BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final MomentProfileMapper profileMapper;
    private final MomentEnrollmentMapper enrollmentMapper;
    private final MomentMatchResultMapper matchResultMapper;
    private final MomentMatchResultContentMapper matchResultContentMapper;
    private final MomentMatchConfirmMapper matchConfirmMapper;
    private final MomentMatchResetSnapshotMapper momentMatchResetSnapshotMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final FileUploadService fileUploadService;
    private final MomentActivityWeekService activityWeekService;
    private final UserPortraitService userPortraitService;
    private final QuestionnaireOceanMapper questionnaireOceanMapper;
    private final OceanConfidenceService oceanConfidenceService;
    private final MomentResultContentService momentResultContentService;
    private final FollowService followService;

    @Lazy
    @Autowired
    private PairDateService pairDateService;

    /**
     * 判断本周报名是否仍开放：
     * 1. 未被管理员手动截止
     * 2. 匹配尚未触发（不存在 MATCHED/UNMATCHED 状态的报名记录）
     */
    public boolean isEnrollmentOpen(String weekTag) {
        try {
            return activityWeekService.isEnrollmentOpen(weekTag);
        } catch (Exception e) {
            log.warn("查询报名开放状态失败，默认开放", e);
            return true;
        }
    }

    /** 与 PairDateTimeUtils.revealFriday 一致：当周周五 12:00（上海），供端上倒计时展示。 */
    private Long computeRevealAtEpochMillis(String weekTag) {
        if (weekTag == null || !weekTag.contains("-W")) {
            return null;
        }
        try {
            LocalDate friday = PairDateTimeUtils.revealFriday(weekTag);
            return friday.atTime(12, 0).atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== 状态查询 ====================

    public MomentStatusResponse getStatus() {
        Long userId = CurrentUser.getId();
        String weekTag = getCurrentWeekTag();

        String status;
        try {
            List<MomentEnrollment> enrollments = enrollmentMapper.selectList(
                    new LambdaQueryWrapper<MomentEnrollment>()
                            .eq(MomentEnrollment::getUserId, userId)
                            .eq(MomentEnrollment::getWeekTag, weekTag)
            );
            if (enrollments == null || enrollments.isEmpty()) {
                status = "NOT_ENROLLED";
            } else {
                boolean anyMatched = enrollments.stream().anyMatch(e -> MomentEnrollment.STATUS_MATCHED.equals(e.getStatus()));
                boolean anyWaiting = enrollments.stream().anyMatch(e -> MomentEnrollment.STATUS_WAITING.equals(e.getStatus()));
                status = anyMatched ? MomentEnrollment.STATUS_MATCHED
                        : anyWaiting ? MomentEnrollment.STATUS_WAITING
                        : MomentEnrollment.STATUS_UNMATCHED;
            }
        } catch (Exception e) {
            log.warn("查询心动时刻报名状态失败（表可能不存在），默认未报名", e);
            status = "NOT_ENROLLED";
        }

        int participantCount = 0;
        try {
            Long count = enrollmentMapper.selectCount(
                    new LambdaQueryWrapper<MomentEnrollment>()
                            .eq(MomentEnrollment::getWeekTag, weekTag)
            );
            participantCount = count != null ? count.intValue() : 0;
        } catch (Exception e) {
            log.warn("查询心动时刻报名人数失败", e);
        }

        boolean open = isEnrollmentOpen(weekTag);
        String weekStatus = MomentActivityWeek.STATUS_ENROLLING;
        try {
            weekStatus = activityWeekService.getOrCreateWeek(weekTag).getStatus();
        } catch (Exception e) {
            log.warn("查询周状态失败", e);
        }

        // 以匹配结果表为准纠偏：流水线/手工改库可能导致报名状态仍为 UNMATCHED 但已落库匹配结果
        MomentMatchResult reconcileMatch = findMatchResult(weekTag, userId);
        if (reconcileMatch != null) {
            if (MomentEnrollment.STATUS_UNMATCHED.equals(status)) {
                log.warn("心动状态纠偏: userId={} weekTag={} 报名为 UNMATCHED 但存在匹配结果，按已匹配展示", userId, weekTag);
                status = MomentEnrollment.STATUS_MATCHED;
            } else if ("NOT_ENROLLED".equals(status)
                    && MomentWeekStatusPolicy.userMayViewPublishedResults(weekStatus)) {
                log.warn("心动状态纠偏: userId={} weekTag={} 无报名记录但本周已发布且存在匹配结果，按已匹配展示", userId, weekTag);
                status = MomentEnrollment.STATUS_MATCHED;
            }
        }

        String matchedTitle = null;
        if (MomentEnrollment.STATUS_MATCHED.equals(status)) {
            MomentMatchResult matchResult = reconcileMatch != null ? reconcileMatch : findMatchResult(weekTag, userId);
            if (matchResult != null) {
                ensureResultContent(matchResult);
                MomentMatchResultContent c = loadContentByMatchResultId(matchResult.getId());
                matchedTitle = c != null ? c.getYuanfenTitle() : null;
            }
        }

        return MomentStatusResponse.builder()
                .currentWeek(weekTag)
                .status(status)
                .participantCount(participantCount)
                .enrollmentOpen(open)
                .matchedTitle(matchedTitle)
                .weekStatus(weekStatus)
                .revealAtEpochMillis(computeRevealAtEpochMillis(weekTag))
                .build();
    }

    // ==================== 报名 ====================

    @Transactional
    public MomentStatusResponse enroll(MomentEnrollRequest request) {
        Long userId = CurrentUser.getId();
        String weekTag = getCurrentWeekTag();

        // 检查报名是否仍开放
        if (!isEnrollmentOpen(weekTag)) {
            throw new BusinessException(ResultCode.MOMENT_ENROLLMENT_CLOSED);
        }

        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);
        if (Boolean.TRUE.equals(user.getMomentBanned())) throw new BusinessException(ResultCode.MOMENT_BANNED);

        // 检查本周是否已报名（支持多池：删除旧记录后重新创建）
        List<MomentEnrollment> existingList = enrollmentMapper.selectList(
                new LambdaQueryWrapper<MomentEnrollment>()
                        .eq(MomentEnrollment::getUserId, userId)
                        .eq(MomentEnrollment::getWeekTag, weekTag)
        );
        if (!existingList.isEmpty()) {
            enrollmentMapper.delete(new LambdaQueryWrapper<MomentEnrollment>()
                    .eq(MomentEnrollment::getUserId, userId)
                    .eq(MomentEnrollment::getWeekTag, weekTag));
        }

        // 保存/更新自评分
        user.setMomentSelfScore(request.getSelfScore());
        userMapper.updateById(user);

        // 保存/更新问卷档案（upsert）
        MomentProfile profile = profileMapper.selectOne(
                new LambdaQueryWrapper<MomentProfile>()
                        .eq(MomentProfile::getUserId, userId)
        );
        if (profile == null) {
            profile = new MomentProfile();
            profile.setUserId(userId);
        }
        copyProfileFromRequest(profile, request);
        if (profile.getId() == null) {
            profileMapper.insert(profile);
        } else {
            profileMapper.updateById(profile);
        }

        UserPortrait portrait = userPortraitService.getPortrait(userId);
        if (portrait == null) portrait = new UserPortrait();
        portrait.setUserId(userId);
        copyMomentProfileToPortrait(profile, portrait);
        portrait.setQuestionnaireSnapshot(buildQuestionnaireSnapshot(request));
        questionnaireOceanMapper.blendIntoPortrait(portrait);
        portrait.setOceanConfidence(oceanConfidenceService.toJson(
                oceanConfidenceService.computeOceanConfidence(
                        portrait.getMbti(),
                        com.campus.love.common.utils.InterestTagConverter.extractCodesFromNewFormat(portrait.getInterestTags()),
                        true
                )
        ));
        userPortraitService.savePortrait(portrait);

        // 确定匹配池（支持双池：any 时男→MF+MM，女→MF+FF）
        List<MomentPool> pools = MomentPool.determine(user.getGender(), request.getTargetGender());

        // 创建多条报名记录
        for (MomentPool pool : pools) {
            MomentEnrollment enrollment = new MomentEnrollment();
            enrollment.setUserId(userId);
            enrollment.setWeekTag(weekTag);
            enrollment.setPool(pool.getCode());
            enrollment.setStatus(MomentEnrollment.STATUS_WAITING);
            enrollmentMapper.insert(enrollment);
        }

        log.info("用户{}报名心动时刻: weekTag={}, pools={}", userId, weekTag, pools.stream().map(MomentPool::getCode).toList());

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

        return MomentStatusResponse.builder()
                .currentWeek(weekTag)
                .status("WAITING")
                .participantCount(participantCount)
                .enrollmentOpen(true)
                .revealAtEpochMillis(computeRevealAtEpochMillis(weekTag))
                .build();
    }

    // ==================== 获取匹配结果 ====================

    public MomentResultResponse getResult(String weekTag) {
        Long userId = CurrentUser.getId();
        String targetWeekTag = (weekTag == null || weekTag.isBlank()) ? getCurrentWeekTag() : weekTag.trim();

        List<MomentEnrollment> enrollments = enrollmentMapper.selectList(
                new LambdaQueryWrapper<MomentEnrollment>()
                        .eq(MomentEnrollment::getUserId, userId)
                        .eq(MomentEnrollment::getWeekTag, targetWeekTag)
        );
        if (enrollments == null || enrollments.isEmpty()) {
            MomentMatchResetSnapshot snap = loadLatestSnapshotWithContent(userId, targetWeekTag);
            if (snap != null) {
                return buildResultFromResetSnapshot(snap, userId, targetWeekTag);
            }
            MomentMatchResult orphan = findMatchResult(targetWeekTag, userId);
            if (orphan != null) {
                MomentActivityWeek w = activityWeekService.getOrCreateWeek(targetWeekTag);
                if (MomentWeekStatusPolicy.userMayViewPublishedResults(w.getStatus())) {
                    return buildMatchedMomentResponse(orphan, userId, targetWeekTag, false);
                }
                throw new BusinessException(ResultCode.MOMENT_NO_RESULT);
            }
            throw new BusinessException(ResultCode.MOMENT_NOT_ENROLLED);
        }

        boolean anyWaiting = enrollments.stream().anyMatch(e -> MomentEnrollment.STATUS_WAITING.equals(e.getStatus()));
        if (anyWaiting) {
            throw new BusinessException(ResultCode.MOMENT_NO_RESULT);
        }

        boolean allUnmatched = enrollments.stream().allMatch(e -> MomentEnrollment.STATUS_UNMATCHED.equals(e.getStatus()));
        if (allUnmatched) {
            return MomentResultResponse.builder()
                    .matched(false)
                    .weekTag(targetWeekTag)
                    .build();
        }

        MomentActivityWeek week = activityWeekService.getOrCreateWeek(targetWeekTag);
        if (!MomentWeekStatusPolicy.userMayViewPublishedResults(week.getStatus())) {
            throw new BusinessException(ResultCode.MOMENT_NO_RESULT);
        }

        MomentMatchResult matchResult = findMatchResult(targetWeekTag, userId);
        if (matchResult == null) {
            return MomentResultResponse.builder()
                    .matched(false)
                    .weekTag(targetWeekTag)
                    .build();
        }

        return buildMatchedMomentResponse(matchResult, userId, targetWeekTag, true);
    }

    @Transactional
    public MomentResultResponse confirmChoice(MomentConfirmRequest request) {
        Long currentUserId = CurrentUser.getId();
        String weekTag = getCurrentWeekTag();
        MomentActivityWeek week = activityWeekService.getOrCreateWeek(weekTag);
        if (!MomentWeekStatusPolicy.userMayViewPublishedResults(week.getStatus())) {
            throw new BusinessException(ResultCode.MOMENT_NO_RESULT);
        }
        MomentMatchResult matchResult = findMatchResult(weekTag, currentUserId);
        if (matchResult == null) {
            throw new BusinessException(ResultCode.MOMENT_NO_RESULT);
        }

        ensureResultContent(matchResult);
        MomentMatchConfirm confirm = getOrCreateConfirm(matchResult);
        applyTimeoutIfNeeded(confirm);
        if (bothYue(confirm) || hasAnyGuanzhu(confirm)) {
            if (bothYue(confirm)) {
                pairDateService.ensureNegotiationReadyForMatch(matchResult.getId());
            }
            return getResult(weekTag);
        }

        boolean isA = Objects.equals(matchResult.getUserIdA(), currentUserId);
        String existingChoice = isA ? confirm.getChoiceA() : confirm.getChoiceB();
        if (existingChoice != null && !existingChoice.equals(request.getChoice())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "你已经做出选择，暂不支持修改");
        }

        if (existingChoice == null) {
            LocalDateTime now = LocalDateTime.now();
            if (isA) {
                confirm.setChoiceA(request.getChoice());
                confirm.setChoiceAAt(now);
            } else {
                confirm.setChoiceB(request.getChoice());
                confirm.setChoiceBAt(now);
            }
            matchConfirmMapper.updateById(confirm);
        }

        if (hasAnyGuanzhu(confirm)) {
            followService.mutualFollow(confirm.getUserIdA(), confirm.getUserIdB());
        }
        if (bothYue(confirm)) {
            pairDateService.ensureNegotiationReadyForMatch(matchResult.getId());
        }
        return getResult(weekTag);
    }

    @Transactional
    public MomentDatePrepResponse getDatePrep() {
        Long currentUserId = CurrentUser.getId();
        String weekTag = getCurrentWeekTag();
        MomentActivityWeek week = activityWeekService.getOrCreateWeek(weekTag);
        if (!MomentWeekStatusPolicy.userMayViewPublishedResults(week.getStatus())) {
            throw new BusinessException(ResultCode.MOMENT_NO_RESULT);
        }
        MomentMatchResult matchResult = findMatchResult(weekTag, currentUserId);
        if (matchResult == null) {
            throw new BusinessException(ResultCode.MOMENT_NO_RESULT);
        }
        ensureResultContent(matchResult);
        MomentMatchResultContent content = requireContent(matchResult.getId());
        ConfirmView confirmView = resolveConfirmView(matchResult, currentUserId);
        if (!Boolean.TRUE.equals(confirmView.datePrepUnlocked())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "双方确认约会后才会解锁约会准备");
        }

        pairDateService.ensureNegotiationReadyForMatch(matchResult.getId());

        Long targetUserId = Objects.equals(matchResult.getUserIdA(), currentUserId)
                ? matchResult.getUserIdB()
                : matchResult.getUserIdA();
        User requester = userMapper.selectById(currentUserId);
        User target = userMapper.selectById(targetUserId);
        MomentProfile requesterProfile = loadProfile(currentUserId);
        MomentProfile targetProfile = loadProfile(targetUserId);
        UserPortrait requesterPortrait = userPortraitService.getPortrait(currentUserId);
        UserPortrait targetPortrait = userPortraitService.getPortrait(targetUserId);
        MomentDatePrepResponse response = momentResultContentService.getOrGenerateDatePrep(
                matchResult, content, currentUserId, requester, target, requesterProfile, targetProfile, requesterPortrait, targetPortrait
        );
        matchResultContentMapper.updateById(content);
        return response;
    }

    // ==================== 获取已有问卷 ====================

    public MomentProfile getMyProfile() {
        Long userId = CurrentUser.getId();
        MomentProfile profile = profileMapper.selectOne(
                new LambdaQueryWrapper<MomentProfile>()
                        .eq(MomentProfile::getUserId, userId)
        );
        if (profile != null) {
            // 附加用户的心动照片和自评分
            User user = userMapper.selectById(userId);
            if (user != null) {
                profile.setMomentPhotoUrl(user.getMomentPhotoUrl());
                profile.setMomentSelfScore(user.getMomentSelfScore());
            }
        }
        return profile;
    }

    // ==================== 上传照片 ====================

    public String uploadPhoto(MultipartFile file) throws IOException {
        Long userId = CurrentUser.getId();
        String photoUrl = fileUploadService.uploadImage(file, userId, "moment_" + userId + "_");

        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getMomentPhotoUrl, photoUrl)
        );

        log.info("用户{}上传心动时刻照片: {}", userId, photoUrl);
        return photoUrl;
    }

    // ==================== 工具方法 ====================

    /**
     * 管理员重置后报名表可能已清空，但历史列表仍展示该周；此时从快照 JSON 还原详情，且不调用 getOrCreateConfirm。
     */
    private MomentMatchResetSnapshot loadLatestSnapshotWithContent(Long userId, String weekTag) {
        List<MomentMatchResetSnapshot> list = momentMatchResetSnapshotMapper.selectList(
                new LambdaQueryWrapper<MomentMatchResetSnapshot>()
                        .eq(MomentMatchResetSnapshot::getWeekTag, weekTag)
                        .and(w -> w.eq(MomentMatchResetSnapshot::getUserIdA, userId)
                                .or()
                                .eq(MomentMatchResetSnapshot::getUserIdB, userId))
                        .isNotNull(MomentMatchResetSnapshot::getContentSnapshotJson)
                        .orderByDesc(MomentMatchResetSnapshot::getArchivedAt)
        );
        for (MomentMatchResetSnapshot s : list) {
            if (s.getContentSnapshotJson() != null && !s.getContentSnapshotJson().isBlank()) {
                return s;
            }
        }
        return null;
    }

    private MomentResultResponse buildResultFromResetSnapshot(MomentMatchResetSnapshot snap, Long userId, String responseWeekTag) {
        MomentMatchResultContent content;
        try {
            content = objectMapper.readValue(snap.getContentSnapshotJson(), MomentMatchResultContent.class);
        } catch (Exception e) {
            log.warn("重置快照 content 反序列化失败 weekTag={} snapshotId={}", snap.getWeekTag(), snap.getId(), e);
            throw new BusinessException(ResultCode.MOMENT_NO_RESULT, "历史匹配详情暂不可用");
        }

        MomentMatchResult ref = new MomentMatchResult();
        ref.setId(snap.getOriginalMatchResultId());
        ref.setWeekTag(snap.getWeekTag());
        ref.setUserIdA(snap.getUserIdA());
        ref.setUserIdB(snap.getUserIdB());
        ref.setTotalScore(snap.getTotalScore());
        ref.setCreatedAt(snap.getResultCreatedAt());

        Long matchedUserId = Objects.equals(snap.getUserIdA(), userId) ? snap.getUserIdB() : snap.getUserIdA();
        User matchedUser = userMapper.selectById(matchedUserId);
        if (matchedUser == null) {
            return MomentResultResponse.builder()
                    .matched(false)
                    .weekTag(responseWeekTag)
                    .build();
        }

        Integer age = null;
        if (matchedUser.getBirthDate() != null) {
            age = Period.between(matchedUser.getBirthDate(), LocalDate.now()).getYears();
        }

        ConfirmView confirmView = resolveConfirmViewReadOnly(snap.getOriginalMatchResultId(), ref, userId);
        Integer matchScorePercent = resolveMatchScorePercent(ref, content);

        return MomentResultResponse.builder()
                .matched(true)
                .weekTag(responseWeekTag)
                .matchResultId(snap.getOriginalMatchResultId())
                .yuanfenTitle(content.getYuanfenTitle())
                .matchedUserId(matchedUserId)
                .nickname(matchedUser.getNickname())
                .avatarUrl(matchedUser.getAvatarUrl())
                .gender(matchedUser.getGender())
                .school(matchedUser.getSchool())
                .major(matchedUser.getMajor())
                .grade(matchedUser.getGrade())
                .bio(matchedUser.getBio())
                .mbti(matchedUser.getMbti())
                .zodiac(matchedUser.getZodiac())
                .age(age)
                .complementaryModes(momentResultContentService.parseJsonList(content.getComplementaryModes()))
                .insightCards(momentResultContentService.buildInsightCards(content))
                .goldenSentence(content.getGoldenSentence())
                .dimensionLabels(momentResultContentService.parseJsonList(content.getDimensionLabels()))
                .aboutMatchedUser(Objects.equals(snap.getUserIdA(), userId) ? content.getAboutUserB() : content.getAboutUserA())
                .confirmStatus(confirmView.status())
                .myChoice(confirmView.myChoice())
                .datePrepUnlocked(confirmView.datePrepUnlocked())
                .matchScorePercent(matchScorePercent)
                .build();
    }

    private MomentResultResponse buildMatchedMomentResponse(MomentMatchResult matchResult, Long userId, String responseWeekTag,
                                                            boolean writableConfirmFlow) {
        ensureResultContent(matchResult);
        MomentMatchResultContent content = requireContent(matchResult.getId());

        Long matchedUserId = Objects.equals(matchResult.getUserIdA(), userId)
                ? matchResult.getUserIdB()
                : matchResult.getUserIdA();

        User matchedUser = userMapper.selectById(matchedUserId);
        if (matchedUser == null) {
            return MomentResultResponse.builder()
                    .matched(false)
                    .weekTag(responseWeekTag)
                    .build();
        }

        Integer age = null;
        if (matchedUser.getBirthDate() != null) {
            age = Period.between(matchedUser.getBirthDate(), LocalDate.now()).getYears();
        }

        ConfirmView confirmView = writableConfirmFlow
                ? resolveConfirmView(matchResult, userId)
                : resolveConfirmViewReadOnly(matchResult.getId(), matchResult, userId);

        Integer matchScorePercent = resolveMatchScorePercent(matchResult, content);

        return MomentResultResponse.builder()
                .matched(true)
                .weekTag(responseWeekTag)
                .matchResultId(matchResult.getId())
                .yuanfenTitle(content.getYuanfenTitle())
                .matchedUserId(matchedUserId)
                .nickname(matchedUser.getNickname())
                .avatarUrl(matchedUser.getAvatarUrl())
                .gender(matchedUser.getGender())
                .school(matchedUser.getSchool())
                .major(matchedUser.getMajor())
                .grade(matchedUser.getGrade())
                .bio(matchedUser.getBio())
                .mbti(matchedUser.getMbti())
                .zodiac(matchedUser.getZodiac())
                .age(age)
                .complementaryModes(momentResultContentService.parseJsonList(content.getComplementaryModes()))
                .insightCards(momentResultContentService.buildInsightCards(content))
                .goldenSentence(content.getGoldenSentence())
                .dimensionLabels(momentResultContentService.parseJsonList(content.getDimensionLabels()))
                .aboutMatchedUser(Objects.equals(matchResult.getUserIdA(), userId) ? content.getAboutUserB() : content.getAboutUserA())
                .confirmStatus(confirmView.status())
                .myChoice(confirmView.myChoice())
                .datePrepUnlocked(confirmView.datePrepUnlocked())
                .matchScorePercent(matchScorePercent)
                .build();
    }

    private MomentMatchResult findMatchResult(String weekTag, Long userId) {
        return matchResultMapper.selectOne(
                new LambdaQueryWrapper<MomentMatchResult>()
                        .eq(MomentMatchResult::getWeekTag, weekTag)
                        .and(w -> w
                                .eq(MomentMatchResult::getUserIdA, userId)
                                .or()
                                .eq(MomentMatchResult::getUserIdB, userId)
                        )
        );
    }

    private MomentProfile loadProfile(Long userId) {
        return profileMapper.selectOne(
                new LambdaQueryWrapper<MomentProfile>()
                        .eq(MomentProfile::getUserId, userId)
        );
    }

    private MomentMatchConfirm getOrCreateConfirm(MomentMatchResult matchResult) {
        MomentMatchConfirm confirm = matchConfirmMapper.selectOne(
                new LambdaQueryWrapper<MomentMatchConfirm>()
                        .eq(MomentMatchConfirm::getMatchResultId, matchResult.getId())
        );
        if (confirm != null) {
            return confirm;
        }
        confirm = new MomentMatchConfirm();
        confirm.setMatchResultId(matchResult.getId());
        confirm.setUserIdA(matchResult.getUserIdA());
        confirm.setUserIdB(matchResult.getUserIdB());
        matchConfirmMapper.insert(confirm);
        return confirm;
    }

    private ConfirmView resolveConfirmView(MomentMatchResult matchResult, Long currentUserId) {
        MomentMatchConfirm confirm = getOrCreateConfirm(matchResult);
        boolean timedOut = applyTimeoutIfNeeded(confirm);
        String myChoice = Objects.equals(matchResult.getUserIdA(), currentUserId)
                ? confirm.getChoiceA()
                : confirm.getChoiceB();
        if (bothYue(confirm)) {
            return new ConfirmView("BOTH_YUE", myChoice, true);
        }
        if (hasAnyGuanzhu(confirm)) {
            return new ConfirmView(timedOut ? "TIMEOUT_GUANZHU" : "ANY_GUANZHU", myChoice, false);
        }
        return new ConfirmView("PENDING", myChoice, false);
    }

    /** 仅查询确认状态，不 insert 确认行、不写超时互关（用于重置后仅快照可读的历史详情）。 */
    private ConfirmView resolveConfirmViewReadOnly(Long matchResultId, MomentMatchResult matchRef, Long currentUserId) {
        if (matchResultId == null || matchRef == null) {
            return new ConfirmView("PENDING", null, false);
        }
        MomentMatchConfirm confirm = matchConfirmMapper.selectOne(
                new LambdaQueryWrapper<MomentMatchConfirm>()
                        .eq(MomentMatchConfirm::getMatchResultId, matchResultId)
        );
        if (confirm == null) {
            return new ConfirmView("PENDING", null, false);
        }
        String myChoice = Objects.equals(matchRef.getUserIdA(), currentUserId)
                ? confirm.getChoiceA()
                : confirm.getChoiceB();
        if (bothYue(confirm)) {
            return new ConfirmView("BOTH_YUE", myChoice, true);
        }
        if (hasAnyGuanzhu(confirm)) {
            return new ConfirmView("ANY_GUANZHU", myChoice, false);
        }
        return new ConfirmView("PENDING", myChoice, false);
    }

    private boolean applyTimeoutIfNeeded(MomentMatchConfirm confirm) {
        if (confirm == null || confirm.getCreatedAt() == null) {
            return false;
        }
        if (bothYue(confirm) || hasAnyGuanzhu(confirm)) {
            return false;
        }
        if (confirm.getCreatedAt().plusHours(48).isAfter(LocalDateTime.now())) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (confirm.getChoiceA() == null) {
            confirm.setChoiceA(MomentMatchConfirm.CHOICE_GUANZHU);
            confirm.setChoiceAAt(now);
        }
        if (confirm.getChoiceB() == null) {
            confirm.setChoiceB(MomentMatchConfirm.CHOICE_GUANZHU);
            confirm.setChoiceBAt(now);
        }
        matchConfirmMapper.updateById(confirm);
        followService.mutualFollow(confirm.getUserIdA(), confirm.getUserIdB());
        return true;
    }

    private boolean bothYue(MomentMatchConfirm confirm) {
        return Objects.equals(confirm.getChoiceA(), MomentMatchConfirm.CHOICE_YUE)
                && Objects.equals(confirm.getChoiceB(), MomentMatchConfirm.CHOICE_YUE);
    }

    private boolean hasAnyGuanzhu(MomentMatchConfirm confirm) {
        return Objects.equals(confirm.getChoiceA(), MomentMatchConfirm.CHOICE_GUANZHU)
                || Objects.equals(confirm.getChoiceB(), MomentMatchConfirm.CHOICE_GUANZHU);
    }

    private MomentMatchResultContent loadContentByMatchResultId(Long matchResultId) {
        if (matchResultId == null) {
            return null;
        }
        return matchResultContentMapper.selectOne(
                new LambdaQueryWrapper<MomentMatchResultContent>()
                        .eq(MomentMatchResultContent::getMatchResultId, matchResultId)
                        .last("limit 1")
        );
    }

    private MomentMatchResultContent requireContent(Long matchResultId) {
        MomentMatchResultContent c = loadContentByMatchResultId(matchResultId);
        if (c == null) {
            throw new BusinessException(ResultCode.MOMENT_NO_RESULT);
        }
        return c;
    }

    private void ensureResultContent(MomentMatchResult matchResult) {
        if (matchResult == null || matchResult.getId() == null) {
            return;
        }
        MomentMatchResultContent content = loadContentByMatchResultId(matchResult.getId());
        if (content == null) {
            content = new MomentMatchResultContent();
            content.setMatchResultId(matchResult.getId());
        }
        boolean ready = content.getYuanfenTitle() != null
                && content.getInsightCard1() != null
                && content.getInsightCard2() != null
                && content.getInsightCard3() != null
                && content.getDimensionLabels() != null
                && content.getAboutUserA() != null
                && content.getAboutUserB() != null;
        if (ready) {
            return;
        }

        User userA = userMapper.selectById(matchResult.getUserIdA());
        User userB = userMapper.selectById(matchResult.getUserIdB());
        MomentProfile profileA = loadProfile(matchResult.getUserIdA());
        MomentProfile profileB = loadProfile(matchResult.getUserIdB());
        UserPortrait portraitA = userPortraitService.getPortrait(matchResult.getUserIdA());
        UserPortrait portraitB = userPortraitService.getPortrait(matchResult.getUserIdB());
        Map<String, Object> scoreDetail = parseScoreDetail(content.getScoreDetail());
        momentResultContentService.fillRuleBasedContent(
                content, userA, userB, profileA, profileB, portraitA, portraitB, scoreDetail
        );
        if (content.getId() == null) {
            matchResultContentMapper.insert(content);
        } else {
            matchResultContentMapper.updateById(content);
        }
    }

    /**
     * 综合匹配度：优先读 score_detail.finalScore；缺失时回退 t_moment_match_result.total_score
     * （避免 JSON 未写入、旧数据或解析失败导致前端无数字）
     */
    private Integer resolveMatchScorePercent(MomentMatchResult matchResult, MomentMatchResultContent content) {
        Map<String, Object> scoreDetail = parseScoreDetail(content != null ? content.getScoreDetail() : null);
        Object fs = scoreDetail.get("finalScore");
        if (fs instanceof Number) {
            int p = (int) Math.round(((Number) fs).doubleValue());
            return Math.max(0, Math.min(100, p));
        }
        if (fs instanceof String s && !s.isBlank()) {
            try {
                int p = (int) Math.round(Double.parseDouble(s.trim()));
                return Math.max(0, Math.min(100, p));
            } catch (NumberFormatException ignored) {
                // fall through to totalScore
            }
        }
        if (matchResult.getTotalScore() != null) {
            int p = (int) Math.round(matchResult.getTotalScore().doubleValue());
            return Math.max(0, Math.min(100, p));
        }
        return null;
    }

    private Map<String, Object> parseScoreDetail(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.warn("解析分数明细失败", e);
            return new LinkedHashMap<>();
        }
    }

    public String getCurrentWeekTag() {
        LocalDate now = LocalDate.now(MOMENT_BUSINESS_ZONE);
        WeekFields wf = WeekFields.of(DayOfWeek.SUNDAY, 1);
        int year = now.get(wf.weekBasedYear());
        int week = now.get(wf.weekOfWeekBasedYear());
        return String.format("%d-W%02d", year, week);
    }

    private void copyProfileFromRequest(MomentProfile profile, MomentEnrollRequest req) {
        profile.setTargetGender(req.getTargetGender());
        profile.setAppearanceScore(BigDecimal.valueOf(req.getSelfScore()));
        profile.setSocialStyle(req.getSocialStyle());
        profile.setLifeRhythm(req.getLifeRhythm());
        profile.setPersonalityBase(req.getPersonalityBase());
        profile.setCampusFocus(req.getCampusFocus());
        profile.setEmotionStyle(req.getEmotionStyle());
        profile.setAppearanceRequirement(req.getAppearanceRequirement());
        profile.setAgeRangePreference(req.getAgeRangePreference());
        profile.setAgePreferenceMin(req.getAgePreferenceMin());
        profile.setAgePreferenceMax(req.getAgePreferenceMax());
        profile.setGradeRangePreference(req.getGradeRangePreference());
        profile.setGradeRangeMin(req.getGradeRangeMin());
        profile.setGradeRangeMax(req.getGradeRangeMax());
        profile.setPrioritizeMatching(Boolean.TRUE.equals(req.getPrioritizeMatching()));
        profile.setPartnerPersonality(req.getPartnerPersonality());
        profile.setMajorPreference(req.getMajorPreference());
        profile.setCareerAmbitionPref(req.getCareerAmbitionPref());
        profile.setCompanionshipStyle(req.getCompanionshipStyle());
        profile.setDateStyle(req.getDateStyle());
        profile.setIntimacyPace(req.getIntimacyPace());
        profile.setHonestyLevel(req.getHonestyLevel());
        profile.setPremaritalCohabitation(req.getPremaritalCohabitation());
        profile.setPremaritalSex(req.getPremaritalSex());
        profile.setRelationshipCoreValue(req.getRelationshipCoreValue());
        profile.setConflictStyle(req.getConflictStyle());
        profile.setSocialBoundary(req.getSocialBoundary());
        profile.setFutureLifestyle(req.getFutureLifestyle());
        profile.setCampusLovePlan(req.getCampusLovePlan());
        profile.setIdolRole(req.getIdolRole());
        profile.setTemptationResponse(req.getTemptationResponse());
        profile.setRealityCondition(req.getRealityCondition());
        profile.setHumanNatureView(req.getHumanNatureView());
        profile.setBreakupView(req.getBreakupView());
        profile.setCareerLoveConflict(req.getCareerLoveConflict());
        profile.setEmotionPriority(req.getEmotionPriority());
        profile.setLifeGoalPriority(req.getLifeGoalPriority());
    }

    private void copyMomentProfileToPortrait(MomentProfile mp, UserPortrait p) {
        p.setTargetGender(mp.getTargetGender());
        p.setSocialStyle(mp.getSocialStyle());
        p.setLifeRhythm(mp.getLifeRhythm());
        p.setCompanionshipStyle(mp.getCompanionshipStyle());
        p.setAppearanceRequirement(mp.getAppearanceRequirement());
        p.setPartnerPersonality(mp.getPartnerPersonality());
        p.setMajorPreference(mp.getMajorPreference());
        p.setAgeRangePreference(mp.getAgeRangePreference());
        p.setAgePreferenceMin(mp.getAgePreferenceMin());
        p.setAgePreferenceMax(mp.getAgePreferenceMax());
        p.setGradeRangeMin(mp.getGradeRangeMin());
        p.setGradeRangeMax(mp.getGradeRangeMax());
        p.setGradeRangePreference(mp.getGradeRangePreference());
        p.setPrioritizeMatching(mp.getPrioritizeMatching());
        p.setDateStyle(mp.getDateStyle());
        p.setIntimacyPace(mp.getIntimacyPace());
        p.setPremaritalCohabitation(mp.getPremaritalCohabitation());
        p.setFutureLifestyle(mp.getFutureLifestyle());
        p.setRelationshipCoreValue(mp.getRelationshipCoreValue());
        p.setAppearanceScore(mp.getAppearanceScore());
        p.setPersonalityBase(mp.getPersonalityBase());
        p.setCampusFocus(mp.getCampusFocus());
        p.setEmotionStyle(mp.getEmotionStyle());
        p.setCareerAmbitionPref(mp.getCareerAmbitionPref());
        p.setHonestyLevel(mp.getHonestyLevel());
        p.setPremaritalSex(mp.getPremaritalSex());
        p.setConflictStyle(mp.getConflictStyle());
        p.setSocialBoundary(mp.getSocialBoundary());
        p.setCampusLovePlan(mp.getCampusLovePlan());
        p.setIdolRole(mp.getIdolRole());
        p.setTemptationResponse(mp.getTemptationResponse());
        p.setRealityCondition(mp.getRealityCondition());
        p.setHumanNatureView(mp.getHumanNatureView());
        p.setBreakupView(mp.getBreakupView());
        p.setCareerLoveConflict(mp.getCareerLoveConflict());
        p.setEmotionPriority(mp.getEmotionPriority());
        p.setLifeGoalPriority(mp.getLifeGoalPriority());
    }

    private String buildQuestionnaireSnapshot(MomentEnrollRequest req) {
        try {
            return objectMapper.writeValueAsString(req);
        } catch (Exception e) {
            log.warn("Serialize questionnaire snapshot failed: {}", e.getMessage());
            return null;
        }
    }

    private record ConfirmView(String status, String myChoice, Boolean datePrepUnlocked) {
    }

    public IPage<MomentResultResponse> getMatchHistory(Integer page, Integer size) {
        Long userId = CurrentUser.getId();

        // 查询所有参与过的周期（从报名表）
        List<MomentEnrollment> enrollments = enrollmentMapper.selectList(
                new LambdaQueryWrapper<MomentEnrollment>()
                        .eq(MomentEnrollment::getUserId, userId)
                        .orderByDesc(MomentEnrollment::getCreatedAt)
        );

        // 查询被重置的历史记录（从快照表）
        List<MomentMatchResetSnapshot> snapshots = momentMatchResetSnapshotMapper.selectList(
                new LambdaQueryWrapper<MomentMatchResetSnapshot>()
                        .and(w -> w.eq(MomentMatchResetSnapshot::getUserIdA, userId)
                                .or().eq(MomentMatchResetSnapshot::getUserIdB, userId))
                        .orderByDesc(MomentMatchResetSnapshot::getArchivedAt)
        );

        // 构建历史记录
        List<MomentResultResponse> records = new ArrayList<>();
        Set<String> processedWeeks = new HashSet<>();

        for (MomentEnrollment enrollment : enrollments) {
            String weekTag = enrollment.getWeekTag();
            if (processedWeeks.contains(weekTag)) continue;
            processedWeeks.add(weekTag);

            // 查询该周期是否有匹配结果
            MomentMatchResult matchResult = matchResultMapper.selectOne(
                    new LambdaQueryWrapper<MomentMatchResult>()
                            .eq(MomentMatchResult::getWeekTag, weekTag)
                            .and(w -> w.eq(MomentMatchResult::getUserIdA, userId)
                                    .or().eq(MomentMatchResult::getUserIdB, userId))
            );

            if (matchResult != null) {
                records.add(buildHistoryResponse(matchResult));
            } else {
                records.add(MomentResultResponse.builder()
                        .matched(false)
                        .weekTag(weekTag)
                        .build());
            }
        }

        // 添加快照记录
        for (MomentMatchResetSnapshot snapshot : snapshots) {
            if (processedWeeks.contains(snapshot.getWeekTag())) continue;
            processedWeeks.add(snapshot.getWeekTag());
            records.add(buildHistoryResponseFromSnapshot(snapshot));
        }

        // 手动分页
        int start = (page - 1) * size;
        int end = Math.min(start + size, records.size());
        List<MomentResultResponse> pageRecords = start < records.size() ? records.subList(start, end) : new ArrayList<>();

        Page<MomentResultResponse> responsePage = new Page<>(page, size);
        responsePage.setTotal(records.size());
        responsePage.setRecords(pageRecords);
        return responsePage;
    }

    private MomentResultResponse buildHistoryResponse(MomentMatchResult result) {
        Long userId = CurrentUser.getId();
        boolean isA = result.getUserIdA().equals(userId);
        Long matchedUserId = isA ? result.getUserIdB() : result.getUserIdA();
        User matchedUser = userMapper.selectById(matchedUserId);

        return MomentResultResponse.builder()
                .matched(true)
                .weekTag(result.getWeekTag())
                .matchedUserId(matchedUserId)
                .nickname(matchedUser != null ? matchedUser.getNickname() : "未知用户")
                .avatarUrl(matchedUser != null ? matchedUser.getAvatarUrl() : null)
                .gender(matchedUser != null ? matchedUser.getGender() : null)
                .build();
    }

    private MomentResultResponse buildHistoryResponseFromSnapshot(MomentMatchResetSnapshot snapshot) {
        Long userId = CurrentUser.getId();
        boolean isA = snapshot.getUserIdA().equals(userId);
        Long matchedUserId = isA ? snapshot.getUserIdB() : snapshot.getUserIdA();
        User matchedUser = userMapper.selectById(matchedUserId);

        return MomentResultResponse.builder()
                .matched(true)
                .weekTag(snapshot.getWeekTag())
                .matchedUserId(matchedUserId)
                .nickname(matchedUser != null ? matchedUser.getNickname() : "未知用户")
                .avatarUrl(matchedUser != null ? matchedUser.getAvatarUrl() : null)
                .gender(matchedUser != null ? matchedUser.getGender() : null)
                .build();
    }
}
