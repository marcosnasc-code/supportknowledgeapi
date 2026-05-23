package com.mpsupport.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CasoAtualRequest(
        @NotBlank(message = "descricaoUsuario é obrigatória")
        @Size(min = 5, max = 4000, message = "descricaoUsuario deve ter entre 5 e 4000 caracteres")
        String descricaoUsuario,

        @Size(max = 4000, message = "contextoAdicional deve ter no máximo 4000 caracteres")
        String contextoAdicional,

        @Size(max = 200, message = "sistemaDeclarado deve ter no máximo 200 caracteres")
        String sistemaDeclarado
) {
}
