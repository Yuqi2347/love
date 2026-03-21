-- V41: 补充 t_feed_content_vector 缺失列，供推荐算法使用
-- 注意：不使用 ADD COLUMN IF NOT EXISTS（MySQL 8.0 多数版本不支持该语法），改用存储过程幂等添加。
-- 新环境若 V24 已建表且含这些列，本脚本各分支不会执行。

DROP PROCEDURE IF EXISTS v41_fix_feed_vector;
DELIMITER $$
CREATE PROCEDURE v41_fix_feed_vector()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 't_feed_content_vector'
          AND COLUMN_NAME = 'user_id'
    ) THEN
        ALTER TABLE t_feed_content_vector
            ADD COLUMN user_id BIGINT DEFAULT NULL COMMENT '发帖用户 ID';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 't_feed_content_vector'
          AND COLUMN_NAME = 'ai_tags'
    ) THEN
        ALTER TABLE t_feed_content_vector
            ADD COLUMN ai_tags TEXT DEFAULT NULL COMMENT 'AI 标签 JSON 数组';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 't_feed_content_vector'
          AND COLUMN_NAME = 'primary_category'
    ) THEN
        ALTER TABLE t_feed_content_vector
            ADD COLUMN primary_category VARCHAR(64) DEFAULT NULL COMMENT '主分类';
    END IF;
END$$
DELIMITER ;
CALL v41_fix_feed_vector();
DROP PROCEDURE IF EXISTS v41_fix_feed_vector;
