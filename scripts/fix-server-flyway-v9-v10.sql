-- ============================================================
-- 服务器环境一次性补齐脚本：V9 / V10 迁移效果 + flyway 历史记录
-- 背景：服务器 flyway_schema_history 缺 V9/V10 记录（建库时迁移文件丢失），
--       需手动补齐后，新代码才能通过 Flyway 校验并自动执行 V18。
-- 特性：幂等，可重复执行；已存在的字段/索引/触发器会跳过。
-- 执行方式：在服务器 afs 库上整体执行本脚本（DBeaver 中 Alt+X 执行脚本）。
-- ============================================================

BEGIN;

-- ---------- V9: knowledge 全文检索 ----------
ALTER TABLE knowledge ADD COLUMN IF NOT EXISTS search_vector TSVECTOR;

CREATE OR REPLACE FUNCTION knowledge_search_vector_update() RETURNS TRIGGER AS $$
BEGIN
    NEW."search_vector" :=
        setweight(to_tsvector('simple', coalesce(NEW."title", '')), 'A') ||
        setweight(to_tsvector('simple', coalesce(NEW."content", '')), 'B');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger
        WHERE tgname = 'trg_knowledge_search_vector_update'
          AND tgrelid = 'knowledge'::regclass
    ) THEN
        CREATE TRIGGER trg_knowledge_search_vector_update
            BEFORE INSERT OR UPDATE ON knowledge
            FOR EACH ROW EXECUTE FUNCTION knowledge_search_vector_update();
    END IF;
END $$;

-- 回填存量数据（只处理空值行）
UPDATE knowledge SET search_vector =
    setweight(to_tsvector('simple', coalesce(title, '')), 'A') ||
    setweight(to_tsvector('simple', coalesce(content, '')), 'B')
WHERE search_vector IS NULL;

CREATE INDEX IF NOT EXISTS idx_knowledge_search_vector ON knowledge USING GIN (search_vector);

-- ---------- V10: user 表角色/状态等字段 ----------
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS "role" VARCHAR(20) NOT NULL DEFAULT 'USER';
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS "status" VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS "last_login_time" TIMESTAMP;
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS "login_count" INTEGER DEFAULT 0;
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS "created_by" VARCHAR(64);
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS "updated_by" VARCHAR(64);
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS "deleted" BOOLEAN DEFAULT FALSE;

-- ---------- 补 flyway 历史记录（checksum 与仓库中 V9/V10 文件一致，勿改） ----------
INSERT INTO flyway_schema_history
    (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
SELECT (SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM flyway_schema_history),
       '9', 'add fulltext search', 'SQL', 'V9__add_fulltext_search.sql',
       -448102418, current_user, now(), 0, true
WHERE NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = '9');

INSERT INTO flyway_schema_history
    (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
SELECT (SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM flyway_schema_history),
       '10', 'add user role and status', 'SQL', 'V10__add_user_role_and_status.sql',
       1013902621, current_user, now(), 0, true
WHERE NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = '10');

COMMIT;

-- 执行后自查：应返回 9 和 10 两行
SELECT version, script, checksum, success FROM flyway_schema_history WHERE version IN ('9', '10');
