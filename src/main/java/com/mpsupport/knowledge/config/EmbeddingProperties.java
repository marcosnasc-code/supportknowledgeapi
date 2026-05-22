package com.mpsupport.knowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.embedding")
public class EmbeddingProperties {

    private int dimension = 768;
    private int batchSize = 16;
    private int maxContentChars = 6000;
    /**
     * 0 = sem limite por requisição de indexação.
     */
    private int defaultIndexLimit = 0;

    public int getDimension() {
        return dimension;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaxContentChars() {
        return maxContentChars;
    }

    public void setMaxContentChars(int maxContentChars) {
        this.maxContentChars = maxContentChars;
    }

    public int getDefaultIndexLimit() {
        return defaultIndexLimit;
    }

    public void setDefaultIndexLimit(int defaultIndexLimit) {
        this.defaultIndexLimit = defaultIndexLimit;
    }
}
