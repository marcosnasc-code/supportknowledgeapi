package com.mpsupport.knowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.system-hints")
public class SystemHintsProperties {

    private String resource = "classpath:systems-hints.yml";
    private double minConfidence = 0.35;
    private int topN = 3;
    private double declaredBoostMultiplier = 1.5;

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public double getMinConfidence() {
        return minConfidence;
    }

    public void setMinConfidence(double minConfidence) {
        this.minConfidence = minConfidence;
    }

    public int getTopN() {
        return topN;
    }

    public void setTopN(int topN) {
        this.topN = topN;
    }

    public double getDeclaredBoostMultiplier() {
        return declaredBoostMultiplier;
    }

    public void setDeclaredBoostMultiplier(double declaredBoostMultiplier) {
        this.declaredBoostMultiplier = declaredBoostMultiplier;
    }
}
