package com.mpsupport.knowledge.service;

import com.mpsupport.knowledge.dto.IndexStatusResponse;
import com.mpsupport.knowledge.repository.ImportBatchRepository;
import com.mpsupport.knowledge.repository.KnowledgeChunkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IndexStatusService {

    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final ImportBatchRepository importBatchRepository;

    public IndexStatusService(
            KnowledgeChunkRepository knowledgeChunkRepository,
            ImportBatchRepository importBatchRepository
    ) {
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.importBatchRepository = importBatchRepository;
    }

    @Transactional(readOnly = true)
    public IndexStatusResponse getStatus() {
        long totalChunks = knowledgeChunkRepository.count();
        long distinctTickets = knowledgeChunkRepository.countDistinctTicketId();

        return importBatchRepository.findFirstByOrderByCreatedAtDesc()
                .map(batch -> new IndexStatusResponse(
                        totalChunks,
                        distinctTickets,
                        batch.getCreatedAt(),
                        batch.getId(),
                        batch.getStatus().name()
                ))
                .orElseGet(() -> new IndexStatusResponse(
                        totalChunks,
                        distinctTickets,
                        null,
                        null,
                        null
                ));
    }
}
