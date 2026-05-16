package com.mpsupport.knowledge.service;

import com.mpsupport.knowledge.config.ImportCsvProperties;
import com.mpsupport.knowledge.config.ImportProperties;
import com.mpsupport.knowledge.domain.ChunkSource;
import org.apache.commons.csv.CSVRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ChunkPersistenceService {

    private static final String UPSERT_SQL = """
            INSERT INTO knowledge_chunk (id, import_batch_id, ticket_id, source, content, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (ticket_id, source) DO UPDATE SET
                content = EXCLUDED.content,
                import_batch_id = EXCLUDED.import_batch_id,
                created_at = EXCLUDED.created_at
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ImportCsvProperties csvProperties;
    private final ImportProperties importProperties;
    private final List<ChunkPending> buffer = new ArrayList<>();

    public ChunkPersistenceService(
            JdbcTemplate jdbcTemplate,
            ImportCsvProperties csvProperties,
            ImportProperties importProperties
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.csvProperties = csvProperties;
        this.importProperties = importProperties;
    }

    /**
     * Extrai campos não vazios do registro e enfileira chunks; faz flush quando o buffer atinge o tamanho configurado.
     *
     * @return quantidade de chunks enfileirados/gravados nesta chamada (após flushes internos)
     */
    public long persistFromRecord(UUID importBatchId, String ticketId, CSVRecord record) {
        long added = 0;
        added += enqueueIfPresent(importBatchId, ticketId, ChunkSource.DESCRICAO,
                safeGet(record, csvProperties.getUserDescriptionHeader()));
        added += enqueueIfPresent(importBatchId, ticketId, ChunkSource.LOG_PUBLICO,
                safeGet(record, csvProperties.getPublicLogHeader()));
        added += enqueueIfPresent(importBatchId, ticketId, ChunkSource.SOLUCAO,
                safeGet(record, csvProperties.getFinalSolutionHeader()));
        added += enqueueIfPresent(importBatchId, ticketId, ChunkSource.LOG_PRIVADO,
                safeGet(record, csvProperties.getPrivateLogHeader()));
        return added;
    }

    /**
     * Grava o buffer no PostgreSQL (commit por lote; adequado para imports grandes).
     */
    public void flush() {
        if (buffer.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(UPSERT_SQL, buffer, buffer.size(), (PreparedStatement ps, ChunkPending chunk) -> {
            ps.setObject(1, chunk.id());
            ps.setObject(2, chunk.importBatchId());
            ps.setString(3, chunk.ticketId());
            ps.setString(4, chunk.source().name());
            ps.setString(5, chunk.content());
            ps.setTimestamp(6, Timestamp.from(chunk.createdAt()));
        });
        buffer.clear();
    }

    public void flushIfBufferFull() {
        if (buffer.size() >= importProperties.getChunkBatchSize()) {
            flush();
        }
    }

    public void flushRemaining() {
        flush();
    }

    private long enqueueIfPresent(UUID importBatchId, String ticketId, ChunkSource source, String content) {
        if (content.isEmpty()) {
            return 0;
        }
        buffer.add(new ChunkPending(
                UUID.randomUUID(),
                importBatchId,
                ticketId,
                source,
                content,
                Instant.now()
        ));
        flushIfBufferFull();
        return 1;
    }

    private static String safeGet(CSVRecord record, String header) {
        try {
            String v = record.get(header);
            return v == null ? "" : v.strip();
        } catch (IllegalArgumentException ex) {
            return "";
        }
    }
}
