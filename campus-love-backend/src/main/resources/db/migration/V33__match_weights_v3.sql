-- 匹配权重重构：切换为 ocean/interest/values/age_grade/major/zodiac 六维

DROP PROCEDURE IF EXISTS upgrade_match_weights_v3;
DELIMITER $$
CREATE PROCEDURE upgrade_match_weights_v3()
BEGIN
    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user_match_weights'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user_match_weights' AND COLUMN_NAME = 'weight_ocean'
        ) THEN
            ALTER TABLE t_user_match_weights ADD COLUMN weight_ocean DECIMAL(5,4) DEFAULT 0.3500 COMMENT 'OCEAN权重' AFTER user_id;
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user_match_weights' AND COLUMN_NAME = 'weight_values'
        ) THEN
            ALTER TABLE t_user_match_weights ADD COLUMN weight_values DECIMAL(5,4) DEFAULT 0.2000 COMMENT '价值观权重' AFTER weight_interest;
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user_match_weights' AND COLUMN_NAME = 'weight_age_grade'
        ) THEN
            ALTER TABLE t_user_match_weights ADD COLUMN weight_age_grade DECIMAL(5,4) DEFAULT 0.1000 COMMENT '年龄年级权重' AFTER weight_values;
        END IF;

        UPDATE t_user_match_weights
        SET weight_ocean = 0.3500,
            weight_interest = 0.2200,
            weight_values = 0.2000,
            weight_age_grade = 0.1000,
            weight_major = 0.0700,
            weight_zodiac = 0.0600,
            action_count = 0;

        IF EXISTS (
            SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user_match_weights' AND COLUMN_NAME = 'weight_mbti'
        ) THEN
            ALTER TABLE t_user_match_weights DROP COLUMN weight_mbti;
        END IF;

        IF EXISTS (
            SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user_match_weights' AND COLUMN_NAME = 'weight_bazi'
        ) THEN
            ALTER TABLE t_user_match_weights DROP COLUMN weight_bazi;
        END IF;

        IF EXISTS (
            SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user_match_weights' AND COLUMN_NAME = 'weight_age'
        ) THEN
            ALTER TABLE t_user_match_weights DROP COLUMN weight_age;
        END IF;
    END IF;
END$$
DELIMITER ;
CALL upgrade_match_weights_v3();
DROP PROCEDURE IF EXISTS upgrade_match_weights_v3;
