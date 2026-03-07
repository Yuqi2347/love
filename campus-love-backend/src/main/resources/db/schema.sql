-- =============================================
-- 校园交友 App - 数据库完整初始化脚本（最新结构）
-- 新环境首次初始化时只需执行本脚本一次：
--   mysql -uroot -p campus_love < schema.sql
-- 或在客户端：SOURCE /绝对路径/schema.sql;
-- 已有数据库升级请使用增量脚本 V2～V10。
-- =============================================

CREATE DATABASE IF NOT EXISTS campus_love
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE campus_love;

-- 用户基础信息表（含 V1.0.1 等级/活跃度、V2 邀约统计、V8 红点已读时间）
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
    invite_count    INT             DEFAULT 0 COMMENT '发起邀约次数',
    participate_count INT           DEFAULT 0 COMMENT '参与邀约次数',
    credit_score    INT             DEFAULT 100 COMMENT '信用分',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_follower_viewed_at       DATETIME DEFAULT NULL COMMENT '上次查看粉丝列表时间',
    last_feed_activity_viewed_at  DATETIME DEFAULT NULL COMMENT '上次查看朋友圈动态活动时间',
    last_invite_activity_viewed_at DATETIME DEFAULT NULL COMMENT '上次查看邀约活动时间',
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

-- 聊天消息表（含 V4 群聊 group_id）
CREATE TABLE IF NOT EXISTS t_message (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    sender_id       BIGINT          NOT NULL,
    receiver_id     BIGINT          NOT NULL,
    group_id        BIGINT          DEFAULT NULL COMMENT '群聊ID',
    content         TEXT            NOT NULL,
    msg_type        TINYINT         NOT NULL DEFAULT 1 COMMENT '1=文字 2=图片 3=表情',
    is_read         TINYINT(1)      DEFAULT 0,
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation (sender_id, receiver_id),
    INDEX idx_receiver (receiver_id, is_read),
    INDEX idx_message_group_id (group_id),
    INDEX idx_message_created (created_at)
) COMMENT '聊天消息';

-- 朋友圈动态表（含 V1.0.1 post_type / required_level，V11 多媒体字段）
CREATE TABLE IF NOT EXISTS t_feed_post (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL COMMENT '发布者',
    content         TEXT            DEFAULT NULL COMMENT '文字内容',
    post_type       VARCHAR(32)     DEFAULT 'USER' COMMENT '帖子类型：SYSTEM/ADMIN/USER',
    required_level  INT             DEFAULT 1 COMMENT '发布所需等级',
    images          VARCHAR(1024)   DEFAULT NULL COMMENT '图片URL列表，逗号分隔',
    videos          TEXT            DEFAULT NULL COMMENT '视频URL列表，逗号分隔',
    link_url        VARCHAR(500)    DEFAULT NULL COMMENT '链接URL',
    link_title      VARCHAR(200)    DEFAULT NULL COMMENT '链接标题',
    link_image      VARCHAR(500)    DEFAULT NULL COMMENT '链接预览图',
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

-- 活跃度记录表（V1.0.1）
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

-- 邀约主表（V2，含 V3 chat_group_id、V7 索引）
CREATE TABLE IF NOT EXISTS t_invite (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    creator_id      BIGINT          NOT NULL COMMENT '发起人ID',
    invite_type     VARCHAR(32)     NOT NULL COMMENT '邀约类型：DINNER/SPORT/STUDY/DRAMA/OTHER',
    invite_mode     VARCHAR(16)     NOT NULL COMMENT '邀约模式：PUBLIC/PRIVATE',
    target_user_id  BIGINT          DEFAULT NULL COMMENT '一对一邀约目标用户ID',
    title           VARCHAR(64)     NOT NULL COMMENT '标题',
    content         VARCHAR(512)    DEFAULT NULL COMMENT '内容描述',
    invite_period   VARCHAR(16)     DEFAULT 'ONCE' COMMENT '周期：ONCE/WEEKLY/MONTHLY',
    period_config   VARCHAR(128)    DEFAULT NULL COMMENT '周期配置JSON',
    invite_time     DATETIME        NOT NULL COMMENT '邀约时间',
    location        VARCHAR(256)    DEFAULT NULL COMMENT '地点',
    max_participants INT            DEFAULT NULL COMMENT '最大人数',
    participant_count INT           DEFAULT 0 COMMENT '当前参与人数',
    status          VARCHAR(16)     DEFAULT 'RECRUITING' COMMENT '状态',
    deadline_hours  INT             DEFAULT 1 COMMENT '报名截止小时数',
    atmosphere_tags VARCHAR(128)    DEFAULT NULL COMMENT '氛围标签',
    is_urgent       TINYINT(1)      DEFAULT 0 COMMENT '是否急需',
    social_rating   DECIMAL(2,1)    DEFAULT NULL COMMENT '社交体验平均评分',
    org_rating      DECIMAL(2,1)    DEFAULT NULL COMMENT '组织力平均评分',
    rating_count    INT             DEFAULT 0 COMMENT '评价人数',
    deleted         TINYINT(1)      DEFAULT 0,
    chat_group_id   BIGINT          DEFAULT NULL COMMENT '关联的临时群聊ID',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_creator (creator_id),
    INDEX idx_status (status),
    INDEX idx_invite_time (invite_time),
    INDEX idx_invite_type (invite_type),
    INDEX idx_invite_list (creator_id, status, deleted, invite_time)
) COMMENT '邀约主表';

-- 邀约参与者表（V2，含 V7 索引、V9 left_at 退出时间）
CREATE TABLE IF NOT EXISTS t_invite_participant (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    invite_id       BIGINT          NOT NULL COMMENT '邀约ID',
    user_id         BIGINT          NOT NULL COMMENT '参与者ID',
    social_rating   DECIMAL(2,1)    DEFAULT NULL COMMENT '该参与者给发起人的社交体验评分',
    join_at         DATETIME        DEFAULT CURRENT_TIMESTAMP,
    left_at         DATETIME        DEFAULT NULL COMMENT '退出时间',
    UNIQUE KEY uk_invite_user (invite_id, user_id),
    INDEX idx_invite (invite_id),
    INDEX idx_user (user_id),
    INDEX idx_user_invite (user_id, invite_id)
) COMMENT '邀约参与者表';

-- 等待邀约表（V2）
CREATE TABLE IF NOT EXISTS t_invite_wait (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL COMMENT '用户ID',
    invite_types    VARCHAR(128)    NOT NULL COMMENT '邀约类型，逗号分隔',
    period_config   VARCHAR(256)    DEFAULT NULL COMMENT '时间偏好JSON',
    location_pref   VARCHAR(64)     DEFAULT NULL COMMENT '地点偏好',
    auto_accept     TINYINT(1)      DEFAULT 0 COMMENT '是否自动受邀',
    expire_hours    INT             NOT NULL COMMENT '有效时长（小时）',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_created (created_at)
) COMMENT '等待邀约表';

-- 邀约评价表（V2）
CREATE TABLE IF NOT EXISTS t_invite_rating (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    invite_id       BIGINT          NOT NULL COMMENT '邀约ID',
    rater_id        BIGINT          NOT NULL COMMENT '评价人ID',
    rated_user_id   BIGINT          NOT NULL COMMENT '被评价人ID',
    social_rating   DECIMAL(2,1)    NOT NULL COMMENT '社交体验评分 0-5',
    org_rating      DECIMAL(2,1)    DEFAULT NULL COMMENT '组织力评分',
    content         VARCHAR(256)    DEFAULT NULL COMMENT '评价内容',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_rating (invite_id, rater_id, rated_user_id),
    INDEX idx_invite (invite_id),
    INDEX idx_rated_user (rated_user_id)
) COMMENT '邀约评价表';

-- 邀约拒绝记录表（V10：一对一邀约被目标用户拒绝后不再出现在待处理列表）
CREATE TABLE IF NOT EXISTS t_invite_decline (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    invite_id   BIGINT NOT NULL COMMENT '邀约ID',
    user_id     BIGINT NOT NULL COMMENT '拒绝者（目标用户）',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_invite_user (invite_id, user_id),
    INDEX idx_user (user_id)
) COMMENT '邀约拒绝记录';

-- 聊天群表（V3）
CREATE TABLE IF NOT EXISTS t_chat_group (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    invite_id       BIGINT          DEFAULT NULL COMMENT '关联的邀约ID（临时群聊）',
    name            VARCHAR(64)     NOT NULL COMMENT '群名称',
    created_by      BIGINT          NOT NULL COMMENT '创建者用户ID',
    status          VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/ARCHIVED',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_invite (invite_id),
    INDEX idx_created_by (created_by)
) COMMENT '聊天群表';

-- 聊天群成员表（V3）
CREATE TABLE IF NOT EXISTS t_chat_group_member (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id        BIGINT          NOT NULL COMMENT '群ID',
    user_id         BIGINT          NOT NULL COMMENT '用户ID',
    join_at         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    is_muted        TINYINT(1)      DEFAULT 0 COMMENT '是否静音',
    UNIQUE KEY uk_group_user (group_id, user_id),
    INDEX idx_group (group_id),
    INDEX idx_user (user_id)
) COMMENT '聊天群成员表';

-- 站内通知表（V5，含 V7 索引）
CREATE TABLE IF NOT EXISTS t_notification (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL COMMENT '接收通知的用户ID',
    sender_id       BIGINT          DEFAULT NULL COMMENT '触发通知的用户ID',
    invite_id       BIGINT          DEFAULT NULL COMMENT '关联的邀约ID',
    type            VARCHAR(32)     NOT NULL COMMENT '通知类型',
    title           VARCHAR(128)    NOT NULL COMMENT '通知标题',
    content         VARCHAR(512)    DEFAULT NULL COMMENT '通知内容',
    is_read         TINYINT(1)      DEFAULT 0 COMMENT '是否已读',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    read_at         DATETIME        DEFAULT NULL COMMENT '阅读时间',
    INDEX idx_user_read (user_id, is_read),
    INDEX idx_invite (invite_id),
    INDEX idx_notification_list (user_id, is_read, created_at)
) COMMENT '站内通知表';

-- 用户个性化匹配权重表（V6）
CREATE TABLE IF NOT EXISTS t_user_match_weights (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL UNIQUE COMMENT '用户ID',
    weight_interest DECIMAL(5,4)    DEFAULT 0.3000 COMMENT '兴趣权重',
    weight_mbti     DECIMAL(5,4)    DEFAULT 0.2500 COMMENT 'MBTI权重',
    weight_zodiac   DECIMAL(5,4)    DEFAULT 0.1500 COMMENT '星座权重',
    weight_bazi     DECIMAL(5,4)    DEFAULT 0.1500 COMMENT '八字权重',
    weight_major    DECIMAL(5,4)    DEFAULT 0.1000 COMMENT '专业权重',
    weight_age      DECIMAL(5,4)    DEFAULT 0.0500 COMMENT '年龄权重',
    action_count    INT             DEFAULT 0 COMMENT '累计行为次数',
    last_updated    DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '用户个性化匹配权重';

-- 用户匹配行为日志表（V6）
CREATE TABLE IF NOT EXISTS t_user_match_action (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL COMMENT '操作用户',
    target_user_id  BIGINT          NOT NULL COMMENT '目标用户',
    action_type     VARCHAR(20)     NOT NULL COMMENT '行为类型',
    signal_strength INT             NOT NULL COMMENT '信号强度',
    match_score     TINYINT         DEFAULT NULL COMMENT '行为时的综合匹配分',
    detail_snapshot JSON            DEFAULT NULL COMMENT '行为时各维度得分快照',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_action (user_id, created_at),
    INDEX idx_target_user (target_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '用户匹配行为日志';
