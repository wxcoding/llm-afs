-- 知识表增加全文检索：search_vector 由触发器自动维护，title 权重(A)高于 content 权重(B)
-- 'simple' 分类器不做词干提取，适合中文
ALTER TABLE knowledge ADD COLUMN search_vector TSVECTOR;

CREATE OR REPLACE FUNCTION knowledge_search_vector_update() RETURNS TRIGGER AS $$
BEGIN
    -- 将 title 和 content 合并转换为搜索向量
    NEW."search_vector" :=
        setweight(to_tsvector('simple', coalesce(NEW."title", '')), 'A') ||
        setweight(to_tsvector('simple', coalesce(NEW."content", '')), 'B');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_knowledge_search_vector_update
    BEFORE INSERT OR UPDATE ON knowledge
    FOR EACH ROW EXECUTE FUNCTION knowledge_search_vector_update();

-- 回填存量数据
UPDATE knowledge SET search_vector =
    setweight(to_tsvector('simple', coalesce(title, '')), 'A') ||
    setweight(to_tsvector('simple', coalesce(content, '')), 'B');

CREATE INDEX idx_knowledge_search_vector ON knowledge USING GIN (search_vector);
