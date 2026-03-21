package com.appgestion.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    /** Valor histórico inseguro del repo; rechazado fuera de local/test. */
    private static final String LEGACY_INSECURE_DEFAULT =
            "tu-clave-secreta-muy-larga-minimo-256-bits-para-HS256";

    private static final int MIN_SECRET_LENGTH_NON_LOCAL = 32;

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @Value("${app.jwt.issuer}")
    private String issuer;

    private final Environment environment;

    public JwtService(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validateSecret() {
        boolean devProfile = environment.matchesProfiles("local", "test");
        if (secret == null || secret.isBlank()) {
            if (devProfile) {
                log.warn("app.jwt.secret vacío: define JWT_SECRET o perfil local con valor por defecto solo para desarrollo.");
            } else {
                throw new IllegalStateException(
                        "JWT: app.jwt.secret / JWT_SECRET es obligatorio. Defínelo en el entorno (mín. 32 caracteres para HS256).");
            }
            return;
        }
        if (!devProfile) {
            if (secret.length() < MIN_SECRET_LENGTH_NON_LOCAL) {
                throw new IllegalStateException(
                        "JWT: el secreto debe tener al menos " + MIN_SECRET_LENGTH_NON_LOCAL + " caracteres en entornos no locales.");
            }
            if (LEGACY_INSECURE_DEFAULT.equals(secret.trim())) {
                throw new IllegalStateException(
                        "JWT: no uses el secreto por defecto del repositorio. Establece JWT_SECRET con un valor aleatorio fuerte.");
            }
        }
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String email, String rol) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("rol", rol != null ? rol : "USER")
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    public String extractRol(String token) {
        return getClaims(token).get("rol", String.class);
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT expirado: exp={}", e.getClaims().getExpiration());
            return false;
        } catch (MalformedJwtException | SignatureException | IllegalArgumentException e) {
            log.warn("JWT inválido (firma/secret): {}", e.getMessage());
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
