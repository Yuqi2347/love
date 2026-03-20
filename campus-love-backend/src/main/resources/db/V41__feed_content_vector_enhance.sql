-- V41: 补充 t_feed_content_vector 缺失列，供推荐算法使用

ALTER TABLE t_feed_content_vector
  ADD COLUMN IF NOT EXISTS user_id      BIGINT       DEFAULT NULL COMMENT '发帖用户 ID',
  ADD COLUMN IF NOT EXISTS ai_tags      TEXT         DEFAULT NULL COMMENT 'AI 标签 JSON 数组',
  ADD COLUMN IF NOT EXISTS primary_category VARCHAR(64) DEFAULT NULL COMMENT '主分类';

-- 若 ADD COLUMN IF NOT EXISTS 语法不支持（MySQL < 8.0），改用存储过程
DROP PROCEDURE IF EXISTS v41_fix_feed_vector;
DELIMITER $$
CREATE PROCEDURE v41_fix_feed_vector()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'T_FEED_CONTENT_VECTOR'
          AND COLUMN_NAME = 'user_id'
    ) THEN
        ALTER TABLE t_feed_content_vector
            ADD COLUMN user_id BIGINT DEFAULT NULL,
            ADD COLUMN ai_tags TEXT DEFAULT NULL,
            ADD COLUMN primary_category VARCHAR(64) DEFAULT NULL;
    END IF;
END$$
DELIMITER ;
CALL v41_fix_feed_vector();
DROP PROCEDURE IF EXISTS v41_fix_feed_vector;
