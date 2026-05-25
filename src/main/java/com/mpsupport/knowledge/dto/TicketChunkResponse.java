package com.mpsupport.knowledge.dto;

import com.mpsupport.knowledge.domain.ChunkSource;

import java.util.UUID;

/** Conteúdo completo de um chunk (ex.: expandir solução no front). */
public record TicketChunkResponse(
        UUID chunkId,
        String ticketId,
        ChunkSource source,
        String content
) {
}
