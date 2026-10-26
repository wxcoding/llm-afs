-- 知识库文档向量化处理状态：PENDING 待处理 / PROCESSING 处理中 / SUCCESS 已完成 / FAILED 失败
-- 存量数据在旧流程中已完成索引（或正文为空无需索引），统一置为 SUCCESS
ALTER TABLE knowledge ADD COLUMN IF NOT EXISTS vector_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

COMMENT ON COLUMN knowledge.vector_status IS '向量化处理状态：PENDING/PROCESSING/SUCCESS/FAILED';

UPDATE knowledge SET vector_status = 'SUCCESS';
