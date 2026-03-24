package com.campus.love.moment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.moment.entity.MomentActivityWeek;
import com.campus.love.moment.mapper.MomentActivityWeekMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MomentActivityWeekService {

    private final MomentActivityWeekMapper activityWeekMapper;

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
        if (MomentWeekStatusPolicy.closeEnrollmentNoOp(week.getStatus())) {
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
        if (MomentWeekStatusPolicy.blocksReopenEnrollment(week.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "当前阶段不可重新开放报名");
        }
        week.setStatus(MomentActivityWeek.STATUS_ENROLLING);
        week.setEnrollmentOpen(true);
        week.setClosedAt(null);
        week.setMatchedAt(null);
        week.setErrorMessage(null);
        activityWeekMapper.updateById(week);
        return week;
    }

    /** 匹配与分级落库完成，进入长文 AI 队列阶段 */
    @Transactional
    public MomentActivityWeek markAiAnalyzing(String weekTag) {
        MomentActivityWeek week = getOrCreateWeek(weekTag);
        week.setStatus(MomentActivityWeek.STATUS_AI_ANALYZING);
        week.setEnrollmentOpen(false);
        week.setErrorMessage(null);
        activityWeekMapper.updateById(week);
        return week;
    }

    /** 全部 AI 任务结束（或无需 AI），管理员可预览 */
    @Transactional
    public MomentActivityWeek markResultReady(String weekTag) {
        MomentActivityWeek week = getOrCreateWeek(weekTag);
        LocalDateTime now = LocalDateTime.now();
        week.setStatus(MomentActivityWeek.STATUS_RESULT_READY);
        week.setEnrollmentOpen(false);
        if (week.getClosedAt() == null) {
            week.setClosedAt(now);
        }
        week.setMatchedAt(now);
        week.setErrorMessage(null);
        activityWeekMapper.updateById(week);
        return week;
    }

    @Transactional
    public MomentActivityWeek markPublished(String weekTag) {
        MomentActivityWeek week = getOrCreateWeek(weekTag);
        week.setStatus(MomentActivityWeek.STATUS_PUBLISHED);
        week.setEnrollmentOpen(false);
        week.setErrorMessage(null);
        if (week.getPublishedAt() == null) {
            week.setPublishedAt(LocalDateTime.now());
        }
        activityWeekMapper.updateById(week);
        return week;
    }

    @Transactional
    public MomentActivityWeek markFailed(String weekTag, String errorMessage) {
        MomentActivityWeek week = getOrCreateWeek(weekTag);
        week.setStatus(MomentActivityWeek.STATUS_FAILED);
        week.setErrorMessage(errorMessage != null && errorMessage.length() > 1000 ? errorMessage.substring(0, 1000) : errorMessage);
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

    /** 重置为「已截止、待触发匹配」，不自动开放报名 */
    @Transactional
    public MomentActivityWeek resetWeekToWaitingMatch(String weekTag) {
        MomentActivityWeek week = getOrCreateWeek(weekTag);
        week.setStatus(MomentActivityWeek.STATUS_WAITING_MATCH);
        week.setEnrollmentOpen(false);
        week.setClosedAt(week.getClosedAt() != null ? week.getClosedAt() : LocalDateTime.now());
        week.setMatchedAt(null);
        week.setPublishedAt(null);
        week.setErrorMessage(null);
        activityWeekMapper.updateById(week);
        return week;
    }

    @Transactional
    public MomentActivityWeek setMatching(String weekTag) {
        MomentActivityWeek week = getOrCreateWeek(weekTag);
        week.setStatus(MomentActivityWeek.STATUS_MATCHING);
        week.setEnrollmentOpen(false);
        week.setErrorMessage(null);
        activityWeekMapper.updateById(week);
        return week;
    }

    private MomentActivityWeek buildInitialWeek(String weekTag) {
        MomentActivityWeek week = new MomentActivityWeek();
        week.setWeekTag(weekTag);
        week.setStatus(MomentActivityWeek.STATUS_ENROLLING);
        week.setEnrollmentOpen(true);
        return week;
    }
}
