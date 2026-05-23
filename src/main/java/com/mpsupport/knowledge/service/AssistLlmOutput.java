package com.mpsupport.knowledge.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record AssistLlmOutput(
        List<String> perguntasAoUsuario,
        List<AssistLlmHipoteses> hipoteses,
        AssistLlmHandoff rascunhoHandoff
) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record AssistLlmHipoteses(String texto, String referencia) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record AssistLlmHandoff(
        AssistLlmHandoffCampo sistema,
        AssistLlmHandoffCampo local,
        AssistLlmHandoffCampo erro,
        AssistLlmHandoffCampo pedido
) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record AssistLlmHandoffCampo(String valor, String origem, String referencia) {
}
