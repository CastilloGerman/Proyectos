package com.appgestion.api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AppEmailProperties.class)
public class AppEmailConfiguration {
}
