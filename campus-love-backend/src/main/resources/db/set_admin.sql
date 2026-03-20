-- =============================================
-- 设置管理员账号脚本
-- =============================================

USE campus_love;

-- 设置指定邮箱的用户为管理员
-- 请将 'your_email@example.com' 替换为实际的管理员邮箱
UPDATE t_user SET is_admin = 1 WHERE email = '2410105025@mails.szu.edu.cn';

-- 如果需要设置多个管理员，复制上面的行并修改邮箱
-- UPDATE t_user SET is_admin = 1, user_level = 10, activity_score = 3000 WHERE email = 'another_admin@example.com';

-- 查询管理员账号
SELECT id, email, nickname, is_admin, user_level, activity_score FROM t_user WHERE is_admin = 1;
