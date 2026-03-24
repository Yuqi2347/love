package com.campus.love.moment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.moment.entity.MomentAiAnalysisTask;
import com.campus.love.moment.entity.MomentMatchConfirm;
import com.campus.love.moment.entity.MomentMatchResult;
import com.campus.love.moment.entity.MomentMatchResultContent;
import com.campus.love.moment.entity.MomentPairScore;
import com.campus.love.moment.entity.MomentRejectSummary;
import com.campus.love.moment.entity.MomentUserPoolBest;
import com.campus.love.moment.mapper.MomentAiAnalysisTaskMapper;
import com.campus.love.moment.mapper.MomentMatchConfirmMapper;
import com.campus.love.moment.mapper.MomentMatchResultContentMapper;
import com.campus.love.moment.mapper.MomentMatchResultMapper;
import com.campus.love.moment.mapper.MomentPairScoreMapper;
import com.campus.love.moment.mapper.MomentRejectSummaryMapper;
import com.campus.love.moment.mapper.MomentUserPoolBestMapper;
import com.campus.love.pairdate.entity.MomentYueIntent;
import com.campus.love.pairdate.entity.PairDateNegotiation;
import com.campus.love.pairdate.mapper.MomentYueIntentMapper;
import com.campus.love.pairdate.mapper.PairDateNegotiationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 心动时刻按周数据删除（短事务，供匹配流水线调用）。
 */
@Service
@RequiredArgsConstructor
public class MomentWeekDataService {

    private final MomentMatchResultMapper matchResultMapper;
    private final MomentMatchResultContentMapper matchResultContentMapper;
    private final MomentMatchConfirmMapper matchConfirmMapper;
    private final MomentPairScoreMapper pairScoreMapper;
    private final MomentRejectSummaryMapper rejectSummaryMapper;
    private final MomentUserPoolBestMapper userPoolBestMapper;
    private final MomentAiAnalysisTaskMapper aiAnalysisTaskMapper;
    private final PairDateNegotiationMapper pairDateNegotiationMapper;
    private final MomentYueIntentMapper momentYueIntentMapper;

    @Transactional
    public void deletePipelineDataForWeek(String weekTag) {
        List<Long> resultIds = matchResultMapper.selectList(
                        new LambdaQueryWrapper<MomentMatchResult>()
                                .eq(MomentMatchResult::getWeekTag, weekTag))
                .stream()
                .map(MomentMatchResult::getId)
                .toList();
        if (!resultIds.isEmpty()) {
            matchResultContentMapper.delete(new LambdaQueryWrapper<MomentMatchResultContent>()
                    .in(MomentMatchResultContent::getMatchResultId, resultIds));
            matchConfirmMapper.delete(new LambdaQueryWrapper<MomentMatchConfirm>()
                    .in(MomentMatchConfirm::getMatchResultId, resultIds));
            momentYueIntentMapper.delete(new LambdaQueryWrapper<MomentYueIntent>()
                    .in(MomentYueIntent::getMatchResultId, resultIds));
        }
        pairDateNegotiationMapper.delete(new LambdaQueryWrapper<PairDateNegotiation>()
                .eq(PairDateNegotiation::getWeekTag, weekTag));
        matchResultMapper.delete(new LambdaQueryWrapper<MomentMatchResult>()
                .eq(MomentMatchResult::getWeekTag, weekTag));
        pairScoreMapper.delete(new LambdaQueryWrapper<MomentPairScore>()
                .eq(MomentPairScore::getWeekTag, weekTag));
        rejectSummaryMapper.delete(new LambdaQueryWrapper<MomentRejectSummary>()
                .eq(MomentRejectSummary::getWeekTag, weekTag));
        userPoolBestMapper.delete(new LambdaQueryWrapper<MomentUserPoolBest>()
                .eq(MomentUserPoolBest::getWeekTag, weekTag));
        aiAnalysisTaskMapper.delete(new LambdaQueryWrapper<MomentAiAnalysisTask>()
                .eq(MomentAiAnalysisTask::getWeekTag, weekTag));
    }
}
