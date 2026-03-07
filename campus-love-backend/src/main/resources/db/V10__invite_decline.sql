-- 邀约拒绝记录（一对一邀约被目标用户拒绝后不再出现在「待处理」列表）
-- Version: V10

CREATE TABLE IF NOT EXISTS t_invite_decline (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    invite_id   BIGINT NOT NULL COMMENT '邀约ID',
    user_id     BIGINT NOT NULL COMMENT '拒绝者（目标用户）',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_invite_user (invite_id, user_id),
    INDEX idx_user (user_id)
) COMMENT '邀约拒绝记录';
