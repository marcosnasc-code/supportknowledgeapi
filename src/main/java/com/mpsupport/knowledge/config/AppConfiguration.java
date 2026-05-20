package com.mpsupport.knowledge.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ImportCsvProperties.class, ImportProperties.class, SearchProperties.class})
public class AppConfiguration {
}
