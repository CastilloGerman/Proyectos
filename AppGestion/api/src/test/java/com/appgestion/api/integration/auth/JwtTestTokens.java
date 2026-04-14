package com.appgestion.api.integration.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT de prueba firmados con la misma clave e issuer que {@code application-test.properties}
 * ({@code app.jwt.secret}, {@code app.jwt.issuer}). Reutilizable en otros bloques de tests.
 * Para simular un token inválido (p. ej. webhooks o cabeceras), usar {@link #tamperedToken(String, String)}.
 */
public final class JwtTestTokens {

    /** Debe coincidir con {@code app.jwt.secret} en perfil {@code test}. */
    public static final String TEST_JWT_SECRET = "test-secret-at-least-32-characters-long!!";

    public static final String TEST_JWT_ISSUER = "appgestion-api-test";

    private JwtTestTokens() {
    }

    private static SecretKey signingKey() {
        return Keys.hmacShaKeyFor(TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Token válido (no expirado), sin {@code sid}, mismo formato que {@link com.appgestion.api.security.JwtService}.
     */
    public static String validToken(String email, String rol) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + 3600_000L);
        return Jwts.builder()
                .subject(email)
                .claim("rol", rol != null ? rol : "USER")
                .issuer(TEST_JWT_ISSUER)
                .issuedAt(now)
                .expiration(exp)
                .signWith(signingKey())
                .compact();
    }

    public static String expiredToken(String email, String rol) {
        Date now = new Date();
        Date exp = new Date(now.getTime() - 3_600_000L);
        return Jwts.builder()
                .subject(email)
                .claim("rol", rol != null ? rol : "USER")
                .issuer(TEST_JWT_ISSUER)
                .issuedAt(new Date(exp.getTime() - 3_600_000L))
                .expiration(exp)
                .signWith(signingKey())
                .compact();
    }

    /**
     * JWT con cabecera y payload válidos pero firma sustituida (no verificable con el secreto de la API).
     */
    public static String tamperedToken(String email, String rol) {
        String valid = validToken(email, rol);
        int i = valid.lastIndexOf('.');
        if (i < 0) {
            return valid + "x";
        }
        return valid.substring(0, i + 1) + "ZmFrZS1zaWduYXR1cmU";
    }
}
