-- =============================================
-- V24 表结构修复脚本（仅建表，无 ALTER）
-- 当人物画像管理报错「表不存在」时执行本脚本
-- 用法：mysql -u用户 -p 数据库名 < fix_v24_tables.sql
-- 或：SOURCE /path/to/fix_v24_tables.sql;
-- =============================================

-- ① 用户 AI 画像表
CREATE TABLE IF NOT EXISTS t_user_ai_profile (
    user_id               BIGINT        PRIMARY KEY,
    interest_tags         JSON          COMMENT '兴趣标签树',
    ocean_o_long          DECIMAL(4,1)  COMMENT '开放性-长期',
    ocean_c_long          DECIMAL(4,1)  COMMENT '尽责性-长期',
    ocean_e_long          DECIMAL(4,1)  COMMENT '外向性-长期',
    ocean_a_long          DECIMAL(4,1)  COMMENT '宜人性-长期',
    ocean_n_long          DECIMAL(4,1)  COMMENT '神经质-长期',
    ocean_o_short         DECIMAL(4,1)  COMMENT '开放性-短期',
    ocean_c_short         DECIMAL(4,1)  COMMENT '尽责性-短期',
    ocean_e_short         DECIMAL(4,1)  COMMENT '外向性-短期',
    ocean_a_short         DECIMAL(4,1)  COMMENT '宜人性-短期',
    ocean_n_short         DECIMAL(4,1)  COMMENT '神经质-短期',
    has_real_ocean        TINYINT(1)    DEFAULT 0 COMMENT '是否有行为数据支撑的真实OCEAN',
    natural_language_tags JSON        COMMENT 'AI自然语言标签',
    love_attachment_type  VARCHAR(20)   COMMENT '依恋类型',
    attracted_to_traits   JSON          COMMENT '倾向被吸引的对方特质',
    friction_points       JSON          COMMENT '潜在摩擦场景',
    user_corrected_fields JSON         COMMENT '用户手动修正过的字段名',
    profile_version       INT           DEFAULT 1,
    last_long_update      DATE,
    last_short_update     DATE,
    created_at            DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT 'AI深度人物画像（V24）';

-- ② 行为统计摘要表
CREATE TABLE IF NOT EXISTS t_user_behavior_summary (
    user_id                BIGINT    PRIMARY KEY,
    browse_pref_short      JSON,
    browse_pref_long       JSON,
    chat_partner_traits    JSON,
    match_interest_pattern JSON,
    updated_at             DATETIME  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '行为统计摘要（V24）';

-- ③ 行为原始日志
CREATE TABLE IF NOT EXISTS t_user_behavior_log (
    id            BIGINT        PRIMARY KEY AUTO_INCREMENT,
    user_id       BIGINT        NOT NULL,
    behavior_type VARCHAR(30)   NOT NULL,
    target_id     BIGINT,
    metadata      JSON,
    created_at    DATETIME      DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_date (user_id, created_at)
) COMMENT '行为原始日志（V24）';

-- ④ 人物画像向量表（RAG）
CREATE TABLE IF NOT EXISTS t_user_profile_vector (
    user_id         BIGINT PRIMARY KEY,
    profile_vector  JSON    NOT NULL COMMENT '画像向量 1536维',
    behavior_vector JSON,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '用户画像向量 RAG（V24）';

-- ⑤ 朋友圈内容向量表（RAG）
CREATE TABLE IF NOT EXISTS t_feed_content_vector (
    feed_id          BIGINT PRIMARY KEY,
    user_id          BIGINT NOT NULL,
    content_vector   JSON   NOT NULL COMMENT '内容向量 1536维',
    ai_tags          JSON,
    primary_category VARCHAR(20),
    created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id)
) COMMENT '朋友圈内容向量 RAG（V24）';

-- ⑥ 破冰提醒日志
CREATE TABLE IF NOT EXISTS t_ice_break_reminder_log (
    id            BIGINT      PRIMARY KEY AUTO_INCREMENT,
    from_user_id  BIGINT      NOT NULL,
    to_user_id    BIGINT      NOT NULL,
    reminder_type VARCHAR(30) DEFAULT 'ICE_BREAK',
    created_at    DATETIME    DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_reminder (from_user_id, to_user_id, reminder_type)
) COMMENT '破冰提醒记录（V24）';

-- ⑦ 关系节点记录
CREATE TABLE IF NOT EXISTS t_relation_milestone (
    id             BIGINT      PRIMARY KEY AUTO_INCREMENT,
    user_id_a      BIGINT      NOT NULL,
    user_id_b      BIGINT      NOT NULL,
    milestone_type VARCHAR(30) NOT NULL,
    notified_at    DATETIME    DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_milestone (user_id_a, user_id_b, milestone_type)
) COMMENT '关系节点提醒记录（V24）';
