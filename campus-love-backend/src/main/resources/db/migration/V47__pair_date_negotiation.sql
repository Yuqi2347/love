-- V47: 心动「约一下」三步协商（见 V1.2.0_INVITATION_MODULE.md）

CREATE TABLE IF NOT EXISTS t_moment_yue_intent (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    match_result_id  BIGINT       NOT NULL COMMENT 't_moment_match_result.id',
    user_id          BIGINT       NOT NULL,
    created_at       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_match_user (match_result_id, user_id),
    INDEX idx_match (match_result_id)
) COMMENT '心动匹配双方「约一下」意向（双方均记录后创建协商）';

CREATE TABLE IF NOT EXISTS t_pair_date_negotiation (
    id                    BIGINT PRIMARY KEY AUTO_INCREMENT,
    match_result_id       BIGINT       NOT NULL COMMENT '心动匹配结果',
    user_id_a             BIGINT       NOT NULL COMMENT '与 t_moment_match_result.user_id_a 一致',
    user_id_b             BIGINT       NOT NULL COMMENT '与 t_moment_match_result.user_id_b 一致',
    first_yue_user_id     BIGINT       DEFAULT NULL COMMENT '先点约一下的用户',
    week_tag              VARCHAR(16)  NOT NULL COMMENT '活动周',

    date_options          JSON         NOT NULL COMMENT 'AI 生成的3个约会方式',

    a_excluded_option     TINYINT      DEFAULT NULL COMMENT 'user_id_a 侧排除的 rank 1-3',
    b_excluded_option     TINYINT      DEFAULT NULL,
    a_time_slots          JSON         DEFAULT NULL COMMENT 'user_id_a 选中的时段编码列表',
    b_time_slots          JSON         DEFAULT NULL,
    a_location_choice     VARCHAR(16)  DEFAULT NULL COMMENT 'SELF/PARTNER/EITHER',
    b_location_choice     VARCHAR(16)  DEFAULT NULL,

    final_date_option     JSON         DEFAULT NULL,
    meeting_time_slot     VARCHAR(24)  DEFAULT NULL COMMENT '如 TUE_PM',
    meeting_timestamp     BIGINT       DEFAULT NULL COMMENT '毫秒时间戳，时段起点（上海时区）',
    location_decider_id   BIGINT       DEFAULT NULL,
    decider_reason_key    VARCHAR(32)  DEFAULT NULL,

    status                VARCHAR(24)  NOT NULL DEFAULT 'PENDING'
        COMMENT 'PENDING/SIDE_A_DONE/SIDE_B_DONE/CALCULATING/COMPLETED/TIME_MISMATCH/EXPIRED',
    version               INT          NOT NULL DEFAULT 0,
    time_mismatch         TINYINT(1)   NOT NULL DEFAULT 0,

    created_at            DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_match (match_result_id),
    UNIQUE KEY uk_pair_week (user_id_a, user_id_b, week_tag),
    INDEX idx_week (week_tag),
    INDEX idx_status (status)
) COMMENT '心动约一下：约会方式/时间/地点协商';

DROP PROCEDURE IF EXISTS add_notification_related_id;
DELIMITER $$
CREATE PROCEDURE add_notification_related_id()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_notification' AND COLUMN_NAME = 'related_id') THEN
        ALTER TABLE t_notification ADD COLUMN related_id BIGINT DEFAULT NULL COMMENT '泛型业务关联ID';
    END IF;
END$$
DELIMITER ;
CALL add_notification_related_id();
DROP PROCEDURE IF EXISTS add_notification_related_id;
