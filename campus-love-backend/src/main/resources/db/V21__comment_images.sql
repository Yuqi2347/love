-- 评论支持图片
ALTER TABLE t_feed_comment ADD COLUMN images VARCHAR(1024) NULL COMMENT '评论图片URL，逗号分隔' AFTER content;
