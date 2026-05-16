package com.mpsupport.knowledge.repository;

import com.mpsupport.knowledge.domain.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, UUID> {

    @Query("SELECT COUNT(DISTINCT k.ticketId) FROM KnowledgeChunk k")
    long countDistinctTicketId();
}
