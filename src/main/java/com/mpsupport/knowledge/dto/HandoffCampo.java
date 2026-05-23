package com.mpsupport.knowledge.dto;

public record HandoffCampo(
        String valor,
        HandoffOrigem origem,
        String referencia
) {
    public static HandoffCampo naoIdentificado() {
        return new HandoffCampo(null, HandoffOrigem.NAO_IDENTIFICADO, null);
    }
}
