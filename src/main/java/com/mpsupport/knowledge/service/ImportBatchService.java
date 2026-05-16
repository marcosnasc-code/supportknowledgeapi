package com.mpsupport.knowledge.service;

import com.mpsupport.knowledge.domain.ImportBatch;
import com.mpsupport.knowledge.repository.ImportBatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ImportBatchService {

    private final ImportBatchRepository importBatchRepository;

    public ImportBatchService(ImportBatchRepository importBatchRepository) {
        this.importBatchRepository = importBatchRepository;
    }

    @Transactional
    public ImportBatch startBatch(UUID batchId, String sourceFileName) {
        ImportBatch batch = ImportBatch.start(batchId, sourceFileName);
        return importBatchRepository.save(batch);
    }

    @Transactional
    public void completeBatch(UUID batchId, long processedRows, long skippedRows, long chunksCreated) {
        ImportBatch batch = importBatchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalStateException("Lote de importação não encontrado: " + batchId));
        batch.complete(processedRows, skippedRows, chunksCreated);
        importBatchRepository.save(batch);
    }

    @Transactional
    public void failBatch(UUID batchId) {
        importBatchRepository.findById(batchId).ifPresent(batch -> {
            batch.fail();
            importBatchRepository.save(batch);
        });
    }
}
