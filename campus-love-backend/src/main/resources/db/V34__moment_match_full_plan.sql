-- =============================================
-- V34: 心动时刻完整匹配方案
-- 新增优先匹配、阈值配置、候选边缓存
-- =============================================

DROP PROCEDURE IF EXISTS upgrade_moment_match_full_plan;
DELIMITER $$
CREATE PROCEDURE upgrade_moment_match_full_plan()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 't_moment_profile'
          AND COLUMN_NAME = 'prioritize_matching'
    ) THEN
        ALTER TABLE t_moment_profile
            ADD COLUMN prioritize_matching TINYINT(1) DEFAULT 0 COMMENT '是否开启优先匹配';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 't_user'
          AND COLUMN_NAME = 'moment_priority_count'
    ) THEN
        ALTER TABLE t_user
            ADD COLUMN moment_priority_count INT DEFAULT 0 COMMENT '心动时刻连续未匹配优先权计数';
    END IF;

    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 't_user_portrait'
    ) AND NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 't_user_portrait'
          AND COLUMN_NAME = 'prioritize_matching'
    ) THEN
        ALTER TABLE t_user_portrait
            ADD COLUMN prioritize_matching TINYINT(1) DEFAULT 0 COMMENT '是否开启优先匹配';
    END IF;
END$$
DELIMITER ;
CALL upgrade_moment_match_full_plan();
DROP PROCEDURE IF EXISTS upgrade_moment_match_full_plan;

CREATE TABLE IF NOT EXISTS t_moment_match_config (
    id                  BIGINT PRIMARY KEY,
    base_threshold      INT NOT NULL DEFAULT 60 COMMENT '基础阈值',
    prioritize_offset   INT NOT NULL DEFAULT 10 COMMENT '优先匹配阈值减免',
    priority_offset     INT NOT NULL DEFAULT 5 COMMENT '优先权单次阈值减免',
    priority_max_stack  INT NOT NULL DEFAULT 2 COMMENT '优先权最大叠加次数',
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
