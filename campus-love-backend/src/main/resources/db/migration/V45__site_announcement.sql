-- 全站公告（管理端发布，登录用户可见；用户已读记录在 t_user_announcement_read）
CREATE TABLE IF NOT EXISTS t_site_announcement (
    id           BIGINT       NOT NULL PRIMARY KEY AUTO_INCREMENT,
    title        VARCHAR(255) NOT NULL COMMENT '标题',
    content      TEXT         NOT NULL COMMENT '正文（纯文本/Markdown 由前端约定）',
    status       VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT / PUBLISHED',
    valid_from   DATETIME     NOT NULL COMMENT '生效时间',
    valid_until  DATETIME     NOT NULL COMMENT '截止时间',
    published_at DATETIME              DEFAULT NULL COMMENT '发布时间',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status_valid (status, valid_from, valid_until),
    INDEX idx_published (published_at)
) COMMENT '全站公告';

CREATE TABLE IF NOT EXISTS t_user_announcement_read (
    id               BIGINT   NOT NULL PRIMARY KEY AUTO_INCREMENT,
    user_id          BIGINT   NOT NULL COMMENT '用户 ID',
    announcement_id  BIGINT   NOT NULL COMMENT '公告 ID',
    read_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_ann (user_id, announcement_id),
    INDEX idx_ann (announcement_id),
    CONSTRAINT fk_uar_announcement FOREIGN KEY (announcement_id) REFERENCES t_site_announcement (id) ON DELETE CASCADE
) COMMENT '用户公告已读（关闭浮窗时批量写入）';
