package com.mpsupport.knowledge.dto;

import java.util.List;

public record AssistResponse(
        String queryUsadaNaBusca,
        SearchMode modoBusca,
        int topK,
        AssistMode modo,
        List<EvidenciaItem> evidencias,
        List<HipotesesItem> hipoteses,
        List<String> perguntasAoUsuario,
        HandoffRascunho rascunhoHandoff,
        String sistemaSugerido,
        List<SistemaSugeridoItem> sistemasSugeridos,
        AssistMetadados metadados
) {
}
