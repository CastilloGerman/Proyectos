package com.appgestion.api.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Jwk;
import io.jsonwebtoken.security.JwkSet;
import io.jsonwebtoken.security.Jwks;
import io.jsonwebtoken.security.RsaPublicJwk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Verifica el ID token de Google en local (firma RS256 con las claves públicas de Google) y devuelve los datos
 * del usuario solo si:
 * <ul>
 *   <li>la firma coincide con una clave vigente de {@code https://www.googleapis.com/oauth2/v3/certs},</li>
 *   <li>{@code aud} coincide con un Client ID propio,</li>
 *   <li>{@code iss} es Google,</li>
 *   <li>el token no está caducado y el email está verificado.</li>
 * </ul>
 * Las claves se cachean. La descarga tiene un plazo máximo corto: si Google no responde, el login falla enseguida
 * en lugar de quedarse colgado hasta que el proxy devuelve 504. Si no hay ningún Client ID configurado, el login
 * con Google se rechaza (fail-closed).
 */
@Component
public class GoogleTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifier.class);
    private static final URI JWKS_URI = URI.create("https://www.googleapis.com/oauth2/v3/certs");
    private static final Set<String> VALID_ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(4);
    private static final Duration JWKS_TTL = Duration.ofHours(6);
    private static final int MAX_TOKEN_CHARS = 8192;

    private final Set<String> allowedClientIds;
    private final JwksSource jwksSource;
    private final HttpClient httpClient;
    private volatile CachedKeys cachedKeys;

    @Autowired
    public GoogleTokenVerifier(@Value("${app.auth.google.client-ids:}") String clientIdsCsv) {
        this(clientIdsCsv, null);
    }

    /** {@code jwksSource} nulo usa la descarga real de Google. Los tests inyectan un origen fijo. */
    GoogleTokenVerifier(String clientIdsCsv, JwksSource jwksSource) {
        this.allowedClientIds = parseClientIds(clientIdsCsv);
        this.jwksSource = jwksSource;
        this.httpClient = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build();
        if (allowedClientIds.isEmpty()) {
            log.error("app.auth.google.client-ids está vacío: el login con Google quedará DESACTIVADO hasta configurarlo.");
        }
    }

    @FunctionalInterface
    interface JwksSource {
        String load() throws IOException, InterruptedException;
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
        String token = idToken.trim();
        if (token.length() > MAX_TOKEN_CHARS || countDots(token) != 2) {
            log.warn("GoogleTokenVerifier: idToken con formato inválido");
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .keyLocator(header -> {
                        Object kid = header.get("kid");
                        PublicKey key = kid == null ? null : resolveKey(kid.toString());
                        if (key == null) {
                            throw new JwtException("kid de Google desconocido");
                        }
                        return key;
                    })
                    .clockSkewSeconds(60)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return validate(toTokenInfo(claims));
        } catch (RuntimeException e) {
            ResponseStatusException unavailable = findUnavailable(e);
            if (unavailable != null) {
                throw unavailable;
            }
            log.warn("GoogleTokenVerifier: token rechazado ({})", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    /** Validaciones de contenido sobre los claims ya firmados (visible para tests). */
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

    private static ResponseStatusException findUnavailable(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof ResponseStatusException status
                    && status.getStatusCode().value() == HttpStatus.SERVICE_UNAVAILABLE.value()) {
                return status;
            }
            current = current.getCause();
        }
        return null;
    }

    private TokenInfo toTokenInfo(Claims claims) {
        Date expiration = claims.getExpiration();
        Long exp = expiration == null ? null : expiration.toInstant().getEpochSecond();
        return new TokenInfo(
                claims.get("email", String.class),
                emailVerified(claims.get("email_verified")),
                claims.get("name", String.class),
                matchingAudience(claims.getAudience()),
                claims.getIssuer(),
                exp
        );
    }

    private String matchingAudience(Set<String> audience) {
        if (audience == null || audience.isEmpty()) {
            return null;
        }
        String fallback = null;
        for (String candidate : audience) {
            if (fallback == null) {
                fallback = candidate;
            }
            if (allowedClientIds.contains(candidate)) {
                return candidate;
            }
        }
        return fallback;
    }

    private static Boolean emailVerified(Object raw) {
        if (raw instanceof Boolean verified) {
            return verified;
        }
        if (raw instanceof String text) {
            return Boolean.valueOf(text);
        }
        return null;
    }

    private PublicKey resolveKey(String kid) {
        Map<String, PublicKey> keys = keys(false);
        PublicKey key = keys.get(kid);
        if (key != null) {
            return key;
        }
        return keys(true).get(kid);
    }

    private Map<String, PublicKey> keys(boolean forceRefresh) {
        CachedKeys current = cachedKeys;
        if (!forceRefresh && current != null && current.fresh()) {
            return current.keys();
        }
        synchronized (this) {
            current = cachedKeys;
            if (!forceRefresh && current != null && current.fresh()) {
                return current.keys();
            }
            try {
                Map<String, PublicKey> parsed = parseKeys(loadJwks());
                cachedKeys = new CachedKeys(parsed, Instant.now());
                return parsed;
            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                if (current != null) {
                    log.warn("GoogleTokenVerifier: no se pudieron refrescar las claves; se usa la caché anterior");
                    return current.keys();
                }
                log.warn("GoogleTokenVerifier: Google no respondió al pedir las claves públicas");
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "No se pudo contactar con Google para verificar el inicio de sesión. Inténtalo de nuevo.");
            }
        }
    }

    private String loadJwks() throws IOException, InterruptedException {
        if (jwksSource != null) {
            return jwksSource.load();
        }
        HttpRequest request = HttpRequest.newBuilder(JWKS_URI)
                .timeout(HTTP_TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 || response.body() == null || response.body().isBlank()) {
            throw new IOException("JWKS HTTP " + response.statusCode());
        }
        return response.body();
    }

    private static Map<String, PublicKey> parseKeys(String json) throws IOException {
        JwkSet set = Jwks.setParser().build().parse(json);
        Map<String, PublicKey> keys = new HashMap<>();
        for (Jwk<?> jwk : set.getKeys()) {
            if (jwk instanceof RsaPublicJwk rsa && rsa.getId() != null) {
                RSAPublicKey key = rsa.toKey();
                keys.put(rsa.getId(), key);
            }
        }
        if (keys.isEmpty()) {
            throw new IOException("JWKS sin claves RSA");
        }
        return Map.copyOf(keys);
    }

    private static int countDots(String token) {
        int dots = 0;
        for (int i = 0; i < token.length(); i++) {
            if (token.charAt(i) == '.') {
                dots++;
            }
        }
        return dots;
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

    private record CachedKeys(Map<String, PublicKey> keys, Instant fetchedAt) {
        boolean fresh() {
            return fetchedAt.plus(JWKS_TTL).isAfter(Instant.now());
        }
    }
}
