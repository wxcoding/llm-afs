-- 助手消息降级标记：AI 调用失败时返回的是兜底文案，不是基于知识库生成的内容
-- 用于保证实时渲染与历史回放一致（降级消息不展示参考来源）
ALTER TABLE chat_message ADD COLUMN IF NOT EXISTS degraded BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN chat_message.degraded IS '是否为降级回复（AI 调用失败时的兜底文案）';
