-- V17: 心动双池、消息软删、评论软删、帖子独立可见性、举报表

-- 1. 心动时刻：允许同一用户同一周报名多个池
ALTER TABLE t_moment_enrollment DROP INDEX uk_user_week;
ALTER TABLE t_moment_enrollment ADD UNIQUE KEY uk_user_week_pool (user_id, week_tag, pool);

-- 2. 消息软删除
ALTER TABLE t_message ADD COLUMN deleted TINYINT(1) DEFAULT 0 COMMENT '0=正常 1=已撤回';

-- 3. 评论软删除
ALTER TABLE t_feed_comment ADD COLUMN deleted TINYINT(1) DEFAULT 0 COMMENT '0=正常 1=已删除';

-- 4. 帖子独立可见性（替代 t_user.feed_visibility 全局设置）
ALTER TABLE t_feed_post ADD COLUMN visibility VARCHAR(16) DEFAULT 'ALL'
  COMMENT '可见性：ALL/FOLLOWERS/FRIENDS/SELF' AFTER comment_count;

-- 5. 举报表
CREATE TABLE IF NOT EXISTS t_report (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    reporter_id   BIGINT NOT NULL COMMENT '举报人',
    target_type   VARCHAR(16) NOT NULL COMMENT 'POST/COMMENT/USER/MESSAGE',
    target_id     BIGINT NOT NULL COMMENT '目标ID',
    reason        VARCHAR(500) NOT NULL COMMENT '举报理由',
    status        VARCHAR(16) DEFAULT 'PENDING' COMMENT 'PENDING/REVIEWED/RESOLVED',
    admin_note    VARCHAR(500) DEFAULT NULL COMMENT '管理员备注',
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    reviewed_at   DATETIME DEFAULT NULL,
    INDEX idx_reporter (reporter_id),
    INDEX idx_target (target_type, target_id),
    INDEX idx_status (status)
) COMMENT '举报记录表';
