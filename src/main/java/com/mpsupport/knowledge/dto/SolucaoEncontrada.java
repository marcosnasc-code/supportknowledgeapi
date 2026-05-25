package com.mpsupport.knowledge.dto;

import java.util.List;

public record SolucaoEncontrada(
        String resumo,
        List<String> referencias
) {
}
