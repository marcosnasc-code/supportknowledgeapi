package com.mpsupport.knowledge.service;

import com.mpsupport.knowledge.domain.ChunkSource;

import java.time.Instant;
import java.util.UUID;

record ChunkPending(
        UUID id,
        UUID importBatchId,
        String ticketId,
        ChunkSource source,
        String content,
        Instant createdAt
) {
}
