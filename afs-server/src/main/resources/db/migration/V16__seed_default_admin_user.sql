-- 默认管理员账号：新环境首次启动即可用 admin / admin123 登录
-- 密码为 BCrypt 密文（明文 admin123），role 固定为 admin
-- 已存在同名账号时不覆盖，避免误改线上已修改过的密码
INSERT INTO "user" ("username", "password", "nickname", "avatar", "role")
VALUES ('admin', '$2a$10$n/S1Jzcrt4g.WV9i4UCkR.jsRE4X/t020hFbjRn.090erCc78xPLG', '管理员', '/avatars/default.jpg', 'admin')
ON CONFLICT ("username") DO NOTHING;
