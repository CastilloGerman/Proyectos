package com.appgestion.api.unit.service;

import com.appgestion.api.config.AppEmailProperties;
import com.appgestion.api.repository.EmailWebhookEventRepository;
import com.appgestion.api.service.ResendWebhookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** SEC-04: el webhook de Resend es fail-closed. */
class ResendWebhookServiceSecurityTest {

    private EmailWebhookEventRepository repository;
    private AppEmailProperties props;
    private ResendWebhookService service;

    @BeforeEach
    void setUp() {
        repository = mock(EmailWebhookEventRepository.class);
        props = new AppEmailProperties();
        service = new ResendWebhookService(repository, new ObjectMapper(), props);
    }

    @Test
    void sinSecretoConfigurado_rechazaConServiceUnavailable_yNoPersiste() {
        props.getResend().setWebhookSecret("");
        assertThatThrownBy(() -> service.registrarEventoResend("{\"type\":\"email.bounced\"}", null, null, null))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        verify(repository, never()).save(any());
    }

    @Test
    void conSecreto_sinFirma_rechazaConBadRequest() {
        props.getResend().setWebhookSecret("whsec_" + Base64.getEncoder().encodeToString("clave".getBytes(StandardCharsets.UTF_8)));
        assertThatThrownBy(() -> service.registrarEventoResend("{}", null, null, null))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(repository, never()).save(any());
    }

    @Test
    void conSecreto_firmaValida_persiste() throws Exception {
        byte[] key = "clave-de-prueba".getBytes(StandardCharsets.UTF_8);
        props.getResend().setWebhookSecret("whsec_" + Base64.getEncoder().encodeToString(key));
        String payload = "{\"type\":\"email.delivered\",\"data\":{\"email_id\":\"abc\"}}";
        String id = "msg_1";
        String ts = String.valueOf(Instant.now().getEpochSecond());
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        String sig = Base64.getEncoder().encodeToString(
                mac.doFinal((id + "." + ts + "." + payload).getBytes(StandardCharsets.UTF_8)));

        service.registrarEventoResend(payload, id, ts, "v1," + sig);

        verify(repository).save(any());
    }
}
