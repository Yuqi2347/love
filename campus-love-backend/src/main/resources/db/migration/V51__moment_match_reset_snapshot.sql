-- 管理员「重置本周」前：匹配结果 + content 行级快照，供审计（业务表仍物理删除）

CREATE TABLE IF NOT EXISTS t_moment_match_reset_snapshot (
    id                       BIGINT PRIMARY KEY AUTO_INCREMENT,
    snapshot_batch_id        VARCHAR(36)  NOT NULL COMMENT '单次重置操作批次 UUID',
    week_tag                 VARCHAR(16)  NOT NULL,
    archived_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    operator_id              BIGINT       NULL COMMENT '管理员用户 ID',
    original_match_result_id BIGINT       NULL COMMENT '删除前 t_moment_match_result.id',
    pool                     VARCHAR(8)   NOT NULL,
    user_id_a                BIGINT       NOT NULL,
    user_id_b                BIGINT       NOT NULL,
    total_score              DECIMAL(5,2) NULL,
    result_created_at        DATETIME     NULL,
    content_snapshot_json    LONGTEXT     NULL COMMENT 't_moment_match_result_content 行 JSON 快照',
    INDEX idx_week_archived (week_tag, archived_at),
    INDEX idx_batch (snapshot_batch_id)
) COMMENT '心动时刻：重置本周前的匹配结果快照（审计）';
