package com.mpsupport.knowledge.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AssistJsonParser {

    private static final Pattern JSON_FENCE = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    private AssistJsonParser() {
    }

    static AssistLlmOutput parse(ObjectMapper objectMapper, String raw) {
        String json = extractJson(raw);
        try {
            return objectMapper.readValue(json, AssistLlmOutput.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON do LLM inválido: " + ex.getMessage(), ex);
        }
    }

    private static String extractJson(String raw) {
        String trimmed = raw.strip();
        Matcher matcher = JSON_FENCE.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1).strip();
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }
}
