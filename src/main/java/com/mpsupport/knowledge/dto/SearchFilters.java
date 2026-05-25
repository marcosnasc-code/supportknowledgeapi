package com.mpsupport.knowledge.dto;

import com.mpsupport.knowledge.domain.ChunkSource;

import java.util.List;

/**
 * Filtros opcionais. {@code sources} restringe a tipos de chunk (ex.: só SOLUCAO).
 */
public record SearchFilters(
        List<ChunkSource> sources,

        /**
         * Nome ou id do sistema informado pelo agente; usado para reordenar resultados (boost), sem ocultar outros.
         */
        String sistemaDeclarado,

        /**
         * Se true (padrão), anexa a solução do mesmo ticket quando o match não for em SOLUCAO.
         */
        Boolean includeSolution
) {
    public boolean resolvedIncludeSolution() {
        return includeSolution == null || includeSolution;
    }
}
