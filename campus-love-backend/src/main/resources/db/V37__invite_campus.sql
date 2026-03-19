-- =============================================
-- V37: 邀约新增校区字段
-- 在 t_invite 增加 campus，默认 ALL（不限）
-- =============================================

DROP PROCEDURE IF EXISTS upgrade_invite_add_campus;
DELIMITER $$
CREATE PROCEDURE upgrade_invite_add_campus()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 't_invite'
    ) THEN
        IF NOT EXISTS (
            SELECT 1
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 't_invite'
              AND COLUMN_NAME = 'campus'
        ) THEN
            ALTER TABLE t_invite
                ADD COLUMN campus VARCHAR(64) NOT NULL DEFAULT 'ALL' COMMENT '校区（ALL=不限）' AFTER location;
        END IF;
    END IF;
END$$
DELIMITER ;

CALL upgrade_invite_add_campus();
DROP PROCEDURE IF EXISTS upgrade_invite_add_campus;
