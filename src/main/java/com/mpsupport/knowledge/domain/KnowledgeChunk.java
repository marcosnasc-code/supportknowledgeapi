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
@Table(name = "knowledge_chunk")
public class KnowledgeChunk {

    @Id
    private UUID id;

    @Column(name = "import_batch_id", nullable = false)
    private UUID importBatchId;

    @Column(name = "ticket_id", nullable = false, length = 128)
    private String ticketId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ChunkSource source;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected KnowledgeChunk() {
    }

    public UUID getId() {
        return id;
    }

    public UUID getImportBatchId() {
        return importBatchId;
    }

    public String getTicketId() {
        return ticketId;
    }

    public ChunkSource getSource() {
        return source;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
