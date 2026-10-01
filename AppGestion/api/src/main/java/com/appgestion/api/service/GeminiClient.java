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

import java.util.Base64;

/** Cliente HTTP común para solicitudes multimodales con respuesta JSON estructurada. */
@Component
public class GeminiClient {

    private static final String SERVICE_UNAVAILABLE = "Servicio de IA no disponible, inténtalo en unos minutos";
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
        requireEnabled();
        ObjectNode request = buildRequest(systemInstruction, userText, attachment, mimeType, responseSchema);
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                String responseBody = restClient.post()
                        .uri("/v1beta/models/{model}:generateContent", properties.getModel())
                        .header("x-goog-api-key", properties.getApiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(objectMapper.writeValueAsString(request))
                        .retrieve()
                        .body(String.class);
                JsonNode response = responseBody == null ? null : objectMapper.readTree(responseBody);
                return parseResponse(response, responseType);
            } catch (RestClientResponseException ex) {
                int code = ex.getStatusCode().value();
                if (code == 429) {
                    throw new AiServiceException(HttpStatus.TOO_MANY_REQUESTS,
                            "Has alcanzado el límite diario de pruebas. Inténtalo más tarde.", ex);
                }
                if (code == 400) {
                    throw new AiServiceException(HttpStatus.BAD_REQUEST,
                            providerBadRequestMessage(ex), ex);
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
        JsonNode text = response == null ? null
                : response.path("candidates").path(0).path("content").path("parts").path(0).path("text");
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

    private String providerBadRequestMessage(RestClientResponseException ex) {
        try {
            JsonNode message = objectMapper.readTree(ex.getResponseBodyAsString())
                    .path("error").path("message");
            if (message.isTextual() && !message.asText().isBlank()) {
                String detail = message.asText().replaceAll("[\\r\\n\\t]+", " ").trim();
                if (detail.length() > 400) {
                    detail = detail.substring(0, 400) + "…";
                }
                return "Gemini rechazó la solicitud: " + detail;
            }
        } catch (Exception ignored) {
            // El proveedor no siempre devuelve un error JSON con el campo esperado.
        }
        return "Gemini no pudo procesar el archivo enviado. Comprueba el formato e inténtalo de nuevo.";
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
