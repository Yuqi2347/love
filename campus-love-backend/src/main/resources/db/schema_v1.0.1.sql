-- =============================================
-- V1.0.1 版本更新脚本
-- =============================================

USE campus_love;

-- 1. 为用户表添加出生时间字段（用于精确计算八字）
ALTER TABLE t_user ADD COLUMN birth_time TIME DEFAULT NULL COMMENT '出生时间（用于八字计算）' AFTER birth_date;

-- 2. 为用户表添加等级和活跃度字段
ALTER TABLE t_user ADD COLUMN activity_score INT DEFAULT 0 COMMENT '活跃度积分' AFTER grade;
ALTER TABLE t_user ADD COLUMN user_level INT DEFAULT 1 COMMENT '用户等级' AFTER activity_score;
ALTER TABLE t_user ADD COLUMN is_admin TINYINT(1) DEFAULT 0 COMMENT '是否管理员' AFTER user_level;

-- 3. 添加活跃度记录表
CREATE TABLE IF NOT EXISTS t_activity_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL COMMENT '用户ID',
    activity_type   VARCHAR(32)     NOT NULL COMMENT '活动类型：FOLLOW/VIEW/LIKE/COLLECT',
    target_id       BIGINT          DEFAULT NULL COMMENT '目标ID（用户ID/帖子ID）',
    score           INT             NOT NULL COMMENT '获得的活跃度积分',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_type (activity_type),
    INDEX idx_created (created_at)
) COMMENT '活跃度记录表';

-- 4. 为发现模块帖子表添加发布权限要求字段
ALTER TABLE t_feed_post ADD COLUMN post_type VARCHAR(32) DEFAULT 'USER' COMMENT '帖子类型：SYSTEM-系统发布 ADMIN-管理员发布 USER-普通用户（需等级）' AFTER content;
ALTER TABLE t_feed_post ADD COLUMN required_level INT DEFAULT 1 COMMENT '发布所需等级' AFTER post_type;
