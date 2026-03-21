-- 动态引用邀约（站内卡片）；原 link_* 字段保留历史数据，新帖不再写入
ALTER TABLE t_feed_post
    ADD COLUMN invite_id BIGINT NULL COMMENT '引用的邀约 ID' AFTER link_image;

CREATE INDEX idx_feed_invite ON t_feed_post (invite_id);

ALTER TABLE t_feed_post
    ADD CONSTRAINT fk_feed_invite FOREIGN KEY (invite_id) REFERENCES t_invite (id) ON DELETE SET NULL;
