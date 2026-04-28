-- 用户表 (user 是 PostgreSQL 保留关键字，必须用双引号)
CREATE TABLE IF NOT EXISTS "user" (
    "id" BIGSERIAL PRIMARY KEY,
    "username" VARCHAR(50) NOT NULL UNIQUE,
    "password" VARCHAR(100) NOT NULL,
    "nickname" VARCHAR(50),
    "avatar" TEXT,
    "phone" VARCHAR(20),
    "email" VARCHAR(100),
    "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS "idx_user_username" ON "user" ("username");

-- 对话会话表
CREATE TABLE IF NOT EXISTS "chat_session" (
    "id" BIGSERIAL PRIMARY KEY,
    "user_id" BIGINT NOT NULL,
    "title" VARCHAR(200),
    "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS "idx_chat_session_user_id" ON "chat_session" ("user_id");

-- 聊天消息表
CREATE TABLE IF NOT EXISTS "chat_message" (
    "id" BIGSERIAL PRIMARY KEY,
    "session_id" BIGINT NOT NULL,
    "role" VARCHAR(20) NOT NULL,
    "content" TEXT NOT NULL,
    "sources" TEXT,
    "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS "idx_chat_message_session_id" ON "chat_message" ("session_id");

-- 诈骗案例表
CREATE TABLE IF NOT EXISTS "scam_case" (
    "id" BIGSERIAL PRIMARY KEY,
    "title" VARCHAR(200) NOT NULL,
    "type" VARCHAR(50) NOT NULL,
    "content" TEXT,
    "tips" TEXT,
    "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS "idx_scam_case_type" ON "scam_case" ("type");

-- 知识库表
CREATE TABLE IF NOT EXISTS "knowledge" (
    "id" BIGSERIAL PRIMARY KEY,
    "title" VARCHAR(200) NOT NULL,
    "category" VARCHAR(50) NOT NULL,
    "content" TEXT,
    "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS "idx_knowledge_category" ON "knowledge" ("category");
