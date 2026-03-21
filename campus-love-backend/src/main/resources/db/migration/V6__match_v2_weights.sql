-- Campus Love V2.0 匹配系统升级
-- 新增用户个性化权重表和行为日志表

-- 用户个性化权重表
CREATE TABLE IF NOT EXISTS t_user_match_weights (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL UNIQUE COMMENT '用户ID',
    weight_interest DECIMAL(5,4)    DEFAULT 0.3000 COMMENT '兴趣权重',
    weight_mbti     DECIMAL(5,4)    DEFAULT 0.2500 COMMENT 'MBTI权重',
    weight_zodiac   DECIMAL(5,4)    DEFAULT 0.1500 COMMENT '星座权重',
    weight_bazi     DECIMAL(5,4)    DEFAULT 0.1500 COMMENT '八字权重',
    weight_major    DECIMAL(5,4)    DEFAULT 0.1000 COMMENT '专业权重',
    weight_age      DECIMAL(5,4)    DEFAULT 0.0500 COMMENT '年龄权重',
    action_count    INT             DEFAULT 0      COMMENT '累计行为次数（<30次权重更新较保守）',
    last_updated    DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '用户个性化匹配权重';

-- 用户行为日志表（用于离线分析和权重回溯）
CREATE TABLE IF NOT EXISTS t_user_match_action (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL COMMENT '操作用户',
    target_user_id  BIGINT          NOT NULL COMMENT '目标用户',
    action_type     VARCHAR(20)     NOT NULL COMMENT '行为类型: FOLLOW, IGNORE, CHAT_INIT, BLOCK, PROFILE_VIEW',
    signal_strength INT             NOT NULL COMMENT '信号强度: +1(关注), -1(忽略), +2(聊天), -3(拉黑), 0(查看)',
    match_score     TINYINT         COMMENT '行为时的综合匹配分',
    detail_snapshot JSON            COMMENT '行为时各维度得分快照',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_action (user_id, created_at),
    INDEX idx_target_user (target_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '用户匹配行为日志';
