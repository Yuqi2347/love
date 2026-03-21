-- 邀约系统数据库表
-- Version: V2
-- Date: 2026-03-05

-- 邀约主表
CREATE TABLE IF NOT EXISTS t_invite (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    creator_id      BIGINT          NOT NULL COMMENT '发起人ID',
    invite_type     VARCHAR(32)     NOT NULL COMMENT '邀约类型：DINNER/SPORT/STUDY/DRAMA/OTHER',
    invite_mode     VARCHAR(16)     NOT NULL COMMENT '邀约模式：PUBLIC/PRIVATE',
    target_user_id  BIGINT          DEFAULT NULL COMMENT '一对一邀约目标用户ID',
    title           VARCHAR(64)     NOT NULL COMMENT '标题',
    content         VARCHAR(512)    DEFAULT NULL COMMENT '内容描述',
    invite_period   VARCHAR(16)     DEFAULT 'ONCE' COMMENT '周期：ONCE/WEEKLY/MONTHLY',
    period_config   VARCHAR(128)    DEFAULT NULL COMMENT '周期配置JSON：{day:1, hour:14} 或 {weekday:6, hour:14}',
    invite_time     DATETIME        NOT NULL COMMENT '邀约时间',
    location        VARCHAR(256)    DEFAULT NULL COMMENT '地点',
    max_participants INT             DEFAULT NULL COMMENT '最大人数',
    participant_count INT             DEFAULT 0 COMMENT '当前参与人数',
    status          VARCHAR(16)     DEFAULT 'RECRUITING' COMMENT '状态：RECRUITING/FULL/CONFIRMED/IN_PROGRESS/ENDED/CANCELLED',
    deadline_hours  INT             DEFAULT 1 COMMENT '报名截止小时数（活动前N小时）',
    atmosphere_tags VARCHAR(128)    DEFAULT NULL COMMENT '氛围标签，逗号分隔',
    is_urgent       TINYINT(1)      DEFAULT 0 COMMENT '是否急需',
    social_rating   DECIMAL(2,1)   DEFAULT NULL COMMENT '社交体验平均评分',
    org_rating      DECIMAL(2,1)   DEFAULT NULL COMMENT '组织力平均评分',
    rating_count    INT             DEFAULT 0 COMMENT '评价人数',
    deleted         TINYINT(1)      DEFAULT 0,
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_creator (creator_id),
    INDEX idx_status (status),
    INDEX idx_invite_time (invite_time),
    INDEX idx_invite_type (invite_type)
) COMMENT '邀约主表';

-- 邀约参与者表
CREATE TABLE IF NOT EXISTS t_invite_participant (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    invite_id       BIGINT          NOT NULL COMMENT '邀约ID',
    user_id         BIGINT          NOT NULL COMMENT '参与者ID',
    social_rating   DECIMAL(2,1)   DEFAULT NULL COMMENT '该参与者给发起人的社交体验评分',
    join_at         DATETIME        DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_invite_user (invite_id, user_id),
    INDEX idx_invite (invite_id),
    INDEX idx_user (user_id)
) COMMENT '邀约参与者表';

-- 等待邀约表
CREATE TABLE IF NOT EXISTS t_invite_wait (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL COMMENT '用户ID',
    invite_types    VARCHAR(128)    NOT NULL COMMENT '邀约类型，逗号分隔',
    period_config   VARCHAR(256)    DEFAULT NULL COMMENT '时间偏好JSON',
    location_pref   VARCHAR(64)     DEFAULT NULL COMMENT '地点偏好',
    auto_accept    TINYINT(1)      DEFAULT 0 COMMENT '是否自动受邀',
    expire_hours    INT             NOT NULL COMMENT '有效时长（小时）',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_created (created_at)
) COMMENT '等待邀约表';

-- 邀约评价表
CREATE TABLE IF NOT EXISTS t_invite_rating (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    invite_id       BIGINT          NOT NULL COMMENT '邀约ID',
    rater_id        BIGINT          NOT NULL COMMENT '评价人ID',
    rated_user_id   BIGINT          NOT NULL COMMENT '被评价人ID',
    social_rating   DECIMAL(2,1)   NOT NULL COMMENT '社交体验评分 0-5',
    org_rating      DECIMAL(2,1)   DEFAULT NULL COMMENT '组织力评分（仅发起人）',
    content         VARCHAR(256)    DEFAULT NULL COMMENT '评价内容',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_rating (invite_id, rater_id, rated_user_id),
    INDEX idx_invite (invite_id),
    INDEX idx_rated_user (rated_user_id)
) COMMENT '邀约评价表';

-- 用户表新增邀约统计字段（MySQL 5.7 兼容：若列已存在会报 Duplicate column，可忽略）
ALTER TABLE t_user ADD COLUMN invite_count INT DEFAULT 0 COMMENT '发起邀约次数';
ALTER TABLE t_user ADD COLUMN participate_count INT DEFAULT 0 COMMENT '参与邀约次数';
ALTER TABLE t_user ADD COLUMN credit_score INT DEFAULT 100 COMMENT '信用分';
