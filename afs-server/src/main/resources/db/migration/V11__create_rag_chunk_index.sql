CREATE TABLE IF NOT EXISTS rag_chunk (
    chunk_id VARCHAR(36) PRIMARY KEY,
    source_type VARCHAR(30) NOT NULL,
    source_id BIGINT NOT NULL,
    title VARCHAR(500),
    category VARCHAR(200),
    content TEXT NOT NULL,
    terms TEXT[] NOT NULL,
    term_count INTEGER NOT NULL CHECK (term_count >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_rag_chunk_source
    ON rag_chunk (source_type, source_id);

CREATE INDEX IF NOT EXISTS idx_rag_chunk_terms
    ON rag_chunk USING GIN (terms);
