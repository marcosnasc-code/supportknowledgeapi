package com.mpsupport.knowledge.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.mpsupport.knowledge.domain.SystemHintDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Configuration
public class SystemHintsConfiguration {

    @Bean
    List<SystemHintDefinition> systemHintDefinitions(
            SystemHintsProperties properties,
            ResourceLoader resourceLoader
    ) throws IOException {
        Resource resource = resourceLoader.getResource(properties.getResource());
        try (InputStream in = resource.getInputStream()) {
            ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
            SystemsHintsFile file = yamlMapper.readValue(in, SystemsHintsFile.class);
            if (file.systems() == null || file.systems().isEmpty()) {
                throw new IllegalStateException("systems-hints.yml não contém sistemas.");
            }
            return List.copyOf(file.systems());
        }
    }

    private record SystemsHintsFile(List<SystemHintDefinition> systems) {
    }
}
