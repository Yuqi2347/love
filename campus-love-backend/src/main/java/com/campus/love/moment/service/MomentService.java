package com.campus.love.moment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.common.service.FileUploadService;
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
            MomentEnrollment enrollment = enrollmentMapper.selectOne(
                    new LambdaQueryWrapper<MomentEnrollment>()
                            .eq(MomentEnrollment::getUserId, userId)
                            .eq(MomentEnrollment::getWeekTag, weekTag)
            );
            status = enrollment == null ? "NOT_ENROLLED" : enrollment.getStatus();
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

        // 检查本周是否已报名
        MomentEnrollment existing = enrollmentMapper.selectOne(
                new LambdaQueryWrapper<MomentEnrollment>()
                        .eq(MomentEnrollment::getUserId, userId)
                        .eq(MomentEnrollment::getWeekTag, weekTag)
        );
        if (existing != null) throw new BusinessException(ResultCode.MOMENT_ALREADY_ENROLLED);

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

        // 确定匹配池
        MomentPool pool = MomentPool.determine(user.getGender(), request.getTargetGender());

        // 创建报名记录
        MomentEnrollment enrollment = new MomentEnrollment();
        enrollment.setUserId(userId);
        enrollment.setWeekTag(weekTag);
        enrollment.setPool(pool.getCode());
        enrollment.setStatus(MomentEnrollment.STATUS_WAITING);
        enrollmentMapper.insert(enrollment);

        log.info("用户{}报名心动时刻: weekTag={}, pool={}", userId, weekTag, pool.getCode());

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

        MomentEnrollment enrollment = enrollmentMapper.selectOne(
                new LambdaQueryWrapper<MomentEnrollment>()
                        .eq(MomentEnrollment::getUserId, userId)
                        .eq(MomentEnrollment::getWeekTag, weekTag)
        );
        if (enrollment == null) throw new BusinessException(ResultCode.MOMENT_NOT_ENROLLED);

        if (MomentEnrollment.STATUS_WAITING.equals(enrollment.getStatus())) {
            throw new BusinessException(ResultCode.MOMENT_NO_RESULT);
        }

        if (MomentEnrollment.STATUS_UNMATCHED.equals(enrollment.getStatus())) {
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
        profile.setSocialStyle(req.getSocialStyle());
        profile.setLifeRhythm(req.getLifeRhythm());
        profile.setCompanionshipStyle(req.getCompanionshipStyle());
        profile.setAppearanceRequirement(req.getAppearanceRequirement());
        profile.setPartnerPersonality(req.getPartnerPersonality());
        profile.setMajorPreference(req.getMajorPreference());
        profile.setAgeRangePreference(req.getAgeRangePreference());
        profile.setDateStyle(req.getDateStyle());
        profile.setIntimacyPace(req.getIntimacyPace());
        profile.setLoyaltyValue(req.getLoyaltyValue());
        profile.setPremaritalCohabitation(req.getPremaritalCohabitation());
        profile.setFutureLifestyle(req.getFutureLifestyle());
        profile.setRelationshipCoreValue(req.getRelationshipCoreValue());
    }
}
