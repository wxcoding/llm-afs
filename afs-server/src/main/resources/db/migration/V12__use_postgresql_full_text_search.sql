ALTER TABLE rag_chunk
    ADD COLUMN search_text TEXT NOT NULL DEFAULT '';

UPDATE rag_chunk
SET search_text = array_to_string(terms, ' ');

ALTER TABLE rag_chunk
    ALTER COLUMN search_text DROP DEFAULT,
    ADD COLUMN search_vector TSVECTOR GENERATED ALWAYS AS (
        to_tsvector('simple'::regconfig, search_text)
    ) STORED;

DROP INDEX IF EXISTS idx_rag_chunk_terms;

CREATE INDEX IF NOT EXISTS idx_rag_chunk_search_vector
    ON rag_chunk USING GIN (search_vector);

ALTER TABLE rag_chunk
    DROP COLUMN terms,
    DROP COLUMN term_count;
