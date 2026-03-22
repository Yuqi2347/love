-- 心动协商完成后关联系统生成的一对一邀约 t_invite.id

ALTER TABLE t_pair_date_negotiation
    ADD COLUMN pair_invite_id BIGINT DEFAULT NULL COMMENT '协商完成后生成的一对一邀约 t_invite.id' AFTER decider_reason_key;

CREATE INDEX idx_pair_invite ON t_pair_date_negotiation (pair_invite_id);
