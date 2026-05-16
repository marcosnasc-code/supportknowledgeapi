package com.mpsupport.knowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.import")
public class ImportProperties {

    private int chunkBatchSize = 500;

    public int getChunkBatchSize() {
        return chunkBatchSize;
    }

    public void setChunkBatchSize(int chunkBatchSize) {
        this.chunkBatchSize = chunkBatchSize;
    }
}
