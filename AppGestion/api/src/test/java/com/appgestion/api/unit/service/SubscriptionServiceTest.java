package com.appgestion.api.unit.service;

import com.appgestion.api.domain.entity.StripeInvoiceLedger;
import com.appgestion.api.domain.entity.StripeSubscription;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.SubscriptionStatus;
import com.appgestion.api.repository.StripeInvoiceLedgerRepository;
import com.appgestion.api.repository.StripeSubscriptionRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.service.SubscriptionService;
import com.appgestion.api.service.stripe.StripeSubscriptionFetcher;
import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private StripeSubscriptionRepository stripeSubscriptionRepository;

    @Mock
    private StripeInvoiceLedgerRepository stripeInvoiceLedgerRepository;

    @Mock
    private StripeSubscriptionFetcher stripeSubscriptionFetcher;

    @Test
    void recordInvoicePaid_ignoraSincronizacionSiLaFacturaEsDeOtraSuscripcionDelCliente() throws Exception {
        SubscriptionService service = service();
        Usuario usuario = usuario("user@example.com", "sub_current", SubscriptionStatus.ACTIVE);
        usuario.setSubscriptionRequiresPaymentAction(true);
        Invoice invoice = invoice("in_stale", "sub_old", "cus_123");

        when(usuarioRepository.findByStripeSubscriptionId("sub_old")).thenReturn(Optional.empty());
        when(usuarioRepository.findByStripeCustomerId("cus_123")).thenReturn(Optional.of(usuario));
        when(stripeInvoiceLedgerRepository.findByStripeInvoiceId("in_stale")).thenReturn(Optional.empty());

        service.recordInvoicePaid(invoice);

        assertThat(usuario.getStripeSubscriptionId()).isEqualTo("sub_current");
        assertThat(usuario.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(usuario.isSubscriptionRequiresPaymentAction()).isTrue();
        verify(stripeInvoiceLedgerRepository).save(any(StripeInvoiceLedger.class));
        verify(stripeSubscriptionFetcher, never()).fetch(any());
        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(stripeSubscriptionRepository, never()).save(any(StripeSubscription.class));
    }

    @Test
    void recordInvoicePaid_noPromueveAActiveSiStripeNoPermiteLeerLaSuscripcion() throws Exception {
        SubscriptionService service = service();
        Usuario usuario = usuario("user@example.com", "sub_current", SubscriptionStatus.PAST_DUE);
        usuario.setSubscriptionRequiresPaymentAction(true);
        Invoice invoice = invoice("in_current", "sub_current", null);
        StripeException stripeException = org.mockito.Mockito.mock(StripeException.class);

        when(stripeException.getMessage()).thenReturn("stripe unavailable");
        when(usuarioRepository.findByStripeSubscriptionId("sub_current")).thenReturn(Optional.of(usuario));
        when(stripeInvoiceLedgerRepository.findByStripeInvoiceId("in_current")).thenReturn(Optional.empty());
        when(stripeSubscriptionFetcher.fetch("sub_current")).thenThrow(stripeException);

        service.recordInvoicePaid(invoice);

        assertThat(usuario.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
        assertThat(usuario.isSubscriptionRequiresPaymentAction()).isFalse();
        verify(usuarioRepository).save(usuario);
        verify(stripeSubscriptionFetcher).fetch("sub_current");
        verify(stripeSubscriptionRepository, never()).save(any(StripeSubscription.class));
    }

    private SubscriptionService service() {
        return new SubscriptionService(
                usuarioRepository,
                stripeSubscriptionRepository,
                stripeInvoiceLedgerRepository,
                stripeSubscriptionFetcher);
    }

    private static Usuario usuario(String email, String subscriptionId, SubscriptionStatus status) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setStripeCustomerId("cus_123");
        usuario.setStripeSubscriptionId(subscriptionId);
        usuario.setSubscriptionStatus(status);
        return usuario;
    }

    private static Invoice invoice(String invoiceId, String subscriptionId, String customerId) {
        Invoice invoice = org.mockito.Mockito.mock(Invoice.class);
        when(invoice.getId()).thenReturn(invoiceId);
        when(invoice.getSubscription()).thenReturn(subscriptionId);
        if (customerId != null) {
            when(invoice.getCustomer()).thenReturn(customerId);
        }
        when(invoice.getAmountPaid()).thenReturn(999L);
        when(invoice.getCurrency()).thenReturn("eur");
        when(invoice.getStatus()).thenReturn("paid");
        when(invoice.getCreated()).thenReturn(1_700_000_000L);
        return invoice;
    }
}
