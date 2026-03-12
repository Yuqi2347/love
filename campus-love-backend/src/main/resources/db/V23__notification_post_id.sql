-- 通知表增加 post_id 字段，用于动态评论/回复类通知的跳转
ALTER TABLE t_notification ADD COLUMN post_id BIGINT DEFAULT NULL COMMENT '关联的动态ID（评论/回复通知）' AFTER invite_id;
CREATE INDEX idx_notification_post ON t_notification (post_id);
