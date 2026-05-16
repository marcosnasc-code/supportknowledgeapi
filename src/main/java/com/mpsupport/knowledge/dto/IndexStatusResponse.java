package com.mpsupport.knowledge.dto;

import java.time.Instant;
import java.util.UUID;

public record IndexStatusResponse(
        long totalChunks,
        long distinctTickets,
        Instant lastImportAt,
        UUID lastImportBatchId,
        String lastImportStatus
) {
}
