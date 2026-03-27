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

ALTER TABLE t_user
    ADD COLUMN IF NOT EXISTS wechat_openid VARCHAR(96) DEFAULT NULL COMMENT '微信小程序openid（V59）';

ALTER TABLE t_user
    ADD COLUMN IF NOT EXISTS wechat_unionid VARCHAR(96) DEFAULT NULL COMMENT '微信开放平台unionid（V59）';

ALTER TABLE t_user
    ADD COLUMN IF NOT EXISTS wechat_bound_at DATETIME DEFAULT NULL COMMENT '微信绑定时间（V59）';

DROP PROCEDURE IF EXISTS repair_add_wechat_openid_unique;
DELIMITER $$
CREATE PROCEDURE repair_add_wechat_openid_unique()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 't_user'
          AND INDEX_NAME = 'uk_wechat_openid'
    ) THEN
        ALTER TABLE t_user ADD UNIQUE KEY uk_wechat_openid (wechat_openid);
    END IF;
END$$
DELIMITER ;
CALL repair_add_wechat_openid_unique();
DROP PROCEDURE IF EXISTS repair_add_wechat_openid_unique;
