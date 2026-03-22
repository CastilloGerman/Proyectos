package com.appgestion.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class WebConfig {

    /**
     * Orígenes permitidos separados por coma. Ej.: https://app.ejemplo.com,https://www.ejemplo.com
     * En producción debe acotarse explícitamente (no usar * con credentials).
     */
    @Value("${app.cors.allowed-origins:http://localhost:4200,http://127.0.0.1:4200}")
    private String allowedOriginsRaw;

    /**
     * Patrones CORS (p. ej. {@code http://*:4200} en perfil local) para probar desde la LAN sin listar cada IP.
     * Si está definido y no vacío, sustituye a {@code allowed-origins} (no deben usarse ambos a la vez).
     */
    @Value("${app.cors.allowed-origin-patterns:}")
    private String allowedOriginPatternsRaw;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> patterns = Arrays.stream(allowedOriginPatternsRaw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());

        CorsConfiguration config = new CorsConfiguration();
        if (!patterns.isEmpty()) {
            config.setAllowedOriginPatterns(patterns);
        } else {
            List<String> origins = Arrays.stream(allowedOriginsRaw.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList());
            if (origins.isEmpty()) {
                origins = List.of("http://localhost:4200");
            }
            config.setAllowedOrigins(origins);
        }
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
