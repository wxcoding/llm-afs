-- RBAC：用户表增加角色字段（user/admin），默认普通用户
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS "role" VARCHAR(20) NOT NULL DEFAULT 'user';

-- 约定用户名为 admin 的账号为内置管理员
UPDATE "user" SET "role" = 'admin' WHERE "username" = 'admin';
