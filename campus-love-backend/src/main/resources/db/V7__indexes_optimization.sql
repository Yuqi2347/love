-- 查询与列表常用索引优化（邀约、通知、消息等）
-- 执行前请确认表已存在；若索引已存在会报错，可忽略或先检查 information_schema。

-- 邀约列表/统计：按发起人、状态、未删除、时间
ALTER TABLE t_invite ADD INDEX idx_invite_list (creator_id, status, deleted, invite_time);

-- 邀约参与者：按用户查参与中的邀约
ALTER TABLE t_invite_participant ADD INDEX idx_user_invite (user_id, invite_id);

-- 通知列表：按用户、已读、创建时间倒序
ALTER TABLE t_notification ADD INDEX idx_notification_list (user_id, is_read, created_at);

-- 消息：单聊/群聊会话与分页（若 t_message 已有 group_id）
-- ALTER TABLE t_message ADD INDEX idx_message_conversation (sender_id, receiver_id, group_id, created_at);
-- 若表结构无 group_id 则仅加 created_at 用于排序
ALTER TABLE t_message ADD INDEX idx_message_created (created_at);
