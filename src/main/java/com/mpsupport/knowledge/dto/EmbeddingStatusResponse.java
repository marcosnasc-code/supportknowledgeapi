package com.mpsupport.knowledge.dto;

public record EmbeddingStatusResponse(
        long totalChunks,
        long embeddedChunks,
        long missingChunks,
        double embeddedPercent,
        boolean indexingRunning,
        String embeddingModel
) {
}
