package com.appgestion.api.unit.service;

import com.appgestion.api.config.GeminiProperties;
import com.appgestion.api.exception.AiServiceException;
import com.appgestion.api.service.GeminiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiClientTest {

    private static final String URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    private final ObjectMapper mapper = new ObjectMapper();
    private GeminiProperties properties;
    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private GeminiClient client;
    private final JsonNode schema = mapper.createObjectNode().put("type", "OBJECT");

    @BeforeEach
    void setUp() {
        properties = new GeminiProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-key");
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GeminiClient(properties, mapper, builder, false);
    }

    @Test
    void sendsStructuredRequestAndParsesResponse() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andExpect(jsonPath("$.generationConfig.responseMimeType").value("application/json"))
                .andExpect(jsonPath("$.generationConfig.temperature").value(0.1))
                .andExpect(jsonPath("$.generationConfig.responseSchema.type").value("OBJECT"))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"{\\\"answer\\\":\\\"ok\\\"}"}]}}]}
                        """, org.springframework.http.MediaType.APPLICATION_JSON));

        Result result = client.generate("instrucción", "texto", new byte[]{1, 2}, "image/jpeg", schema, Result.class);

        assertEquals("ok", result.answer());
        server.verify();
    }

    @Test
    void mapsQuotaResponseTo429() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        AiServiceException ex = assertThrows(AiServiceException.class,
                () -> client.generate("sistema", "usuario", null, null, schema, Result.class));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
        server.verify();
    }

    @Test
    void mapsProviderBadRequestTo400() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST));
        AiServiceException ex = assertThrows(AiServiceException.class,
                () -> client.generate("sistema", "usuario", null, null, schema, Result.class));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        server.verify();
    }

    @Test
    void retriesServerErrorOnce() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        server.expect(requestTo(URL)).andRespond(withSuccess("""
                {"candidates":[{"content":{"parts":[{"text":"{\\\"answer\\\":\\\"ok\\\"}"}]}}]}
                """, org.springframework.http.MediaType.APPLICATION_JSON));

        Result result = client.generate("sistema", "usuario", null, null, schema, Result.class);

        assertEquals("ok", result.answer());
        server.verify();
    }

    @Test
    void mapsTimeoutToServiceUnavailable() {
        server.expect(requestTo(URL)).andRespond(withException(new java.io.IOException("timeout")));
        AiServiceException ex = assertThrows(AiServiceException.class,
                () -> client.generate("sistema", "usuario", null, null, schema, Result.class));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
        server.verify();
    }

    @Test
    void rejectsMalformedStructuredJson() {
        server.expect(requestTo(URL)).andRespond(withSuccess("""
                {"candidates":[{"content":{"parts":[{"text":"no es json"}]}}]}
                """, org.springframework.http.MediaType.APPLICATION_JSON));
        AiServiceException ex = assertThrows(AiServiceException.class,
                () -> client.generate("sistema", "usuario", null, null, schema, Result.class));
        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatus());
        server.verify();
    }

    private record Result(String answer) {}
}
