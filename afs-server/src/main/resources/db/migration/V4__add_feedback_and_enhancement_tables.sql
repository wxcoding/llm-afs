-- V4__add_feedback_and_enhancement_tables.sql

-- 用户反馈表（对话评分、有用/没用）
CREATE TABLE IF NOT EXISTS "message_feedback" (
    "id" BIGSERIAL PRIMARY KEY,
    "message_id" BIGINT NOT NULL,
    "user_id" BIGINT NOT NULL,
    "rating" INTEGER CHECK (rating >= 1 AND rating <= 5),
    "is_helpful" BOOLEAN,
    "feedback_type" VARCHAR(20),
    "feedback_content" TEXT,
    "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS "idx_message_feedback_message_id" ON "message_feedback" ("message_id");
CREATE INDEX IF NOT EXISTS "idx_message_feedback_user_id" ON "message_feedback" ("user_id");

-- 搜索历史表
CREATE TABLE IF NOT EXISTS "search_history" (
    "id" BIGSERIAL PRIMARY KEY,
    "user_id" BIGINT,
    "keyword" VARCHAR(200) NOT NULL,
    "search_type" VARCHAR(50),
    "result_count" INTEGER DEFAULT 0,
    "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS "idx_search_history_user_id" ON "search_history" ("user_id");
CREATE INDEX IF NOT EXISTS "idx_search_history_keyword" ON "search_history" ("keyword");

-- 知识标签表
CREATE TABLE IF NOT EXISTS "knowledge_tag" (
    "id" BIGSERIAL PRIMARY KEY,
    "name" VARCHAR(50) NOT NULL UNIQUE,
    "color" VARCHAR(20),
    "description" VARCHAR(200),
    "knowledge_count" INTEGER DEFAULT 0,
    "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 知识-标签关联表
CREATE TABLE IF NOT EXISTS "knowledge_tag_relation" (
    "id" BIGSERIAL PRIMARY KEY,
    "knowledge_id" BIGINT NOT NULL,
    "tag_id" BIGINT NOT NULL,
    "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS "idx_knowledge_tag_relation_knowledge" ON "knowledge_tag_relation" ("knowledge_id");
CREATE INDEX IF NOT EXISTS "idx_knowledge_tag_relation_tag" ON "knowledge_tag_relation" ("tag_id");

-- 知识版本历史表
CREATE TABLE IF NOT EXISTS "knowledge_version" (
    "id" BIGSERIAL PRIMARY KEY,
    "knowledge_id" BIGINT NOT NULL,
    "title" VARCHAR(200),
    "content" TEXT,
    "change_summary" VARCHAR(500),
    "operator_id" BIGINT,
    "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS "idx_knowledge_version_knowledge_id" ON "knowledge_version" ("knowledge_id");

-- 对话收藏表
CREATE TABLE IF NOT EXISTS "conversation_favorite" (
    "id" BIGSERIAL PRIMARY KEY,
    "user_id" BIGINT NOT NULL,
    "message_id" BIGINT NOT NULL,
    "title" VARCHAR(200),
    "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS "idx_conversation_favorite_user_id" ON "conversation_favorite" ("user_id");

-- 对话模板表
CREATE TABLE IF NOT EXISTS "conversation_template" (
    "id" BIGSERIAL PRIMARY KEY,
    "title" VARCHAR(200) NOT NULL,
    "content" TEXT NOT NULL,
    "category" VARCHAR(50),
    "usage_count" INTEGER DEFAULT 0,
    "is_public" BOOLEAN DEFAULT false,
    "create_user_id" BIGINT,
    "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 系统配置表
CREATE TABLE IF NOT EXISTS "system_config" (
    "id" BIGSERIAL PRIMARY KEY,
    "config_key" VARCHAR(100) NOT NULL UNIQUE,
    "config_value" TEXT,
    "config_type" VARCHAR(50),
    "description" VARCHAR(200),
    "is_editable" BOOLEAN DEFAULT true,
    "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS "idx_system_config_key" ON "system_config" ("config_key");

-- 知识审核表
CREATE TABLE IF NOT EXISTS "knowledge_audit" (
    "id" BIGSERIAL PRIMARY KEY,
    "knowledge_id" BIGINT,
    "title" VARCHAR(200),
    "content" TEXT,
    "category" VARCHAR(50),
    "audit_status" VARCHAR(20) DEFAULT 'pending',
    "audit意见" TEXT,
    "submit_user_id" BIGINT,
    "audit_user_id" BIGINT,
    "submit_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "audit_time" TIMESTAMP
);

CREATE INDEX IF NOT EXISTS "idx_knowledge_audit_status" ON "knowledge_audit" ("audit_status");

-- 热门搜索统计表（定时更新）
CREATE TABLE IF NOT EXISTS "hot_search" (
    "id" BIGSERIAL PRIMARY KEY,
    "keyword" VARCHAR(200) NOT NULL,
    "search_count" INTEGER DEFAULT 0,
    "period" VARCHAR(20),
    "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS "idx_hot_search_keyword" ON "hot_search" ("keyword");
