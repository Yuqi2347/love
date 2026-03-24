-- 心动时刻：自动公布结果时间（与自动匹配并列配置）
DROP PROCEDURE IF EXISTS upgrade_moment_auto_publish_config;
DELIMITER $$
CREATE PROCEDURE upgrade_moment_auto_publish_config()
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
              AND COLUMN_NAME = 'auto_publish_enabled'
        ) THEN
            ALTER TABLE t_moment_match_config
                ADD COLUMN auto_publish_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否自动公布匹配结果'
                AFTER auto_match_time;
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 't_moment_match_config'
              AND COLUMN_NAME = 'auto_publish_day_of_week'
        ) THEN
            ALTER TABLE t_moment_match_config
                ADD COLUMN auto_publish_day_of_week TINYINT NOT NULL DEFAULT 5 COMMENT '自动公布周几(1=周一..7=周日)'
                AFTER auto_publish_enabled;
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 't_moment_match_config'
              AND COLUMN_NAME = 'auto_publish_time'
        ) THEN
            ALTER TABLE t_moment_match_config
                ADD COLUMN auto_publish_time VARCHAR(8) NOT NULL DEFAULT '12:00' COMMENT '自动公布时刻 HH:mm(北京时间)'
                AFTER auto_publish_day_of_week;
        END IF;
    END IF;
END$$
DELIMITER ;

CALL upgrade_moment_auto_publish_config();
DROP PROCEDURE IF EXISTS upgrade_moment_auto_publish_config;
