-- V13: 心动一刻模块
-- 每周匿名 AI 深度配对活动

-- 1. 用户表新增心动一刻字段
ALTER TABLE t_user ADD COLUMN moment_photo_url   VARCHAR(256) DEFAULT NULL COMMENT '心动一刻照片URL（可选）';
ALTER TABLE t_user ADD COLUMN moment_self_score   TINYINT      DEFAULT NULL COMMENT '自评颜值分（1-10）';
ALTER TABLE t_user ADD COLUMN moment_banned       TINYINT(1)   DEFAULT 0   COMMENT '是否被禁止参加心动一刻';

-- 2. 心动一刻用户档案（问卷答案，每用户一份，可复用）
CREATE TABLE t_moment_profile (
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id                 BIGINT       NOT NULL UNIQUE COMMENT '用户ID（一人一份档案）',
    target_gender           VARCHAR(10)  NOT NULL COMMENT '期望匹配性别: male/female/any',
    social_style            CHAR(1)      COMMENT 'Q3 社交风格: A/B/C',
    life_rhythm             CHAR(1)      COMMENT 'Q4 生活节奏: A/B/C',
    companionship_style     CHAR(1)      COMMENT 'Q5 陪伴方式: A/B/C',
    appearance_requirement  CHAR(1)      COMMENT 'Q6 颜值要求: A/B/C',
    partner_personality     CHAR(1)      COMMENT 'Q7 期望性格: A/B/C',
    major_preference        CHAR(1)      COMMENT 'Q8 专业偏好: A/B/C',
    age_range_preference    VARCHAR(20)  COMMENT 'Q9 年龄偏好（可多选逗号分隔）: A/B/C/D',
    date_style              CHAR(1)      COMMENT 'Q10 约会风格: A/B/C',
    intimacy_pace           CHAR(1)      COMMENT 'Q11 亲密节奏: A/B/C',
    loyalty_value           CHAR(1)      COMMENT 'Q12 忠诚观: A/B/C',
    premarital_cohabitation CHAR(1)      COMMENT 'Q13 同居观: A/B/C',
    future_lifestyle        CHAR(1)      COMMENT 'Q14 未来规划: A/B/C',
    relationship_core_value CHAR(1)      COMMENT 'Q15 核心价值: A/B/C/D',
    created_at              DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) COMMENT '心动一刻用户档案（问卷答案）';

-- 3. 每周报名记录
CREATE TABLE t_moment_enrollment (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    week_tag    VARCHAR(10)  NOT NULL COMMENT '活动周标识，如 2026-W10',
    pool        VARCHAR(4)   NOT NULL COMMENT '匹配池: MF/MM/FF',
    status      VARCHAR(20)  DEFAULT 'WAITING' COMMENT 'WAITING/MATCHED/UNMATCHED',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_week (user_id, week_tag)
) COMMENT '心动一刻每周报名记录';

-- 4. 每周匹配结果
CREATE TABLE t_moment_match_result (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    week_tag    VARCHAR(10)  NOT NULL COMMENT '活动周标识',
    pool        VARCHAR(4)   NOT NULL COMMENT '匹配池: MF/MM/FF',
    user_id_a   BIGINT       NOT NULL,
    user_id_b   BIGINT       NOT NULL,
    total_score DECIMAL(5,2) COMMENT '配对综合分',
    score_detail JSON        COMMENT '四维度分数明细',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_week (week_tag),
    INDEX idx_user_a (user_id_a),
    INDEX idx_user_b (user_id_b)
) COMMENT '心动一刻每周配对结果';
