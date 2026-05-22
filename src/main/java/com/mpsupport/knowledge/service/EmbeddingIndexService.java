package com.mpsupport.knowledge.service;

import com.mpsupport.knowledge.config.EmbeddingProperties;
import com.mpsupport.knowledge.config.OllamaProperties;
import com.mpsupport.knowledge.dto.EmbeddingIndexRequest;
import com.mpsupport.knowledge.dto.EmbeddingIndexResponse;
import com.mpsupport.knowledge.dto.EmbeddingStatusResponse;
import com.mpsupport.knowledge.integration.OllamaEmbeddingClient;
import com.mpsupport.knowledge.util.VectorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class EmbeddingIndexService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingIndexService.class);

    private final JdbcTemplate jdbcTemplate;
    private final OllamaEmbeddingClient ollamaEmbeddingClient;
    private final OllamaProperties ollamaProperties;
    private final EmbeddingProperties embeddingProperties;
    private final AtomicBoolean indexingRunning = new AtomicBoolean(false);

    public EmbeddingIndexService(
            JdbcTemplate jdbcTemplate,
            OllamaEmbeddingClient ollamaEmbeddingClient,
            OllamaProperties ollamaProperties,
            EmbeddingProperties embeddingProperties
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.ollamaEmbeddingClient = ollamaEmbeddingClient;
        this.ollamaProperties = ollamaProperties;
        this.embeddingProperties = embeddingProperties;
    }

    public EmbeddingStatusResponse getStatus() {
        long total = countTotalChunks();
        long embedded = countEmbeddedChunks();
        long missing = total - embedded;
        double percent = total == 0 ? 0.0 : (embedded * 100.0) / total;
        return new EmbeddingStatusResponse(
                total,
                embedded,
                missing,
                percent,
                indexingRunning.get(),
                ollamaProperties.getEmbeddingModel()
        );
    }

    public EmbeddingIndexResponse index(EmbeddingIndexRequest request) {
        if (!indexingRunning.compareAndSet(false, true)) {
            throw new IllegalStateException("Indexação de embeddings já está em execução.");
        }

        try {
            int limit = request.resolvedLimit(embeddingProperties.getDefaultIndexLimit());
            List<ChunkEmbeddingRow> pending = loadPendingChunks(request.resolvedOnlyMissing(), limit);

            if (pending.isEmpty()) {
                return buildIndexResponse(0, "Nenhum chunk pendente de embedding.");
            }

            log.info("Iniciando indexação de embeddings: pendentes={}, loteOllama={}, modelo={}. "
                            + "Esta requisição HTTP pode levar horas; acompanhe os logs.",
                    pending.size(), embeddingProperties.getBatchSize(), ollamaProperties.getEmbeddingModel());

            long processed = 0;
            int batchSize = embeddingProperties.getBatchSize();
            String updateSql = """
                    UPDATE knowledge_chunk
                    SET embedding = ?::vector,
                        embedding_model = ?,
                        embedded_at = NOW()
                    WHERE id = ?
                    """;

            for (int offset = 0; offset < pending.size(); offset += batchSize) {
                int end = Math.min(offset + batchSize, pending.size());
                List<ChunkEmbeddingRow> batch = pending.subList(offset, end);

                List<String> texts = batch.stream().map(ChunkEmbeddingRow::content).toList();
                List<float[]> vectors = ollamaEmbeddingClient.embedBatch(texts);

                for (int i = 0; i < batch.size(); i++) {
                    ChunkEmbeddingRow row = batch.get(i);
                    String vectorLiteral = VectorUtils.toVectorLiteral(vectors.get(i));
                    jdbcTemplate.update(
                            updateSql,
                            vectorLiteral,
                            ollamaProperties.getEmbeddingModel(),
                            row.id()
                    );
                    processed++;
                }

                if (processed % 500 == 0 || end == pending.size()) {
                    log.info("Embeddings indexados nesta execução: {}/{}", processed, pending.size());
                }
            }

            return buildIndexResponse(processed, "Indexação concluída para este lote.");
        } catch (Exception e) {
            log.error("Falha na indexação de embeddings", e);
            throw new IllegalStateException(
                    "Falha na indexação de embeddings: " + rootMessage(e)
                            + ". Verifique Ollama (ollama serve), modelo "
                            + ollamaProperties.getEmbeddingModel()
                            + ", pgvector (Flyway V3) e logs da API.",
                    e
            );
        } finally {
            indexingRunning.set(false);
        }
    }

    private static String rootMessage(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage() != null ? root.getMessage() : e.getClass().getSimpleName();
    }

    private EmbeddingIndexResponse buildIndexResponse(long processedInRun, String message) {
        long total = countTotalChunks();
        long embedded = countEmbeddedChunks();
        long missing = total - embedded;
        return new EmbeddingIndexResponse(
                processedInRun,
                missing,
                total,
                ollamaProperties.getEmbeddingModel(),
                message
        );
    }

    private List<ChunkEmbeddingRow> loadPendingChunks(boolean onlyMissing, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, content
                FROM knowledge_chunk
                WHERE content IS NOT NULL AND length(trim(content)) > 0
                """);
        if (onlyMissing) {
            sql.append(" AND embedding IS NULL ");
        }
        sql.append(" ORDER BY created_at ");

        List<Object> params = new ArrayList<>();
        if (limit > 0) {
            sql.append(" LIMIT ? ");
            params.add(limit);
        }

        return jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> new ChunkEmbeddingRow(
                        (UUID) rs.getObject("id"),
                        rs.getString("content")
                ),
                params.toArray()
        );
    }

    private long countTotalChunks() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM knowledge_chunk", Long.class);
        return count != null ? count : 0;
    }

    private long countEmbeddedChunks() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM knowledge_chunk WHERE embedding IS NOT NULL",
                Long.class
        );
        return count != null ? count : 0;
    }
}
