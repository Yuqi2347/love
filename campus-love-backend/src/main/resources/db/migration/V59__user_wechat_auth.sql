-- 用户表：微信小程序登录绑定字段
ALTER TABLE t_user
    ADD COLUMN wechat_openid VARCHAR(96) DEFAULT NULL COMMENT '微信小程序openid（唯一）',
    ADD COLUMN wechat_unionid VARCHAR(96) DEFAULT NULL COMMENT '微信开放平台unionid',
    ADD COLUMN wechat_bound_at DATETIME DEFAULT NULL COMMENT '微信绑定时间';

ALTER TABLE t_user
    ADD UNIQUE KEY uk_wechat_openid (wechat_openid);
