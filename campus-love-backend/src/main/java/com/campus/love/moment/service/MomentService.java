package com.campus.love.moment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.common.service.FileUploadService;
import com.campus.love.ai.agent.MomentSummaryReActAgent;
import com.campus.love.moment.dto.MomentEnrollRequest;
import com.campus.love.moment.dto.MomentResultResponse;
import com.campus.love.moment.dto.MomentStatusResponse;
import com.campus.love.moment.entity.MomentEnrollment;
import com.campus.love.moment.entity.MomentMatchResult;
import com.campus.love.moment.entity.MomentProfile;
import com.campus.love.moment.enums.MomentPool;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.WeekFields;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MomentService {

    private final MomentProfileMapper profileMapper;
    private final MomentEnrollmentMapper enrollmentMapper;
    private final MomentMatchResultMapper matchResultMapper;
    private final UserMapper userMapper;
    private final MomentMatcher matcher;
    private final ObjectMapper objectMapper;
    private final FileUploadService fileUploadService;
    private final MomentEnrollmentState enrollmentState;
    private final MomentSummaryReActAgent momentSummaryAgent;

    /**
     * 判断本周报名是否仍开放：
     * 1. 未被管理员手动截止
     * 2. 匹配尚未触发（不存在 MATCHED/UNMATCHED 状态的报名记录）
     */
    public boolean isEnrollmentOpen(String weekTag) {
        if (Boolean.TRUE.equals(enrollmentState.isClosed(weekTag))) {
            return false;
        }
        try {
            Long matchedCount = enrollmentMapper.selectCount(
                    new LambdaQueryWrapper<MomentEnrollment>()
                            .eq(MomentEnrollment::getWeekTag, weekTag)
                            .ne(MomentEnrollment::getStatus, MomentEnrollment.STATUS_WAITING)
            );
            return matchedCount == null || matchedCount == 0;
        } catch (Exception e) {
            log.warn("查询报名开放状态失败，默认开放", e);
            return true;
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

        return MomentStatusResponse.builder()
                .currentWeek(weekTag)
                .status(status)
                .participantCount(participantCount)
                .enrollmentOpen(open)
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
                .build();
    }

    // ==================== 获取匹配结果 ====================

    public MomentResultResponse getResult() {
        Long userId = CurrentUser.getId();
        String weekTag = getCurrentWeekTag();

        List<MomentEnrollment> enrollments = enrollmentMapper.selectList(
                new LambdaQueryWrapper<MomentEnrollment>()
                        .eq(MomentEnrollment::getUserId, userId)
                        .eq(MomentEnrollment::getWeekTag, weekTag)
        );
        if (enrollments == null || enrollments.isEmpty()) throw new BusinessException(ResultCode.MOMENT_NOT_ENROLLED);

        boolean anyWaiting = enrollments.stream().anyMatch(e -> MomentEnrollment.STATUS_WAITING.equals(e.getStatus()));
        if (anyWaiting) {
            throw new BusinessException(ResultCode.MOMENT_NO_RESULT);
        }

        boolean allUnmatched = enrollments.stream().allMatch(e -> MomentEnrollment.STATUS_UNMATCHED.equals(e.getStatus()));
        if (allUnmatched) {
            return MomentResultResponse.builder()
                    .matched(false)
                    .weekTag(weekTag)
                    .build();
        }

        // 查找匹配结果
        MomentMatchResult matchResult = matchResultMapper.selectOne(
                new LambdaQueryWrapper<MomentMatchResult>()
                        .eq(MomentMatchResult::getWeekTag, weekTag)
                        .and(w -> w
                                .eq(MomentMatchResult::getUserIdA, userId)
                                .or()
                                .eq(MomentMatchResult::getUserIdB, userId)
                        )
        );

        if (matchResult == null) {
            return MomentResultResponse.builder()
                    .matched(false)
                    .weekTag(weekTag)
                    .build();
        }

        Long matchedUserId = matchResult.getUserIdA().equals(userId)
                ? matchResult.getUserIdB()
                : matchResult.getUserIdA();

        User matchedUser = userMapper.selectById(matchedUserId);
        if (matchedUser == null) {
            return MomentResultResponse.builder()
                    .matched(false)
                    .weekTag(weekTag)
                    .build();
        }

        Map<String, Object> scoreDetail = null;
        if (matchResult.getScoreDetail() != null) {
            try {
                scoreDetail = objectMapper.readValue(matchResult.getScoreDetail(), Map.class);
            } catch (Exception e) {
                log.warn("解析分数明细失败", e);
            }
        }

        Integer age = null;
        if (matchedUser.getBirthDate() != null) {
            age = Period.between(matchedUser.getBirthDate(), LocalDate.now()).getYears();
        }

        String summary = null;
        try {
            java.math.BigDecimal score = matchResult.getTotalScore();
            summary = momentSummaryAgent.generateSummary(userId, matchedUserId, score != null ? score.doubleValue() : null);
        } catch (Exception e) {
            log.warn("心动配对总结生成失败: {}", e.getMessage());
        }

        return MomentResultResponse.builder()
                .matched(true)
                .weekTag(weekTag)
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
                .totalScore(matchResult.getTotalScore())
                .scoreDetail(scoreDetail)
                .summary(summary)
                .build();
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
        String photoUrl = fileUploadService.uploadImage(file, "moment_" + userId + "_");

        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getMomentPhotoUrl, photoUrl)
        );

        log.info("用户{}上传心动时刻照片: {}", userId, photoUrl);
        return photoUrl;
    }

    // ==================== 工具方法 ====================

    public String getCurrentWeekTag() {
        LocalDate now = LocalDate.now();
        WeekFields wf = WeekFields.ISO;
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
}
