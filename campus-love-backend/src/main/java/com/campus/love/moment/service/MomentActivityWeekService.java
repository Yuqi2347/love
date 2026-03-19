package com.campus.love.moment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.moment.entity.MomentActivityWeek;
import com.campus.love.moment.entity.MomentEnrollment;
import com.campus.love.moment.entity.MomentMatchResult;
import com.campus.love.moment.mapper.MomentActivityWeekMapper;
import com.campus.love.moment.mapper.MomentEnrollmentMapper;
import com.campus.love.moment.mapper.MomentMatchResultMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MomentActivityWeekService {

    private final MomentActivityWeekMapper activityWeekMapper;
    private final MomentEnrollmentMapper enrollmentMapper;
    private final MomentMatchResultMapper matchResultMapper;

    public MomentActivityWeek getOrCreateWeek(String weekTag) {
        MomentActivityWeek existing = activityWeekMapper.selectOne(
                new LambdaQueryWrapper<MomentActivityWeek>()
                        .eq(MomentActivityWeek::getWeekTag, weekTag)
                        .last("limit 1")
        );
        if (existing != null) {
            return existing;
        }

        MomentActivityWeek initial = buildInitialWeek(weekTag);
        try {
            activityWeekMapper.insert(initial);
            return initial;
        } catch (DuplicateKeyException ignored) {
            return activityWeekMapper.selectOne(
                    new LambdaQueryWrapper<MomentActivityWeek>()
                            .eq(MomentActivityWeek::getWeekTag, weekTag)
                            .last("limit 1")
            );
        }
    }

    public boolean isEnrollmentOpen(String weekTag) {
        return Boolean.TRUE.equals(getOrCreateWeek(weekTag).getEnrollmentOpen());
    }

    @Transactional
    public MomentActivityWeek closeEnrollment(String weekTag) {
        MomentActivityWeek week = getOrCreateWeek(weekTag);
        if (MomentActivityWeek.STATUS_RESULT_READY.equals(week.getStatus())) {
            return week;
        }
        week.setStatus(MomentActivityWeek.STATUS_WAITING_MATCH);
        week.setEnrollmentOpen(false);
        if (week.getClosedAt() == null) {
            week.setClosedAt(LocalDateTime.now());
        }
        activityWeekMapper.updateById(week);
        return week;
    }

    @Transactional
    public MomentActivityWeek reopenEnrollment(String weekTag) {
        MomentActivityWeek week = getOrCreateWeek(weekTag);
        if (MomentActivityWeek.STATUS_RESULT_READY.equals(week.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "匹配结果已生成，不能重新开放报名");
        }
        week.setStatus(MomentActivityWeek.STATUS_ENROLLING);
        week.setEnrollmentOpen(true);
        week.setClosedAt(null);
        week.setMatchedAt(null);
        activityWeekMapper.updateById(week);
        return week;
    }

    @Transactional
    public MomentActivityWeek markMatched(String weekTag) {
        MomentActivityWeek week = getOrCreateWeek(weekTag);
        LocalDateTime now = LocalDateTime.now();
        week.setStatus(MomentActivityWeek.STATUS_RESULT_READY);
        week.setEnrollmentOpen(false);
        if (week.getClosedAt() == null) {
            week.setClosedAt(now);
        }
        week.setMatchedAt(now);
        activityWeekMapper.updateById(week);
        return week;
    }

    @Transactional
    public MomentActivityWeek markAutoProcessed(String weekTag) {
        MomentActivityWeek week = getOrCreateWeek(weekTag);
        if (week.getAutoMatchAt() == null) {
            week.setAutoMatchAt(LocalDateTime.now());
            activityWeekMapper.updateById(week);
        }
        return week;
    }

    @Transactional
    public MomentActivityWeek resetWeek(String weekTag) {
        MomentActivityWeek week = getOrCreateWeek(weekTag);
        week.setStatus(MomentActivityWeek.STATUS_ENROLLING);
        week.setEnrollmentOpen(true);
        week.setClosedAt(null);
        week.setMatchedAt(null);
        activityWeekMapper.updateById(week);
        return week;
    }

    private MomentActivityWeek buildInitialWeek(String weekTag) {
        Long resultCount = matchResultMapper.selectCount(
                new LambdaQueryWrapper<MomentMatchResult>()
                        .eq(MomentMatchResult::getWeekTag, weekTag)
        );
        Long resolvedCount = enrollmentMapper.selectCount(
                new LambdaQueryWrapper<MomentEnrollment>()
                        .eq(MomentEnrollment::getWeekTag, weekTag)
                        .ne(MomentEnrollment::getStatus, MomentEnrollment.STATUS_WAITING)
        );
        MomentActivityWeek week = new MomentActivityWeek();
        week.setWeekTag(weekTag);
        if ((resultCount != null && resultCount > 0) || (resolvedCount != null && resolvedCount > 0)) {
            week.setStatus(MomentActivityWeek.STATUS_RESULT_READY);
            week.setEnrollmentOpen(false);
            MomentMatchResult lastResult = matchResultMapper.selectOne(
                    new LambdaQueryWrapper<MomentMatchResult>()
                            .eq(MomentMatchResult::getWeekTag, weekTag)
                            .orderByDesc(MomentMatchResult::getCreatedAt)
                            .last("limit 1")
            );
            if (lastResult != null) {
                week.setMatchedAt(lastResult.getCreatedAt());
                week.setClosedAt(lastResult.getCreatedAt());
            }
        } else {
            week.setStatus(MomentActivityWeek.STATUS_ENROLLING);
            week.setEnrollmentOpen(true);
        }
        return week;
    }
}
