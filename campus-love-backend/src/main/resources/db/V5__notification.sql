-- 站内通知表：用于邀约相关的报名成功、有人加入、取消等事件通知
CREATE TABLE IF NOT EXISTS t_notification (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL COMMENT '接收通知的用户ID',
    sender_id   BIGINT                DEFAULT NULL COMMENT '触发通知的用户ID（如发起人/参与者）',
    invite_id   BIGINT                DEFAULT NULL COMMENT '关联的邀约ID',
    type        VARCHAR(32)  NOT NULL COMMENT '通知类型：INVITE_JOIN_SUCCESS/INVITE_NEW_PARTICIPANT/INVITE_CANCELLED 等',
    title       VARCHAR(128) NOT NULL COMMENT '通知标题',
    content     VARCHAR(512)          DEFAULT NULL COMMENT '通知内容',
    is_read     TINYINT(1)            DEFAULT 0 COMMENT '是否已读：0 未读，1 已读',
    created_at  DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    read_at     DATETIME              DEFAULT NULL COMMENT '阅读时间',
    INDEX idx_user_read (user_id, is_read),
    INDEX idx_invite (invite_id)
) COMMENT '站内通知表';

