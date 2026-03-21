-- V27: 清除心动时刻旧数据，重新收集（防止旧问卷结构与新版本冲突）
-- 执行前请确认：此操作不可逆，所有用户需重新填写问卷

-- 1. 清除匹配结果（依赖报名记录）
DELETE FROM t_moment_match_result;

-- 2. 清除报名记录
DELETE FROM t_moment_enrollment;

-- 3. 清除问卷档案
DELETE FROM t_moment_profile;
