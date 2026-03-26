package com.appgestion.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Boot 4 registra por defecto Jackson 3 ({@code JsonMapper}); servicios que inyectan
 * {@link ObjectMapper} (Jackson 2 API) necesitan un bean explícito.
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
