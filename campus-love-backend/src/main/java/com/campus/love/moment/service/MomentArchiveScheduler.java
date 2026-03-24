package com.campus.love.moment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.moment.entity.MomentAiAnalysisTask;
import com.campus.love.moment.entity.MomentPairScore;
import com.campus.love.moment.entity.MomentRejectSummary;
import com.campus.love.moment.entity.MomentUserPoolBest;
import com.campus.love.moment.mapper.MomentAiAnalysisTaskMapper;
import com.campus.love.moment.mapper.MomentPairScoreMapper;
import com.campus.love.moment.mapper.MomentRejectSummaryMapper;
import com.campus.love.moment.mapper.MomentUserPoolBestMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 滚动清理匹配流水线中间表，与主流程解耦；每周一凌晨执行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MomentArchiveScheduler {

    /** pair_score、reject_summary：约 4 周 */
    private static final int PAIR_SCORE_RETENTION_DAYS = 28;
    /** user_pool_best：约 2 周 */
    private static final int USER_POOL_BEST_RETENTION_DAYS = 14;
    /** 已终态的 AI 任务：完成后约 2 周 */
    private static final int AI_TASK_RETENTION_DAYS = 14;

    private final MomentPairScoreMapper pairScoreMapper;
    private final MomentRejectSummaryMapper rejectSummaryMapper;
    private final MomentUserPoolBestMapper userPoolBestMapper;
    private final MomentAiAnalysisTaskMapper aiAnalysisTaskMapper;

    /**
     * 每周一 03:00（服务器时区）执行。
     */
    @Scheduled(cron = "0 0 3 ? * MON")
    public void purgeOldPipelineRows() {
        Thread.currentThread().setPriority(Thread.NORM_PRIORITY - 1);
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime pairCutoff = now.minusDays(PAIR_SCORE_RETENTION_DAYS);
        int pairDeleted = pairScoreMapper.delete(
                new LambdaQueryWrapper<MomentPairScore>()
                        .lt(MomentPairScore::getCreatedAt, pairCutoff));
        int rejectDeleted = rejectSummaryMapper.delete(
                new LambdaQueryWrapper<MomentRejectSummary>()
                        .lt(MomentRejectSummary::getCreatedAt, pairCutoff));

        LocalDateTime poolBestCutoff = now.minusDays(USER_POOL_BEST_RETENTION_DAYS);
        int poolBestDeleted = userPoolBestMapper.delete(
                new LambdaQueryWrapper<MomentUserPoolBest>()
                        .lt(MomentUserPoolBest::getCreatedAt, poolBestCutoff));

        LocalDateTime aiCutoff = now.minusDays(AI_TASK_RETENTION_DAYS);
        int aiDeleted = aiAnalysisTaskMapper.delete(
                new LambdaQueryWrapper<MomentAiAnalysisTask>()
                        .in(MomentAiAnalysisTask::getStatus, List.of(
                                MomentAiAnalysisTask.STATUS_DONE,
                                MomentAiAnalysisTask.STATUS_FAILED))
                        .lt(MomentAiAnalysisTask::getUpdatedAt, aiCutoff));

        log.info(
                "MomentArchiveScheduler: deleted pair_score={}, reject_summary={}, user_pool_best={}, ai_task={}",
                pairDeleted,
                rejectDeleted,
                poolBestDeleted,
                aiDeleted
        );
    }
}
