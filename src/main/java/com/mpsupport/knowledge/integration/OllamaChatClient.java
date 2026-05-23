package com.mpsupport.knowledge.integration;

import com.mpsupport.knowledge.config.OllamaProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OllamaChatClient {

    private final RestClient ollamaRestClient;
    private final OllamaProperties ollamaProperties;

    public OllamaChatClient(RestClient ollamaRestClient, OllamaProperties ollamaProperties) {
        this.ollamaRestClient = ollamaRestClient;
        this.ollamaProperties = ollamaProperties;
    }

    public String chatJson(String systemPrompt, String userPrompt) {
        OllamaChatRequest request = OllamaChatRequest.json(
                ollamaProperties.getChatModel(),
                systemPrompt,
                userPrompt
        );

        try {
            OllamaChatResponse response = ollamaRestClient.post()
                    .uri("/api/chat")
                    .body(request)
                    .retrieve()
                    .body(OllamaChatResponse.class);

            if (response == null || response.message() == null) {
                throw new IllegalStateException("Ollama /api/chat retornou resposta vazia.");
            }
            String content = response.message().content();
            if (content == null || content.isBlank()) {
                throw new IllegalStateException("Ollama /api/chat retornou conteúdo vazio.");
            }
            return content.strip();
        } catch (RestClientException ex) {
            throw new IllegalStateException(
                    "Falha ao chamar Ollama /api/chat. Confirme: ollama serve, ollama pull "
                            + ollamaProperties.getChatModel() + ". Detalhe: " + ex.getMessage(),
                    ex
            );
        }
    }
}
