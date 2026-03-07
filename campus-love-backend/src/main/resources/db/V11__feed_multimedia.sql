-- 朋友圈多媒体支持：视频和链接
-- Version: V11

ALTER TABLE t_feed_post ADD COLUMN videos TEXT COMMENT '视频URL列表，逗号分隔' AFTER images;
ALTER TABLE t_feed_post ADD COLUMN link_url VARCHAR(500) COMMENT '链接URL' AFTER videos;
ALTER TABLE t_feed_post ADD COLUMN link_title VARCHAR(200) COMMENT '链接标题' AFTER link_url;
ALTER TABLE t_feed_post ADD COLUMN link_image VARCHAR(500) COMMENT '链接预览图' AFTER link_title;
