package com.mpsupport.knowledge.dto;

public record SearchResultItem(
        double score,
        String ticketId,
        String source,
        String snippet,
        SearchCitation citation,
        /**
         * Preenchido quando {@code filters.includeSolution} e o match não é em SOLUCAO:
         * trecho da solução do mesmo chamado (use GET ticket chunk para texto completo ao expandir).
         */
        RelatedSolutionPreview solution
) {
    public SearchResultItem(
            double score,
            String ticketId,
            String source,
            String snippet,
            SearchCitation citation
    ) {
        this(score, ticketId, source, snippet, citation, null);
    }
}
