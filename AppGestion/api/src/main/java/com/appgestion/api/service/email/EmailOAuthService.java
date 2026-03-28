package com.appgestion.api.service.email;

import com.appgestion.api.config.AppEmailProperties;
import com.appgestion.api.domain.entity.Empresa;
import com.appgestion.api.domain.entity.OauthPending;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.EmailProviderMode;
import com.appgestion.api.domain.enums.OAuthOnFailureMode;
import com.appgestion.api.dto.response.EmailOAuthStatusResponse;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.repository.OauthPendingRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.service.crypto.TokenEncryptionService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class EmailOAuthService {

    private final AppEmailProperties props;
    private final TokenEncryptionService tokenEncryption;
    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final OauthPendingRepository oauthPendingRepository;
    private final RestClient googleApiClient;
    private final RestClient googleTokenClient;
    private final RestClient microsoftTokenClient;
    private final RestClient graphClient;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public EmailOAuthService(
            AppEmailProperties props,
            TokenEncryptionService tokenEncryption,
            UsuarioRepository usuarioRepository,
            EmpresaRepository empresaRepository,
            OauthPendingRepository oauthPendingRepository,
            RestClient.Builder restClientBuilder) {
        this.props = props;
        this.tokenEncryption = tokenEncryption;
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.oauthPendingRepository = oauthPendingRepository;
        this.googleApiClient = restClientBuilder.baseUrl("https://www.googleapis.com").build();
        this.googleTokenClient = restClientBuilder.baseUrl("https://oauth2.googleapis.com").build();
        this.microsoftTokenClient = restClientBuilder.baseUrl("https://login.microsoftonline.com").build();
        this.graphClient = restClientBuilder.baseUrl("https://graph.microsoft.com").build();
    }

    public String buildGoogleAuthorizeUrl(Long usuarioId) {
        requireOAuthConfigGoogle();
        Usuario u = usuarioRepository.findById(usuarioId).orElseThrow();
        String state = randomState();
        String verifier = randomVerifier();
        String challenge = pkceChallenge(verifier);
        OauthPending p = new OauthPending();
        p.setStateToken(state);
        p.setUsuario(u);
        p.setProvider("google");
        p.setCodeVerifier(verifier);
        p.setExpiresAt(Instant.now().plusSeconds(900));
        oauthPendingRepository.save(p);

        String scope = "https://www.googleapis.com/auth/gmail.send";
        return "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + enc(props.getGoogle().getClientId())
                + "&redirect_uri=" + enc(props.getGoogle().getRedirectUri())
                + "&response_type=code"
                + "&scope=" + enc(scope)
                + "&state=" + enc(state)
                + "&access_type=offline"
                + "&prompt=consent"
                + "&code_challenge=" + enc(challenge)
                + "&code_challenge_method=S256";
    }

    public String buildMicrosoftAuthorizeUrl(Long usuarioId) {
        requireOAuthConfigMicrosoft();
        Usuario u = usuarioRepository.findById(usuarioId).orElseThrow();
        String state = randomState();
        String verifier = randomVerifier();
        String challenge = pkceChallenge(verifier);
        OauthPending p = new OauthPending();
        p.setStateToken(state);
        p.setUsuario(u);
        p.setProvider("microsoft");
        p.setCodeVerifier(verifier);
        p.setExpiresAt(Instant.now().plusSeconds(900));
        oauthPendingRepository.save(p);

        String tenant = props.getMicrosoft().getTenant();
        String scope = "offline_access https://graph.microsoft.com/Mail.Send openid profile";
        return "https://login.microsoftonline.com/" + tenant + "/oauth2/v2.0/authorize"
                + "?client_id=" + enc(props.getMicrosoft().getClientId())
                + "&response_type=code"
                + "&redirect_uri=" + enc(props.getMicrosoft().getRedirectUri())
                + "&response_mode=query"
                + "&scope=" + enc(scope)
                + "&state=" + enc(state)
                + "&code_challenge=" + enc(challenge)
                + "&code_challenge_method=S256";
    }

    @Transactional
    public String handleGoogleCallback(String code, String state) {
        OauthPending pending = oauthPendingRepository.findByStateToken(state)
                .filter(p -> p.getExpiresAt().isAfter(Instant.now()))
                .filter(p -> "google".equals(p.getProvider()))
                .orElse(null);
        if (pending == null) {
            return frontendUrl + "/cuenta/datos-empresa?oauth=error&reason=state";
        }
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("code", code);
            form.add("client_id", props.getGoogle().getClientId());
            form.add("client_secret", props.getGoogle().getClientSecret());
            form.add("redirect_uri", props.getGoogle().getRedirectUri());
            form.add("grant_type", "authorization_code");
            form.add("code_verifier", pending.getCodeVerifier());

            JsonNode res = googleTokenClient.post()
                    .uri("/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formBody(form))
                    .retrieve()
                    .body(JsonNode.class);
            Objects.requireNonNull(res, "token google");
            String access = res.path("access_token").asText(null);
            if (!StringUtils.hasText(access)) {
                return frontendUrl + "/cuenta/datos-empresa?oauth=error&reason=token";
            }
            String refresh = res.path("refresh_token").asText(null);
            long expiresIn = res.path("expires_in").asLong(3600);

            String email = fetchGoogleEmail(access);

            Empresa emp = empresaRepository.findByUsuarioId(pending.getUsuario().getId())
                    .orElseGet(() -> {
                        Empresa e = new Empresa();
                        e.setUsuario(pending.getUsuario());
                        e.setNombre("");
                        return e;
                    });
            emp.setEmailProvider(EmailProviderMode.gmail);
            emp.setOauthProvider("google");
            emp.setOauthAccessTokenEnc(tokenEncryption.encrypt(access));
            if (StringUtils.hasText(refresh)) {
                emp.setOauthRefreshTokenEnc(tokenEncryption.encrypt(refresh));
            }
            emp.setOauthTokenExpiresAt(Instant.now().plusSeconds(expiresIn));
            emp.setOauthConnectedAt(Instant.now());
            if (StringUtils.hasText(email)) {
                emp.setMailUsername(email.trim());
            }
            empresaRepository.save(emp);
            oauthPendingRepository.delete(pending);
            return frontendUrl + "/cuenta/datos-empresa?oauth=success";
        } catch (Exception e) {
            return frontendUrl + "/cuenta/datos-empresa?oauth=error&reason=callback";
        }
    }

    @Transactional
    public String handleMicrosoftCallback(String code, String state) {
        OauthPending pending = oauthPendingRepository.findByStateToken(state)
                .filter(p -> p.getExpiresAt().isAfter(Instant.now()))
                .filter(p -> "microsoft".equals(p.getProvider()))
                .orElse(null);
        if (pending == null) {
            return frontendUrl + "/cuenta/datos-empresa?oauth=error&reason=state";
        }
        try {
            String tenant = props.getMicrosoft().getTenant();
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("client_id", props.getMicrosoft().getClientId());
            form.add("client_secret", props.getMicrosoft().getClientSecret());
            form.add("grant_type", "authorization_code");
            form.add("code", code);
            form.add("redirect_uri", props.getMicrosoft().getRedirectUri());
            form.add("code_verifier", pending.getCodeVerifier());
            form.add("scope", "offline_access https://graph.microsoft.com/Mail.Send");

            JsonNode res = microsoftTokenClient.post()
                    .uri("/" + tenant + "/oauth2/v2.0/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formBody(form))
                    .retrieve()
                    .body(JsonNode.class);
            Objects.requireNonNull(res, "token ms");
            String access = res.path("access_token").asText(null);
            if (!StringUtils.hasText(access)) {
                return frontendUrl + "/cuenta/datos-empresa?oauth=error&reason=token";
            }
            String refresh = res.path("refresh_token").asText(null);
            long expiresIn = res.path("expires_in").asLong(3600);

            String email = fetchMicrosoftEmail(access);

            Empresa emp = empresaRepository.findByUsuarioId(pending.getUsuario().getId())
                    .orElseGet(() -> {
                        Empresa e = new Empresa();
                        e.setUsuario(pending.getUsuario());
                        e.setNombre("");
                        return e;
                    });
            emp.setEmailProvider(EmailProviderMode.outlook);
            emp.setOauthProvider("microsoft");
            emp.setOauthAccessTokenEnc(tokenEncryption.encrypt(access));
            if (StringUtils.hasText(refresh)) {
                emp.setOauthRefreshTokenEnc(tokenEncryption.encrypt(refresh));
            }
            emp.setOauthTokenExpiresAt(Instant.now().plusSeconds(expiresIn));
            emp.setOauthConnectedAt(Instant.now());
            if (StringUtils.hasText(email)) {
                emp.setMailUsername(email.trim());
            }
            empresaRepository.save(emp);
            oauthPendingRepository.delete(pending);
            return frontendUrl + "/cuenta/datos-empresa?oauth=success";
        } catch (Exception e) {
            return frontendUrl + "/cuenta/datos-empresa?oauth=error&reason=callback";
        }
    }

    @Transactional
    public void disconnect(Long usuarioId) {
        Empresa emp = empresaRepository.findByUsuarioId(usuarioId).orElse(null);
        if (emp == null) {
            return;
        }
        emp.setEmailProvider(EmailProviderMode.system);
        emp.setOauthProvider(null);
        emp.setOauthAccessTokenEnc(null);
        emp.setOauthRefreshTokenEnc(null);
        emp.setOauthTokenExpiresAt(null);
        emp.setOauthConnectedAt(null);
        empresaRepository.save(emp);
    }

    public EmailOAuthStatusResponse status(Long usuarioId) {
        boolean googleCfg = isGoogleOAuthConfigured();
        boolean msCfg = isMicrosoftOAuthConfigured();
        Empresa emp = empresaRepository.findByUsuarioId(usuarioId).orElse(null);
        if (emp == null) {
            return new EmailOAuthStatusResponse(
                    EmailProviderMode.system.name(),
                    null,
                    false,
                    null,
                    OAuthOnFailureMode.system.name(),
                    null,
                    googleCfg,
                    msCfg);
        }
        boolean connected = StringUtils.hasText(emp.getOauthRefreshTokenEnc());
        return new EmailOAuthStatusResponse(
                emp.getEmailProvider() != null ? emp.getEmailProvider().name() : EmailProviderMode.system.name(),
                emp.getOauthProvider(),
                connected,
                emp.getOauthConnectedAt(),
                emp.getOauthOnFailure() != null ? emp.getOauthOnFailure().name() : OAuthOnFailureMode.system.name(),
                emp.getSystemFromOverride(),
                googleCfg,
                msCfg);
    }

    public boolean isGoogleOAuthConfigured() {
        return StringUtils.hasText(props.getGoogle().getClientId())
                && StringUtils.hasText(props.getGoogle().getClientSecret());
    }

    public boolean isMicrosoftOAuthConfigured() {
        return StringUtils.hasText(props.getMicrosoft().getClientId())
                && StringUtils.hasText(props.getMicrosoft().getClientSecret());
    }

    private String fetchGoogleEmail(String accessToken) {
        try {
            JsonNode me = googleApiClient.get()
                    .uri("/oauth2/v3/userinfo")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(JsonNode.class);
            if (me != null && me.hasNonNull("email")) {
                return me.get("email").asText();
            }
        } catch (Exception ignored) {
            // omitir
        }
        return null;
    }

    private String fetchMicrosoftEmail(String accessToken) {
        try {
            JsonNode me = graphClient.get()
                    .uri("/v1.0/me")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(JsonNode.class);
            if (me == null) {
                return null;
            }
            if (me.hasNonNull("mail")) {
                return me.get("mail").asText();
            }
            if (me.hasNonNull("userPrincipalName")) {
                return me.get("userPrincipalName").asText();
            }
        } catch (Exception ignored) {
            // omitir
        }
        return null;
    }

    private void requireOAuthConfigGoogle() {
        if (!isGoogleOAuthConfigured()) {
            throw new IllegalStateException(
                    "OAuth Google no está configurado en el servidor. Añade GOOGLE_OAUTH_CLIENT_ID y "
                            + "GOOGLE_OAUTH_CLIENT_SECRET (o app.email.google.client-id / client-secret en YAML). "
                            + "Redirect URI de desarrollo: " + props.getGoogle().getRedirectUri());
        }
    }

    private void requireOAuthConfigMicrosoft() {
        if (!isMicrosoftOAuthConfigured()) {
            throw new IllegalStateException(
                    "OAuth Microsoft no está configurado en el servidor. Añade MICROSOFT_OAUTH_CLIENT_ID y "
                            + "MICROSOFT_OAUTH_CLIENT_SECRET. Redirect URI: " + props.getMicrosoft().getRedirectUri());
        }
    }

    private static String randomState() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    private static String randomVerifier() {
        byte[] b = new byte[32];
        new SecureRandom().nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static String pkceChallenge(String verifier) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(verifier.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(d);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String formBody(MultiValueMap<String, String> form) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, java.util.List<String>> e : form.entrySet()) {
            for (String v : e.getValue()) {
                if (!first) {
                    sb.append('&');
                }
                first = false;
                sb.append(enc(e.getKey())).append('=').append(enc(v));
            }
        }
        return sb.toString();
    }
}
