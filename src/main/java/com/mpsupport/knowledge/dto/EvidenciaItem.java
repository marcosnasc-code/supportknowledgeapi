package com.mpsupport.knowledge.dto;

import java.util.UUID;

public record EvidenciaItem(
        String referencia,
        String ticketId,
        String source,
        String snippet,
        RelatedSolutionPreview solucaoRelacionada,
        UUID chunkId,
        double scoreBusca
) {
}
