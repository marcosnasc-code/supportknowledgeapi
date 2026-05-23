package com.mpsupport.knowledge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssistJsonParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parse_extractsJsonFromMarkdownFence() {
        String raw = """
                Aqui está a resposta:
                ```json
                {"perguntasAoUsuario":["a"],"hipoteses":[],"rascunhoHandoff":null}
                ```
                """;

        AssistLlmOutput output = AssistJsonParser.parse(objectMapper, raw);

        assertThat(output.perguntasAoUsuario()).containsExactly("a");
    }
}
