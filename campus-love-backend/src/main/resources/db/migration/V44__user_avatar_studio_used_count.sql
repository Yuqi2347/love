-- AI 头像工作室：每人免费生成次数累计（成功调用图生图后 +1）
ALTER TABLE t_user
    ADD COLUMN avatar_studio_used_count INT NOT NULL DEFAULT 0 COMMENT 'AI头像工作室已用免费次数';
