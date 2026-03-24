-- 心动时刻：匹配图 Top-K 配置（默认 200，控制峰值内存）
ALTER TABLE t_moment_match_config
    ADD COLUMN eligible_top_k INT NOT NULL DEFAULT 200 COMMENT '每人进入图的最大 eligible 边数' AFTER priority_max_stack;

UPDATE t_moment_match_config SET eligible_top_k = 200 WHERE id = 1;
