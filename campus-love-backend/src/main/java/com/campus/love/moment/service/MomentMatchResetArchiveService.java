package com.campus.love.moment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.moment.entity.MomentMatchResult;
import com.campus.love.moment.entity.MomentMatchResultContent;
import com.campus.love.moment.entity.MomentMatchResetSnapshot;
import com.campus.love.moment.mapper.MomentMatchResultContentMapper;
import com.campus.love.moment.mapper.MomentMatchResultMapper;
import com.campus.love.moment.mapper.MomentMatchResetSnapshotMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 管理员重置本周前，将当周 {@code t_moment_match_result} + content 快照写入审计表。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MomentMatchResetArchiveService {

    private final MomentMatchResultMapper matchResultMapper;
    private final MomentMatchResultContentMapper matchResultContentMapper;
    private final MomentMatchResetSnapshotMapper snapshotMapper;
    private final ObjectMapper objectMapper;

    /**
     * @return 若有匹配结果则返回本次批次的 snapshotBatchId，否则 empty（由调用方 {@code resetWeek} 事务包裹）
     */
    public Optional<String> archiveWeekMatchResultsBeforeDelete(String weekTag, Long operatorId) {
        List<MomentMatchResult> results = matchResultMapper.selectList(
                new LambdaQueryWrapper<MomentMatchResult>()
                        .eq(MomentMatchResult::getWeekTag, weekTag)
                        .orderByAsc(MomentMatchResult::getId)
        );
        if (results.isEmpty()) {
            return Optional.empty();
        }
        String batchId = UUID.randomUUID().toString();
        LocalDateTime archivedAt = LocalDateTime.now();
        for (MomentMatchResult r : results) {
            MomentMatchResultContent content = matchResultContentMapper.selectOne(
                    new LambdaQueryWrapper<MomentMatchResultContent>()
                            .eq(MomentMatchResultContent::getMatchResultId, r.getId())
                            .last("limit 1")
            );
            String contentJson = null;
            if (content != null) {
                try {
                    contentJson = objectMapper.writeValueAsString(content);
                } catch (JsonProcessingException e) {
                    throw new IllegalStateException("序列化匹配 content 快照失败: matchResultId=" + r.getId(), e);
                }
            }
            MomentMatchResetSnapshot row = new MomentMatchResetSnapshot();
            row.setSnapshotBatchId(batchId);
            row.setWeekTag(weekTag);
            row.setArchivedAt(archivedAt);
            row.setOperatorId(operatorId);
            row.setOriginalMatchResultId(r.getId());
            row.setPool(r.getPool());
            row.setUserIdA(r.getUserIdA());
            row.setUserIdB(r.getUserIdB());
            row.setTotalScore(r.getTotalScore());
            row.setResultCreatedAt(r.getCreatedAt());
            row.setContentSnapshotJson(contentJson);
            snapshotMapper.insert(row);
        }
        log.info(
                "心动时刻重置前已归档匹配快照: weekTag={} batchId={} rows={}",
                weekTag,
                batchId,
                results.size()
        );
        return Optional.of(batchId);
    }
}
