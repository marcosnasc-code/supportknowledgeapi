package com.mpsupport.knowledge.service;

import com.mpsupport.knowledge.dto.AssistMode;
import com.mpsupport.knowledge.dto.CasoAtualRequest;
import com.mpsupport.knowledge.dto.EvidenciaItem;
import com.mpsupport.knowledge.dto.SistemaSugeridoItem;

import java.util.List;

final class AssistPromptBuilder {

    private AssistPromptBuilder() {
    }

    static String systemPrompt(AssistMode modo, String catalogoSistemas) {
        String modoRegra = modo == AssistMode.CITADO_OBRIGATORIO_PARA_SOLUCOES
                ? "O array hipoteses DEVE ser vazio []. Não preencha campos do handoff com origem INFERENCIA."
                : "hipoteses pode conter itens claramente rotulados; handoff pode usar origem INFERENCIA quando necessário.";

        return """
                Você é assistente de suporte técnico do Ministério Público (Brasil).
                Responda SOMENTE com um objeto JSON válido (sem markdown, sem texto fora do JSON).
                
                Sistemas válidos do MP (use estes nomes no campo sistema do handoff quando citado nas evidências):
                %s
                
                Regras obrigatórias:
                - Use APENAS as evidências numeradas E1, E2, ... fornecidas pelo usuário.
                - Nunca invente número de chamado (ticketId), soluções ou telas que não apareçam nas evidências.
                - Se faltar informação (print, vídeo, passos, ambiente), liste em perguntasAoUsuario.
                - rascunhoHandoff: quatro campos (sistema, local, erro, pedido). Cada campo: valor (string ou null), origem (CITACAO | INFERENCIA | NAO_IDENTIFICADO), referencia (Ex: "E1" ou null).
                - %s
                
                Formato JSON esperado:
                {
                  "perguntasAoUsuario": ["..."],
                  "hipoteses": [{"texto": "...", "referencia": "E1"}],
                  "rascunhoHandoff": {
                    "sistema": {"valor": null, "origem": "NAO_IDENTIFICADO", "referencia": null},
                    "local": {"valor": "...", "origem": "CITACAO", "referencia": "E1"},
                    "erro": {"valor": "...", "origem": "CITACAO", "referencia": "E1"},
                    "pedido": {"valor": "...", "origem": "NAO_IDENTIFICADO", "referencia": null}
                  }
                }
                """.formatted(catalogoSistemas, modoRegra);
    }

    static String userPrompt(
            CasoAtualRequest caso,
            List<EvidenciaItem> evidenciasParaLlm,
            List<SistemaSugeridoItem> sistemasSugeridos
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== CASO ATUAL ===\n");
        sb.append(caso.descricaoUsuario().strip());
        if (caso.contextoAdicional() != null && !caso.contextoAdicional().isBlank()) {
            sb.append("\n\nContexto adicional do agente:\n");
            sb.append(caso.contextoAdicional().strip());
        }
        if (caso.sistemaDeclarado() != null && !caso.sistemaDeclarado().isBlank()) {
            sb.append("\n\nSistema declarado pelo agente: ");
            sb.append(caso.sistemaDeclarado().strip());
        }

        sb.append("\n\n=== SUGESTÃO HEURÍSTICA DE SISTEMA (pode estar errada; priorize evidências) ===\n");
        if (sistemasSugeridos.isEmpty()) {
            sb.append("(Nenhum sistema identificado com confiança no texto.)\n");
        } else {
            for (SistemaSugeridoItem s : sistemasSugeridos) {
                sb.append("- ")
                        .append(s.displayName())
                        .append(" (id=")
                        .append(s.id())
                        .append(", score=")
                        .append(s.score())
                        .append(", confiança=")
                        .append(s.confianca())
                        .append(")\n");
            }
        }

        sb.append("\n=== EVIDÊNCIAS DO HISTÓRICO ===\n");
        if (evidenciasParaLlm.isEmpty()) {
            sb.append("(Nenhuma evidência recuperada na busca.)\n");
        } else {
            for (EvidenciaItem ev : evidenciasParaLlm) {
                sb.append(ev.referencia())
                        .append(" | ticket=").append(ev.ticketId())
                        .append(" | fonte=").append(ev.source())
                        .append(" | score=").append(String.format("%.4f", ev.scoreBusca()))
                        .append("\n")
                        .append(ev.snippet())
                        .append("\n\n");
            }
        }

        sb.append("Monte perguntasAoUsuario, rascunhoHandoff e hipoteses conforme as regras do system prompt.");
        return sb.toString();
    }

    static String buildSearchQuery(CasoAtualRequest caso) {
        String descricao = caso.descricaoUsuario().strip();
        if (caso.contextoAdicional() == null || caso.contextoAdicional().isBlank()) {
            return descricao.replaceAll("\\s+", " ");
        }
        String contexto = caso.contextoAdicional().strip();
        return (descricao + "\n" + contexto).replaceAll("\\s+", " ");
    }
}
