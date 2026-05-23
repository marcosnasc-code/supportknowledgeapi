package com.mpsupport.knowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ollama")
public class OllamaProperties {

    private String baseUrl = "http://localhost:11434";
    private String embeddingModel = "nomic-embed-text";
    private String chatModel = "llama3.2:1b";
    private int connectTimeoutSeconds = 30;
    private int readTimeoutSeconds = 120;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public String getChatModel() {
        return chatModel;
    }

    public void setChatModel(String chatModel) {
        this.chatModel = chatModel;
    }

    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
    }

    public int getReadTimeoutSeconds() {
        return readTimeoutSeconds;
    }

    public void setReadTimeoutSeconds(int readTimeoutSeconds) {
        this.readTimeoutSeconds = readTimeoutSeconds;
    }
}
