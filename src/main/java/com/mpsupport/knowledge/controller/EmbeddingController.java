package com.mpsupport.knowledge.controller;

import com.mpsupport.knowledge.dto.EmbeddingIndexRequest;
import com.mpsupport.knowledge.dto.EmbeddingIndexResponse;
import com.mpsupport.knowledge.dto.EmbeddingStatusResponse;
import com.mpsupport.knowledge.integration.OllamaEmbeddingClient;
import com.mpsupport.knowledge.service.EmbeddingIndexService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/embeddings")
public class EmbeddingController {

    private final EmbeddingIndexService embeddingIndexService;
    private final OllamaEmbeddingClient ollamaEmbeddingClient;

    public EmbeddingController(
            EmbeddingIndexService embeddingIndexService,
            OllamaEmbeddingClient ollamaEmbeddingClient
    ) {
        this.embeddingIndexService = embeddingIndexService;
        this.ollamaEmbeddingClient = ollamaEmbeddingClient;
    }

    @GetMapping("/status")
    public EmbeddingStatusResponse status() {
        return embeddingIndexService.getStatus();
    }

    /**
     * Testa Ollama com um texto mínimo (útil antes de indexar 400k chunks).
     */
    @GetMapping("/ollama-ping")
    public Map<String, Object> ollamaPing() {
        float[] vector = ollamaEmbeddingClient.embed("teste de embedding");
        return Map.of(
                "ok", true,
                "dimensions", vector.length
        );
    }

    @PostMapping("/index")
    public EmbeddingIndexResponse index(@Valid @RequestBody(required = false) EmbeddingIndexRequest request) {
        EmbeddingIndexRequest effective = request != null ? request : new EmbeddingIndexRequest(null, true);
        return embeddingIndexService.index(effective);
    }
}
