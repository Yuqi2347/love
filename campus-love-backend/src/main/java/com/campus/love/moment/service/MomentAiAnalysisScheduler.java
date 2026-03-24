package com.campus.love.moment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.moment.entity.MomentActivityWeek;
import com.campus.love.moment.entity.MomentAiAnalysisTask;
import com.campus.love.moment.entity.MomentMatchResult;
import com.campus.love.moment.entity.MomentMatchResultContent;
import com.campus.love.moment.entity.MomentProfile;
import com.campus.love.moment.mapper.MomentActivityWeekMapper;
import com.campus.love.moment.mapper.MomentAiAnalysisTaskMapper;
import com.campus.love.moment.mapper.MomentMatchResultContentMapper;
import com.campus.love.moment.mapper.MomentMatchResultMapper;
import com.campus.love.moment.mapper.MomentProfileMapper;
import com.campus.love.profile.service.UserPortraitService;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 消费长文 AI 分析任务；某周全部任务结束后将周状态置为 RESULT_READY。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MomentAiAnalysisScheduler {

    private static final int BATCH = 5;
    private static final int MAX_RETRY = 3;

    private final MomentAiAnalysisTaskMapper taskMapper;
    private final MomentActivityWeekMapper activityWeekMapper;
    private final MomentMatchResultMapper matchResultMapper;
    private final MomentMatchResultContentMapper contentMapper;
    private final UserMapper userMapper;
    private final MomentProfileMapper profileMapper;
    private final UserPortraitService userPortraitService;
    private final MomentLongAnalysisAgent longAnalysisAgent;
    private final MomentActivityWeekService activityWeekService;

    @Scheduled(fixedDelay = 10_000L)
    public void tick() {
        List<MomentAiAnalysisTask> batch = taskMapper.selectList(
                new LambdaQueryWrapper<MomentAiAnalysisTask>()
                        .eq(MomentAiAnalysisTask::getStatus, MomentAiAnalysisTask.STATUS_PENDING)
                        .last("limit " + BATCH)
        );
        for (MomentAiAnalysisTask task : batch) {
            try {
                processOne(task);
            } catch (Exception e) {
                log.error("AI task {} failed", task.getId(), e);
                failTask(task, e.getMessage());
            }
        }
        finalizeWeeksIfDone();
    }

    private void processOne(MomentAiAnalysisTask task) {
        task.setStatus(MomentAiAnalysisTask.STATUS_PROCESSING);
        taskMapper.updateById(task);

        MomentMatchResult result = matchResultMapper.selectById(task.getMatchResultId());
        if (result == null) {
            failTask(task, "match_result missing");
            return;
        }
        MomentMatchResultContent content = contentMapper.selectOne(
                new LambdaQueryWrapper<MomentMatchResultContent>()
                        .eq(MomentMatchResultContent::getMatchResultId, result.getId())
                        .last("limit 1")
        );
        if (content == null) {
            failTask(task, "content missing");
            return;
        }

        User userA = userMapper.selectById(result.getUserIdA());
        User userB = userMapper.selectById(result.getUserIdB());
        MomentProfile pa = profileMapper.selectOne(
                new LambdaQueryWrapper<MomentProfile>().eq(MomentProfile::getUserId, result.getUserIdA()).last("limit 1"));
        MomentProfile pb = profileMapper.selectOne(
                new LambdaQueryWrapper<MomentProfile>().eq(MomentProfile::getUserId, result.getUserIdB()).last("limit 1"));

        String text = longAnalysisAgent.generateLongAnalysis(
                userA, userB, pa, pb,
                userPortraitService.getPortrait(result.getUserIdA()),
                userPortraitService.getPortrait(result.getUserIdB()),
                content.getScoreDetail()
        );
        content.setAiAnalysis(text);
        contentMapper.updateById(content);

        task.setStatus(MomentAiAnalysisTask.STATUS_DONE);
        task.setErrorMsg(null);
        taskMapper.updateById(task);
    }

    private void failTask(MomentAiAnalysisTask task, String msg) {
        int retries = task.getRetryCount() != null ? task.getRetryCount() : 0;
        retries++;
        task.setRetryCount(retries);
        task.setErrorMsg(msg != null && msg.length() > 1000 ? msg.substring(0, 1000) : msg);
        if (retries >= MAX_RETRY) {
            task.setStatus(MomentAiAnalysisTask.STATUS_FAILED);
        } else {
            task.setStatus(MomentAiAnalysisTask.STATUS_PENDING);
        }
        taskMapper.updateById(task);
    }

    private void finalizeWeeksIfDone() {
        List<MomentActivityWeek> weeks = activityWeekMapper.selectList(
                new LambdaQueryWrapper<MomentActivityWeek>()
                        .eq(MomentActivityWeek::getStatus, MomentActivityWeek.STATUS_AI_ANALYZING));
        for (MomentActivityWeek w : weeks) {
            long undone = taskMapper.selectCount(
                    new LambdaQueryWrapper<MomentAiAnalysisTask>()
                            .eq(MomentAiAnalysisTask::getWeekTag, w.getWeekTag())
                            .in(MomentAiAnalysisTask::getStatus,
                                    MomentAiAnalysisTask.STATUS_PENDING,
                                    MomentAiAnalysisTask.STATUS_PROCESSING)
            );
            if (undone == 0) {
                activityWeekService.markResultReady(w.getWeekTag());
                log.info("周次 {} 长文 AI 已完成，进入 RESULT_READY", w.getWeekTag());
            }
        }
    }
}
