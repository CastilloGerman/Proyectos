package com.appgestion.api.service;

import com.appgestion.api.service.GoogleTokenVerifier.TokenInfo;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Jwks;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** SEC-02: validaciones de audiencia, emisor, caducidad y email verificado sobre la respuesta de tokeninfo. */
class GoogleTokenVerifierTest {

    private static final String MY_CLIENT_ID = "123-mine.apps.googleusercontent.com";
    private static final String ISS = "https://accounts.google.com";

    private final GoogleTokenVerifier verifier = new GoogleTokenVerifier(MY_CLIENT_ID);

    private static TokenInfo info(String aud, String iss, Long exp, Boolean emailVerified, String email) {
        return new TokenInfo(email, emailVerified, "Nombre", aud, iss, exp);
    }

    private static long inFuture() {
        return Instant.now().plusSeconds(600).getEpochSecond();
    }

    @Test
    void audienciaPropia_emisorGoogle_emailVerificado_seAcepta() {
        assertThat(verifier.validate(info(MY_CLIENT_ID, ISS, inFuture(), true, "a@b.com"))).isPresent();
        assertThat(verifier.validate(info(MY_CLIENT_ID, "accounts.google.com", inFuture(), true, "a@b.com"))).isPresent();
    }

    @Test
    void audienciaDeOtraAplicacion_seRechaza() {
        assertThat(verifier.validate(
                info("999-otra-app.apps.googleusercontent.com", ISS, inFuture(), true, "a@b.com"))).isEmpty();
        assertThat(verifier.validate(info(null, ISS, inFuture(), true, "a@b.com"))).isEmpty();
    }

    @Test
    void emisorNoGoogle_seRechaza() {
        assertThat(verifier.validate(info(MY_CLIENT_ID, "https://evil.example.com", inFuture(), true, "a@b.com"))).isEmpty();
        assertThat(verifier.validate(info(MY_CLIENT_ID, null, inFuture(), true, "a@b.com"))).isEmpty();
    }

    @Test
    void tokenCaducadoOSinExp_seRechaza() {
        long past = Instant.now().minusSeconds(10).getEpochSecond();
        assertThat(verifier.validate(info(MY_CLIENT_ID, ISS, past, true, "a@b.com"))).isEmpty();
        assertThat(verifier.validate(info(MY_CLIENT_ID, ISS, null, true, "a@b.com"))).isEmpty();
    }

    @Test
    void emailNoVerificadoOVacio_seRechaza() {
        assertThat(verifier.validate(info(MY_CLIENT_ID, ISS, inFuture(), false, "a@b.com"))).isEmpty();
        assertThat(verifier.validate(info(MY_CLIENT_ID, ISS, inFuture(), null, "a@b.com"))).isEmpty();
        assertThat(verifier.validate(info(MY_CLIENT_ID, ISS, inFuture(), true, " "))).isEmpty();
        assertThat(verifier.validate(null)).isEmpty();
    }

    @Test
    void variosClientIds_separadosPorComas() {
        GoogleTokenVerifier v = new GoogleTokenVerifier("id-1, id-2 ,");
        assertThat(v.validate(info("id-2", ISS, inFuture(), true, "a@b.com"))).isPresent();
        assertThat(v.validate(info("id-3", ISS, inFuture(), true, "a@b.com"))).isEmpty();
    }

    @Test
    void sinClientIdsConfigurados_failClosed_noLlamaAGoogle() {
        GoogleTokenVerifier v = new GoogleTokenVerifier("");
        assertThat(v.verify("cualquier.id.token")).isEmpty();
    }

    @Test
    void tokenVacio_seRechaza() {
        assertThat(verifier.verify(null)).isEmpty();
        assertThat(verifier.verify("  ")).isEmpty();
    }

    @Test
    void firmaRsaDelClientId_seAcepta() throws Exception {
        KeyPair pair = Jwts.SIG.RS256.keyPair().build();
        GoogleTokenVerifier local = verifierConClave(pair);
        assertThat(local.verify(jwtFirmado(pair, MY_CLIENT_ID, true))).isPresent();
    }

    @Test
    void firmaDeOtraClave_seRechaza() {
        KeyPair signer = Jwts.SIG.RS256.keyPair().build();
        KeyPair published = Jwts.SIG.RS256.keyPair().build();
        GoogleTokenVerifier local = verifierConClave(published);
        assertThat(local.verify(jwtFirmado(signer, MY_CLIENT_ID, true))).isEmpty();
    }

    @Test
    void googleNoResponde_responde503SinQuedarseColgado() {
        GoogleTokenVerifier local = new GoogleTokenVerifier(MY_CLIENT_ID, () -> {
            throw new IOException("timeout");
        });
        assertThatThrownBy(() -> local.verify("eyJhbGciOiJSUzI1NiIsImtpZCI6ImtpZC0xIn0.eyJzdWIiOiJ4In0.c2ln"))
                .isInstanceOf(ResponseStatusException.class);
    }

    private static GoogleTokenVerifier verifierConClave(KeyPair published) {
        String jwk = Jwks.json(Jwks.builder().key((RSAPublicKey) published.getPublic()).id("kid-1").build());
        return new GoogleTokenVerifier(MY_CLIENT_ID, () -> "{\"keys\":[" + jwk + "]}");
    }

    private static String jwtFirmado(KeyPair signer, String audience, boolean emailVerified) {
        return Jwts.builder()
                .header().keyId("kid-1").and()
                .claim("email", "a@b.com")
                .claim("email_verified", emailVerified)
                .claim("name", "Ada")
                .issuer(ISS)
                .audience().add(audience).and()
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(signer.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }
}
