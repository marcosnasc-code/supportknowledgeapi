package com.mpsupport.knowledge.service;

import com.mpsupport.knowledge.config.SearchProperties;
import com.mpsupport.knowledge.domain.ChunkSource;
import com.mpsupport.knowledge.dto.SearchCitation;
import com.mpsupport.knowledge.dto.SearchFilters;
import com.mpsupport.knowledge.dto.SearchRequest;
import com.mpsupport.knowledge.dto.SearchResponse;
import com.mpsupport.knowledge.dto.SearchResultItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SearchService {

    private final JdbcTemplate jdbcTemplate;
    private final SearchProperties searchProperties;

    public SearchService(JdbcTemplate jdbcTemplate, SearchProperties searchProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.searchProperties = searchProperties;
    }

    public SearchResponse search(SearchRequest request) {
        String config = searchProperties.getTextSearchConfig();
        String queryText = request.query().strip();
        int topK = request.resolvedTopK();

        // Cast explícito: JDBC envia parâmetros como text; sem ::regconfig o Postgres
        // não resolve plainto_tsquery(regconfig, text) e lança BadSqlGrammarException.
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

        List<SearchResultItem> results = jdbcTemplate.query(
                selectSql,
                (rs, rowNum) -> {
                    UUID chunkId = (UUID) rs.getObject("id");
                    String ticketId = rs.getString("ticket_id");
                    ChunkSource source = ChunkSource.valueOf(rs.getString("source"));
                    String content = rs.getString("content");
                    double score = rs.getDouble("rank");
                    String snippet = buildSnippet(content);
                    return new SearchResultItem(
                            score,
                            ticketId,
                            source.name(),
                            snippet,
                            new SearchCitation(chunkId, ticketId, source)
                    );
                },
                selectParams.toArray()
        );

        return new SearchResponse(queryText, topK, totalMatches, results);
    }

    private long countMatches(StringBuilder where, List<Object> params) {
        String countSql = "SELECT COUNT(*) " + where;
        Long count = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
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
