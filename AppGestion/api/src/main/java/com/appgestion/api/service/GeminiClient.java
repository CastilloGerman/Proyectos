package com.appgestion.api.service;

import com.appgestion.api.config.GeminiProperties;
import com.appgestion.api.exception.AiServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

/** Cliente HTTP común para solicitudes multimodales con respuesta JSON estructurada. */
@Component
public class GeminiClient {

    private static final String SERVICE_UNAVAILABLE = "Servicio de IA no disponible, inténtalo en unos minutos";
    private static final String AI_CONFIGURATION_ERROR =
            "La configuración del servicio de IA es incorrecta, contacta con soporte";
    private static final String NO_CANDIDATES =
            "El servicio de IA no devolvió ningún resultado para el documento";
    private static final String BLOCKED_RESPONSE =
            "El servicio de IA bloqueó la respuesta para este documento";
    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);
    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Autowired
    public GeminiClient(GeminiProperties properties, ObjectMapper objectMapper, RestClient.Builder builder) {
        this(properties, objectMapper, builder, true);
    }

    /** Constructor de prueba para conservar el request factory del servidor HTTP simulado. */
    public GeminiClient(
            GeminiProperties properties,
            ObjectMapper objectMapper,
            RestClient.Builder builder,
            boolean configureTimeouts
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        RestClient.Builder configuredBuilder = builder.baseUrl(properties.getBaseUrl());
        if (configureTimeouts) {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(properties.getTimeoutSeconds() * 1000);
            requestFactory.setReadTimeout(properties.getTimeoutSeconds() * 1000);
            configuredBuilder.requestFactory(requestFactory);
        }
        this.restClient = configuredBuilder.build();
    }

    public void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de IA está desactivado");
        }
    }

    public <T> T generate(
            String systemInstruction,
            String userText,
            byte[] attachment,
            String mimeType,
            JsonNode responseSchema,
            Class<T> responseType
    ) {
        return generate(systemInstruction, userText, attachment, mimeType, responseSchema, responseType, () -> { });
    }

    public <T> T generate(
            String systemInstruction,
            String userText,
            byte[] attachment,
            String mimeType,
            JsonNode responseSchema,
            Class<T> responseType,
            Runnable beforeProviderAttempt
    ) {
        return generateWithMetadata(systemInstruction, userText, attachment, mimeType, responseSchema, responseType,
                beforeProviderAttempt)
                .content();
    }

    public <T> GeminiGenerationResult<T> generateWithMetadata(
            String systemInstruction,
            String userText,
            byte[] attachment,
            String mimeType,
            JsonNode responseSchema,
            Class<T> responseType
    ) {
        return generateWithMetadata(systemInstruction, userText, attachment, mimeType, responseSchema, responseType,
                () -> { });
    }

    public <T> GeminiGenerationResult<T> generateWithMetadata(
            String systemInstruction,
            String userText,
            byte[] attachment,
            String mimeType,
            JsonNode responseSchema,
            Class<T> responseType,
            Runnable beforeProviderAttempt
    ) {
        requireEnabled();
        ObjectNode request = buildRequest(systemInstruction, userText, attachment, mimeType, responseSchema);
        for (int attempt = 0; attempt < 2; attempt++) {
            beforeProviderAttempt.run();
            try {
                String responseBody = restClient.post()
                        .uri("/v1beta/models/{model}:generateContent", properties.getModel())
                        .header("x-goog-api-key", properties.getApiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(objectMapper.writeValueAsString(request))
                        .retrieve()
                        .body(String.class);
                JsonNode response = responseBody == null ? null : objectMapper.readTree(responseBody);
                return new GeminiGenerationResult<>(parseResponse(response, responseType),
                        optionalInt(response == null ? null : response.path("usageMetadata").path("promptTokenCount")),
                        optionalInt(response == null ? null : response.path("usageMetadata").path("candidatesTokenCount")));
            } catch (RestClientResponseException ex) {
                int code = ex.getStatusCode().value();
                log.warn("Gemini devolvió HTTP {} (intento {}/2)", code, attempt + 1);
                if (code == 429) {
                    throw new AiServiceException(HttpStatus.TOO_MANY_REQUESTS,
                            "Has alcanzado el límite de cuota del servicio de IA. Inténtalo más tarde.", ex);
                }
                if (code == 403 || code == 404) {
                    throw new AiServiceException(HttpStatus.BAD_GATEWAY, AI_CONFIGURATION_ERROR, ex);
                }
                if (code == 400) {
                    throw new AiServiceException(HttpStatus.BAD_REQUEST,
                            providerBadRequestMessage(), ex);
                }
                if (code >= 500 && code <= 599) {
                    if (attempt == 0) {
                        pauseBeforeRetry();
                        continue;
                    }
                    throw new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE, ex);
                }
                throw new AiServiceException(HttpStatus.BAD_GATEWAY, SERVICE_UNAVAILABLE, ex);
            } catch (ResourceAccessException ex) {
                if (attempt == 0) {
                    pauseBeforeRetry();
                    continue;
                }
                throw new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE, ex);
            } catch (AiServiceException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new AiServiceException(HttpStatus.BAD_GATEWAY,
                        "La respuesta del servicio de IA no tiene un formato válido", ex);
            }
        }
        throw new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE);
    }

    private static Integer optionalInt(JsonNode value) {
        return value != null && value.canConvertToInt() ? value.intValue() : null;
    }

    private ObjectNode buildRequest(
            String systemInstruction,
            String userText,
            byte[] attachment,
            String mimeType,
            JsonNode responseSchema
    ) {
        ObjectNode request = objectMapper.createObjectNode();
        request.putObject("systemInstruction").putArray("parts").addObject().put("text", systemInstruction);
        ArrayNode parts = request.putArray("contents").addObject().put("role", "user").putArray("parts");
        if (StringUtils.hasText(userText)) {
            parts.addObject().put("text", userText);
        }
        if (attachment != null && attachment.length > 0) {
            parts.addObject().putObject("inlineData")
                    .put("mimeType", mimeType)
                    .put("data", Base64.getEncoder().encodeToString(attachment));
        }
        ObjectNode generationConfig = request.putObject("generationConfig");
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.set("responseSchema", responseSchema);
        generationConfig.put("temperature", 0.1);
        return request;
    }

    private <T> T parseResponse(JsonNode response, Class<T> responseType) {
        if (isBlocked(response)) {
            throw new AiServiceException(HttpStatus.BAD_GATEWAY, BLOCKED_RESPONSE);
        }
        JsonNode candidates = response == null ? null : response.path("candidates");
        if (candidates == null || !candidates.isArray() || candidates.isEmpty()) {
            throw new AiServiceException(HttpStatus.BAD_GATEWAY, NO_CANDIDATES);
        }
        JsonNode text = candidates.path(0).path("content").path("parts").path(0).path("text");
        if (text == null || !text.isTextual() || text.asText().isBlank()) {
            throw new AiServiceException(HttpStatus.BAD_GATEWAY,
                    "La respuesta del servicio de IA no tiene un formato válido");
        }
        try {
            return objectMapper.readValue(text.asText(), responseType);
        } catch (Exception ex) {
            throw new AiServiceException(HttpStatus.BAD_GATEWAY,
                    "La respuesta del servicio de IA no tiene un formato válido", ex);
        }
    }

    private boolean isBlocked(JsonNode response) {
        if (response == null) {
            return false;
        }
        JsonNode promptFeedback = response.path("promptFeedback");
        if (promptFeedback.path("blockReason").isTextual()) {
            return true;
        }
        JsonNode candidates = response.path("candidates");
        if (!candidates.isArray()) {
            return false;
        }
        for (JsonNode candidate : candidates) {
            String finishReason = candidate.path("finishReason").asText("");
            if (isBlockingFinishReason(finishReason)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlockingFinishReason(String finishReason) {
        return switch (finishReason) {
            case "SAFETY", "BLOCKLIST", "PROHIBITED_CONTENT", "SPII", "IMAGE_SAFETY" -> true;
            default -> false;
        };
    }

    private String providerBadRequestMessage() {
        return "La solicitud de IA no se pudo procesar. Comprueba el contenido e inténtalo de nuevo.";
    }

    private static void pauseBeforeRetry() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE, ex);
        }
    }
}
