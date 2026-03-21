-- V40: user avatar BLOB table + fix ocean_confidence column

-- 用户头像 BLOB 独立存储表
CREATE TABLE IF NOT EXISTS t_user_avatar (
    user_id      BIGINT PRIMARY KEY,
    avatar_data  MEDIUMBLOB NOT NULL,
    content_type VARCHAR(64) DEFAULT 'image/jpeg',
    file_size    INT,
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_updated (updated_at)
);

-- AI 画像：补充 ocean_confidence 列（各维度置信区间，JSON）
DROP PROCEDURE IF EXISTS v40_fix_columns;
DELIMITER $$
CREATE PROCEDURE v40_fix_columns()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 't_user_ai_profile'
          AND COLUMN_NAME = 'ocean_confidence'
    ) THEN
        ALTER TABLE t_user_ai_profile ADD COLUMN ocean_confidence JSON DEFAULT NULL;
    END IF;
END$$
DELIMITER ;
CALL v40_fix_columns();
DROP PROCEDURE IF EXISTS v40_fix_columns;
