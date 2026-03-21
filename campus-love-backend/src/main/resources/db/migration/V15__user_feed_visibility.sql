-- 朋友圈展示设置：所有人可见/粉丝可见/仅自己可见
ALTER TABLE t_user ADD COLUMN feed_visibility VARCHAR(16) DEFAULT 'ALL' COMMENT '朋友圈可见性：ALL=所有人可见，FOLLOWERS=粉丝可见，SELF=仅自己可见' AFTER last_invite_activity_viewed_at;
