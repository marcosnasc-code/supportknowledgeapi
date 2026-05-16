CREATE TABLE import_batch (
    id              UUID PRIMARY KEY,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    source_file_name VARCHAR(512),
    processed_rows  BIGINT NOT NULL DEFAULT 0,
    skipped_rows    BIGINT NOT NULL DEFAULT 0,
    chunks_created  BIGINT NOT NULL DEFAULT 0,
    status          VARCHAR(32) NOT NULL
);

CREATE TABLE knowledge_chunk (
    id               UUID PRIMARY KEY,
    import_batch_id  UUID NOT NULL REFERENCES import_batch (id),
    ticket_id        VARCHAR(128) NOT NULL,
    source           VARCHAR(32) NOT NULL,
    content          TEXT NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_knowledge_chunk_ticket_source UNIQUE (ticket_id, source)
);

CREATE INDEX idx_knowledge_chunk_ticket_id ON knowledge_chunk (ticket_id);
CREATE INDEX idx_knowledge_chunk_import_batch_id ON knowledge_chunk (import_batch_id);
