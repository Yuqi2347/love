-- 邀约结束时间（可选）
ALTER TABLE t_invite ADD COLUMN invite_end_time DATETIME NULL COMMENT '邀约结束时间（可选）' AFTER invite_time;
