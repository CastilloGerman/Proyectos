package com.appgestion.api.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

/**
 * Verifica el ID token de Google llamando a tokeninfo y devuelve el email si es válido.
 */
@Component
public class GoogleTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifier.class);
    private static final String TOKENINFO_BASE = "https://oauth2.googleapis.com/tokeninfo";

    private final RestTemplate restTemplate = new RestTemplate();

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TokenInfo(
            @JsonProperty("email") String email,
            @JsonProperty("email_verified") Boolean emailVerified,
            @JsonProperty("name") String name
    ) {}

    public Optional<TokenInfo> verify(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            log.warn("GoogleTokenVerifier: idToken vacío");
            return Optional.empty();
        }
        try {
            URI url = UriComponentsBuilder.fromUriString(TOKENINFO_BASE)
                    .queryParam("id_token", idToken)
                    .build()
                    .toUri();
            TokenInfo info = restTemplate.getForObject(url, TokenInfo.class);
            if (info != null && Boolean.TRUE.equals(info.emailVerified()) && info.email() != null && !info.email().isBlank()) {
                return Optional.of(info);
            }
            log.warn("GoogleTokenVerifier: tokeninfo sin email verificado o email vacío");
            return Optional.empty();
        } catch (RestClientException e) {
            log.warn("GoogleTokenVerifier: error al verificar token con Google: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
