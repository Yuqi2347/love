-- 更新帖子类型，将旧数据设置为 TIMELINE（朋友圈）
-- 因为发现模块是新增功能，之前的帖子应该都是朋友圈类型
UPDATE t_feed_post SET post_type = 'TIMELINE' WHERE post_type IS NULL OR post_type = 'USER';

-- 验证更新结果
SELECT post_type, COUNT(*) as count FROM t_feed_post GROUP BY post_type;
