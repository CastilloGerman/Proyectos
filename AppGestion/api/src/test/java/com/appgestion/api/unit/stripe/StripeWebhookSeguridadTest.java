package com.appgestion.api.unit.stripe;

import com.appgestion.api.controller.StripeWebhookController;
import com.appgestion.api.service.StripeWebhookService;
import com.appgestion.api.service.stripe.StripeWebhookProcessingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StripeWebhookSeguridadTest {

    @Mock
    private StripeWebhookService stripeWebhookService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new StripeWebhookController(stripeWebhookService)).build();
    }

    @Test
    void webhookConFirmaValida_procesaYDevuelve200() throws Exception {
        when(stripeWebhookService.processWebhook(any(), any())).thenReturn(StripeWebhookProcessingResult.ok());

        mockMvc.perform(post("/webhook/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("Stripe-Signature", "t=123,v1=abc"))
                .andExpect(status().isOk());
    }

    @Test
    void webhookConFirmaInvalida_devuelve400() throws Exception {
        when(stripeWebhookService.processWebhook(any(), any())).thenReturn(StripeWebhookProcessingResult.badSignature());

        mockMvc.perform(post("/webhook/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("Stripe-Signature", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid signature"));
    }

    @Test
    void webhookConEventoNoProcesable_devuelve500ParaReintento() throws Exception {
        when(stripeWebhookService.processWebhook(any(), any()))
                .thenReturn(StripeWebhookProcessingResult.processingFailed());

        mockMvc.perform(post("/webhook/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("Stripe-Signature", "t=123,v1=abc"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Webhook processing failed"));
    }

    @Test
    void webhookSinHeaderFirma_devuelve400() throws Exception {
        mockMvc.perform(post("/webhook/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
