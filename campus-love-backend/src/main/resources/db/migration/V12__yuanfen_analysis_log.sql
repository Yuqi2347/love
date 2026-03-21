-- 缘分解析调用记录表
-- Version: V12
CREATE TABLE IF NOT EXISTS t_yuanfen_analysis_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id_a       BIGINT          NOT NULL COMMENT '较小的用户ID（保证对称性）',
    user_id_b       BIGINT          NOT NULL COMMENT '较大的用户ID',
    total_score     TINYINT         COMMENT '触发时综合匹配分',
    ai_result       JSON            COMMENT 'AI返回结果快照',
    tokens_used     INT             COMMENT 'Token消耗量（成本统计）',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_users (user_id_a, user_id_b),
    INDEX idx_created (created_at)
) COMMENT '缘分解析调用记录';
