package com.mpsupport.knowledge.dto;

public record SearchResultItem(
        double score,
        String ticketId,
        String source,
        String snippet,
        SearchCitation citation
) {
}
