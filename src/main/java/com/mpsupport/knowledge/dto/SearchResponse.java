package com.mpsupport.knowledge.dto;

import java.util.List;

public record SearchResponse(
        String query,
        int topK,
        long totalMatches,
        List<SearchResultItem> results
) {
}
