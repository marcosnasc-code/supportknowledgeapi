package com.mpsupport.knowledge.dto;

public record HandoffRascunho(
        HandoffCampo sistema,
        HandoffCampo local,
        HandoffCampo erro,
        HandoffCampo pedido
) {
    public static HandoffRascunho vazio() {
        HandoffCampo nao = HandoffCampo.naoIdentificado();
        return new HandoffRascunho(nao, nao, nao, nao);
    }
}
