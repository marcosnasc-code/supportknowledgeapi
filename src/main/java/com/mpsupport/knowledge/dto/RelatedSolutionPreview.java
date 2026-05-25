package com.mpsupport.knowledge.dto;

/**
 * Solução do mesmo {@code ticketId} do resultado da busca (ex.: match em DESCRICAO → solução para expandir).
 */
public record RelatedSolutionPreview(
        String snippet,
        SearchCitation citation
) {
}
