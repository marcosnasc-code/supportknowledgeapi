-- pgvector + colunas de embedding para busca semântica (Ollama nomic-embed-text = 768 dimensões).

CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE knowledge_chunk
    ADD COLUMN IF NOT EXISTS embedding vector(768),
    ADD COLUMN IF NOT EXISTS embedding_model VARCHAR(64),
    ADD COLUMN IF NOT EXISTS embedded_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_embedding_hnsw
    ON knowledge_chunk
    USING hnsw (embedding vector_cosine_ops)
    WHERE embedding IS NOT NULL;
