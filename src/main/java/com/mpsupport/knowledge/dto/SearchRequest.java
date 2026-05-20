package com.mpsupport.knowledge.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SearchRequest(
        @NotBlank(message = "query é obrigatória")
        @Size(min = 2, max = 2000, message = "query deve ter entre 2 e 2000 caracteres")
        String query,

        @Min(value = 1, message = "topK mínimo é 1")
        @Max(value = 50, message = "topK máximo é 50")
        Integer topK,

        @Valid
        SearchFilters filters
) {
    public int resolvedTopK() {
        return topK != null ? topK : 8;
    }
}
