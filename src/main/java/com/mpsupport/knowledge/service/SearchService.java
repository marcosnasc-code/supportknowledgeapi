package com.mpsupport.knowledge.service;

import com.mpsupport.knowledge.config.SearchProperties;
import com.mpsupport.knowledge.config.SystemHintsProperties;
import com.mpsupport.knowledge.domain.SystemHintDefinition;
import com.mpsupport.knowledge.domain.ChunkSource;
import com.mpsupport.knowledge.dto.RelatedSolutionPreview;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private static final int HYBRID_RRF_K = 60;

    private final JdbcTemplate jdbcTemplate;
    private final SearchProperties searchProperties;
    private final OllamaEmbeddingClient ollamaEmbeddingClient;
    private final EmbeddingIndexService embeddingIndexService;
    private final SystemHintService systemHintService;
    private final SystemHintsProperties systemHintsProperties;

    public SearchService(
            JdbcTemplate jdbcTemplate,
            SearchProperties searchProperties,
            OllamaEmbeddingClient ollamaEmbeddingClient,
            EmbeddingIndexService embeddingIndexService,
            SystemHintService systemHintService,
            SystemHintsProperties systemHintsProperties
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.searchProperties = searchProperties;
        this.ollamaEmbeddingClient = ollamaEmbeddingClient;
        this.embeddingIndexService = embeddingIndexService;
        this.systemHintService = systemHintService;
        this.systemHintsProperties = systemHintsProperties;
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
        List<SearchResultItem> results = enrichWithSolutions(
                applyDeclaredSystemBoost(
                        queryTextResults(where, params, topK),
                        request.filters()
                ),
                request.filters()
        );

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

        results = enrichWithSolutions(
                applyDeclaredSystemBoost(results, request.filters()),
                request.filters()
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
                            item.citation(),
                            item.solution()
                    );
                })
                .toList();

        long totalMatches = Math.max(textResponse.totalMatches(), semanticResponse.totalMatches());
        List<SearchResultItem> boosted = enrichWithSolutions(
                applyDeclaredSystemBoost(merged, request.filters()),
                request.filters()
        );
        return new SearchResponse(request.query().strip(), request.resolvedTopK(), totalMatches, boosted);
    }

    private List<SearchResultItem> applyDeclaredSystemBoost(
            List<SearchResultItem> results,
            SearchFilters filters
    ) {
        if (results.isEmpty() || filters == null || filters.sistemaDeclarado() == null
                || filters.sistemaDeclarado().isBlank()) {
            return results;
        }

        Optional<SystemHintDefinition> declared = systemHintService.resolveDeclared(filters.sistemaDeclarado());
        if (declared.isEmpty()) {
            return results;
        }

        double multiplier = systemHintsProperties.getDeclaredBoostMultiplier();
        SystemHintDefinition system = declared.get();

        return results.stream()
                .map(item -> {
                    if (systemHintService.textMentionsSystem(item.snippet(), system)) {
                        return new SearchResultItem(
                                item.score() * multiplier,
                                item.ticketId(),
                                item.source(),
                                item.snippet(),
                                item.citation(),
                                item.solution()
                        );
                    }
                    return item;
                })
                .sorted(Comparator.comparingDouble(SearchResultItem::score).reversed())
                .toList();
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

    /**
     * Para cada match (ex.: DESCRICAO), anexa trecho da SOLUCAO do mesmo ticket.
     */
    private List<SearchResultItem> enrichWithSolutions(List<SearchResultItem> results, SearchFilters filters) {
        if (results.isEmpty() || filters == null || !filters.resolvedIncludeSolution()) {
            return results;
        }

        List<String> ticketIds = results.stream()
                .filter(item -> !ChunkSource.SOLUCAO.name().equals(item.source()))
                .map(SearchResultItem::ticketId)
                .distinct()
                .toList();
        if (ticketIds.isEmpty()) {
            return results;
        }

        Map<String, SolutionRow> solutionsByTicket = loadSolutionsByTicket(ticketIds);

        return results.stream()
                .map(item -> attachSolution(item, solutionsByTicket.get(item.ticketId())))
                .toList();
    }

    private Map<String, SolutionRow> loadSolutionsByTicket(List<String> ticketIds) {
        String placeholders = ticketIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        String sql = """
                SELECT id, ticket_id, content
                FROM knowledge_chunk
                WHERE source = 'SOLUCAO' AND ticket_id IN (%s)
                """.formatted(placeholders);

        List<SolutionRow> rows = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new SolutionRow(
                        (UUID) rs.getObject("id"),
                        rs.getString("ticket_id"),
                        rs.getString("content")
                ),
                ticketIds.toArray()
        );

        Map<String, SolutionRow> map = new HashMap<>();
        for (SolutionRow row : rows) {
            map.putIfAbsent(row.ticketId(), row);
        }
        return map;
    }

    private SearchResultItem attachSolution(SearchResultItem item, SolutionRow solution) {
        if (solution == null || ChunkSource.SOLUCAO.name().equals(item.source())) {
            return item;
        }
        RelatedSolutionPreview preview = new RelatedSolutionPreview(
                buildSnippet(solution.content()),
                new SearchCitation(solution.id(), solution.ticketId(), ChunkSource.SOLUCAO)
        );
        return new SearchResultItem(
                item.score(),
                item.ticketId(),
                item.source(),
                item.snippet(),
                item.citation(),
                preview
        );
    }

    private record SolutionRow(UUID id, String ticketId, String content) {
    }
}
