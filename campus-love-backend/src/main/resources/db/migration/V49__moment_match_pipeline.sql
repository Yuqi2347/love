-- 心动时刻：匹配流水线、content 附表、周状态扩展、历史 RESULT_READY -> PUBLISHED
-- 首次部署执行一次；若某列已存在导致失败，需人工对齐后 repair。

-- 1) 周表
ALTER TABLE t_moment_activity_week
    ADD COLUMN error_message VARCHAR(1000) NULL COMMENT 'MATCHING/AI 失败信息' AFTER matched_at,
    ADD COLUMN published_at DATETIME NULL COMMENT '对用户公布时间' AFTER error_message;

-- 2) 历史：旧 RESULT_READY 视为已对用户公布
UPDATE t_moment_activity_week SET status = 'PUBLISHED' WHERE status = 'RESULT_READY';

-- 3) 匹配结果大字段附表
CREATE TABLE IF NOT EXISTS t_moment_match_result_content (
  id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
  match_result_id    BIGINT NOT NULL COMMENT 't_moment_match_result.id',
  score_detail       MEDIUMTEXT NULL COMMENT 'JSON 维度分',
  ai_analysis        MEDIUMTEXT NULL COMMENT '长文 AI 分析',
  yuanfen_title      VARCHAR(64) NULL,
  complementary_modes JSON NULL,
  soft_penalty_reasons JSON NULL,
  date_scene_type    VARCHAR(32) NULL,
  insight_card_1     TEXT NULL,
  insight_card_2     TEXT NULL,
  insight_card_3     TEXT NULL,
  golden_sentence    VARCHAR(256) NULL,
  dimension_labels   JSON NULL,
  about_user_a       TEXT NULL,
  about_user_b       TEXT NULL,
  date_prep_json     JSON NULL,
  created_at         DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at         DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_moment_content_result (match_result_id)
) COMMENT '心动时刻匹配结果大字段';

-- 4) 回填 content（主表仍含旧列时）
INSERT INTO t_moment_match_result_content (
  match_result_id, score_detail, yuanfen_title, complementary_modes, soft_penalty_reasons,
  date_scene_type, insight_card_1, insight_card_2, insight_card_3, golden_sentence,
  dimension_labels, about_user_a, about_user_b, date_prep_json
)
SELECT
  id,
  score_detail,
  yuanfen_title,
  complementary_modes,
  soft_penalty_reasons,
  date_scene_type,
  insight_card_1,
  insight_card_2,
  insight_card_3,
  golden_sentence,
  dimension_labels,
  about_user_a,
  about_user_b,
  date_prep_json
FROM t_moment_match_result
WHERE NOT EXISTS (
  SELECT 1 FROM t_moment_match_result_content c WHERE c.match_result_id = t_moment_match_result.id
);

-- 5) 主表瘦身：删除已迁入 content 的列
ALTER TABLE t_moment_match_result
    DROP COLUMN score_detail,
    DROP COLUMN yuanfen_title,
    DROP COLUMN complementary_modes,
    DROP COLUMN soft_penalty_reasons,
    DROP COLUMN date_scene_type,
    DROP COLUMN insight_card_1,
    DROP COLUMN insight_card_2,
    DROP COLUMN insight_card_3,
    DROP COLUMN golden_sentence,
    DROP COLUMN dimension_labels,
    DROP COLUMN about_user_a,
    DROP COLUMN about_user_b,
    DROP COLUMN date_prep_json;

-- 6) 拒绝汇总 / 用户池最优 / AI 任务队列
CREATE TABLE IF NOT EXISTS t_moment_reject_summary (
  id                        BIGINT PRIMARY KEY AUTO_INCREMENT,
  week_tag                  VARCHAR(16) NOT NULL,
  pool                      VARCHAR(8) NOT NULL,
  hard_filter_count         INT NOT NULL DEFAULT 0,
  hard_filter_reason_dist   JSON NULL,
  below_threshold_count     INT NOT NULL DEFAULT 0,
  score_distribution        JSON NULL COMMENT '11 桶 0-10..90-100',
  soft_penalty_reason_dist  JSON NULL,
  created_at                DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at                DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_reject_week_pool (week_tag, pool)
) COMMENT '心动时刻硬筛/阈值拒绝聚合';

CREATE TABLE IF NOT EXISTS t_moment_user_pool_best (
  id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
  week_tag             VARCHAR(16) NOT NULL,
  pool                 VARCHAR(8) NOT NULL,
  user_id              BIGINT NOT NULL,
  max_eligible_score   DECIMAL(6,2) NULL,
  has_any_eligible     TINYINT(1) NOT NULL DEFAULT 0,
  tier2_truncated      TINYINT(1) NOT NULL DEFAULT 0,
  created_at           DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at           DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_pool_best (week_tag, pool, user_id),
  INDEX idx_week_pool (week_tag, pool)
) COMMENT '心动时刻用户在某池 eligible 最高分';

CREATE TABLE IF NOT EXISTS t_moment_ai_analysis_task (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  week_tag        VARCHAR(16) NOT NULL,
  match_result_id BIGINT NOT NULL,
  status          TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1处理中 2完成 3失败',
  retry_count     INT NOT NULL DEFAULT 0,
  error_msg       VARCHAR(1000) NULL,
  created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_week_status (week_tag, status),
  INDEX idx_match_result (match_result_id)
) COMMENT '心动时刻长文 AI 分析任务';
