-- V16: 关注备注名 + 评论回复用户ID
-- 2026-03-11

-- 关注关系表增加备注名字段（用户为关注的人设置的备注，优先展示备注名）
ALTER TABLE t_follow
    ADD COLUMN remark VARCHAR(50) DEFAULT NULL COMMENT '备注名' AFTER is_mutual;

-- 评论表增加被回复用户ID（用于显示"回复 @用户名"）
ALTER TABLE t_feed_comment
    ADD COLUMN replied_user_id BIGINT DEFAULT NULL COMMENT '被回复的用户ID' AFTER parent_id;
