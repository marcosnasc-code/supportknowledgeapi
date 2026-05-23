package com.mpsupport.knowledge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mpsupport.knowledge.config.AssistProperties;
import com.mpsupport.knowledge.config.OllamaProperties;
import com.mpsupport.knowledge.domain.SystemHintDefinition;
import com.mpsupport.knowledge.dto.AssistMetadados;
import com.mpsupport.knowledge.dto.AssistMode;
import com.mpsupport.knowledge.dto.AssistRequest;
import com.mpsupport.knowledge.dto.AssistResponse;
import com.mpsupport.knowledge.dto.CasoAtualRequest;
import com.mpsupport.knowledge.dto.EvidenciaItem;
import com.mpsupport.knowledge.dto.HandoffCampo;
import com.mpsupport.knowledge.dto.HandoffOrigem;
import com.mpsupport.knowledge.dto.HandoffRascunho;
import com.mpsupport.knowledge.dto.HipotesesItem;
import com.mpsupport.knowledge.dto.SearchFilters;
import com.mpsupport.knowledge.dto.SearchMode;
import com.mpsupport.knowledge.dto.SearchRequest;
import com.mpsupport.knowledge.dto.SearchResponse;
import com.mpsupport.knowledge.dto.SearchResultItem;
import com.mpsupport.knowledge.dto.SistemaSugeridoItem;
import com.mpsupport.knowledge.integration.OllamaChatClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AssistService {

    private static final Logger log = LoggerFactory.getLogger(AssistService.class);
    private static final Pattern REFERENCIA_EVIDENCIA = Pattern.compile("^E\\d+$");

    private static final List<String> PERGUNTAS_FALLBACK = List.of(
            "Pode enviar print da tela completa com a mensagem de erro?",
            "Há vídeo ou gravação do passo a passo do problema?",
            "O problema ocorre para um único usuário ou para vários?",
            "Em qual horário e ambiente (produção/homologação) o erro foi observado?",
            "Qual sistema/aplicação está em uso? (veja GET /api/v1/systems)"
    );

    private final SearchService searchService;
    private final OllamaChatClient ollamaChatClient;
    private final AssistProperties assistProperties;
    private final OllamaProperties ollamaProperties;
    private final ObjectMapper objectMapper;
    private final SystemHintService systemHintService;

    public AssistService(
            SearchService searchService,
            OllamaChatClient ollamaChatClient,
            AssistProperties assistProperties,
            OllamaProperties ollamaProperties,
            ObjectMapper objectMapper,
            SystemHintService systemHintService
    ) {
        this.searchService = searchService;
        this.ollamaChatClient = ollamaChatClient;
        this.assistProperties = assistProperties;
        this.ollamaProperties = ollamaProperties;
        this.objectMapper = objectMapper;
        this.systemHintService = systemHintService;
    }

    public AssistResponse assist(AssistRequest request) {
        String query = AssistPromptBuilder.buildSearchQuery(request.casoAtual());
        int topK = request.resolvedTopK(assistProperties.getDefaultTopK());
        SearchMode modoBusca = request.resolvedModoBusca(assistProperties.getDefaultModoBusca());
        AssistMode modo = request.resolvedModo(assistProperties.getDefaultModo());

        SearchFilters filters = new SearchFilters(null, request.casoAtual().sistemaDeclarado());
        SearchResponse searchResponse = searchService.search(new SearchRequest(
                query,
                topK,
                filters,
                modoBusca
        ));

        List<EvidenciaItem> evidencias = mapEvidencias(searchResponse.results());
        List<EvidenciaItem> evidenciasParaLlm = limitForLlm(evidencias);

        String textoParaSistemas = buildTextForSystemScoring(query, evidencias);
        List<SistemaSugeridoItem> sistemasSugeridos = systemHintService.suggest(
                textoParaSistemas,
                request.casoAtual().sistemaDeclarado()
        );
        String sistemaSugerido = resolveSistemaSugeridoLabel(sistemasSugeridos, request.casoAtual());
        Optional<SystemHintDefinition> sistemaDeclarado = systemHintService.resolveDeclared(
                request.casoAtual().sistemaDeclarado()
        );

        try {
            String system = AssistPromptBuilder.systemPrompt(modo, systemHintService.formatCatalogForPrompt());
            String user = AssistPromptBuilder.userPrompt(request.casoAtual(), evidenciasParaLlm, sistemasSugeridos);
            String rawJson = ollamaChatClient.chatJson(system, user);
            AssistLlmOutput llmOutput = AssistJsonParser.parse(objectMapper, rawJson);

            Set<String> referenciasValidas = evidenciasParaLlm.stream()
                    .map(EvidenciaItem::referencia)
                    .collect(Collectors.toSet());

            List<String> perguntas = sanitizePerguntas(llmOutput.perguntasAoUsuario());
            List<HipotesesItem> hipoteses = sanitizeHipoteses(llmOutput.hipoteses(), modo, referenciasValidas);
            HandoffRascunho handoff = applySystemToHandoff(
                    sanitizeHandoff(llmOutput.rascunhoHandoff(), modo, referenciasValidas),
                    sistemaDeclarado,
                    sistemasSugeridos
            );

            if (perguntas.isEmpty()) {
                perguntas = mergePerguntas(perguntas, PERGUNTAS_FALLBACK);
            }
            perguntas = ensureSystemQuestion(perguntas, sistemaSugerido);

            return buildResponse(
                    query,
                    modoBusca,
                    topK,
                    modo,
                    evidencias,
                    hipoteses,
                    perguntas,
                    handoff,
                    sistemaSugerido,
                    sistemasSugeridos,
                    true,
                    null
            );
        } catch (Exception ex) {
            log.warn("Assistência com fallback (LLM indisponível ou JSON inválido): {}", ex.getMessage());
            return buildFallbackResponse(
                    query,
                    modoBusca,
                    topK,
                    modo,
                    evidencias,
                    sistemaSugerido,
                    sistemasSugeridos,
                    sistemaDeclarado,
                    ex.getMessage()
            );
        }
    }

    private AssistResponse buildFallbackResponse(
            String query,
            SearchMode modoBusca,
            int topK,
            AssistMode modo,
            List<EvidenciaItem> evidencias,
            String sistemaSugerido,
            List<SistemaSugeridoItem> sistemasSugeridos,
            Optional<SystemHintDefinition> sistemaDeclarado,
            String detalheErro
    ) {
        String aviso = "LLM indisponível ou resposta inválida; retornando evidências da busca e perguntas padrão.";
        if (detalheErro != null && !detalheErro.isBlank()) {
            aviso = aviso + " Detalhe: " + detalheErro;
        }

        HandoffRascunho handoff = applySystemToHandoff(
                HandoffRascunho.vazio(),
                sistemaDeclarado,
                sistemasSugeridos
        );

        return buildResponse(
                query,
                modoBusca,
                topK,
                modo,
                evidencias,
                List.of(),
                ensureSystemQuestion(PERGUNTAS_FALLBACK, sistemaSugerido),
                handoff,
                sistemaSugerido,
                sistemasSugeridos,
                false,
                aviso
        );
    }

    private AssistResponse buildResponse(
            String query,
            SearchMode modoBusca,
            int topK,
            AssistMode modo,
            List<EvidenciaItem> evidencias,
            List<HipotesesItem> hipoteses,
            List<String> perguntas,
            HandoffRascunho handoff,
            String sistemaSugerido,
            List<SistemaSugeridoItem> sistemasSugeridos,
            boolean llmUsado,
            String aviso
    ) {
        return new AssistResponse(
                query,
                modoBusca,
                topK,
                modo,
                evidencias,
                hipoteses,
                perguntas,
                handoff,
                sistemaSugerido,
                sistemasSugeridos,
                new AssistMetadados(ollamaProperties.getChatModel(), llmUsado, aviso)
        );
    }

    private static String buildTextForSystemScoring(String query, List<EvidenciaItem> evidencias) {
        StringBuilder sb = new StringBuilder(query);
        for (EvidenciaItem ev : evidencias) {
            sb.append(' ').append(ev.snippet());
        }
        return sb.toString();
    }

    private static String resolveSistemaSugeridoLabel(
            List<SistemaSugeridoItem> sugestoes,
            CasoAtualRequest caso
    ) {
        if (caso.sistemaDeclarado() != null && !caso.sistemaDeclarado().isBlank()) {
            return caso.sistemaDeclarado().strip();
        }
        if (sugestoes.isEmpty()) {
            return null;
        }
        return sugestoes.getFirst().displayName();
    }

    private HandoffRascunho applySystemToHandoff(
            HandoffRascunho handoff,
            Optional<SystemHintDefinition> sistemaDeclarado,
            List<SistemaSugeridoItem> sistemasSugeridos
    ) {
        HandoffCampo sistemaCampo = handoff.sistema();

        if (sistemaDeclarado.isPresent()) {
            sistemaCampo = new HandoffCampo(
                    sistemaDeclarado.get().displayName(),
                    HandoffOrigem.DECLARADO,
                    null
            );
        } else if (shouldApplyHeuristicSistema(sistemaCampo, sistemasSugeridos)) {
            SistemaSugeridoItem top = sistemasSugeridos.getFirst();
            sistemaCampo = new HandoffCampo(
                    top.displayName(),
                    HandoffOrigem.HEURISTICA_SISTEMA,
                    null
            );
        }

        return new HandoffRascunho(sistemaCampo, handoff.local(), handoff.erro(), handoff.pedido());
    }

    private static boolean shouldApplyHeuristicSistema(
            HandoffCampo sistemaCampo,
            List<SistemaSugeridoItem> sistemasSugeridos
    ) {
        if (sistemasSugeridos.isEmpty()) {
            return false;
        }
        if (sistemaCampo.origem() == HandoffOrigem.CITACAO && sistemaCampo.valor() != null) {
            return false;
        }
        SistemaSugeridoItem top = sistemasSugeridos.getFirst();
        return "ALTA".equals(top.confianca()) || "MEDIA".equals(top.confianca());
    }

    private static List<String> ensureSystemQuestion(List<String> perguntas, String sistemaSugerido) {
        if (sistemaSugerido != null && !sistemaSugerido.isBlank()) {
            return perguntas;
        }
        String perguntaSistema = "Qual sistema/aplicação está em uso? Consulte GET /api/v1/systems.";
        if (perguntas.contains(perguntaSistema)) {
            return perguntas;
        }
        List<String> merged = new ArrayList<>(perguntas);
        merged.add(perguntaSistema);
        return merged;
    }

    private List<EvidenciaItem> mapEvidencias(List<SearchResultItem> results) {
        List<EvidenciaItem> items = new ArrayList<>(results.size());
        for (int i = 0; i < results.size(); i++) {
            SearchResultItem result = results.get(i);
            items.add(new EvidenciaItem(
                    "E" + (i + 1),
                    result.ticketId(),
                    result.source(),
                    result.snippet(),
                    result.citation().chunkId(),
                    result.score()
            ));
        }
        return items;
    }

    private List<EvidenciaItem> limitForLlm(List<EvidenciaItem> evidencias) {
        int max = assistProperties.getMaxEvidencesForLlm();
        if (evidencias.size() <= max) {
            return evidencias;
        }
        return evidencias.subList(0, max);
    }

    private static List<String> sanitizePerguntas(List<String> perguntas) {
        if (perguntas == null) {
            return List.of();
        }
        return perguntas.stream()
                .filter(p -> p != null && !p.isBlank())
                .map(String::strip)
                .distinct()
                .toList();
    }

    private List<HipotesesItem> sanitizeHipoteses(
            List<AssistLlmHipoteses> hipoteses,
            AssistMode modo,
            Set<String> referenciasValidas
    ) {
        if (modo == AssistMode.CITADO_OBRIGATORIO_PARA_SOLUCOES || hipoteses == null) {
            return List.of();
        }
        return hipoteses.stream()
                .filter(h -> h != null && h.texto() != null && !h.texto().isBlank())
                .map(h -> new HipotesesItem(
                        h.texto().strip(),
                        validarReferencia(h.referencia(), referenciasValidas)
                ))
                .toList();
    }

    private HandoffRascunho sanitizeHandoff(
            AssistLlmHandoff handoff,
            AssistMode modo,
            Set<String> referenciasValidas
    ) {
        if (handoff == null) {
            return HandoffRascunho.vazio();
        }
        return new HandoffRascunho(
                sanitizeCampo(handoff.sistema(), modo, referenciasValidas),
                sanitizeCampo(handoff.local(), modo, referenciasValidas),
                sanitizeCampo(handoff.erro(), modo, referenciasValidas),
                sanitizeCampo(handoff.pedido(), modo, referenciasValidas)
        );
    }

    private HandoffCampo sanitizeCampo(
            AssistLlmHandoffCampo campo,
            AssistMode modo,
            Set<String> referenciasValidas
    ) {
        if (campo == null) {
            return HandoffCampo.naoIdentificado();
        }

        HandoffOrigem origem = parseOrigem(campo.origem());
        String referencia = validarReferencia(campo.referencia(), referenciasValidas);
        String valor = campo.valor() != null && !campo.valor().isBlank() ? campo.valor().strip() : null;

        if (modo == AssistMode.CITADO_OBRIGATORIO_PARA_SOLUCOES) {
            if (origem == HandoffOrigem.INFERENCIA || origem == HandoffOrigem.HEURISTICA_SISTEMA) {
                return HandoffCampo.naoIdentificado();
            }
            if (origem == HandoffOrigem.CITACAO && referencia == null) {
                return HandoffCampo.naoIdentificado();
            }
            if (origem == HandoffOrigem.CITACAO && valor == null) {
                return HandoffCampo.naoIdentificado();
            }
            if (origem == HandoffOrigem.NAO_IDENTIFICADO || origem == HandoffOrigem.DECLARADO) {
                return new HandoffCampo(null, HandoffOrigem.NAO_IDENTIFICADO, null);
            }
            return new HandoffCampo(valor, HandoffOrigem.CITACAO, referencia);
        }

        if (origem == HandoffOrigem.CITACAO && referencia == null) {
            return HandoffCampo.naoIdentificado();
        }
        if (origem == HandoffOrigem.INFERENCIA) {
            return new HandoffCampo(valor, HandoffOrigem.INFERENCIA, null);
        }
        if (valor == null) {
            return HandoffCampo.naoIdentificado();
        }
        return new HandoffCampo(valor, origem, referencia);
    }

    private static HandoffOrigem parseOrigem(String origem) {
        if (origem == null || origem.isBlank()) {
            return HandoffOrigem.NAO_IDENTIFICADO;
        }
        String normalized = origem.strip().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "CITACAO", "CITAÇÃO", "CITADO" -> HandoffOrigem.CITACAO;
            case "INFERENCIA", "INFERÊNCIA" -> HandoffOrigem.INFERENCIA;
            case "HEURISTICA_SISTEMA", "HEURÍSTICA_SISTEMA", "HEURISTICA" -> HandoffOrigem.HEURISTICA_SISTEMA;
            case "DECLARADO" -> HandoffOrigem.DECLARADO;
            default -> HandoffOrigem.NAO_IDENTIFICADO;
        };
    }

    private static String validarReferencia(String referencia, Set<String> referenciasValidas) {
        if (referencia == null || referencia.isBlank()) {
            return null;
        }
        String ref = referencia.strip().toUpperCase(Locale.ROOT);
        if (!REFERENCIA_EVIDENCIA.matcher(ref).matches()) {
            return null;
        }
        if (!referenciasValidas.contains(ref)) {
            return null;
        }
        return ref;
    }

    private static List<String> mergePerguntas(List<String> atuais, List<String> padrao) {
        List<String> merged = new ArrayList<>(atuais);
        for (String p : padrao) {
            if (!merged.contains(p)) {
                merged.add(p);
            }
        }
        return merged;
    }
}
