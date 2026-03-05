-- 聊天消息表扩展：群聊支持

ALTER TABLE t_message
    ADD COLUMN group_id BIGINT DEFAULT NULL COMMENT '群聊ID';

CREATE INDEX idx_message_group_id ON t_message(group_id);

