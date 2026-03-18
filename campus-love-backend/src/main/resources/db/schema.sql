-- =============================================
-- 校园交友 App - 数据库完整初始化脚本（最新结构）
-- 新环境首次初始化时只需执行本脚本一次：
--   mysql -uroot -p campus_love < schema.sql
-- 或在客户端：SOURCE /绝对路径/schema.sql;
-- 已有数据库升级请使用增量脚本 V2～V24。
-- =============================================

CREATE DATABASE IF NOT EXISTS campus_love
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE campus_love;

-- 用户基础信息表（含 V1.0.1 等级/活跃度、V2 邀约统计、V8 红点已读时间、V13 心动一刻字段）
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
    feed_visibility               VARCHAR(16) DEFAULT 'ALL' COMMENT '朋友圈可见性：ALL=所有人可见，FOLLOWERS=粉丝可见，SELF=仅自己可见（V15）',
    moment_photo_url              VARCHAR(256) DEFAULT NULL COMMENT '心动一刻照片URL（V13）',
    moment_self_score             TINYINT      DEFAULT NULL COMMENT '自评颜值分1-10（V13）',
    moment_banned                 TINYINT(1)   DEFAULT 0   COMMENT '是否被禁止参加心动一刻（V13）',
    moment_priority_count         INT          DEFAULT 0   COMMENT '心动时刻连续未匹配优先权计数（V34）',
    bazi_unknown                   TINYINT(1)   DEFAULT 0   COMMENT '生辰时辰是否不知道（V24）',
    ice_break_enabled              TINYINT(1)   DEFAULT 0   COMMENT '是否开启破冰功能（V24）',
    ai_disclosure_settings         JSON         DEFAULT (JSON_OBJECT('mbti', true, 'zodiac', true, 'majorCategory', true, 'interestTags', true, 'naturalLangTags', false, 'baziInfo', false, 'questionnaireHints', false)) COMMENT 'AI信息公开授权（V24）',
    UNIQUE KEY uk_email (email)
) COMMENT '用户基础信息';

-- 关注关系表（含 V16 备注名）
CREATE TABLE IF NOT EXISTS t_follow (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    follower_id     BIGINT          NOT NULL COMMENT '关注者',
    following_id    BIGINT          NOT NULL COMMENT '被关注者',
    is_mutual       TINYINT(1)      DEFAULT 0 COMMENT '是否互相关注',
    remark          VARCHAR(50)     DEFAULT NULL COMMENT '备注名（V16）',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_follow (follower_id, following_id),
    INDEX idx_following (following_id)
) COMMENT '关注关系';

-- 聊天消息表（含 V4 群聊 group_id，V17 软删除）
CREATE TABLE IF NOT EXISTS t_message (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    sender_id       BIGINT          NOT NULL,
    receiver_id     BIGINT          NOT NULL,
    group_id        BIGINT          DEFAULT NULL COMMENT '群聊ID',
    content         TEXT            NOT NULL,
    msg_type        TINYINT         NOT NULL DEFAULT 1 COMMENT '1=文字 2=图片 3=表情',
    is_read         TINYINT(1)      DEFAULT 0,
    deleted         TINYINT(1)      DEFAULT 0 COMMENT '0=正常 1=已撤回（V17）',
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
    visibility      VARCHAR(16)     DEFAULT 'ALL' COMMENT '可见性：ALL/FOLLOWERS/FRIENDS/SELF（V17）',
    ai_tags         TEXT            COMMENT 'AI提取的标签，逗号分隔（V24/V29 由JSON改为TEXT）',
    primary_category VARCHAR(20)    COMMENT '主要兴趣分类（V24）',
    tag_sentiment   VARCHAR(10)     COMMENT 'positive/neutral/negative（V24）',
    ocean_hints     JSON            COMMENT 'AI推断的OCEAN信号（V24）',
    tag_confidence  DECIMAL(3,2)    COMMENT '标签置信权重（V24）',
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

-- 朋友圈评论表（含 V16 被回复用户ID，V17 软删除）
CREATE TABLE IF NOT EXISTS t_feed_comment (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id         BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    content         VARCHAR(512)    NOT NULL,
    parent_id       BIGINT          DEFAULT NULL COMMENT '父评论ID，用于回复',
    replied_user_id BIGINT          DEFAULT NULL COMMENT '被回复的用户ID（V16）',
    deleted         TINYINT(1)      DEFAULT 0 COMMENT '0=正常 1=已删除（V17）',
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

-- 邀约参与者表（V2，含 V7 索引、V9 left_at、V14 left_reason）
CREATE TABLE IF NOT EXISTS t_invite_participant (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    invite_id       BIGINT          NOT NULL COMMENT '邀约ID',
    user_id         BIGINT          NOT NULL COMMENT '参与者ID',
    social_rating   DECIMAL(2,1)    DEFAULT NULL COMMENT '该参与者给发起人的社交体验评分',
    join_at         DATETIME        DEFAULT CURRENT_TIMESTAMP,
    left_at         DATETIME        DEFAULT NULL COMMENT '退出时间',
    left_reason     VARCHAR(500)    DEFAULT NULL COMMENT '退出理由（被踢时由发起人填写，V14）',
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
    weight_ocean    DECIMAL(5,4)    DEFAULT 0.3500 COMMENT 'OCEAN权重',
    weight_interest DECIMAL(5,4)    DEFAULT 0.2200 COMMENT '兴趣权重',
    weight_values   DECIMAL(5,4)    DEFAULT 0.2000 COMMENT '价值观权重',
    weight_age_grade DECIMAL(5,4)   DEFAULT 0.1000 COMMENT '年龄年级权重',
    weight_major    DECIMAL(5,4)    DEFAULT 0.0700 COMMENT '专业权重',
    weight_zodiac   DECIMAL(5,4)    DEFAULT 0.0600 COMMENT '星座权重',
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

-- 缘分解析调用记录表（V12）
CREATE TABLE IF NOT EXISTS t_yuanfen_analysis_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id_a       BIGINT          NOT NULL COMMENT '较小的用户ID（保证对称性）',
    user_id_b       BIGINT          NOT NULL COMMENT '较大的用户ID',
    total_score     TINYINT         COMMENT '触发时综合匹配分',
    ai_result       JSON            COMMENT 'AI返回结果快照',
    tokens_used     INT             COMMENT 'Token消耗量（成本统计）',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_users (user_id_a, user_id_b),
    INDEX idx_created (created_at)
) COMMENT '缘分解析调用记录';

-- 心动一刻模块（V13）
CREATE TABLE IF NOT EXISTS t_moment_profile (
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id                 BIGINT       NOT NULL UNIQUE COMMENT '用户ID（一人一份档案）',
    target_gender           VARCHAR(10)  NOT NULL COMMENT '期望匹配性别: male/female/any',
    appearance_score        DECIMAL(3,1) DEFAULT NULL COMMENT '1.1 颜值自评',
    social_style            CHAR(1)      DEFAULT NULL COMMENT '1.2 社交风格',
    life_rhythm             CHAR(1)      DEFAULT NULL COMMENT '1.3 生活节奏',
    personality_base        CHAR(1)      DEFAULT NULL COMMENT '1.4 性格底色',
    campus_focus            CHAR(1)      DEFAULT NULL COMMENT '1.5 校园重心',
    emotion_style           CHAR(1)      DEFAULT NULL COMMENT '1.6 情绪表达',
    appearance_requirement  CHAR(1)      DEFAULT NULL COMMENT '2.2 颜值要求',
    age_range_preference    VARCHAR(20)  DEFAULT NULL COMMENT '2.3 年龄偏好（兼容旧版多选）',
    age_preference_min      INT          DEFAULT -10 COMMENT '年龄偏好下限 -10~10',
    age_preference_max      INT          DEFAULT 10 COMMENT '年龄偏好上限',
    grade_range_preference  CHAR(1)      DEFAULT NULL COMMENT '2.4 年级偏好',
    grade_range_min         INT          DEFAULT 1 COMMENT '年级范围下限 1~11',
    grade_range_max         INT          DEFAULT 11 COMMENT '年级范围上限',
    prioritize_matching     TINYINT(1)   DEFAULT 0 COMMENT '是否开启优先匹配',
    partner_personality     CHAR(1)      DEFAULT NULL COMMENT '2.5 对方性格偏好',
    major_preference        CHAR(1)      DEFAULT NULL COMMENT '2.6 专业偏好',
    career_ambition_pref    CHAR(1)      DEFAULT NULL COMMENT '2.7 事业心偏好',
    companionship_style     CHAR(1)      DEFAULT NULL COMMENT '2.8 陪伴方式',
    date_style              CHAR(1)      DEFAULT NULL COMMENT '2.9 约会方式',
    intimacy_pace           CHAR(1)      DEFAULT NULL COMMENT '2.10 亲密节奏',
    honesty_level           CHAR(1)      DEFAULT NULL COMMENT '3.1 坦诚度',
    premarital_cohabitation CHAR(1)      DEFAULT NULL COMMENT '3.2 婚前同居',
    premarital_sex          CHAR(1)      DEFAULT NULL COMMENT '3.3 婚前性行为',
    relationship_core_value CHAR(1)      DEFAULT NULL COMMENT '3.4 关系核心价值',
    conflict_style          CHAR(1)      DEFAULT NULL COMMENT '3.5 冲突解决',
    social_boundary         CHAR(1)      DEFAULT NULL COMMENT '3.6 社交边界',
    future_lifestyle        CHAR(1)      DEFAULT NULL COMMENT '3.7 未来生活方式',
    campus_love_plan        CHAR(1)      DEFAULT NULL COMMENT '3.8 校园恋爱规划',
    idol_role               CHAR(1)      DEFAULT NULL COMMENT '3.9 偶像角色认知',
    temptation_response     CHAR(1)      DEFAULT NULL COMMENT '3.10 面对诱惑',
    reality_condition       CHAR(1)      DEFAULT NULL COMMENT '3.11 现实条件',
    human_nature_view       CHAR(1)      DEFAULT NULL COMMENT '3.12 人性观',
    breakup_view            CHAR(1)      DEFAULT NULL COMMENT '3.13 分手观',
    career_love_conflict    CHAR(1)      DEFAULT NULL COMMENT '3.14 事业爱情冲突',
    emotion_priority        CHAR(1)      DEFAULT NULL COMMENT '3.15 情感排序',
    life_goal_priority      CHAR(1)      DEFAULT NULL COMMENT '3.16 人生目标优先级',
    created_at              DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) COMMENT '心动一刻用户档案（问卷答案）';

CREATE TABLE IF NOT EXISTS t_moment_enrollment (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    week_tag    VARCHAR(10)  NOT NULL COMMENT '活动周标识，如 2026-W10',
    pool        VARCHAR(4)   NOT NULL COMMENT '匹配池: MF/MM/FF',
    status      VARCHAR(20)  DEFAULT 'WAITING' COMMENT 'WAITING/MATCHED/UNMATCHED',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_week_pool (user_id, week_tag, pool)
) COMMENT '心动一刻每周报名记录（V17 支持同周多池）';

CREATE TABLE IF NOT EXISTS t_moment_match_result (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    week_tag    VARCHAR(10)  NOT NULL COMMENT '活动周标识',
    pool        VARCHAR(4)   NOT NULL COMMENT '匹配池: MF/MM/FF',
    user_id_a   BIGINT       NOT NULL,
    user_id_b   BIGINT       NOT NULL,
    total_score DECIMAL(5,2) COMMENT '配对综合分',
    score_detail JSON        COMMENT '四维度分数明细',
    yuanfen_title VARCHAR(32) DEFAULT NULL COMMENT '缘分标题',
    complementary_modes JSON DEFAULT NULL COMMENT '命中的互补模式列表',
    soft_penalty_reasons JSON DEFAULT NULL COMMENT '软惩罚触发原因列表',
    date_scene_type VARCHAR(20) DEFAULT NULL COMMENT '约会场景类型',
    insight_card_1 TEXT DEFAULT NULL COMMENT '心动之处卡片一',
    insight_card_2 TEXT DEFAULT NULL COMMENT '心动之处卡片二',
    insight_card_3 TEXT DEFAULT NULL COMMENT '心动之处卡片三',
    golden_sentence VARCHAR(128) DEFAULT NULL COMMENT '专属金句',
    dimension_labels JSON DEFAULT NULL COMMENT '四维度标签',
    about_user_a TEXT DEFAULT NULL COMMENT '展示给B看的A画像',
    about_user_b TEXT DEFAULT NULL COMMENT '展示给A看的B画像',
    date_prep_json JSON DEFAULT NULL COMMENT '约会准备内容',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_week (week_tag),
    INDEX idx_user_a (user_id_a),
    INDEX idx_user_b (user_id_b)
) COMMENT '心动一刻每周配对结果';

CREATE TABLE IF NOT EXISTS t_moment_match_confirm (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    match_result_id BIGINT NOT NULL COMMENT '关联匹配结果',
    user_id_a       BIGINT NOT NULL,
    user_id_b       BIGINT NOT NULL,
    choice_a        VARCHAR(20) DEFAULT NULL COMMENT 'A的选择: YUE/GUANZHU',
    choice_b        VARCHAR(20) DEFAULT NULL COMMENT 'B的选择: YUE/GUANZHU',
    choice_a_at     DATETIME DEFAULT NULL COMMENT 'A选择时间',
    choice_b_at     DATETIME DEFAULT NULL COMMENT 'B选择时间',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_match_result_id (match_result_id),
    INDEX idx_user_pair (user_id_a, user_id_b)
) COMMENT '心动一刻配对确认记录';

CREATE TABLE IF NOT EXISTS t_moment_match_config (
    id                  BIGINT PRIMARY KEY,
    base_threshold      INT NOT NULL DEFAULT 60 COMMENT '基础阈值',
    prioritize_offset   INT NOT NULL DEFAULT 10 COMMENT '优先匹配阈值减免',
    priority_offset     INT NOT NULL DEFAULT 5 COMMENT '优先权单次阈值减免',
    priority_max_stack  INT NOT NULL DEFAULT 2 COMMENT '优先权最大叠加次数',
    auto_match_enabled  TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否开启每周自动匹配',
    auto_match_day_of_week TINYINT NOT NULL DEFAULT 1 COMMENT '每周几触发(1=周一..7=周日)',
    auto_match_time     VARCHAR(5) NOT NULL DEFAULT '16:00' COMMENT '触发时间(24h HH:mm)',
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '心动时刻匹配配置';

INSERT INTO t_moment_match_config (id, base_threshold, prioritize_offset, priority_offset, priority_max_stack)
VALUES (1, 60, 10, 5, 2)
ON DUPLICATE KEY UPDATE
    base_threshold = VALUES(base_threshold),
    prioritize_offset = VALUES(prioritize_offset),
    priority_offset = VALUES(priority_offset),
    priority_max_stack = VALUES(priority_max_stack);

CREATE TABLE IF NOT EXISTS t_moment_pair_score (
    id                    BIGINT PRIMARY KEY AUTO_INCREMENT,
    week_tag              VARCHAR(10) NOT NULL,
    pool                  VARCHAR(4) NOT NULL COMMENT 'MF/MM/FF',
    user_id_a             BIGINT NOT NULL,
    user_id_b             BIGINT NOT NULL,
    score                 DECIMAL(5,2) DEFAULT 0 COMMENT '最终得分',
    score_detail          JSON DEFAULT NULL COMMENT '维度得分明细',
    hard_filter_passed    TINYINT(1) DEFAULT 0 COMMENT '是否通过硬筛选',
    hard_filter_reason    VARCHAR(64) DEFAULT NULL COMMENT '硬筛选原因',
    soft_penalty          INT DEFAULT 0 COMMENT '最高单项软惩罚',
    soft_penalty_reason   VARCHAR(64) DEFAULT NULL COMMENT '软惩罚来源',
    threshold_offset_a    INT DEFAULT 0 COMMENT '用户A阈值减免',
    threshold_offset_b    INT DEFAULT 0 COMMENT '用户B阈值减免',
    effective_threshold_a INT DEFAULT 0 COMMENT '用户A有效阈值',
    effective_threshold_b INT DEFAULT 0 COMMENT '用户B有效阈值',
    threshold_required    INT DEFAULT 0 COMMENT '边纳入所需阈值',
    included_by_threshold TINYINT(1) DEFAULT 0 COMMENT '是否通过阈值',
    matched               TINYINT(1) DEFAULT 0 COMMENT '是否进入最终匹配',
    created_at            DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_week_pair (week_tag, pool, user_id_a, user_id_b),
    INDEX idx_week_pool (week_tag, pool),
    INDEX idx_week_user_a (week_tag, user_id_a),
    INDEX idx_week_user_b (week_tag, user_id_b)
) COMMENT '心动时刻候选对分数缓存';

-- 举报记录表（V17）
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

-- RAG + AI 人物画像（V24，依据技术文档 V1.1.0-final Rev2）
CREATE TABLE IF NOT EXISTS t_user_ai_profile (
    user_id               BIGINT        PRIMARY KEY,
    interest_tags         JSON          COMMENT '兴趣标签树',
    ocean_o_long          DECIMAL(4,1)  COMMENT '开放性-长期',
    ocean_c_long          DECIMAL(4,1)  COMMENT '尽责性-长期',
    ocean_e_long          DECIMAL(4,1)  COMMENT '外向性-长期',
    ocean_a_long          DECIMAL(4,1)  COMMENT '宜人性-长期',
    ocean_n_long          DECIMAL(4,1)  COMMENT '神经质-长期',
    ocean_o_short         DECIMAL(4,1)  COMMENT '开放性-短期',
    ocean_c_short         DECIMAL(4,1)  COMMENT '尽责性-短期',
    ocean_e_short         DECIMAL(4,1)  COMMENT '外向性-短期',
    ocean_a_short         DECIMAL(4,1)  COMMENT '宜人性-短期',
    ocean_n_short         DECIMAL(4,1)  COMMENT '神经质-短期',
    ocean_confidence      JSON          COMMENT 'OCEAN置信度 {\"O\":0.75,...}',
    has_real_ocean        TINYINT(1)    DEFAULT 0 COMMENT '是否有行为数据支撑的真实OCEAN',
    natural_language_tags JSON          COMMENT 'AI自然语言标签',
    love_attachment_type  VARCHAR(20)   COMMENT '依恋类型（不进AI Prompt）',
    attracted_to_traits   JSON          COMMENT '倾向被吸引的对方特质',
    friction_points       JSON          COMMENT '潜在摩擦场景（不进AI Prompt）',
    user_corrected_fields JSON          COMMENT '用户手动修正过的字段名',
    profile_version       INT           DEFAULT 1,
    last_long_update      DATE,
    last_short_update     DATE,
    created_at            DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT 'AI深度人物画像（V24）';

CREATE TABLE IF NOT EXISTS t_user_behavior_summary (
    user_id                BIGINT    PRIMARY KEY,
    browse_pref_short      JSON      COMMENT '近14天兴趣类目分布',
    browse_pref_long       JSON      COMMENT '近6个月兴趣类目分布',
    chat_partner_traits    JSON      COMMENT '聊天对象MBTI/专业/OCEAN均值',
    match_interest_pattern JSON      COMMENT '有效停留偏好',
    updated_at             DATETIME  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '行为统计摘要（V24）';

CREATE TABLE IF NOT EXISTS t_user_behavior_log (
    id            BIGINT        PRIMARY KEY AUTO_INCREMENT,
    user_id       BIGINT        NOT NULL,
    behavior_type VARCHAR(30)   NOT NULL,
    target_id     BIGINT,
    metadata      JSON,
    created_at    DATETIME      DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_date (user_id, created_at)
) COMMENT '行为原始日志（V24）';

CREATE TABLE IF NOT EXISTS t_user_profile_vector (
    user_id         BIGINT PRIMARY KEY,
    profile_vector  JSON    NOT NULL COMMENT '画像向量 1536维',
    behavior_vector JSON,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '用户画像向量 RAG（V24）';

CREATE TABLE IF NOT EXISTS t_feed_content_vector (
    feed_id          BIGINT PRIMARY KEY,
    user_id          BIGINT NOT NULL,
    content_vector   JSON   NOT NULL COMMENT '内容向量 1536维',
    ai_tags          JSON,
    primary_category VARCHAR(20),
    created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id)
) COMMENT '朋友圈内容向量 RAG（V24）';

CREATE TABLE IF NOT EXISTS t_ice_break_reminder_log (
    id            BIGINT      PRIMARY KEY AUTO_INCREMENT,
    from_user_id  BIGINT      NOT NULL,
    to_user_id    BIGINT      NOT NULL,
    reminder_type VARCHAR(30) DEFAULT 'ICE_BREAK',
    created_at    DATETIME    DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_reminder (from_user_id, to_user_id, reminder_type)
) COMMENT '破冰提醒记录（V24）';

CREATE TABLE IF NOT EXISTS t_relation_milestone (
    id             BIGINT      PRIMARY KEY AUTO_INCREMENT,
    user_id_a      BIGINT      NOT NULL,
    user_id_b      BIGINT      NOT NULL,
    milestone_type VARCHAR(30) NOT NULL,
    notified_at    DATETIME    DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_milestone (user_id_a, user_id_b, milestone_type)
) COMMENT '关系节点提醒记录（V24）';
