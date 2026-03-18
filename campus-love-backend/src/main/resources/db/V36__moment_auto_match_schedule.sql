-- =============================================
-- V36: 心动时刻每周自动匹配配置
-- 在 t_moment_match_config 上增加自动匹配开关与时间配置
-- =============================================

DROP PROCEDURE IF EXISTS upgrade_moment_auto_match_schedule;
DELIMITER $$
CREATE PROCEDURE upgrade_moment_auto_match_schedule()
BEGIN
    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 't_moment_match_config'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 't_moment_match_config'
              AND COLUMN_NAME = 'auto_match_enabled'
        ) THEN
            ALTER TABLE t_moment_match_config
                ADD COLUMN auto_match_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否开启每周自动匹配';
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 't_moment_match_config'
              AND COLUMN_NAME = 'auto_match_day_of_week'
        ) THEN
            ALTER TABLE t_moment_match_config
                ADD COLUMN auto_match_day_of_week TINYINT NOT NULL DEFAULT 1 COMMENT '每周几触发(1=周一..7=周日)';
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 't_moment_match_config'
              AND COLUMN_NAME = 'auto_match_time'
        ) THEN
            ALTER TABLE t_moment_match_config
                ADD COLUMN auto_match_time VARCHAR(5) NOT NULL DEFAULT '16:00' COMMENT '触发时间(24h HH:mm)';
        END IF;
    END IF;
END$$
DELIMITER ;

CALL upgrade_moment_auto_match_schedule();
DROP PROCEDURE IF EXISTS upgrade_moment_auto_match_schedule;

