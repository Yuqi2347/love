-- 聊天系统：邀约临时群聊相关表

-- 聊天群表（与邀约关联，用于公开邀约临时群聊）
CREATE TABLE IF NOT EXISTS t_chat_group (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    invite_id    BIGINT       DEFAULT NULL COMMENT '关联的邀约ID（临时群聊）',
    name         VARCHAR(64)  NOT NULL COMMENT '群名称',
    created_by   BIGINT       NOT NULL COMMENT '创建者用户ID',
    status       VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/ARCHIVED',
    created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_invite (invite_id),
    INDEX idx_created_by (created_by)
) COMMENT '聊天群表';

-- 聊天群成员表
CREATE TABLE IF NOT EXISTS t_chat_group_member (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id   BIGINT       NOT NULL COMMENT '群ID',
    user_id    BIGINT       NOT NULL COMMENT '用户ID',
    join_at    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    is_muted   TINYINT(1)   DEFAULT 0 COMMENT '是否静音',
    UNIQUE KEY uk_group_user (group_id, user_id),
    INDEX idx_group (group_id),
    INDEX idx_user (user_id)
) COMMENT '聊天群成员表';

-- 邀约表新增：群聊ID（公开邀约临时群聊）
ALTER TABLE t_invite
    ADD COLUMN chat_group_id BIGINT DEFAULT NULL COMMENT '关联的临时群聊ID';

