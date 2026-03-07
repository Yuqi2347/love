-- 红点/角标“已查看”时间：用于新粉丝、朋友圈新点赞评论、邀约新动态的消除
ALTER TABLE t_user ADD COLUMN last_follower_viewed_at DATETIME DEFAULT NULL COMMENT '上次查看粉丝列表时间' AFTER updated_at;
ALTER TABLE t_user ADD COLUMN last_feed_activity_viewed_at DATETIME DEFAULT NULL COMMENT '上次查看朋友圈动态活动时间' AFTER last_follower_viewed_at;
ALTER TABLE t_user ADD COLUMN last_invite_activity_viewed_at DATETIME DEFAULT NULL COMMENT '上次查看邀约活动时间' AFTER last_feed_activity_viewed_at;
