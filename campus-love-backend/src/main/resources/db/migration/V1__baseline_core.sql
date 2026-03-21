-- =============================================
-- V1: 空库基线（供 V2～V46 增量脚本使用）
-- 内容对应 schema.sql 中「早于邀约模块」的核心表，且已剔除 V2 及之后脚本会通过 ALTER 添加的列，
-- 避免与增量脚本重复执行冲突。
-- 说明：全量结构快照见 db/schema.sql（仅供参考/比对，新环境请依赖 Flyway）。
-- =============================================

-- 用户基础信息（不含 V2 邀约统计、V8 红点、V13 心动、V15 可见性、V19/V20/V24/V30/V34/V44 等增量列）
CREATE TABLE IF NOT EXISTS t_user (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    email           VARCHAR(128)    NOT NULL COMMENT '校园邮箱',
    password        VARCHAR(256)    NOT NULL COMMENT '加密密码',
    nickname        VARCHAR(32)     NOT NULL COMMENT '昵称',
    gender          TINYINT         NOT NULL DEFAULT 0 COMMENT '0=未知 1=男 2=女',
    birth_date      DATE            DEFAULT NULL COMMENT '生日',
    birth_time      TIME            DEFAULT NULL COMMENT '出生时间（用于八字计算）',
    school          VARCHAR(64)     DEFAULT NULL COMMENT '学校',
    major           VARCHAR(64)     DEFAULT NULL COMMENT '专业',
    grade           VARCHAR(16)     DEFAULT NULL COMMENT '年级',
    activity_score  INT             DEFAULT 0 COMMENT '活跃度积分',
    user_level      INT             DEFAULT 1 COMMENT '用户等级',
    is_admin        TINYINT(1)      DEFAULT 0 COMMENT '是否管理员',
    mbti            VARCHAR(8)      DEFAULT NULL COMMENT 'MBTI类型',
    zodiac          VARCHAR(16)     DEFAULT NULL COMMENT '星座（自动计算）',
    bazi            VARCHAR(64)     DEFAULT NULL COMMENT '八字信息',
    avatar_url      VARCHAR(256)    DEFAULT NULL COMMENT '头像URL',
    bio             VARCHAR(256)    DEFAULT NULL COMMENT '个人简介',
    interests       VARCHAR(512)    DEFAULT NULL COMMENT '兴趣标签，逗号分隔',
    profile_complete TINYINT(1)     DEFAULT 0 COMMENT '资料是否完善',
    status          TINYINT         DEFAULT 1 COMMENT '1=正常 0=禁用',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_email (email)
) COMMENT '用户基础信息';

-- 关注（不含 V16 remark）
CREATE TABLE IF NOT EXISTS t_follow (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    follower_id     BIGINT          NOT NULL COMMENT '关注者',
    following_id    BIGINT          NOT NULL COMMENT '被关注者',
    is_mutual       TINYINT(1)      DEFAULT 0 COMMENT '是否互相关注',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_follow (follower_id, following_id),
    INDEX idx_following (following_id)
) COMMENT '关注关系';

-- 聊天消息（不含 V4 group_id、V17 deleted）
CREATE TABLE IF NOT EXISTS t_message (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    sender_id       BIGINT          NOT NULL,
    receiver_id     BIGINT          NOT NULL,
    content         TEXT            NOT NULL,
    msg_type        TINYINT         NOT NULL DEFAULT 1 COMMENT '1=文字 2=图片 3=表情',
    is_read         TINYINT(1)      DEFAULT 0,
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation (sender_id, receiver_id),
    INDEX idx_receiver (receiver_id, is_read)
) COMMENT '聊天消息';

-- 朋友圈动态（不含 V11 多媒体与链接、V17 visibility、V24 AI 字段、V39 置顶、V46 invite_id）
CREATE TABLE IF NOT EXISTS t_feed_post (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL COMMENT '发布者',
    content         TEXT            DEFAULT NULL COMMENT '文字内容',
    post_type       VARCHAR(32)     DEFAULT 'USER' COMMENT '帖子类型：SYSTEM/ADMIN/USER',
    required_level  INT             DEFAULT 1 COMMENT '发布所需等级',
    images          VARCHAR(1024)   DEFAULT NULL COMMENT '图片URL列表，逗号分隔',
    like_count      INT             DEFAULT 0 COMMENT '点赞数',
    comment_count   INT             DEFAULT 0 COMMENT '评论数',
    deleted         TINYINT(1)      DEFAULT 0,
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_created (created_at)
) COMMENT '朋友圈动态';

CREATE TABLE IF NOT EXISTS t_feed_like (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id         BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_like (post_id, user_id)
) COMMENT '朋友圈点赞';

-- 评论（不含 V16 replied_user_id、V17 deleted、V21 images、V39 like_count）
CREATE TABLE IF NOT EXISTS t_feed_comment (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id         BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    content         VARCHAR(512)    NOT NULL,
    parent_id       BIGINT          DEFAULT NULL COMMENT '父评论ID，用于回复',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_post (post_id)
) COMMENT '朋友圈评论';

-- t_feed_comment_like 由 V39 创建（IF NOT EXISTS）

CREATE TABLE IF NOT EXISTS t_user_album (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL,
    image_url       VARCHAR(256)    NOT NULL,
    sort_order      INT             DEFAULT 0,
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id)
) COMMENT '用户相册';

CREATE TABLE IF NOT EXISTS t_activity_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL COMMENT '用户ID',
    activity_type   VARCHAR(32)     NOT NULL COMMENT '活动类型',
    target_id       BIGINT          DEFAULT NULL COMMENT '目标ID',
    score           INT             NOT NULL COMMENT '获得的活跃度积分',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_type (activity_type),
    INDEX idx_created (created_at)
) COMMENT '活跃度记录表';
