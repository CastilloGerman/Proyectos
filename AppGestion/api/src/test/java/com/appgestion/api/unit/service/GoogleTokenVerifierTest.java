package com.appgestion.api.unit.service;

import com.appgestion.api.service.GoogleTokenVerifier;
import com.appgestion.api.service.GoogleTokenVerifier.TokenInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleTokenVerifierTest {

    private static final String APP_CLIENT_ID =
            "622654316729-itkgprp568mrobd3v8lgnah0cfjchog9.apps.googleusercontent.com";

    private final GoogleTokenVerifier verifier = new GoogleTokenVerifier(APP_CLIENT_ID);

    @Test
    void acceptsVerifiedTokenIssuedToThisApp() {
        assertThat(verifier.isUsableLoginToken(token(
                "victim@example.com", true, APP_CLIENT_ID, "https://accounts.google.com"))).isTrue();
        assertThat(verifier.isUsableLoginToken(token(
                "victim@example.com", true, APP_CLIENT_ID, "accounts.google.com"))).isTrue();
    }

    @Test
    void rejectsTokenIssuedToAnotherOAuthClient() {
        String attackerClientId = "999999999999-attacker.apps.googleusercontent.com";
        assertThat(verifier.isUsableLoginToken(token(
                "victim@example.com", true, attackerClientId, "https://accounts.google.com"))).isFalse();
    }

    @Test
    void rejectsWhenClientIdNotConfigured() {
        GoogleTokenVerifier unconfigured = new GoogleTokenVerifier("  ");
        assertThat(unconfigured.isUsableLoginToken(token(
                "victim@example.com", true, APP_CLIENT_ID, "https://accounts.google.com"))).isFalse();
    }

    @Test
    void rejectsUnverifiedEmailOrWrongIssuer() {
        assertThat(verifier.isUsableLoginToken(token(
                "victim@example.com", false, APP_CLIENT_ID, "https://accounts.google.com"))).isFalse();
        assertThat(verifier.isUsableLoginToken(token(
                "victim@example.com", true, APP_CLIENT_ID, "https://evil.example"))).isFalse();
        assertThat(verifier.isUsableLoginToken(null)).isFalse();
    }

    private static TokenInfo token(String email, boolean verified, String aud, String iss) {
        return new TokenInfo(email, verified, "Victim", aud, iss);
    }
}
