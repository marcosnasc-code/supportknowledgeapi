package com.mpsupport.knowledge.dto;

import com.mpsupport.knowledge.domain.ChunkSource;

import java.util.UUID;

public record SearchCitation(
        UUID chunkId,
        String ticketId,
        ChunkSource source
) {
}
