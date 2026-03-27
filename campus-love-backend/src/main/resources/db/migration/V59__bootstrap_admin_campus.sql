-- 预置管理员：登录邮箱 admin-campus@campal.social，昵称 Admin-campus，is_admin=1
-- 密码由 BCrypt(10) 存储；若该邮箱或 id 已存在则跳过（幂等）
INSERT INTO t_user (
    id,
    email,
    password,
    nickname,
    gender,
    profile_complete,
    status,
    is_admin,
    user_level,
    activity_score,
    credit_score,
    invite_count,
    participate_count,
    created_at,
    updated_at
)
SELECT
    90887701,
    'admin-campus@campal.social',
    '$2b$10$EDmPu3i58KceM32aJ0PmIu2OFscw7W5HQ.53bul1NGquk0H5j6Z2q',
    'Admin-campus',
    0,
    0,
    1,
    1,
    1,
    0,
    100,
    0,
    0,
    NOW(3),
    NOW(3)
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM t_user u WHERE u.email = 'admin-campus@campal.social')
  AND NOT EXISTS (SELECT 1 FROM t_user u WHERE u.id = 90887701);
