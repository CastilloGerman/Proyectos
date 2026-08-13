package com.appgestion.api.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

/**
 * Verifica el ID token de Google llamando a tokeninfo y devuelve el email si es válido
 * y el token fue emitido para el cliente OAuth de esta aplicación ({@code aud}).
 */
@Component
public class GoogleTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifier.class);
    private static final String TOKENINFO_BASE = "https://oauth2.googleapis.com/tokeninfo";

    private final RestTemplate restTemplate = new RestTemplate();
    private final String expectedClientId;

    public GoogleTokenVerifier(@Value("${app.auth.google.client-id:}") String expectedClientId) {
        this.expectedClientId = expectedClientId == null ? "" : expectedClientId.trim();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TokenInfo(
            @JsonProperty("email") String email,
            @JsonProperty("email_verified") Boolean emailVerified,
            @JsonProperty("name") String name,
            @JsonProperty("aud") String aud,
            @JsonProperty("iss") String iss
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
            if (isUsableLoginToken(info)) {
                return Optional.of(info);
            }
            log.warn("GoogleTokenVerifier: tokeninfo rechazado (email, aud o iss no válidos)");
            return Optional.empty();
        } catch (RestClientException e) {
            log.warn("GoogleTokenVerifier: error al verificar token con Google: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Acepta solo tokens con email verificado emitidos por Google para el client ID de login.
     * Sin comprobar {@code aud}, un ID token de cualquier app OAuth del atacante permitiría
     * iniciar sesión como la víctima.
     */
    boolean isUsableLoginToken(TokenInfo info) {
        if (info == null || !Boolean.TRUE.equals(info.emailVerified())
                || info.email() == null || info.email().isBlank()) {
            return false;
        }
        if (expectedClientId.isEmpty()) {
            log.error("GoogleTokenVerifier: app.auth.google.client-id no configurado; se rechaza el token");
            return false;
        }
        String aud = info.aud() == null ? "" : info.aud().trim();
        if (!expectedClientId.equals(aud)) {
            return false;
        }
        String iss = info.iss() == null ? "" : info.iss().trim();
        return "accounts.google.com".equals(iss) || "https://accounts.google.com".equals(iss);
    }
}
