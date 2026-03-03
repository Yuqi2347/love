-- =============================================
-- 校园交友 App MVP - 数据库初始化脚本
-- =============================================

CREATE DATABASE IF NOT EXISTS campus_love
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE campus_love;

-- 用户基础信息表
CREATE TABLE IF NOT EXISTS t_user (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    email           VARCHAR(128)    NOT NULL COMMENT '校园邮箱',
    password        VARCHAR(256)    NOT NULL COMMENT '加密密码',
    nickname        VARCHAR(32)     NOT NULL COMMENT '昵称',
    gender          TINYINT         NOT NULL DEFAULT 0 COMMENT '0=未知 1=男 2=女',
    birth_date      DATE            DEFAULT NULL COMMENT '生日',
    school          VARCHAR(64)     DEFAULT NULL COMMENT '学校',
    major           VARCHAR(64)     DEFAULT NULL COMMENT '专业',
    grade           VARCHAR(16)     DEFAULT NULL COMMENT '年级',
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

-- 关注关系表
CREATE TABLE IF NOT EXISTS t_follow (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    follower_id     BIGINT          NOT NULL COMMENT '关注者',
    following_id    BIGINT          NOT NULL COMMENT '被关注者',
    is_mutual       TINYINT(1)      DEFAULT 0 COMMENT '是否互相关注',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_follow (follower_id, following_id),
    INDEX idx_following (following_id)
) COMMENT '关注关系';

-- 聊天消息表
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

-- 朋友圈动态表
CREATE TABLE IF NOT EXISTS t_feed_post (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL COMMENT '发布者',
    content         TEXT            DEFAULT NULL COMMENT '文字内容',
    images          VARCHAR(1024)   DEFAULT NULL COMMENT '图片URL列表，逗号分隔',
    like_count      INT             DEFAULT 0 COMMENT '点赞数',
    comment_count   INT             DEFAULT 0 COMMENT '评论数',
    deleted         TINYINT(1)      DEFAULT 0,
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_created (created_at)
) COMMENT '朋友圈动态';

-- 朋友圈点赞表
CREATE TABLE IF NOT EXISTS t_feed_like (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id         BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_like (post_id, user_id)
) COMMENT '朋友圈点赞';

-- 朋友圈评论表
CREATE TABLE IF NOT EXISTS t_feed_comment (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id         BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    content         VARCHAR(512)    NOT NULL,
    parent_id       BIGINT          DEFAULT NULL COMMENT '父评论ID，用于回复',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_post (post_id)
) COMMENT '朋友圈评论';

-- 用户相册表
CREATE TABLE IF NOT EXISTS t_user_album (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL,
    image_url       VARCHAR(256)    NOT NULL,
    sort_order      INT             DEFAULT 0,
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id)
) COMMENT '用户相册';
