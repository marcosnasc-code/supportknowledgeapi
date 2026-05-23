package com.mpsupport.knowledge.integration;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OllamaChatRequest(
        String model,
        List<OllamaChatMessage> messages,
        boolean stream,
        String format
) {
    public static OllamaChatRequest json(String model, String systemPrompt, String userPrompt) {
        return new OllamaChatRequest(
                model,
                List.of(
                        new OllamaChatMessage("system", systemPrompt),
                        new OllamaChatMessage("user", userPrompt)
                ),
                false,
                "json"
        );
    }
}
