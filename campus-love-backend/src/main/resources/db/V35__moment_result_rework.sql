-- =============================================
-- V35: 心动一刻结果页重构
-- 预生成缘分内容 + 双方确认解锁约会准备
-- =============================================

DROP PROCEDURE IF EXISTS upgrade_moment_result_rework;
DELIMITER $$
CREATE PROCEDURE upgrade_moment_result_rework()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 't_moment_match_result'
          AND COLUMN_NAME = 'yuanfen_title'
    ) THEN
        ALTER TABLE t_moment_match_result
            ADD COLUMN yuanfen_title VARCHAR(32) DEFAULT NULL COMMENT '缘分标题';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 't_moment_match_result'
          AND COLUMN_NAME = 'complementary_modes'
    ) THEN
        ALTER TABLE t_moment_match_result
            ADD COLUMN complementary_modes JSON DEFAULT NULL COMMENT '命中的互补模式列表';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 't_moment_match_result'
          AND COLUMN_NAME = 'soft_penalty_reasons'
    ) THEN
        ALTER TABLE t_moment_match_result
            ADD COLUMN soft_penalty_reasons JSON DEFAULT NULL COMMENT '软惩罚触发原因列表';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 't_moment_match_result'
          AND COLUMN_NAME = 'date_scene_type'
    ) THEN
        ALTER TABLE t_moment_match_result
            ADD COLUMN date_scene_type VARCHAR(20) DEFAULT NULL COMMENT '约会场景类型';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 't_moment_match_result'
          AND COLUMN_NAME = 'insight_card_1'
    ) THEN
        ALTER TABLE t_moment_match_result
            ADD COLUMN insight_card_1 TEXT DEFAULT NULL COMMENT '心动之处卡片一';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 't_moment_match_result'
          AND COLUMN_NAME = 'insight_card_2'
    ) THEN
        ALTER TABLE t_moment_match_result
            ADD COLUMN insight_card_2 TEXT DEFAULT NULL COMMENT '心动之处卡片二';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 't_moment_match_result'
          AND COLUMN_NAME = 'insight_card_3'
    ) THEN
        ALTER TABLE t_moment_match_result
            ADD COLUMN insight_card_3 TEXT DEFAULT NULL COMMENT '心动之处卡片三';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 't_moment_match_result'
          AND COLUMN_NAME = 'golden_sentence'
    ) THEN
        ALTER TABLE t_moment_match_result
            ADD COLUMN golden_sentence VARCHAR(128) DEFAULT NULL COMMENT '专属金句';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 't_moment_match_result'
          AND COLUMN_NAME = 'dimension_labels'
    ) THEN
        ALTER TABLE t_moment_match_result
            ADD COLUMN dimension_labels JSON DEFAULT NULL COMMENT '四维度标签';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 't_moment_match_result'
          AND COLUMN_NAME = 'about_user_a'
    ) THEN
        ALTER TABLE t_moment_match_result
            ADD COLUMN about_user_a TEXT DEFAULT NULL COMMENT '展示给B看的A画像';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 't_moment_match_result'
          AND COLUMN_NAME = 'about_user_b'
    ) THEN
        ALTER TABLE t_moment_match_result
            ADD COLUMN about_user_b TEXT DEFAULT NULL COMMENT '展示给A看的B画像';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 't_moment_match_result'
          AND COLUMN_NAME = 'date_prep_json'
    ) THEN
        ALTER TABLE t_moment_match_result
            ADD COLUMN date_prep_json JSON DEFAULT NULL COMMENT '约会准备内容';
    END IF;
END$$
DELIMITER ;
CALL upgrade_moment_result_rework();
DROP PROCEDURE IF EXISTS upgrade_moment_result_rework;

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
