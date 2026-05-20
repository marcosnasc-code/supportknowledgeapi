-- Busca textual (full-text) em português sobre o conteúdo dos chunks.

ALTER TABLE knowledge_chunk
    ADD COLUMN search_vector tsvector;

UPDATE knowledge_chunk
SET search_vector = to_tsvector('portuguese', COALESCE(content, ''))
WHERE search_vector IS NULL;

CREATE INDEX idx_knowledge_chunk_search_vector ON knowledge_chunk USING GIN (search_vector);

CREATE OR REPLACE FUNCTION knowledge_chunk_search_vector_update()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector := to_tsvector('portuguese', COALESCE(NEW.content, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_knowledge_chunk_search_vector
    BEFORE INSERT OR UPDATE OF content ON knowledge_chunk
    FOR EACH ROW
    EXECUTE FUNCTION knowledge_chunk_search_vector_update();
