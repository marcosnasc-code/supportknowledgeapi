package com.mpsupport.knowledge.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "import_batch")
public class ImportBatch {

    @Id
    private UUID id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "source_file_name", length = 512)
    private String sourceFileName;

    @Column(name = "processed_rows", nullable = false)
    private long processedRows;

    @Column(name = "skipped_rows", nullable = false)
    private long skippedRows;

    @Column(name = "chunks_created", nullable = false)
    private long chunksCreated;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ImportBatchStatus status;

    protected ImportBatch() {
    }

    public static ImportBatch start(UUID id, String sourceFileName) {
        ImportBatch batch = new ImportBatch();
        batch.id = id;
        batch.createdAt = Instant.now();
        batch.sourceFileName = sourceFileName;
        batch.processedRows = 0;
        batch.skippedRows = 0;
        batch.chunksCreated = 0;
        batch.status = ImportBatchStatus.PROCESSING;
        return batch;
    }

    public void complete(long processedRows, long skippedRows, long chunksCreated) {
        this.processedRows = processedRows;
        this.skippedRows = skippedRows;
        this.chunksCreated = chunksCreated;
        this.status = ImportBatchStatus.DONE;
    }

    public void fail() {
        this.status = ImportBatchStatus.FAILED;
    }

    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public long getProcessedRows() {
        return processedRows;
    }

    public long getSkippedRows() {
        return skippedRows;
    }

    public long getChunksCreated() {
        return chunksCreated;
    }

    public ImportBatchStatus getStatus() {
        return status;
    }
}
