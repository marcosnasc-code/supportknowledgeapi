package com.mpsupport.knowledge.dto;

public record EmbeddingIndexResponse(
        long processedInThisRun,
        long stillMissing,
        long totalChunks,
        String embeddingModel,
        String message
) {
}
