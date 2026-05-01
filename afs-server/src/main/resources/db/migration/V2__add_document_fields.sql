-- 为知识库表添加文档来源字段
ALTER TABLE "knowledge" ADD COLUMN IF NOT EXISTS "document_type" VARCHAR(20);
ALTER TABLE "knowledge" ADD COLUMN IF NOT EXISTS "source_file" VARCHAR(500);
ALTER TABLE "knowledge" ADD COLUMN IF NOT EXISTS "file_size" BIGINT;
ALTER TABLE "knowledge" ADD COLUMN IF NOT EXISTS "char_count" INT;
ALTER TABLE "knowledge" ADD COLUMN IF NOT EXISTS "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

COMMENT ON COLUMN "knowledge"."document_type" IS '文档类型: pdf, word, markdown, excel, text';
COMMENT ON COLUMN "knowledge"."source_file" IS '原始文件名';
COMMENT ON COLUMN "knowledge"."file_size" IS '文件大小(字节)';
COMMENT ON COLUMN "knowledge"."char_count" IS '文档字符数';

CREATE INDEX IF NOT EXISTS "idx_knowledge_document_type" ON "knowledge" ("document_type");