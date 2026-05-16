package com.mpsupport.knowledge.dto;

import java.util.List;
import java.util.UUID;

public record CsvImportResponse(
        UUID importBatchId,
        long processedRows,
        long skippedRows,
        long chunksCreated,
        List<ImportRowError> errors
) {
}
