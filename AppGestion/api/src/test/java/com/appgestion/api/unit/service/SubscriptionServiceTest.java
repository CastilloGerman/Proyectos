package com.appgestion.api.unit.service;

import com.appgestion.api.domain.entity.StripeInvoiceLedger;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.SubscriptionStatus;
import com.appgestion.api.repository.StripeInvoiceLedgerRepository;
import com.appgestion.api.repository.StripeSubscriptionRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.service.SubscriptionService;
import com.appgestion.api.service.stripe.StripeSubscriptionFetcher;
import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
    private StripeSubscriptionFetcher subscriptionFetcher;

    @Test
    void recordInvoicePaid_noActivaUsuarioCanceladoSiStripeNoPuedeCargarSuscripcion() throws Exception {
        SubscriptionService service = service();
        Usuario usuario = usuario(10L, "cus_old", null, SubscriptionStatus.CANCELED);
        Invoice invoice = invoice("in_old", "sub_old", "cus_old");

        when(usuarioRepository.findByStripeSubscriptionId("sub_old")).thenReturn(Optional.empty());
        when(usuarioRepository.findByStripeCustomerId("cus_old")).thenReturn(Optional.of(usuario));
        when(stripeInvoiceLedgerRepository.findByStripeInvoiceId("in_old")).thenReturn(Optional.empty());
        when(subscriptionFetcher.fetch("sub_old")).thenThrow(mock(StripeException.class));

        service.recordInvoicePaid(invoice);

        assertThat(usuario.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.CANCELED);
        assertThat(usuario.getStripeSubscriptionId()).isNull();
        verify(stripeInvoiceLedgerRepository).save(any(StripeInvoiceLedger.class));
        verify(usuarioRepository, never()).save(usuario);
    }

    @Test
    void recordInvoicePaid_noSobrescribeSuscripcionActualConFacturaDeOtraSuscripcion() throws Exception {
        SubscriptionService service = service();
        Usuario usuario = usuario(11L, "cus_shared", "sub_current", SubscriptionStatus.ACTIVE);
        Subscription staleSubscription = mock(Subscription.class);
        Invoice invoice = invoice("in_stale", "sub_old", "cus_shared");

        when(usuarioRepository.findByStripeSubscriptionId("sub_old")).thenReturn(Optional.empty());
        when(usuarioRepository.findByStripeCustomerId("cus_shared")).thenReturn(Optional.of(usuario));
        when(stripeInvoiceLedgerRepository.findByStripeInvoiceId("in_stale")).thenReturn(Optional.empty());
        when(subscriptionFetcher.fetch("sub_old")).thenReturn(staleSubscription);

        service.recordInvoicePaid(invoice);

        assertThat(usuario.getStripeSubscriptionId()).isEqualTo("sub_current");
        assertThat(usuario.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        verify(stripeInvoiceLedgerRepository).save(any(StripeInvoiceLedger.class));
        verify(usuarioRepository, never()).save(usuario);
    }

    private SubscriptionService service() {
        return new SubscriptionService(
                usuarioRepository,
                stripeSubscriptionRepository,
                stripeInvoiceLedgerRepository,
                subscriptionFetcher);
    }

    private static Usuario usuario(
            Long id,
            String stripeCustomerId,
            String stripeSubscriptionId,
            SubscriptionStatus subscriptionStatus) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setStripeCustomerId(stripeCustomerId);
        usuario.setStripeSubscriptionId(stripeSubscriptionId);
        usuario.setSubscriptionStatus(subscriptionStatus);
        return usuario;
    }

    private static Invoice invoice(String invoiceId, String subscriptionId, String customerId) {
        Invoice invoice = mock(Invoice.class);
        when(invoice.getId()).thenReturn(invoiceId);
        when(invoice.getSubscription()).thenReturn(subscriptionId);
        when(invoice.getCustomer()).thenReturn(customerId);
        when(invoice.getAmountPaid()).thenReturn(999L);
        when(invoice.getCurrency()).thenReturn("eur");
        when(invoice.getStatus()).thenReturn("paid");
        when(invoice.getCreated()).thenReturn(1_762_000_000L);
        return invoice;
    }
}
