-- V29: 将 t_feed_post.ai_tags 从 JSON 改为 TEXT
-- JSON 类型会校验格式，导致逗号分隔的标签字符串无法存入
ALTER TABLE t_feed_post MODIFY COLUMN ai_tags TEXT COMMENT 'AI提取的标签，逗号分隔';
