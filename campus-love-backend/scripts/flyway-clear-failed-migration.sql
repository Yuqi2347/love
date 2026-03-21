-- 用途：迁移脚本曾执行失败时，Flyway 会在 flyway_schema_history 中留下 success=0，
--       导致下次启动校验失败。修复迁移 SQL 后执行本脚本，再重启应用即可重试。
--
-- 用法（示例）：
--   mysql -h127.0.0.1 -uroot -p campus_love < scripts/flyway-clear-failed-migration.sql
--
-- 若表结构无 success 列，请 DESC flyway_schema_history; 后手动删除失败版本对应行。

DELETE FROM flyway_schema_history WHERE success = 0;
