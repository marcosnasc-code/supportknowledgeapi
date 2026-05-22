package com.mpsupport.knowledge.service;

import com.mpsupport.knowledge.config.SearchProperties;
import com.mpsupport.knowledge.domain.ChunkSource;
import com.mpsupport.knowledge.dto.SearchCitation;
import com.mpsupport.knowledge.dto.SearchFilters;
import com.mpsupport.knowledge.dto.SearchMode;
import com.mpsupport.knowledge.dto.SearchRequest;
import com.mpsupport.knowledge.dto.SearchResponse;
import com.mpsupport.knowledge.dto.SearchResultItem;
import com.mpsupport.knowledge.integration.OllamaEmbeddingClient;
import com.mpsupport.knowledge.util.VectorUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SearchService {

    private static final int HYBRID_RRF_K = 60;

    private final JdbcTemplate jdbcTemplate;
    private final SearchProperties searchProperties;
    private final OllamaEmbeddingClient ollamaEmbeddingClient;
    private final EmbeddingIndexService embeddingIndexService;

    public SearchService(
            JdbcTemplate jdbcTemplate,
            SearchProperties searchProperties,
            OllamaEmbeddingClient ollamaEmbeddingClient,
            EmbeddingIndexService embeddingIndexService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.searchProperties = searchProperties;
        this.ollamaEmbeddingClient = ollamaEmbeddingClient;
        this.embeddingIndexService = embeddingIndexService;
    }

    public SearchResponse search(SearchRequest request) {
        return switch (request.resolvedMode()) {
            case TEXT -> searchText(request);
            case SEMANTIC -> searchSemantic(request);
            case HYBRID -> searchHybrid(request);
        };
    }

    private SearchResponse searchText(SearchRequest request) {
        String config = searchProperties.getTextSearchConfig();
        String queryText = request.query().strip();
        int topK = request.resolvedTopK();

        StringBuilder where = new StringBuilder("""
                FROM knowledge_chunk k,
                     plainto_tsquery(?::regconfig, ?) query
                WHERE k.search_vector @@ query
                """);
        List<Object> params = new ArrayList<>();
        params.add(config);
        params.add(queryText);
        appendSourceFilter(request.filters(), where, params);

        long totalMatches = countMatches(where, params);
        List<SearchResultItem> results = queryTextResults(where, params, topK);

        return new SearchResponse(queryText, topK, totalMatches, results);
    }

    private SearchResponse searchSemantic(SearchRequest request) {
        ensureEmbeddingsAvailable();

        String queryText = request.query().strip();
        int topK = request.resolvedTopK();
        float[] queryVector = ollamaEmbeddingClient.embed(queryText);
        String vectorLiteral = VectorUtils.toVectorLiteral(queryVector);

        StringBuilder where = new StringBuilder("""
                FROM knowledge_chunk k
                WHERE k.embedding IS NOT NULL
                """);
        List<Object> params = new ArrayList<>();
        appendSourceFilter(request.filters(), where, params);

        long totalMatches = countSemanticMatches(where, vectorLiteral, params);

        String selectSql = """
                SELECT k.id,
                       k.ticket_id,
                       k.source,
                       k.content,
                       (k.embedding <=> ?::vector) AS distance
                """ + where + """
                ORDER BY distance
                LIMIT ?
                """;
        List<Object> selectParams = new ArrayList<>();
        selectParams.add(vectorLiteral);
        selectParams.addAll(params);
        selectParams.add(topK);

        List<SearchResultItem> results = jdbcTemplate.query(
                selectSql,
                (rs, rowNum) -> toResultItem(
                        (UUID) rs.getObject("id"),
                        rs.getString("ticket_id"),
                        rs.getString("source"),
                        rs.getString("content"),
                        distanceToScore(rs.getDouble("distance"))
                ),
                selectParams.toArray()
        );

        return new SearchResponse(queryText, topK, totalMatches, results);
    }

    private SearchResponse searchHybrid(SearchRequest request) {
        int fetchK = Math.min(request.resolvedTopK() * 3, 50);

        SearchRequest fetchRequest = new SearchRequest(
                request.query(),
                fetchK,
                request.filters(),
                request.resolvedMode()
        );
        SearchResponse textResponse = searchText(widenRequestMode(fetchRequest, SearchMode.TEXT));
        SearchResponse semanticResponse = searchSemantic(widenRequestMode(fetchRequest, SearchMode.SEMANTIC));

        Map<UUID, Double> fusedScores = new LinkedHashMap<>();
        Map<UUID, SearchResultItem> items = new LinkedHashMap<>();

        applyRrf(fusedScores, items, textResponse.results());
        applyRrf(fusedScores, items, semanticResponse.results());

        List<SearchResultItem> merged = fusedScores.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(request.resolvedTopK())
                .map(entry -> {
                    SearchResultItem item = items.get(entry.getKey());
                    return new SearchResultItem(
                            entry.getValue(),
                            item.ticketId(),
                            item.source(),
                            item.snippet(),
                            item.citation()
                    );
                })
                .toList();

        long totalMatches = Math.max(textResponse.totalMatches(), semanticResponse.totalMatches());
        return new SearchResponse(request.query().strip(), request.resolvedTopK(), totalMatches, merged);
    }

    private static SearchRequest widenRequestMode(SearchRequest request, SearchMode mode) {
        return new SearchRequest(request.query(), request.topK(), request.filters(), mode);
    }

    private static void applyRrf(
            Map<UUID, Double> fusedScores,
            Map<UUID, SearchResultItem> items,
            List<SearchResultItem> ranked
    ) {
        for (int i = 0; i < ranked.size(); i++) {
            SearchResultItem item = ranked.get(i);
            UUID chunkId = item.citation().chunkId();
            double contribution = 1.0 / (HYBRID_RRF_K + i + 1);
            fusedScores.merge(chunkId, contribution, Double::sum);
            items.putIfAbsent(chunkId, item);
        }
    }

    private List<SearchResultItem> queryTextResults(StringBuilder where, List<Object> params, int topK) {
        String selectSql = """
                SELECT k.id,
                       k.ticket_id,
                       k.source,
                       k.content,
                       ts_rank_cd(k.search_vector, query, 32) AS rank
                """ + where + """
                ORDER BY rank DESC
                LIMIT ?
                """;
        List<Object> selectParams = new ArrayList<>(params);
        selectParams.add(topK);

        return jdbcTemplate.query(
                selectSql,
                (rs, rowNum) -> toResultItem(
                        (UUID) rs.getObject("id"),
                        rs.getString("ticket_id"),
                        rs.getString("source"),
                        rs.getString("content"),
                        rs.getDouble("rank")
                ),
                selectParams.toArray()
        );
    }

    private SearchResultItem toResultItem(UUID chunkId, String ticketId, String source, String content, double score) {
        ChunkSource chunkSource = ChunkSource.valueOf(source);
        String snippet = buildSnippet(content);
        return new SearchResultItem(
                score,
                ticketId,
                source,
                snippet,
                new SearchCitation(chunkId, ticketId, chunkSource)
        );
    }

    private static double distanceToScore(double distance) {
        return 1.0 / (1.0 + distance);
    }

    private void ensureEmbeddingsAvailable() {
        if (embeddingIndexService.getStatus().embeddedChunks() == 0) {
            throw new IllegalArgumentException(
                    "Nenhum chunk com embedding indexado. Execute POST /api/v1/embeddings/index antes da busca SEMANTIC/HYBRID."
            );
        }
    }

    private long countMatches(StringBuilder where, List<Object> params) {
        String countSql = "SELECT COUNT(*) " + where;
        Long count = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        return count != null ? count : 0;
    }

    private long countSemanticMatches(StringBuilder where, String vectorLiteral, List<Object> params) {
        String countSql = "SELECT COUNT(*) " + where + " AND (embedding <=> ?::vector) < 1.0 ";
        List<Object> countParams = new ArrayList<>();
        countParams.addAll(params);
        countParams.add(vectorLiteral);
        Long count = jdbcTemplate.queryForObject(countSql, Long.class, countParams.toArray());
        return count != null ? count : 0;
    }

    private static void appendSourceFilter(SearchFilters filters, StringBuilder where, List<Object> params) {
        if (filters == null || filters.sources() == null || filters.sources().isEmpty()) {
            return;
        }
        where.append(" AND k.source IN (");
        List<ChunkSource> sources = filters.sources();
        for (int i = 0; i < sources.size(); i++) {
            if (i > 0) {
                where.append(", ");
            }
            where.append("?");
            params.add(sources.get(i).name());
        }
        where.append(") ");
    }

    private String buildSnippet(String content) {
        if (content == null) {
            return "";
        }
        String normalized = content.strip().replaceAll("\\s+", " ");
        int max = searchProperties.getSnippetMaxLength();
        if (normalized.length() <= max) {
            return normalized;
        }
        return normalized.substring(0, max) + "...";
    }
}
