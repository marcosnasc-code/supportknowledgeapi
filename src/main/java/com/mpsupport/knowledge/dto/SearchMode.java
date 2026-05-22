package com.mpsupport.knowledge.dto;

public enum SearchMode {
    /** Busca por palavras (full-text PostgreSQL). */
    TEXT,
    /** Busca por similaridade de embedding (Ollama + pgvector). */
    SEMANTIC,
    /** Combina TEXT + SEMANTIC (RRF). */
    HYBRID
}
