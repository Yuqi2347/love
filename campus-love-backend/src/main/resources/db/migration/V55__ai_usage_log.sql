CREATE TABLE IF NOT EXISTS t_ai_usage_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    biz_type VARCHAR(32) NOT NULL COMMENT 'AVATAR / ANALYSIS',
    scene VARCHAR(64) NOT NULL COMMENT '具体场景',
    provider VARCHAR(32) DEFAULT NULL COMMENT '调用通道',
    model_name VARCHAR(128) DEFAULT NULL COMMENT '模型名',
    user_id BIGINT DEFAULT NULL COMMENT '触发用户',
    biz_key VARCHAR(128) DEFAULT NULL COMMENT '业务键',
    tokens_used INT NOT NULL DEFAULT 0 COMMENT 'token 消耗，无返回时记 0',
    call_count INT NOT NULL DEFAULT 1 COMMENT '调用次数',
    source_table VARCHAR(64) DEFAULT NULL COMMENT '历史回填来源表',
    source_id BIGINT DEFAULT NULL COMMENT '历史回填来源主键',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_ai_usage_created_at (created_at),
    KEY idx_ai_usage_biz_type_created_at (biz_type, created_at),
    KEY idx_ai_usage_user_created_at (user_id, created_at),
    UNIQUE KEY uk_ai_usage_source (source_table, source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='全站 AI 使用日志';

INSERT INTO t_ai_usage_log (
    biz_type,
    scene,
    provider,
    model_name,
    user_id,
    biz_key,
    tokens_used,
    call_count,
    source_table,
    source_id,
    created_at
)
SELECT
    'ANALYSIS' AS biz_type,
    'YUANFEN' AS scene,
    'TEXT_CHAT' AS provider,
    NULL AS model_name,
    y.user_id_a AS user_id,
    CONCAT(y.user_id_a, ':', y.user_id_b) AS biz_key,
    IFNULL(y.tokens_used, 0) AS tokens_used,
    1 AS call_count,
    't_yuanfen_analysis_log' AS source_table,
    y.id AS source_id,
    y.created_at
FROM t_yuanfen_analysis_log y
LEFT JOIN t_ai_usage_log l
       ON l.source_table = 't_yuanfen_analysis_log'
      AND l.source_id = y.id
WHERE l.id IS NULL;
