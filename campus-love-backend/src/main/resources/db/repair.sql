-- =============================================
-- 兼容性修复脚本
-- 目的：
-- 1. 兼容旧数据库缺列导致的新版本启动失败
-- 2. 与 schema.sql 配合，保证新旧库都能平滑启动
-- 依赖：MySQL 8.x
-- =============================================

USE campus_love;

ALTER TABLE t_user
    ADD COLUMN IF NOT EXISTS deleted_at DATETIME DEFAULT NULL COMMENT 'NULL=正常，有值=已注销（V30）';

ALTER TABLE t_user
    ADD COLUMN IF NOT EXISTS delete_reason TINYINT DEFAULT NULL COMMENT '注销原因枚举（V30）';
