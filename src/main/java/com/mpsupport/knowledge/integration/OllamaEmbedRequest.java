package com.mpsupport.knowledge.integration;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * API atual do Ollama: POST /api/embed
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OllamaEmbedRequest(
        String model,
        Object input,
        Boolean truncate
) {
    public static OllamaEmbedRequest single(String model, String text) {
        return new OllamaEmbedRequest(model, text, true);
    }

    public static OllamaEmbedRequest batch(String model, java.util.List<String> texts) {
        return new OllamaEmbedRequest(model, texts, true);
    }
}
