-- 破冰功能按好友单独设置（V26）
-- user_id: 我，allowed_user_id: 我允许使用破冰的好友
CREATE TABLE IF NOT EXISTS t_user_ice_break_allow (
    user_id         BIGINT NOT NULL COMMENT '用户ID（我）',
    allowed_user_id BIGINT NOT NULL COMMENT '允许使用破冰的好友ID',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, allowed_user_id),
    INDEX idx_user (user_id)
) COMMENT '破冰功能按好友授权（互关后可单独允许某人使用）';
