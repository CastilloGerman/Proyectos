package com.appgestion.api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiConfiguration {
}
