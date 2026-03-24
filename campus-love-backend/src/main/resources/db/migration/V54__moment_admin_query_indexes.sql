-- =============================================
-- V54: 心动时刻后台查询索引优化
-- 目标：缩短总览、名单、结果中心与看板的周维度查询时延
-- 兼容：若索引已存在则跳过，避免发布中断
-- =============================================

SET @db := DATABASE();

SET @sql := (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE t_moment_enrollment ADD INDEX idx_week_created (week_tag, created_at)',
        'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = @db
      AND table_name = 't_moment_enrollment'
      AND index_name = 'idx_week_created'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE t_moment_enrollment ADD INDEX idx_week_pool_status_user (week_tag, pool, status, user_id)',
        'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = @db
      AND table_name = 't_moment_enrollment'
      AND index_name = 'idx_week_pool_status_user'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE t_moment_match_result ADD INDEX idx_week_pool_created (week_tag, pool, created_at)',
        'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = @db
      AND table_name = 't_moment_match_result'
      AND index_name = 'idx_week_pool_created'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
