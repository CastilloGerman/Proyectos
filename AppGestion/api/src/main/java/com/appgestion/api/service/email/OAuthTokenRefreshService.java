package com.appgestion.api.service.email;

import com.appgestion.api.config.AppEmailProperties;
import com.appgestion.api.domain.entity.Empresa;
import com.appgestion.api.domain.enums.EmailProviderMode;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.service.crypto.TokenEncryptionService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class OAuthTokenRefreshService {

    private static final long SKEW_SECONDS = 300;

    private final AppEmailProperties props;
    private final TokenEncryptionService tokenEncryption;
    private final EmpresaRepository empresaRepository;
    private final RestClient googleTokenClient;
    private final RestClient microsoftTokenClient;

    public OAuthTokenRefreshService(
            AppEmailProperties props,
            TokenEncryptionService tokenEncryption,
            EmpresaRepository empresaRepository,
            RestClient.Builder restClientBuilder) {
        this.props = props;
        this.tokenEncryption = tokenEncryption;
        this.empresaRepository = empresaRepository;
        this.googleTokenClient = restClientBuilder.baseUrl("https://oauth2.googleapis.com").build();
        this.microsoftTokenClient = restClientBuilder.baseUrl("https://login.microsoftonline.com").build();
    }

    public String getGoogleAccessToken(Empresa emp) {
        if (emp.getEmailProvider() != EmailProviderMode.gmail) {
            throw new IllegalStateException("Empresa no está en modo Gmail.");
        }
        refreshGoogleIfNeeded(emp);
        return tokenEncryption.decrypt(emp.getOauthAccessTokenEnc());
    }

    public String getMicrosoftAccessToken(Empresa emp) {
        if (emp.getEmailProvider() != EmailProviderMode.outlook) {
            throw new IllegalStateException("Empresa no está en modo Outlook.");
        }
        refreshMicrosoftIfNeeded(emp);
        return tokenEncryption.decrypt(emp.getOauthAccessTokenEnc());
    }

    public void refreshGoogleIfNeeded(Empresa emp) {
        if (emp.getEmailProvider() != EmailProviderMode.gmail) {
            return;
        }
        if (!StringUtils.hasText(emp.getOauthRefreshTokenEnc())) {
            throw new IllegalStateException("Gmail: sin refresh token; reconecta la cuenta.");
        }
        Instant exp = emp.getOauthTokenExpiresAt();
        if (exp != null && exp.isAfter(Instant.now().plusSeconds(SKEW_SECONDS))) {
            return;
        }
        String refreshPlain = tokenEncryption.decrypt(emp.getOauthRefreshTokenEnc());
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", props.getGoogle().getClientId());
        form.add("client_secret", props.getGoogle().getClientSecret());
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshPlain);

        JsonNode res = googleTokenClient.post()
                .uri("/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData(form))
                .retrieve()
                .body(JsonNode.class);
        Objects.requireNonNull(res, "respuesta token Google");
        applyGoogleTokenResponse(emp, res);
        empresaRepository.save(emp);
    }

    public void refreshMicrosoftIfNeeded(Empresa emp) {
        if (emp.getEmailProvider() != EmailProviderMode.outlook) {
            return;
        }
        if (!StringUtils.hasText(emp.getOauthRefreshTokenEnc())) {
            throw new IllegalStateException("Outlook: sin refresh token; reconecta la cuenta.");
        }
        Instant exp = emp.getOauthTokenExpiresAt();
        if (exp != null && exp.isAfter(Instant.now().plusSeconds(SKEW_SECONDS))) {
            return;
        }
        String refreshPlain = tokenEncryption.decrypt(emp.getOauthRefreshTokenEnc());
        String tenant = props.getMicrosoft().getTenant();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", props.getMicrosoft().getClientId());
        form.add("client_secret", props.getMicrosoft().getClientSecret());
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshPlain);
        form.add("scope", "offline_access https://graph.microsoft.com/Mail.Send");

        JsonNode res = microsoftTokenClient.post()
                .uri("/" + tenant + "/oauth2/v2.0/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData(form))
                .retrieve()
                .body(JsonNode.class);
        Objects.requireNonNull(res, "respuesta token Microsoft");
        applyMicrosoftTokenResponse(emp, res);
        empresaRepository.save(emp);
    }

    private void applyGoogleTokenResponse(Empresa emp, JsonNode res) {
        String access = res.path("access_token").asText(null);
        if (!StringUtils.hasText(access)) {
            throw new IllegalStateException("Google no devolvió access_token.");
        }
        emp.setOauthAccessTokenEnc(tokenEncryption.encrypt(access));
        if (res.hasNonNull("refresh_token")) {
            emp.setOauthRefreshTokenEnc(tokenEncryption.encrypt(res.get("refresh_token").asText()));
        }
        long expiresIn = res.path("expires_in").asLong(3600);
        emp.setOauthTokenExpiresAt(Instant.now().plusSeconds(expiresIn));
    }

    private void applyMicrosoftTokenResponse(Empresa emp, JsonNode res) {
        String access = res.path("access_token").asText(null);
        if (!StringUtils.hasText(access)) {
            throw new IllegalStateException("Microsoft no devolvió access_token.");
        }
        emp.setOauthAccessTokenEnc(tokenEncryption.encrypt(access));
        if (res.hasNonNull("refresh_token")) {
            emp.setOauthRefreshTokenEnc(tokenEncryption.encrypt(res.get("refresh_token").asText()));
        }
        long expiresIn = res.path("expires_in").asLong(3600);
        emp.setOauthTokenExpiresAt(Instant.now().plusSeconds(expiresIn));
    }

    private static String formData(MultiValueMap<String, String> form) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, List<String>> e : form.entrySet()) {
            for (String v : e.getValue()) {
                if (!first) {
                    sb.append('&');
                }
                first = false;
                sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
                        .append('=')
                        .append(URLEncoder.encode(v, StandardCharsets.UTF_8));
            }
        }
        return sb.toString();
    }
}
