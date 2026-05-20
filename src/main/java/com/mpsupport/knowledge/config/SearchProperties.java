package com.mpsupport.knowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.search")
public class SearchProperties {

    /**
     * Configuração do PostgreSQL full-text (ex.: portuguese, simple).
     */
    private String textSearchConfig = "portuguese";

    private int snippetMaxLength = 400;

    public String getTextSearchConfig() {
        return textSearchConfig;
    }

    public void setTextSearchConfig(String textSearchConfig) {
        this.textSearchConfig = textSearchConfig;
    }

    public int getSnippetMaxLength() {
        return snippetMaxLength;
    }

    public void setSnippetMaxLength(int snippetMaxLength) {
        this.snippetMaxLength = snippetMaxLength;
    }
}
