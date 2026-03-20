-- =============================================
-- V39: 评论点赞、帖子置顶
-- =============================================

CREATE TABLE IF NOT EXISTS t_feed_comment_like (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    comment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_comment_user (comment_id, user_id),
    INDEX idx_comment (comment_id)
) COMMENT '评论点赞';

DROP PROCEDURE IF EXISTS v39_add_columns;
DELIMITER $$
CREATE PROCEDURE v39_add_columns()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_feed_comment' AND COLUMN_NAME = 'like_count') THEN
        ALTER TABLE t_feed_comment ADD COLUMN like_count INT DEFAULT 0 COMMENT '点赞数';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_feed_post' AND COLUMN_NAME = 'pinned_at') THEN
        ALTER TABLE t_feed_post ADD COLUMN pinned_at DATETIME DEFAULT NULL COMMENT '置顶时间';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_feed_post' AND COLUMN_NAME = 'pinned_by') THEN
        ALTER TABLE t_feed_post ADD COLUMN pinned_by BIGINT DEFAULT NULL COMMENT '置顶操作人（管理员ID）';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_notification' AND COLUMN_NAME = 'comment_id') THEN
        ALTER TABLE t_notification ADD COLUMN comment_id BIGINT DEFAULT NULL COMMENT '关联的评论ID（评论点赞通知）';
    END IF;
END$$
DELIMITER ;
CALL v39_add_columns();
DROP PROCEDURE IF EXISTS v39_add_columns;
