package com.appgestion.api.unit.service;

import com.appgestion.api.config.GeminiProperties;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.SubscriptionStatus;
import com.appgestion.api.dto.request.PresupuestoIaRequest;
import com.appgestion.api.exception.AiServiceException;
import com.appgestion.api.repository.ClienteRepository;
import com.appgestion.api.repository.MaterialRepository;
import com.appgestion.api.service.AiDailyRequestRateLimiter;
import com.appgestion.api.service.AiRequestRateLimiter;
import com.appgestion.api.service.GeminiClient;
import com.appgestion.api.service.GeminiGenerationResult;
import com.appgestion.api.service.PresupuestoIaService;
import com.appgestion.api.service.SubscriptionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PresupuestoIaQuotaBehaviorTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void providerFailureDoesNotConsumeHourlyOrDailyQuota() throws Exception {
        var fixture = fixture();
        when(fixture.gemini.generateWithMetadata(anyString(), anyString(), isNull(), isNull(), any(JsonNode.class),
                eq(JsonNode.class), any(Runnable.class)))
                .thenThrow(new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE, "timeout"))
                .thenReturn(validResult());

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, assertThrows(AiServiceException.class,
                () -> fixture.service.generarBorrador(new PresupuestoIaRequest("obra", null), fixture.user)).getStatus());
        assertDoesNotThrow(() -> fixture.service.generarBorrador(new PresupuestoIaRequest("obra", null), fixture.user));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, assertThrows(AiServiceException.class,
                () -> fixture.service.generarBorrador(new PresupuestoIaRequest("obra", null), fixture.user)).getStatus());
    }

    @Test
    void providerServerErrorDoesNotConsumeHourlyOrDailyQuota() throws Exception {
        var fixture = fixture();
        when(fixture.gemini.generateWithMetadata(anyString(), anyString(), isNull(), isNull(), any(JsonNode.class),
                eq(JsonNode.class), any(Runnable.class)))
                .thenThrow(new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE, "error del proveedor 5xx"))
                .thenReturn(validResult());

        assertThrows(AiServiceException.class,
                () -> fixture.service.generarBorrador(new PresupuestoIaRequest("obra", null), fixture.user));
        assertDoesNotThrow(() -> fixture.service.generarBorrador(new PresupuestoIaRequest("obra", null), fixture.user));
    }

    @Test
    void irrecoverableJsonDoesNotConsumeHourlyOrDailyQuota() throws Exception {
        var fixture = fixture();
        when(fixture.gemini.generateWithMetadata(anyString(), anyString(), isNull(), isNull(), any(JsonNode.class),
                eq(JsonNode.class), any(Runnable.class)))
                .thenReturn(new GeminiGenerationResult<>(mapper.createArrayNode(), 40, 2))
                .thenReturn(validResult());

        assertEquals(HttpStatus.BAD_GATEWAY, assertThrows(AiServiceException.class,
                () -> fixture.service.generarBorrador(new PresupuestoIaRequest("obra", null), fixture.user)).getStatus());
        assertDoesNotThrow(() -> fixture.service.generarBorrador(new PresupuestoIaRequest("obra", null), fixture.user));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, assertThrows(AiServiceException.class,
                () -> fixture.service.generarBorrador(new PresupuestoIaRequest("obra", null), fixture.user)).getStatus());
    }

    @Test
    void successfulInternalRetryConsumesOneQuotaUnitForTheWholeEndpointCall() throws Exception {
        var fixture = fixture();
        int[] providerAttempts = {0};
        when(fixture.gemini.generateWithMetadata(anyString(), anyString(), isNull(), isNull(), any(JsonNode.class),
                eq(JsonNode.class), any(Runnable.class))).thenAnswer(invocation -> {
            providerAttempts[0] += 2; // GeminiClient hace como mÃ¡ximo un reintento dentro de esta llamada.
            return validResult();
        });

        assertDoesNotThrow(() -> fixture.service.generarBorrador(new PresupuestoIaRequest("obra", null), fixture.user));
        assertEquals(2, providerAttempts[0]);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, assertThrows(AiServiceException.class,
                () -> fixture.service.generarBorrador(new PresupuestoIaRequest("obra", null), fixture.user)).getStatus());
        verify(fixture.gemini, times(1)).generateWithMetadata(anyString(), anyString(), isNull(), isNull(),
                any(JsonNode.class), eq(JsonNode.class), any(Runnable.class));
    }

    private Fixture fixture() {
        GeminiProperties properties = new GeminiProperties();
        properties.setEnabled(true);
        properties.setRequestsPerHour(1);
        properties.setPresupuestoRequestsPerDay(1);
        GeminiClient gemini = mock(GeminiClient.class);
        MaterialRepository materials = mock(MaterialRepository.class);
        ClienteRepository clients = mock(ClienteRepository.class);
        SubscriptionService subscription = mock(SubscriptionService.class);
        AiRequestRateLimiter hourly = new AiRequestRateLimiter(properties);
        AiDailyRequestRateLimiter daily = new AiDailyRequestRateLimiter(properties);
        Usuario user = new Usuario();
        user.setId(72L);
        user.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        when(subscription.canWrite(user)).thenReturn(true);
        when(materials.findTop5MasUsadosByUsuarioId(72L)).thenReturn(java.util.List.of());
        PresupuestoIaService service = new PresupuestoIaService(gemini, properties, mapper, materials, clients,
                subscription, hourly, daily, new com.appgestion.api.service.AiProviderAttemptRateLimiter(properties));
        return new Fixture(gemini, service, user);
    }

    private GeminiGenerationResult<JsonNode> validResult() throws Exception {
        return new GeminiGenerationResult<>(mapper.readTree("""
                {"transcripcion":"obra","partidas":[{"descripcion":"Pintar","cantidad":1,"unidad":"h",
                 "materialId":null,"confianza":"alta"}],"notas":null}
                """), 40, 5);
    }

    private record Fixture(GeminiClient gemini, PresupuestoIaService service, Usuario user) {}
}
