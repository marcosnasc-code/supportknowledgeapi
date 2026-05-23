package com.mpsupport.knowledge.service;

import com.mpsupport.knowledge.IntegrationTestBase;
import com.mpsupport.knowledge.dto.AssistMode;
import com.mpsupport.knowledge.dto.AssistRequest;
import com.mpsupport.knowledge.dto.AssistResponse;
import com.mpsupport.knowledge.dto.CasoAtualRequest;
import com.mpsupport.knowledge.dto.HandoffOrigem;
import com.mpsupport.knowledge.dto.SearchMode;
import com.mpsupport.knowledge.integration.OllamaChatClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@TestPropertySource(properties = "app.import.csv.charset=UTF-8")
class AssistServiceTest extends IntegrationTestBase {

    @Autowired
    AssistService assistService;

    @Autowired
    CsvImportService csvImportService;

    @MockBean
    OllamaChatClient ollamaChatClient;

    @BeforeEach
    void importSampleData() throws Exception {
        String csv = """
                Ref,Descrição,Log público,Solução,Log privado
                100,Erro ao anexar documento na tela principal,Usuário reiniciou navegador,Orientado a limpar cache do navegador,Nota interna
                200,Problema de login com senha expirada,,,
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "tickets.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );
        csvImportService.importTicketsCsv(file);
    }

    @Test
    void assist_llmSuccess_returnsEvidenciasAndHandoffCitado() {
        when(ollamaChatClient.chatJson(anyString(), anyString())).thenReturn("""
                {
                  "perguntasAoUsuario": ["Pode enviar print da mensagem de erro?"],
                  "hipoteses": [],
                  "rascunhoHandoff": {
                    "sistema": {"valor": null, "origem": "NAO_IDENTIFICADO", "referencia": null},
                    "local": {"valor": "tela principal", "origem": "CITACAO", "referencia": "E1"},
                    "erro": {"valor": "anexar documento", "origem": "CITACAO", "referencia": "E1"},
                    "pedido": {"valor": null, "origem": "NAO_IDENTIFICADO", "referencia": null}
                  }
                }
                """);

        AssistResponse response = assistService.assist(new AssistRequest(
                new CasoAtualRequest(
                        "Não consigo anexar documento na tela principal",
                        null,
                        null
                ),
                5,
                AssistMode.CITADO_OBRIGATORIO_PARA_SOLUCOES,
                SearchMode.TEXT
        ));

        assertThat(response.metadados().llmUsado()).isTrue();
        assertThat(response.evidencias()).isNotEmpty();
        assertThat(response.evidencias().getFirst().referencia()).isEqualTo("E1");
        assertThat(response.evidencias().getFirst().ticketId()).isEqualTo("100");
        assertThat(response.hipoteses()).isEmpty();
        assertThat(response.perguntasAoUsuario()).contains("Pode enviar print da mensagem de erro?");
        assertThat(response.rascunhoHandoff().local().origem()).isEqualTo(HandoffOrigem.CITACAO);
        assertThat(response.rascunhoHandoff().local().referencia()).isEqualTo("E1");
        assertThat(response.sistemaSugerido()).isNull();
        assertThat(response.sistemasSugeridos()).isNotNull();
    }

    @Test
    void assist_suggestsSystem_whenMentionedInCase() {
        when(ollamaChatClient.chatJson(anyString(), anyString())).thenReturn("""
                {
                  "perguntasAoUsuario": [],
                  "hipoteses": [],
                  "rascunhoHandoff": {
                    "sistema": {"valor": null, "origem": "NAO_IDENTIFICADO", "referencia": null},
                    "local": {"valor": null, "origem": "NAO_IDENTIFICADO", "referencia": null},
                    "erro": {"valor": null, "origem": "NAO_IDENTIFICADO", "referencia": null},
                    "pedido": {"valor": null, "origem": "NAO_IDENTIFICADO", "referencia": null}
                  }
                }
                """);

        AssistResponse response = assistService.assist(new AssistRequest(
                new CasoAtualRequest(
                        "Erro ao anexar arquivo no MPCloud",
                        null,
                        null
                ),
                5,
                AssistMode.CITADO_OBRIGATORIO_PARA_SOLUCOES,
                SearchMode.TEXT
        ));

        assertThat(response.sistemasSugeridos()).isNotEmpty();
        assertThat(response.sistemasSugeridos().getFirst().displayName()).isEqualTo("MPCloud");
        assertThat(response.sistemaSugerido()).isEqualTo("MPCloud");
        assertThat(response.rascunhoHandoff().sistema().origem())
                .isEqualTo(com.mpsupport.knowledge.dto.HandoffOrigem.HEURISTICA_SISTEMA);
    }

    @Test
    void assist_declaredSystem_fillsHandoffAsDeclarado() {
        when(ollamaChatClient.chatJson(anyString(), anyString())).thenReturn("""
                {
                  "perguntasAoUsuario": [],
                  "hipoteses": [],
                  "rascunhoHandoff": {
                    "sistema": {"valor": null, "origem": "NAO_IDENTIFICADO", "referencia": null},
                    "local": {"valor": null, "origem": "NAO_IDENTIFICADO", "referencia": null},
                    "erro": {"valor": null, "origem": "NAO_IDENTIFICADO", "referencia": null},
                    "pedido": {"valor": null, "origem": "NAO_IDENTIFICADO", "referencia": null}
                  }
                }
                """);

        AssistResponse response = assistService.assist(new AssistRequest(
                new CasoAtualRequest(
                        "Erro genérico de tela",
                        null,
                        "Atena"
                ),
                5,
                AssistMode.CITADO_OBRIGATORIO_PARA_SOLUCOES,
                SearchMode.TEXT
        ));

        assertThat(response.sistemaSugerido()).isEqualTo("Atena");
        assertThat(response.rascunhoHandoff().sistema().origem())
                .isEqualTo(com.mpsupport.knowledge.dto.HandoffOrigem.DECLARADO);
        assertThat(response.rascunhoHandoff().sistema().valor()).isEqualTo("Atena");
    }

    @Test
    void assist_llmFailure_returnsFallbackWithoutLlm() {
        when(ollamaChatClient.chatJson(anyString(), anyString()))
                .thenThrow(new IllegalStateException("Ollama offline"));

        AssistResponse response = assistService.assist(new AssistRequest(
                new CasoAtualRequest(
                        "Erro ao anexar documento",
                        "Usuário já limpou cache",
                        null
                ),
                5,
                AssistMode.CITADO_OBRIGATORIO_PARA_SOLUCOES,
                SearchMode.TEXT
        ));

        assertThat(response.metadados().llmUsado()).isFalse();
        assertThat(response.metadados().aviso()).contains("LLM indisponível");
        assertThat(response.evidencias()).isNotEmpty();
        assertThat(response.hipoteses()).isEmpty();
        assertThat(response.perguntasAoUsuario()).hasSizeGreaterThanOrEqualTo(4);
        assertThat(response.rascunhoHandoff().sistema().origem()).isEqualTo(HandoffOrigem.NAO_IDENTIFICADO);
    }

    @Test
    void assist_strictMode_stripsInferenciaFromHandoff() {
        when(ollamaChatClient.chatJson(anyString(), anyString())).thenReturn("""
                {
                  "perguntasAoUsuario": [],
                  "hipoteses": [{"texto": "hipótese inventada", "referencia": "E1"}],
                  "rascunhoHandoff": {
                    "sistema": {"valor": "Sistema X", "origem": "INFERENCIA", "referencia": null},
                    "local": {"valor": "tela", "origem": "CITACAO", "referencia": "E1"},
                    "erro": {"valor": "erro", "origem": "CITACAO", "referencia": "E1"},
                    "pedido": {"valor": "corrigir", "origem": "INFERENCIA", "referencia": null}
                  }
                }
                """);

        AssistResponse response = assistService.assist(new AssistRequest(
                new CasoAtualRequest("anexar documento tela principal", null, null),
                5,
                AssistMode.CITADO_OBRIGATORIO_PARA_SOLUCOES,
                SearchMode.TEXT
        ));

        assertThat(response.hipoteses()).isEmpty();
        assertThat(response.rascunhoHandoff().sistema().origem()).isEqualTo(HandoffOrigem.NAO_IDENTIFICADO);
        assertThat(response.rascunhoHandoff().pedido().origem()).isEqualTo(HandoffOrigem.NAO_IDENTIFICADO);
        assertThat(response.rascunhoHandoff().local().origem()).isEqualTo(HandoffOrigem.CITACAO);
    }
}
