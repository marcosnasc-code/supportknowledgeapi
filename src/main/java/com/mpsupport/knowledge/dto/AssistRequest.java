package com.mpsupport.knowledge.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AssistRequest(
        @NotNull(message = "casoAtual é obrigatório")
        @Valid
        CasoAtualRequest casoAtual,

        @Min(value = 1, message = "topK mínimo é 1")
        @Max(value = 20, message = "topK máximo é 20 no assist")
        Integer topK,

        AssistMode modo,

        SearchMode modoBusca
) {
    public int resolvedTopK(int defaultTopK) {
        return topK != null ? topK : defaultTopK;
    }

    public AssistMode resolvedModo(AssistMode defaultModo) {
        return modo != null ? modo : defaultModo;
    }

    public SearchMode resolvedModoBusca(SearchMode defaultModoBusca) {
        return modoBusca != null ? modoBusca : defaultModoBusca;
    }
}
