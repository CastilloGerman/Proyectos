package com.appgestion.api.unit.service;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.SubscriptionStatus;
import com.appgestion.api.repository.StripeInvoiceLedgerRepository;
import com.appgestion.api.repository.StripeSubscriptionRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.service.SubscriptionService;
import com.stripe.model.Subscription;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubscriptionServiceTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final StripeSubscriptionRepository stripeSubscriptionRepository = mock(StripeSubscriptionRepository.class);
    private final StripeInvoiceLedgerRepository stripeInvoiceLedgerRepository = mock(StripeInvoiceLedgerRepository.class);
    private final SubscriptionService subscriptionService = new SubscriptionService(
            usuarioRepository,
            stripeSubscriptionRepository,
            stripeInvoiceLedgerRepository);

    @Test
    void syncFromStripeSubscription_preservaTrialActivoAnteEstadoIncomplete() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setEmail("trial@test.local");
        usuario.setSubscriptionStatus(SubscriptionStatus.TRIAL_ACTIVE);
        usuario.setTrialEndDate(LocalDate.now().plusDays(10));

        Subscription subscription = mock(Subscription.class);
        when(subscription.getId()).thenReturn("sub_incomplete");
        when(subscription.getStatus()).thenReturn("incomplete");
        when(subscription.getCustomer()).thenReturn("cus_123");
        when(subscription.getItems()).thenReturn(null);
        when(subscription.getCancelAtPeriodEnd()).thenReturn(false);
        when(stripeSubscriptionRepository.findByStripeSubscriptionId("sub_incomplete")).thenReturn(Optional.empty());
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));

        subscriptionService.syncFromStripeSubscription(subscription, 7L, null);

        assertThat(usuario.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.TRIAL_ACTIVE);
        assertThat(usuario.isSubscriptionRequiresPaymentAction()).isTrue();
        assertThat(usuario.getStripeSubscriptionId()).isEqualTo("sub_incomplete");
        verify(usuarioRepository).save(usuario);
        verify(stripeSubscriptionRepository).save(any(com.appgestion.api.domain.entity.StripeSubscription.class));
    }
}
