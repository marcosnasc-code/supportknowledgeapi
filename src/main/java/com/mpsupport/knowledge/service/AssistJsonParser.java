package com.mpsupport.knowledge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AssistJsonParser {

    private static final Pattern JSON_FENCE = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    private AssistJsonParser() {
    }

    static AssistLlmOutput parse(ObjectMapper objectMapper, String raw) {
        String json = extractJson(raw);
        try {
            JsonNode root = objectMapper.readTree(json);
            List<String> perguntas = coerceStringList(root.get("perguntasAoUsuario"));
            List<AssistLlmSolucao> solucoes = parseSolucoes(objectMapper, root.get("solucoesEncontradas"));
            String analiseDoCaso = textOrNull(root.get("analiseDoCaso"));
            String proximaAcaoRecomendada = textOrNull(root.get("proximaAcaoRecomendada"));
            List<AssistLlmHipoteses> hipoteses = parseHipoteses(objectMapper, root.get("hipoteses"));
            AssistLlmHandoff handoff = parseHandoff(objectMapper, root.get("rascunhoHandoff"));
            return new AssistLlmOutput(
                    perguntas,
                    solucoes,
                    analiseDoCaso,
                    proximaAcaoRecomendada,
                    hipoteses,
                    handoff
            );
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON do LLM inválido: " + ex.getMessage(), ex);
        }
    }

    private static List<AssistLlmSolucao> parseSolucoes(ObjectMapper objectMapper, JsonNode node) throws Exception {
        if (node == null || node.isNull() || !node.isArray()) {
            return List.of();
        }
        List<AssistLlmSolucao> list = new ArrayList<>();
        for (JsonNode item : node) {
            if (item == null || item.isNull()) {
                continue;
            }
            if (item.isObject()) {
                list.add(objectMapper.treeToValue(item, AssistLlmSolucao.class));
            } else if (item.isTextual()) {
                list.add(new AssistLlmSolucao(item.asText(), List.of()));
            }
        }
        return list;
    }

    private static List<AssistLlmHipoteses> parseHipoteses(ObjectMapper objectMapper, JsonNode node) throws Exception {
        if (node == null || node.isNull() || !node.isArray()) {
            return List.of();
        }
        List<AssistLlmHipoteses> list = new ArrayList<>();
        for (JsonNode item : node) {
            if (item == null || item.isNull()) {
                continue;
            }
            if (item.isObject()) {
                list.add(objectMapper.treeToValue(item, AssistLlmHipoteses.class));
            } else if (item.isTextual()) {
                list.add(new AssistLlmHipoteses(item.asText(), null));
            }
        }
        return list;
    }

    private static AssistLlmHandoff parseHandoff(ObjectMapper objectMapper, JsonNode node) throws Exception {
        if (node == null || node.isNull()) {
            return null;
        }
        return objectMapper.treeToValue(node, AssistLlmHandoff.class);
    }

    /**
     * Aceita ["pergunta?"] ou formatos errados do LLM como [{"pergunta":"..."}].
     */
    static List<String> coerceStringList(JsonNode node) {
        if (node == null || node.isNull()) {
            return List.of();
        }
        if (node.isTextual()) {
            String text = node.asText().strip();
            return text.isEmpty() ? List.of() : List.of(text);
        }
        if (!node.isArray()) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        for (JsonNode item : node) {
            String extracted = extractQuestionText(item);
            if (extracted != null && !extracted.isBlank()) {
                result.add(extracted.strip());
            }
        }
        return result;
    }

    private static String extractQuestionText(JsonNode item) {
        if (item == null || item.isNull()) {
            return null;
        }
        if (item.isTextual()) {
            return item.asText();
        }
        if (item.isObject()) {
            for (String field : List.of("pergunta", "texto", "text", "question", "perguntaAoUsuario", "valor")) {
                JsonNode child = item.get(field);
                if (child != null && child.isTextual() && !child.asText().isBlank()) {
                    return child.asText();
                }
            }
        }
        return null;
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            String text = node.asText().strip();
            return text.isEmpty() ? null : text;
        }
        return null;
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
