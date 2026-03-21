-- 参与者退出改为软标记，用于「我的邀约」显示「已退出」状态
ALTER TABLE t_invite_participant ADD COLUMN left_at DATETIME DEFAULT NULL COMMENT '退出时间' AFTER join_at;
