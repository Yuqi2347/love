-- 列表/批量查询：WHERE user_id = ? AND post_id IN (...) / comment_id IN (...)
-- 与 schema.sql 中 idx_user_post、idx_user_comment 一致；已存在则跳过（避免重复执行失败）

DROP PROCEDURE IF EXISTS add_feed_like_batch_indexes;
DELIMITER $$
CREATE PROCEDURE add_feed_like_batch_indexes()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_feed_like' AND INDEX_NAME = 'idx_user_post'
    ) THEN
        ALTER TABLE t_feed_like ADD INDEX idx_user_post (user_id, post_id);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_feed_comment_like' AND INDEX_NAME = 'idx_user_comment'
    ) THEN
        ALTER TABLE t_feed_comment_like ADD INDEX idx_user_comment (user_id, comment_id);
    END IF;
END$$
DELIMITER ;
CALL add_feed_like_batch_indexes();
DROP PROCEDURE IF EXISTS add_feed_like_batch_indexes;
