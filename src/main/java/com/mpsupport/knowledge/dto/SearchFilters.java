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
        String sistemaDeclarado
) {
}
