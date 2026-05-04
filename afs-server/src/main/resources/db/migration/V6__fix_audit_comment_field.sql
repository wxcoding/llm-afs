-- V6__fix_audit_comment_field.sql

-- 重命名 audit意见 为 audit_comment
ALTER TABLE "knowledge_audit" RENAME COLUMN "audit意见" TO "audit_comment";