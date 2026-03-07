-- 邀约参与者退出/踢人理由（被踢时由发起人填写，被踢人可见）
ALTER TABLE t_invite_participant ADD COLUMN left_reason VARCHAR(500) DEFAULT NULL COMMENT '退出理由（被踢时由发起人填写）' AFTER left_at;
