package com.mpsupport.knowledge.dto;

import jakarta.validation.constraints.Min;

public record EmbeddingIndexRequest(
        /**
         * Máximo de chunks a processar nesta chamada. Null ou 0 = usar limite padrão do YAML (0 = todos pendentes).
         */
        @Min(0)
        Integer limit,

        Boolean onlyMissing
) {
    public boolean resolvedOnlyMissing() {
        return onlyMissing == null || onlyMissing;
    }

    public int resolvedLimit(int defaultLimit) {
        if (limit != null && limit > 0) {
            return limit;
        }
        return defaultLimit;
    }
}
