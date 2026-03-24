package com.campus.love.moment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_moment_match_reset_snapshot")
public class MomentMatchResetSnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String snapshotBatchId;
    private String weekTag;
    private LocalDateTime archivedAt;
    private Long operatorId;
    private Long originalMatchResultId;
    private String pool;
    private Long userIdA;
    private Long userIdB;
    private BigDecimal totalScore;
    private LocalDateTime resultCreatedAt;
    private String contentSnapshotJson;
}
