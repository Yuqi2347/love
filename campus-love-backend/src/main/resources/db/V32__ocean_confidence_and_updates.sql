-- V32: OCEAN confidence + portrait update metadata

DROP PROCEDURE IF EXISTS add_ocean_confidence_cols;
DELIMITER $$
CREATE PROCEDURE add_ocean_confidence_cols()
BEGIN
    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 't_user_portrait'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 't_user_portrait'
              AND COLUMN_NAME = 'ocean_confidence'
        ) THEN
            ALTER TABLE t_user_portrait
                ADD COLUMN ocean_confidence JSON DEFAULT NULL COMMENT 'OCEAN置信度 {\"O\":0.75,...}' AFTER ocean_n_short;
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 't_user_portrait'
              AND COLUMN_NAME = 'last_long_update'
        ) THEN
            ALTER TABLE t_user_portrait
                ADD COLUMN last_long_update DATE DEFAULT NULL AFTER profile_version;
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 't_user_portrait'
              AND COLUMN_NAME = 'last_short_update'
        ) THEN
            ALTER TABLE t_user_portrait
                ADD COLUMN last_short_update DATE DEFAULT NULL AFTER last_long_update;
        END IF;
    END IF;

    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 't_user_ai_profile'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 't_user_ai_profile'
              AND COLUMN_NAME = 'ocean_confidence'
        ) THEN
            ALTER TABLE t_user_ai_profile
                ADD COLUMN ocean_confidence JSON DEFAULT NULL COMMENT 'OCEAN置信度 {\"O\":0.75,...}' AFTER ocean_n_short;
        END IF;
    END IF;
END$$
DELIMITER ;

CALL add_ocean_confidence_cols();
DROP PROCEDURE IF EXISTS add_ocean_confidence_cols;
