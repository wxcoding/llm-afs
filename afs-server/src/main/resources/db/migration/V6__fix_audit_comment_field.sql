-- V6__fix_audit_comment_field.sql

-- 重命名 audit意见 为 audit_comment（仅当列存在时执行）
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'knowledge_audit' AND column_name = 'audit意见'
    ) THEN
        ALTER TABLE "knowledge_audit" RENAME COLUMN "audit意见" TO "audit_comment";
    END IF;
END $$;
