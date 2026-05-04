-- 添加知识库状态字段
-- 状态: DRAFT-草稿, PENDING_REVIEW-待审核, ACTIVE-已发布, REJECTED-已拒绝

ALTER TABLE knowledge ADD COLUMN status VARCHAR(20) DEFAULT 'ACTIVE';
