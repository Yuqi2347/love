package com.campus.love.moment.service;

import com.campus.love.moment.entity.MomentActivityWeek;

/**
 * 心动时刻周状态业务规则（单一入口，避免魔法字符串散落）。
 */
public final class MomentWeekStatusPolicy {

    private MomentWeekStatusPolicy() {
    }

    public static boolean userMayViewPublishedResults(String status) {
        return MomentActivityWeek.STATUS_PUBLISHED.equals(status);
    }

    /** 管理端可预览匹配结果（未对用户公布或已公布） */
    public static boolean adminMayPreviewMatchResults(String status) {
        return MomentActivityWeek.STATUS_RESULT_READY.equals(status)
                || MomentActivityWeek.STATUS_PUBLISHED.equals(status);
    }

    /** 匹配流水线进行中（含图匹配与分级写库） */
    public static boolean isMatchingPipelineRunning(String status) {
        return MomentActivityWeek.STATUS_MATCHING.equals(status);
    }

    public static boolean isAiAnalyzing(String status) {
        return MomentActivityWeek.STATUS_AI_ANALYZING.equals(status);
    }

    public static boolean isFailed(String status) {
        return MomentActivityWeek.STATUS_FAILED.equals(status);
    }

    /** 禁止再次触发匹配（需先 reset 的终态或进行中） */
    public static boolean blocksTriggerMatching(String status) {
        return MomentActivityWeek.STATUS_MATCHING.equals(status)
                || MomentActivityWeek.STATUS_AI_ANALYZING.equals(status)
                || MomentActivityWeek.STATUS_RESULT_READY.equals(status)
                || MomentActivityWeek.STATUS_PUBLISHED.equals(status);
    }

    /** 仅允许从「已截止待匹配」或失败回退后触发 */
    public static boolean allowsTriggerMatching(String status) {
        return MomentActivityWeek.STATUS_WAITING_MATCH.equals(status)
                || MomentActivityWeek.STATUS_FAILED.equals(status);
    }

    /** closeEnrollment 应直接返回、不改变周状态 */
    public static boolean closeEnrollmentNoOp(String status) {
        return MomentActivityWeek.STATUS_PUBLISHED.equals(status)
                || MomentActivityWeek.STATUS_RESULT_READY.equals(status)
                || MomentActivityWeek.STATUS_MATCHING.equals(status)
                || MomentActivityWeek.STATUS_AI_ANALYZING.equals(status)
                || MomentActivityWeek.STATUS_FAILED.equals(status);
    }

    /** 重新开放报名：已有结果或流水线中则不允许 */
    public static boolean blocksReopenEnrollment(String status) {
        return MomentActivityWeek.STATUS_PUBLISHED.equals(status)
                || MomentActivityWeek.STATUS_RESULT_READY.equals(status)
                || MomentActivityWeek.STATUS_MATCHING.equals(status)
                || MomentActivityWeek.STATUS_AI_ANALYZING.equals(status);
    }

    /** 本周是否已有「可视为完成匹配」的周状态（用于 overview 等） */
    public static boolean weekHasCompletedMatchingWork(String status) {
        return MomentActivityWeek.STATUS_RESULT_READY.equals(status)
                || MomentActivityWeek.STATUS_PUBLISHED.equals(status);
    }
}
