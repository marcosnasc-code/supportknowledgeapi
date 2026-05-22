package com.mpsupport.knowledge.integration;

import com.mpsupport.knowledge.config.EmbeddingProperties;
import com.mpsupport.knowledge.config.OllamaProperties;
import com.mpsupport.knowledge.util.VectorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;

@Component
public class OllamaEmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaEmbeddingClient.class);

    private final RestClient ollamaRestClient;
    private final OllamaProperties ollamaProperties;
    private final EmbeddingProperties embeddingProperties;

    public OllamaEmbeddingClient(
            RestClient ollamaRestClient,
            OllamaProperties ollamaProperties,
            EmbeddingProperties embeddingProperties
    ) {
        this.ollamaRestClient = ollamaRestClient;
        this.ollamaProperties = ollamaProperties;
        this.embeddingProperties = embeddingProperties;
    }

    public float[] embed(String text) {
        return embedBatch(List.of(text)).getFirst();
    }

    public List<float[]> embedBatch(List<String> texts) {
        if (texts.isEmpty()) {
            return List.of();
        }

        List<String> prompts = texts.stream()
                .map(t -> VectorUtils.truncateForEmbedding(t, embeddingProperties.getMaxContentChars()))
                .filter(s -> !s.isEmpty())
                .toList();

        if (prompts.isEmpty()) {
            throw new IllegalArgumentException("Todos os textos do lote estão vazios após normalização.");
        }

        if (prompts.size() != texts.size()) {
            throw new IllegalArgumentException("Há chunk(s) com conteúdo vazio no lote.");
        }

        try {
            return callEmbedApi(prompts);
        } catch (RestClientException ex) {
            if (isContextLengthError(ex)) {
                log.warn("Texto excedeu contexto do Ollama; re-tentando lote com truncagem menor ({} chars).",
                        embeddingProperties.getMaxContentChars() / 2);
                List<String> shorter = texts.stream()
                        .map(t -> VectorUtils.truncateForEmbedding(t, embeddingProperties.getMaxContentChars() / 2))
                        .toList();
                return callEmbedApi(shorter);
            }
            log.warn("Falha em /api/embed, tentando /api/embeddings legado: {}", ex.getMessage());
            return embedBatchLegacy(prompts);
        }
    }

    private List<float[]> callEmbedApi(List<String> prompts) {
        OllamaEmbedRequest body = prompts.size() == 1
                ? OllamaEmbedRequest.single(ollamaProperties.getEmbeddingModel(), prompts.getFirst())
                : OllamaEmbedRequest.batch(ollamaProperties.getEmbeddingModel(), prompts);

        OllamaEmbedResponse response = ollamaRestClient.post()
                .uri("/api/embed")
                .body(body)
                .retrieve()
                .body(OllamaEmbedResponse.class);

        return parseEmbeddings(response, prompts.size());
    }

    private static boolean isContextLengthError(RestClientException ex) {
        String msg = ex.getMessage();
        return msg != null && msg.toLowerCase().contains("context length");
    }

    private List<float[]> embedBatchLegacy(List<String> prompts) {
        List<float[]> vectors = new ArrayList<>(prompts.size());
        for (String prompt : prompts) {
            OllamaEmbeddingsResponse response = ollamaRestClient.post()
                    .uri("/api/embeddings")
                    .body(new OllamaEmbeddingsRequest(ollamaProperties.getEmbeddingModel(), prompt))
                    .retrieve()
                    .body(OllamaEmbeddingsResponse.class);

            if (response == null || response.embedding() == null || response.embedding().isEmpty()) {
                throw new IllegalStateException(
                        "Ollama retornou embedding vazio. Rode: ollama pull " + ollamaProperties.getEmbeddingModel()
                );
            }
            vectors.add(toFloatArray(response.embedding()));
        }
        return vectors;
    }

    private List<float[]> parseEmbeddings(OllamaEmbedResponse response, int expectedSize) {
        if (response == null || response.embeddings() == null || response.embeddings().isEmpty()) {
            throw new IllegalStateException(
                    "Ollama /api/embed retornou resposta vazia. Verifique: ollama serve, ollama pull "
                            + ollamaProperties.getEmbeddingModel()
            );
        }
        if (response.embeddings().size() != expectedSize) {
            throw new IllegalStateException(
                    "Ollama retornou " + response.embeddings().size() + " embeddings; esperado " + expectedSize
            );
        }

        List<float[]> result = new ArrayList<>(expectedSize);
        for (List<Double> vector : response.embeddings()) {
            result.add(toFloatArray(vector));
        }
        return result;
    }

    private float[] toFloatArray(List<Double> values) {
        float[] vector = new float[values.size()];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = values.get(i).floatValue();
        }
        if (vector.length != embeddingProperties.getDimension()) {
            throw new IllegalStateException(
                    "Dimensão do embedding (" + vector.length + ") difere da configurada ("
                            + embeddingProperties.getDimension() + ")."
            );
        }
        return vector;
    }
}
