package com.mpsupport.knowledge.integration;

import java.util.List;

public record OllamaEmbedResponse(
        List<List<Double>> embeddings
) {
}
