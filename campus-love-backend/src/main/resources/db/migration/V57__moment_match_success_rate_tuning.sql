-- 心动时刻匹配成功率调优：
-- 1) 基础阈值 75 -> 70（扩大 eligible 边，提升匹配覆盖）
-- 2) 图构建 Top-K 200 -> 400（减少截断导致的“有边但未入图”）
UPDATE t_moment_match_config
SET
    base_threshold = 70,
    eligible_top_k = 400
WHERE id = 1;
