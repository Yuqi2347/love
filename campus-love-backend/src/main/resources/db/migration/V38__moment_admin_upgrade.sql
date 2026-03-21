-- =============================================
-- V38: 心动时刻后台升级
-- 1. 增加每周活动状态表，替换内存报名开关
-- 2. 增加后台操作日志表
-- =============================================

CREATE TABLE IF NOT EXISTS t_moment_activity_week (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    week_tag         VARCHAR(10) NOT NULL COMMENT '活动周标识，如 2026-W10',
    status           VARCHAR(20) NOT NULL DEFAULT 'ENROLLING' COMMENT 'ENROLLING/WAITING_MATCH/RESULT_READY',
    enrollment_open  TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否允许继续报名',
    auto_match_at    DATETIME DEFAULT NULL COMMENT '本周自动匹配调度实际执行时间',
    closed_at        DATETIME DEFAULT NULL COMMENT '报名截止时间',
    matched_at       DATETIME DEFAULT NULL COMMENT '结果生成时间',
    created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_week_tag (week_tag)
);

CREATE TABLE IF NOT EXISTS t_moment_admin_log (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    week_tag     VARCHAR(10) DEFAULT NULL COMMENT '活动周标识',
    operator_id  BIGINT DEFAULT NULL COMMENT '操作人，NULL 表示系统任务',
    action_type  VARCHAR(40) NOT NULL COMMENT '操作类型',
    target_type  VARCHAR(40) NOT NULL COMMENT '目标类型',
    target_id    BIGINT DEFAULT NULL COMMENT '目标ID',
    summary      VARCHAR(255) DEFAULT NULL COMMENT '结果摘要',
    detail_json  JSON DEFAULT NULL COMMENT '详细结果快照',
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_week_created (week_tag, created_at),
    INDEX idx_action_created (action_type, created_at)
);
