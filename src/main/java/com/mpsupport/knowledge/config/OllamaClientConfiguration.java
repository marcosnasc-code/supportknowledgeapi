package com.mpsupport.knowledge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class OllamaClientConfiguration {

    @Bean
    RestClient ollamaRestClient(OllamaProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int connectMs = properties.getConnectTimeoutSeconds() * 1000;
        int readMs = properties.getReadTimeoutSeconds() * 1000;
        requestFactory.setConnectTimeout(connectMs);
        requestFactory.setReadTimeout(readMs);

        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
