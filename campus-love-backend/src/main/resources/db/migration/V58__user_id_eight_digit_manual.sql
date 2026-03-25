-- 新注册用户 ID 改为应用层在 [10000000, 99999999] 内随机分配（八位数字，首位非 0）
-- 已存在行保持原 id；此后插入须显式指定 id
ALTER TABLE t_user
    MODIFY COLUMN id BIGINT NOT NULL COMMENT '用户主键：新用户为8位数字随机，历史数据为原自增值';
