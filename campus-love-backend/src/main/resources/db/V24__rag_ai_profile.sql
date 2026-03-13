-- =============================================
-- V24: RAG 向量索引 + AI 人物画像系统
-- 依据技术文档 V1.1.0-final Rev2 第 9、11 节
-- 适配 MySQL 8.0（向量以 JSON 存储，相似度检索在应用层实现）
-- 注：技术文档中 pgvector 方案需 PostgreSQL，本脚本为 MySQL 兼容实现
-- =============================================

-- ① t_user 新增 AI 相关字段
ALTER TABLE t_user
    ADD COLUMN bazi_unknown TINYINT(1) DEFAULT 0 COMMENT '生辰时辰是否不知道（勾选后八字权重清零）',
    ADD COLUMN ice_break_enabled TINYINT(1) DEFAULT 0 COMMENT '是否开启破冰功能',
    ADD COLUMN ai_disclosure_settings JSON DEFAULT (JSON_OBJECT('mbti', true, 'zodiac', true, 'majorCategory', true, 'interestTags', true, 'naturalLangTags', false, 'baziInfo', false, 'questionnaireHints', false)) COMMENT 'AI信息公开授权设置';

-- ② 用户 AI 画像表（第二层：动态标签，人物画像核心）
CREATE TABLE IF NOT EXISTS t_user_ai_profile (
    user_id               BIGINT        PRIMARY KEY,
    interest_tags         JSON          COMMENT '兴趣标签树 {"运动":["街头篮球","羽毛球"], "音乐":["爵士"]}',
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
    natural_language_tags JSON          COMMENT 'AI自然语言标签 ["需要安全感","慢热型"]',
    love_attachment_type  VARCHAR(20)   COMMENT '依恋类型（不进AI Prompt）',
    attracted_to_traits   JSON          COMMENT '倾向被吸引的对方特质（算法内部）',
    friction_points       JSON          COMMENT '潜在摩擦场景（不进AI Prompt）',
    user_corrected_fields JSON          COMMENT '用户手动修正过的字段名（不被AI覆盖）',
    profile_version       INT           DEFAULT 1,
    last_long_update      DATE          COMMENT '上次长期画像更新日期',
    last_short_update     DATE          COMMENT '上次短期画像更新日期',
    created_at            DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT 'AI深度人物画像（动态标签层）';

-- ③ 行为统计摘要表（第三层：永不进入AI Prompt，仅用于OCEAN更新计算）
CREATE TABLE IF NOT EXISTS t_user_behavior_summary (
    user_id                BIGINT    PRIMARY KEY,
    browse_pref_short      JSON      COMMENT '近14天兴趣类目分布',
    browse_pref_long       JSON      COMMENT '近6个月兴趣类目分布',
    chat_partner_traits    JSON      COMMENT '聊天对象MBTI/专业/OCEAN均值',
    match_interest_pattern JSON      COMMENT '有效停留偏好（含交互权重）',
    updated_at             DATETIME  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '行为统计摘要';

-- ④ 行为原始日志（30天保留）
CREATE TABLE IF NOT EXISTS t_user_behavior_log (
    id            BIGINT        PRIMARY KEY AUTO_INCREMENT,
    user_id       BIGINT        NOT NULL,
    behavior_type VARCHAR(30)   NOT NULL COMMENT 'FEED_VIEW/MATCH_CARD_VIEW/CHAT_INITIATED/GOSSIP_VIEW/GOSSIP_REACT/RANKING_VOTE',
    target_id     BIGINT        COMMENT '目标ID',
    metadata      JSON          COMMENT '行为元数据（不含聊天/帖子内容）',
    created_at    DATETIME      DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_date (user_id, created_at)
) COMMENT '行为原始日志';

-- ⑤ 人物画像向量表（RAG：MySQL 以 JSON 存储 1536 维向量，相似度检索在应用层）
CREATE TABLE IF NOT EXISTS t_user_profile_vector (
    user_id         BIGINT PRIMARY KEY,
    profile_vector  JSON    NOT NULL COMMENT '画像向量 [0.1,-0.2,...] 1536维',
    behavior_vector JSON    COMMENT '行为向量（可选）',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '用户画像向量（RAG检索用）';

-- ⑥ 朋友圈内容向量表（RAG：帖子向量化，用于破冰/缘分解析上下文）
CREATE TABLE IF NOT EXISTS t_feed_content_vector (
    feed_id          BIGINT PRIMARY KEY COMMENT '对应 t_feed_post.id',
    user_id          BIGINT NOT NULL,
    content_vector   JSON   NOT NULL COMMENT '内容向量 1536维',
    ai_tags          JSON   COMMENT 'AI提取的标签',
    primary_category VARCHAR(20) COMMENT '主要兴趣分类',
    created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id)
) COMMENT '朋友圈内容向量（RAG检索用）';

-- ⑦ 朋友圈表新增 AI 标签字段
ALTER TABLE t_feed_post
    ADD COLUMN ai_tags JSON COMMENT 'AI提取的标签（含心理学倾向词）',
    ADD COLUMN primary_category VARCHAR(20) COMMENT '主要兴趣分类',
    ADD COLUMN tag_sentiment VARCHAR(10) COMMENT 'positive/neutral/negative',
    ADD COLUMN ocean_hints JSON COMMENT 'AI推断的OCEAN信号 {E:7,O:null}',
    ADD COLUMN tag_confidence DECIMAL(3,2) COMMENT '标签置信权重 0.6/0.7/1.0/1.2';

-- ⑧ 破冰提醒日志
CREATE TABLE IF NOT EXISTS t_ice_break_reminder_log (
    id            BIGINT      PRIMARY KEY AUTO_INCREMENT,
    from_user_id  BIGINT      NOT NULL,
    to_user_id    BIGINT      NOT NULL,
    reminder_type VARCHAR(30) DEFAULT 'ICE_BREAK',
    created_at    DATETIME    DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_reminder (from_user_id, to_user_id, reminder_type)
) COMMENT '破冰提醒记录（每对用户最多2次）';

-- ⑨ 关系节点记录
CREATE TABLE IF NOT EXISTS t_relation_milestone (
    id             BIGINT      PRIMARY KEY AUTO_INCREMENT,
    user_id_a      BIGINT      NOT NULL,
    user_id_b      BIGINT      NOT NULL,
    milestone_type VARCHAR(30) NOT NULL,
    notified_at    DATETIME    DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_milestone (user_id_a, user_id_b, milestone_type)
) COMMENT '关系节点提醒记录';
