package com.appgestion.api.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Verifica el ID token de Google (tokeninfo) y devuelve los datos del usuario solo si:
 * <ul>
 *   <li>Google confirma la firma y la vigencia del token,</li>
 *   <li>{@code aud} coincide con un Client ID propio (evita aceptar tokens emitidos para OTRA aplicación),</li>
 *   <li>{@code iss} es Google,</li>
 *   <li>el email está verificado por Google.</li>
 * </ul>
 * El token se envía por POST (cuerpo), nunca en la URL, para que no quede en logs ni proxies.
 * Si no hay ningún Client ID configurado, el login con Google se rechaza (fail-closed).
 */
@Component
public class GoogleTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifier.class);
    private static final String TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo";
    private static final Set<String> VALID_ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    private final RestTemplate restTemplate;
    private final Set<String> allowedClientIds;

    @Autowired
    public GoogleTokenVerifier(@Value("${app.auth.google.client-ids:}") String clientIdsCsv) {
        this(buildRestTemplate(), parseClientIds(clientIdsCsv));
    }

    GoogleTokenVerifier(RestTemplate restTemplate, Set<String> allowedClientIds) {
        this.restTemplate = restTemplate;
        this.allowedClientIds = allowedClientIds;
        if (allowedClientIds.isEmpty()) {
            log.error("app.auth.google.client-ids está vacío: el login con Google quedará DESACTIVADO hasta configurarlo.");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TokenInfo(
            @JsonProperty("email") String email,
            @JsonProperty("email_verified") Boolean emailVerified,
            @JsonProperty("name") String name,
            @JsonProperty("aud") String aud,
            @JsonProperty("iss") String iss,
            @JsonProperty("exp") Long exp
    ) {}

    public Optional<TokenInfo> verify(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            log.warn("GoogleTokenVerifier: idToken vacío");
            return Optional.empty();
        }
        if (allowedClientIds.isEmpty()) {
            return Optional.empty();
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("id_token", idToken.trim());
            TokenInfo info = restTemplate.postForObject(TOKENINFO_URL, new HttpEntity<>(form, headers), TokenInfo.class);
            return validate(info);
        } catch (RestClientException e) {
            log.warn("GoogleTokenVerifier: error al verificar token con Google: {}", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    /** Validaciones de contenido sobre la respuesta de tokeninfo (visible para tests). */
    Optional<TokenInfo> validate(TokenInfo info) {
        if (info == null) {
            log.warn("GoogleTokenVerifier: respuesta vacía de tokeninfo");
            return Optional.empty();
        }
        if (info.aud() == null || !allowedClientIds.contains(info.aud())) {
            log.warn("GoogleTokenVerifier: audiencia (aud) no autorizada");
            return Optional.empty();
        }
        if (info.iss() == null || !VALID_ISSUERS.contains(info.iss())) {
            log.warn("GoogleTokenVerifier: emisor (iss) no válido");
            return Optional.empty();
        }
        if (info.exp() == null || Instant.ofEpochSecond(info.exp()).isBefore(Instant.now())) {
            log.warn("GoogleTokenVerifier: token caducado o sin exp");
            return Optional.empty();
        }
        if (!Boolean.TRUE.equals(info.emailVerified()) || info.email() == null || info.email().isBlank()) {
            log.warn("GoogleTokenVerifier: tokeninfo sin email verificado o email vacío");
            return Optional.empty();
        }
        return Optional.of(info);
    }

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        return new RestTemplate(factory);
    }

    private static Set<String> parseClientIds(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        List<String> ids = Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        return Set.copyOf(ids);
    }
}
