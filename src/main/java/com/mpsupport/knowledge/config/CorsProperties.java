package com.mpsupport.knowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    /**
     * Padrões de origem permitidos (Spring allowedOriginPatterns).
     * Inclui Vite local e previews do Lovable.
     */
    private List<String> allowedOriginPatterns = new ArrayList<>(List.of(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "https://*.lovable.app",
            "https://*.lovableproject.com"
    ));

    public List<String> getAllowedOriginPatterns() {
        return allowedOriginPatterns;
    }

    public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
        this.allowedOriginPatterns = allowedOriginPatterns;
    }
}
