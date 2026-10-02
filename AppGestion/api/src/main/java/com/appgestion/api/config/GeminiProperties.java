package com.appgestion.api.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.ai.gemini")
public class GeminiProperties {

    private boolean enabled;
    private String apiKey = "";
    @NotBlank
    private String model = "gemini-3.5-flash-lite";
    @NotBlank
    private String baseUrl = "https://generativelanguage.googleapis.com";
    @Min(1)
    private int timeoutSeconds = 30;
    @Min(1)
    private long maxImageBytes = 5L * 1024 * 1024;
    @Min(1)
    private long maxAudioBytes = 10L * 1024 * 1024;
    @Min(1)
    private int maxAudioSeconds = 120;
    @Min(1)
    private int requestsPerHour = 20;

    @AssertTrue(message = "app.ai.gemini.api-key (GEMINI_API_KEY) es obligatorio cuando app.ai.gemini.enabled=true")
    public boolean isApiKeyConfiguredWhenEnabled() {
        return !enabled || (apiKey != null && !apiKey.isBlank());
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public long getMaxImageBytes() { return maxImageBytes; }
    public void setMaxImageBytes(long maxImageBytes) { this.maxImageBytes = maxImageBytes; }
    public long getMaxAudioBytes() { return maxAudioBytes; }
    public void setMaxAudioBytes(long maxAudioBytes) { this.maxAudioBytes = maxAudioBytes; }
    public int getMaxAudioSeconds() { return maxAudioSeconds; }
    public void setMaxAudioSeconds(int maxAudioSeconds) { this.maxAudioSeconds = maxAudioSeconds; }
    public int getRequestsPerHour() { return requestsPerHour; }
    public void setRequestsPerHour(int requestsPerHour) { this.requestsPerHour = requestsPerHour; }
}
