package com.mpsupport.knowledge.repository;

import com.mpsupport.knowledge.domain.ImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, UUID> {

    Optional<ImportBatch> findFirstByOrderByCreatedAtDesc();
}
