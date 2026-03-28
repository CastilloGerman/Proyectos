package com.appgestion.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AppHttpClientConfiguration {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
