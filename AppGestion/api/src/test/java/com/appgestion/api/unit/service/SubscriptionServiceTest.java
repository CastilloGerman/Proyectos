package com.appgestion.api.unit.service;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.SubscriptionStatus;
import com.appgestion.api.repository.StripeInvoiceLedgerRepository;
import com.appgestion.api.repository.StripeSubscriptionRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.service.SubscriptionService;
import com.stripe.model.Subscription;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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

    @Test
    void canWrite_activeSubscriptionWithPaymentActionRequired_deniesWrites() {
        SubscriptionService service = service();
        Usuario usuario = new Usuario();
        usuario.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        usuario.setSubscriptionRequiresPaymentAction(true);

        assertThat(service.canWrite(usuario)).isFalse();
    }

    @Test
    void canWrite_appTrialStillAllowsWritesEvenIfCheckoutNeedsAction() {
        SubscriptionService service = service();
        Usuario usuario = new Usuario();
        usuario.setSubscriptionStatus(SubscriptionStatus.TRIAL_ACTIVE);
        usuario.setSubscriptionRequiresPaymentAction(true);

        assertThat(service.canWrite(usuario)).isTrue();
    }

    @Test
    void syncFromStripeSubscription_doesNotClearPaymentActionFlagOnActiveSnapshot() {
        SubscriptionService service = service();
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("user@test.local");
        usuario.setSubscriptionRequiresPaymentAction(true);
        usuario.setSubscriptionStatus(SubscriptionStatus.INCOMPLETE);

        Subscription subscription = mock(Subscription.class);
        when(subscription.getId()).thenReturn("sub_123");
        when(subscription.getStatus()).thenReturn("active");
        when(subscription.getCancelAtPeriodEnd()).thenReturn(false);
        when(subscription.getItems()).thenReturn(null);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(stripeSubscriptionRepository.findByStripeSubscriptionId("sub_123")).thenReturn(Optional.empty());

        service.syncFromStripeSubscription(subscription, 1L, "cus_123");

        assertThat(usuario.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(usuario.isSubscriptionRequiresPaymentAction()).isTrue();
        verify(usuarioRepository).save(usuario);
        verify(stripeSubscriptionRepository).save(any());
    }

    private SubscriptionService service() {
        return new SubscriptionService(usuarioRepository, stripeSubscriptionRepository, stripeInvoiceLedgerRepository);
    }
}
