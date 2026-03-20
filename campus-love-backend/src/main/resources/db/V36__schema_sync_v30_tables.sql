-- =============================================
-- V36: 补齐 V30 缺失表与字段（已有库升级用）
-- 若 schema.sql 为旧版未含 t_user_stats/t_user_portrait 等，执行本脚本可修复登录/注册失败
-- =============================================

-- 1. t_user 补齐所有缺失字段（幂等，与 User 实体一致）
DROP PROCEDURE IF EXISTS add_t_user_missing_cols;
DELIMITER $$
CREATE PROCEDURE add_t_user_missing_cols()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'deleted_at') THEN
        ALTER TABLE t_user ADD COLUMN deleted_at DATETIME DEFAULT NULL COMMENT 'NULL=正常，有值=已注销';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'delete_reason') THEN
        ALTER TABLE t_user ADD COLUMN delete_reason TINYINT DEFAULT NULL COMMENT '注销原因枚举';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'cover_image_url') THEN
        ALTER TABLE t_user ADD COLUMN cover_image_url VARCHAR(512) DEFAULT NULL COMMENT '个人主页背景图URL';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'moment_priority_count') THEN
        ALTER TABLE t_user ADD COLUMN moment_priority_count INT DEFAULT 0 COMMENT '心动时刻连续未匹配优先权计数';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'feed_visibility_time') THEN
        ALTER TABLE t_user ADD COLUMN feed_visibility_time INT DEFAULT -1 COMMENT '动态可见时间(天)：3=近三天，30=近一月，180=近半年，-1=全部';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'feed_visibility') THEN
        ALTER TABLE t_user ADD COLUMN feed_visibility VARCHAR(16) DEFAULT 'ALL' COMMENT '朋友圈可见性：ALL/FOLLOWERS/SELF';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'last_follower_viewed_at') THEN
        ALTER TABLE t_user ADD COLUMN last_follower_viewed_at DATETIME DEFAULT NULL COMMENT '上次查看粉丝列表时间';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'last_feed_activity_viewed_at') THEN
        ALTER TABLE t_user ADD COLUMN last_feed_activity_viewed_at DATETIME DEFAULT NULL COMMENT '上次查看朋友圈动态活动时间';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'last_invite_activity_viewed_at') THEN
        ALTER TABLE t_user ADD COLUMN last_invite_activity_viewed_at DATETIME DEFAULT NULL COMMENT '上次查看邀约活动时间';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'moment_photo_url') THEN
        ALTER TABLE t_user ADD COLUMN moment_photo_url VARCHAR(256) DEFAULT NULL COMMENT '心动一刻照片URL';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'moment_self_score') THEN
        ALTER TABLE t_user ADD COLUMN moment_self_score TINYINT DEFAULT NULL COMMENT '自评颜值分1-10';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'moment_banned') THEN
        ALTER TABLE t_user ADD COLUMN moment_banned TINYINT(1) DEFAULT 0 COMMENT '是否被禁止参加心动一刻';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'bazi_unknown') THEN
        ALTER TABLE t_user ADD COLUMN bazi_unknown TINYINT(1) DEFAULT 0 COMMENT '生辰时辰是否不知道';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'ice_break_enabled') THEN
        ALTER TABLE t_user ADD COLUMN ice_break_enabled TINYINT(1) DEFAULT 0 COMMENT '是否开启破冰功能';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'ai_disclosure_settings') THEN
        ALTER TABLE t_user ADD COLUMN ai_disclosure_settings JSON DEFAULT (JSON_OBJECT('mbti', true, 'zodiac', true, 'majorCategory', true, 'interestTags', true, 'naturalLangTags', false, 'baziInfo', false, 'questionnaireHints', false)) COMMENT 'AI信息公开授权';
    END IF;
END$$
DELIMITER ;
CALL add_t_user_missing_cols();
DROP PROCEDURE IF EXISTS add_t_user_missing_cols;

-- 1b. t_moment_profile 补齐 prioritize_matching（V34）
DROP PROCEDURE IF EXISTS add_t_moment_profile_prioritize;
DELIMITER $$
CREATE PROCEDURE add_t_moment_profile_prioritize()
BEGIN
    IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_moment_profile') THEN
        IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_moment_profile' AND COLUMN_NAME = 'prioritize_matching') THEN
            ALTER TABLE t_moment_profile ADD COLUMN prioritize_matching TINYINT(1) DEFAULT 0 COMMENT '是否开启优先匹配';
        END IF;
    END IF;
END$$
DELIMITER ;
CALL add_t_moment_profile_prioritize();
DROP PROCEDURE IF EXISTS add_t_moment_profile_prioritize;

-- 2. t_user_stats
CREATE TABLE IF NOT EXISTS t_user_stats (
    user_id             BIGINT PRIMARY KEY,
    activity_score      INT DEFAULT 0,
    user_level          INT DEFAULT 1,
    invite_count        INT DEFAULT 0,
    participate_count   INT DEFAULT 0,
    credit_score        INT DEFAULT 100,
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '用户统计（从 t_user 迁出）';

-- 3. t_user_portrait
CREATE TABLE IF NOT EXISTS t_user_portrait (
    user_id                 BIGINT PRIMARY KEY,
    mbti                    VARCHAR(8) DEFAULT NULL,
    zodiac                  VARCHAR(16) DEFAULT NULL,
    bazi                    VARCHAR(64) DEFAULT NULL,
    bio                     VARCHAR(256) DEFAULT NULL,
    interest_tags           JSON DEFAULT NULL,
    target_gender           VARCHAR(10) DEFAULT NULL,
    social_style            CHAR(1) DEFAULT NULL,
    life_rhythm             CHAR(1) DEFAULT NULL,
    companionship_style     CHAR(1) DEFAULT NULL,
    appearance_requirement  CHAR(1) DEFAULT NULL,
    partner_personality     CHAR(1) DEFAULT NULL,
    major_preference        CHAR(1) DEFAULT NULL,
    age_range_preference    VARCHAR(20) DEFAULT NULL,
    age_preference_min      INT DEFAULT -10,
    age_preference_max      INT DEFAULT 10,
    grade_range_min         INT DEFAULT 1,
    grade_range_max         INT DEFAULT 11,
    grade_range_preference  CHAR(1) DEFAULT NULL,
    prioritize_matching     TINYINT(1) DEFAULT 0,
    date_style              CHAR(1) DEFAULT NULL,
    intimacy_pace           CHAR(1) DEFAULT NULL,
    loyalty_value           CHAR(1) DEFAULT NULL,
    premarital_cohabitation CHAR(1) DEFAULT NULL,
    future_lifestyle        CHAR(1) DEFAULT NULL,
    relationship_core_value CHAR(1) DEFAULT NULL,
    appearance_score        DECIMAL(3,1) DEFAULT NULL,
    personality_base        CHAR(1) DEFAULT NULL,
    campus_focus            CHAR(1) DEFAULT NULL,
    emotion_style           CHAR(1) DEFAULT NULL,
    career_ambition_pref    CHAR(1) DEFAULT NULL,
    honesty_level           CHAR(1) DEFAULT NULL,
    premarital_sex          CHAR(1) DEFAULT NULL,
    conflict_style          CHAR(1) DEFAULT NULL,
    social_boundary         CHAR(1) DEFAULT NULL,
    campus_love_plan        CHAR(1) DEFAULT NULL,
    idol_role               CHAR(1) DEFAULT NULL,
    temptation_response     CHAR(1) DEFAULT NULL,
    reality_condition       CHAR(1) DEFAULT NULL,
    human_nature_view       CHAR(1) DEFAULT NULL,
    breakup_view            CHAR(1) DEFAULT NULL,
    career_love_conflict    CHAR(1) DEFAULT NULL,
    emotion_priority        CHAR(1) DEFAULT NULL,
    life_goal_priority      CHAR(1) DEFAULT NULL,
    questionnaire_snapshot   JSON DEFAULT NULL,
    questionnaire_version   INT DEFAULT 1,
    ocean_o_long            DECIMAL(4,1) DEFAULT NULL,
    ocean_c_long            DECIMAL(4,1) DEFAULT NULL,
    ocean_e_long            DECIMAL(4,1) DEFAULT NULL,
    ocean_a_long            DECIMAL(4,1) DEFAULT NULL,
    ocean_n_long            DECIMAL(4,1) DEFAULT NULL,
    ocean_o_short           DECIMAL(4,1) DEFAULT NULL,
    ocean_c_short           DECIMAL(4,1) DEFAULT NULL,
    ocean_e_short           DECIMAL(4,1) DEFAULT NULL,
    ocean_a_short           DECIMAL(4,1) DEFAULT NULL,
    ocean_n_short           DECIMAL(4,1) DEFAULT NULL,
    ocean_confidence        JSON DEFAULT NULL,
    has_real_ocean          TINYINT(1) DEFAULT 0,
    natural_language_tags   JSON DEFAULT NULL,
    love_attachment_type    VARCHAR(20) DEFAULT NULL,
    attracted_to_traits     JSON DEFAULT NULL,
    friction_points         JSON DEFAULT NULL,
    profile_version         INT DEFAULT 1,
    last_long_update        DATE DEFAULT NULL,
    last_short_update       DATE DEFAULT NULL,
    created_at              DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '用户画像（基础+问卷+OCEAN）';

-- 4. t_interest_tag_meta
CREATE TABLE IF NOT EXISTS t_interest_tag_meta (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    tag_code    VARCHAR(64) NOT NULL UNIQUE,
    tag_name    VARCHAR(64) NOT NULL,
    dimension   VARCHAR(32) NOT NULL,
    signals     JSON DEFAULT NULL,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_dimension (dimension)
) COMMENT '兴趣标签元数据';

-- 5. t_questionnaire_meta
CREATE TABLE IF NOT EXISTS t_questionnaire_meta (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    version     INT NOT NULL UNIQUE,
    questions   JSON NOT NULL,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '问卷元数据';

-- 6. t_user_embedding
CREATE TABLE IF NOT EXISTS t_user_embedding (
    user_id     BIGINT PRIMARY KEY,
    embedding   JSON NOT NULL,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT 'embedding暂用JSON存储';

-- 7. 数据迁移：t_user -> t_user_stats
INSERT INTO t_user_stats (user_id, activity_score, user_level, invite_count, participate_count, credit_score, created_at, updated_at)
SELECT id, COALESCE(activity_score, 0), COALESCE(user_level, 1), COALESCE(invite_count, 0), COALESCE(participate_count, 0), COALESCE(credit_score, 100), created_at, updated_at
FROM t_user
WHERE deleted_at IS NULL
ON DUPLICATE KEY UPDATE
    activity_score = VALUES(activity_score),
    user_level = VALUES(user_level),
    invite_count = VALUES(invite_count),
    participate_count = VALUES(participate_count),
    credit_score = VALUES(credit_score),
    updated_at = VALUES(updated_at);

-- 8. 数据迁移：t_user + t_moment_profile + t_user_ai_profile -> t_user_portrait（若存在）
INSERT INTO t_user_portrait (
    user_id, mbti, zodiac, bazi, bio, interest_tags, questionnaire_version,
    target_gender, social_style, life_rhythm, companionship_style, appearance_requirement,
    partner_personality, major_preference, age_range_preference, age_preference_min, age_preference_max,
    grade_range_min, grade_range_max, grade_range_preference, prioritize_matching, date_style, intimacy_pace,
    loyalty_value, premarital_cohabitation, future_lifestyle, relationship_core_value,
    appearance_score, personality_base, campus_focus, emotion_style, career_ambition_pref,
    honesty_level, premarital_sex, conflict_style, social_boundary,
    campus_love_plan, idol_role, temptation_response, reality_condition, human_nature_view,
    breakup_view, career_love_conflict, emotion_priority, life_goal_priority,
    ocean_o_long, ocean_c_long, ocean_e_long, ocean_a_long, ocean_n_long,
    ocean_o_short, ocean_c_short, ocean_e_short, ocean_a_short, ocean_n_short,
    has_real_ocean, natural_language_tags, love_attachment_type, attracted_to_traits, friction_points, profile_version
)
SELECT
    u.id, u.mbti, u.zodiac, u.bazi, u.bio,
    NULL, 1,
    mp.target_gender, mp.social_style, mp.life_rhythm, mp.companionship_style, mp.appearance_requirement,
    mp.partner_personality, mp.major_preference, mp.age_range_preference, COALESCE(mp.age_preference_min, -10), COALESCE(mp.age_preference_max, 10),
    COALESCE(mp.grade_range_min, 1), COALESCE(mp.grade_range_max, 11), mp.grade_range_preference, 0,
    mp.date_style, mp.intimacy_pace,
    NULL, mp.premarital_cohabitation, mp.future_lifestyle, mp.relationship_core_value,
    mp.appearance_score, mp.personality_base, mp.campus_focus, mp.emotion_style, mp.career_ambition_pref,
    mp.honesty_level, mp.premarital_sex, mp.conflict_style, mp.social_boundary,
    mp.campus_love_plan, mp.idol_role, mp.temptation_response, mp.reality_condition, mp.human_nature_view,
    mp.breakup_view, mp.career_love_conflict, mp.emotion_priority, mp.life_goal_priority,
    aip.ocean_o_long, aip.ocean_c_long, aip.ocean_e_long, aip.ocean_a_long, aip.ocean_n_long,
    aip.ocean_o_short, aip.ocean_c_short, aip.ocean_e_short, aip.ocean_a_short, aip.ocean_n_short,
    COALESCE(aip.has_real_ocean, 0), aip.natural_language_tags, aip.love_attachment_type, aip.attracted_to_traits, aip.friction_points, COALESCE(aip.profile_version, 1)
FROM t_user u
LEFT JOIN t_moment_profile mp ON u.id = mp.user_id
LEFT JOIN t_user_ai_profile aip ON u.id = aip.user_id
WHERE u.deleted_at IS NULL
ON DUPLICATE KEY UPDATE
    mbti = COALESCE(VALUES(mbti), t_user_portrait.mbti),
    zodiac = COALESCE(VALUES(zodiac), t_user_portrait.zodiac),
    bazi = COALESCE(VALUES(bazi), t_user_portrait.bazi),
    bio = COALESCE(VALUES(bio), t_user_portrait.bio),
    target_gender = COALESCE(VALUES(target_gender), t_user_portrait.target_gender),
    social_style = COALESCE(VALUES(social_style), t_user_portrait.social_style),
    life_rhythm = COALESCE(VALUES(life_rhythm), t_user_portrait.life_rhythm),
    companionship_style = COALESCE(VALUES(companionship_style), t_user_portrait.companionship_style),
    appearance_requirement = COALESCE(VALUES(appearance_requirement), t_user_portrait.appearance_requirement),
    partner_personality = COALESCE(VALUES(partner_personality), t_user_portrait.partner_personality),
    major_preference = COALESCE(VALUES(major_preference), t_user_portrait.major_preference),
    age_range_preference = COALESCE(VALUES(age_range_preference), t_user_portrait.age_range_preference),
    age_preference_min = COALESCE(VALUES(age_preference_min), t_user_portrait.age_preference_min),
    age_preference_max = COALESCE(VALUES(age_preference_max), t_user_portrait.age_preference_max),
    grade_range_min = COALESCE(VALUES(grade_range_min), t_user_portrait.grade_range_min),
    grade_range_max = COALESCE(VALUES(grade_range_max), t_user_portrait.grade_range_max),
    grade_range_preference = COALESCE(VALUES(grade_range_preference), t_user_portrait.grade_range_preference),
    prioritize_matching = COALESCE(VALUES(prioritize_matching), t_user_portrait.prioritize_matching),
    date_style = COALESCE(VALUES(date_style), t_user_portrait.date_style),
    intimacy_pace = COALESCE(VALUES(intimacy_pace), t_user_portrait.intimacy_pace),
    premarital_cohabitation = COALESCE(VALUES(premarital_cohabitation), t_user_portrait.premarital_cohabitation),
    future_lifestyle = COALESCE(VALUES(future_lifestyle), t_user_portrait.future_lifestyle),
    relationship_core_value = COALESCE(VALUES(relationship_core_value), t_user_portrait.relationship_core_value),
    appearance_score = COALESCE(VALUES(appearance_score), t_user_portrait.appearance_score),
    personality_base = COALESCE(VALUES(personality_base), t_user_portrait.personality_base),
    campus_focus = COALESCE(VALUES(campus_focus), t_user_portrait.campus_focus),
    emotion_style = COALESCE(VALUES(emotion_style), t_user_portrait.emotion_style),
    career_ambition_pref = COALESCE(VALUES(career_ambition_pref), t_user_portrait.career_ambition_pref),
    honesty_level = COALESCE(VALUES(honesty_level), t_user_portrait.honesty_level),
    premarital_sex = COALESCE(VALUES(premarital_sex), t_user_portrait.premarital_sex),
    conflict_style = COALESCE(VALUES(conflict_style), t_user_portrait.conflict_style),
    social_boundary = COALESCE(VALUES(social_boundary), t_user_portrait.social_boundary),
    campus_love_plan = COALESCE(VALUES(campus_love_plan), t_user_portrait.campus_love_plan),
    idol_role = COALESCE(VALUES(idol_role), t_user_portrait.idol_role),
    temptation_response = COALESCE(VALUES(temptation_response), t_user_portrait.temptation_response),
    reality_condition = COALESCE(VALUES(reality_condition), t_user_portrait.reality_condition),
    human_nature_view = COALESCE(VALUES(human_nature_view), t_user_portrait.human_nature_view),
    breakup_view = COALESCE(VALUES(breakup_view), t_user_portrait.breakup_view),
    career_love_conflict = COALESCE(VALUES(career_love_conflict), t_user_portrait.career_love_conflict),
    emotion_priority = COALESCE(VALUES(emotion_priority), t_user_portrait.emotion_priority),
    life_goal_priority = COALESCE(VALUES(life_goal_priority), t_user_portrait.life_goal_priority),
    ocean_o_long = COALESCE(VALUES(ocean_o_long), t_user_portrait.ocean_o_long),
    ocean_c_long = COALESCE(VALUES(ocean_c_long), t_user_portrait.ocean_c_long),
    ocean_e_long = COALESCE(VALUES(ocean_e_long), t_user_portrait.ocean_e_long),
    ocean_a_long = COALESCE(VALUES(ocean_a_long), t_user_portrait.ocean_a_long),
    ocean_n_long = COALESCE(VALUES(ocean_n_long), t_user_portrait.ocean_n_long),
    ocean_o_short = COALESCE(VALUES(ocean_o_short), t_user_portrait.ocean_o_short),
    ocean_c_short = COALESCE(VALUES(ocean_c_short), t_user_portrait.ocean_c_short),
    ocean_e_short = COALESCE(VALUES(ocean_e_short), t_user_portrait.ocean_e_short),
    ocean_a_short = COALESCE(VALUES(ocean_a_short), t_user_portrait.ocean_a_short),
    ocean_n_short = COALESCE(VALUES(ocean_n_short), t_user_portrait.ocean_n_short),
    has_real_ocean = COALESCE(VALUES(has_real_ocean), t_user_portrait.has_real_ocean),
    natural_language_tags = COALESCE(VALUES(natural_language_tags), t_user_portrait.natural_language_tags),
    love_attachment_type = COALESCE(VALUES(love_attachment_type), t_user_portrait.love_attachment_type),
    attracted_to_traits = COALESCE(VALUES(attracted_to_traits), t_user_portrait.attracted_to_traits),
    friction_points = COALESCE(VALUES(friction_points), t_user_portrait.friction_points),
    profile_version = COALESCE(VALUES(profile_version), t_user_portrait.profile_version),
    updated_at = VALUES(updated_at);

-- 9. 数据迁移：t_user_profile_vector -> t_user_embedding（若 t_user_profile_vector 存在）
DROP PROCEDURE IF EXISTS migrate_profile_vector_to_embedding;
DELIMITER $$
CREATE PROCEDURE migrate_profile_vector_to_embedding()
BEGIN
    IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user_profile_vector') THEN
        INSERT INTO t_user_embedding (user_id, embedding, updated_at)
        SELECT user_id, profile_vector, updated_at
        FROM t_user_profile_vector
        WHERE profile_vector IS NOT NULL
        ON DUPLICATE KEY UPDATE embedding = VALUES(embedding), updated_at = VALUES(updated_at);
    END IF;
END$$
DELIMITER ;
CALL migrate_profile_vector_to_embedding();
DROP PROCEDURE IF EXISTS migrate_profile_vector_to_embedding;

-- 10. t_moment_match_config 自动匹配字段（与 V36__moment_auto_match_schedule 合并）
DROP PROCEDURE IF EXISTS upgrade_moment_auto_match_schedule;
DELIMITER $$
CREATE PROCEDURE upgrade_moment_auto_match_schedule()
BEGIN
    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 't_moment_match_config'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 't_moment_match_config'
              AND COLUMN_NAME = 'auto_match_enabled'
        ) THEN
            ALTER TABLE t_moment_match_config
                ADD COLUMN auto_match_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否开启每周自动匹配';
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 't_moment_match_config'
              AND COLUMN_NAME = 'auto_match_day_of_week'
        ) THEN
            ALTER TABLE t_moment_match_config
                ADD COLUMN auto_match_day_of_week TINYINT NOT NULL DEFAULT 1 COMMENT '每周几触发(1=周一..7=周日)';
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 't_moment_match_config'
              AND COLUMN_NAME = 'auto_match_time'
        ) THEN
            ALTER TABLE t_moment_match_config
                ADD COLUMN auto_match_time VARCHAR(5) NOT NULL DEFAULT '16:00' COMMENT '触发时间(24h HH:mm)';
        END IF;
    END IF;
END$$
DELIMITER ;

CALL upgrade_moment_auto_match_schedule();
DROP PROCEDURE IF EXISTS upgrade_moment_auto_match_schedule;
