package com.appgestion.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.email")
public class AppEmailProperties {

    /**
     * Clave para cifrar tokens OAuth en reposo (mín. 32 caracteres recomendado).
     */
    private String tokenEncryptionKey = "";

    private Resend resend = new Resend();

    /** Remitente por defecto en modo system (Resend). */
    private String systemFrom = "Noemi <onboarding@resend.dev>";

    private Google google = new Google();

    private Microsoft microsoft = new Microsoft();

    public String getTokenEncryptionKey() {
        return tokenEncryptionKey;
    }

    public void setTokenEncryptionKey(String tokenEncryptionKey) {
        this.tokenEncryptionKey = tokenEncryptionKey;
    }

    public Resend getResend() {
        return resend;
    }

    public void setResend(Resend resend) {
        this.resend = resend;
    }

    public String getSystemFrom() {
        return systemFrom;
    }

    public void setSystemFrom(String systemFrom) {
        this.systemFrom = systemFrom;
    }

    public Google getGoogle() {
        return google;
    }

    public void setGoogle(Google google) {
        this.google = google;
    }

    public Microsoft getMicrosoft() {
        return microsoft;
    }

    public void setMicrosoft(Microsoft microsoft) {
        this.microsoft = microsoft;
    }

    public static class Resend {
        private String apiKey = "";
        private String webhookSecret = "";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getWebhookSecret() {
            return webhookSecret;
        }

        public void setWebhookSecret(String webhookSecret) {
            this.webhookSecret = webhookSecret;
        }
    }

    public static class Google {
        private String clientId = "";
        private String clientSecret = "";
        private String redirectUri = "http://localhost:8081/auth/email/oauth/google/callback";

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }
    }

    public static class Microsoft {
        private String clientId = "";
        private String clientSecret = "";
        private String redirectUri = "http://localhost:8081/auth/email/oauth/microsoft/callback";
        private String tenant = "common";

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }

        public String getTenant() {
            return tenant;
        }

        public void setTenant(String tenant) {
            this.tenant = tenant;
        }
    }
}
