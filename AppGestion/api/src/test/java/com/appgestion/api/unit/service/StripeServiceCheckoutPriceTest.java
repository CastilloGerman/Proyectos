package com.appgestion.api.unit.service;

import com.appgestion.api.dto.SubscriptionBillingPeriod;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.service.StripeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class StripeServiceCheckoutPriceTest {

    private StripeService stripeService;

    @BeforeEach
    void setUp() {
        stripeService = new StripeService(mock(UsuarioRepository.class));
        ReflectionTestUtils.setField(stripeService, "priceIdMonthly", "price_monthly");
        ReflectionTestUtils.setField(stripeService, "priceIdYearly", "price_yearly");
    }

    @Test
    void resolveCheckoutPriceId_usaPrecioAnualCuandoSeSolicitaYearly() {
        String priceId = ReflectionTestUtils.invokeMethod(
                stripeService, "resolveCheckoutPriceId", SubscriptionBillingPeriod.YEARLY);

        assertThat(priceId).isEqualTo("price_yearly");
    }

    @Test
    void resolveCheckoutPriceId_usaPrecioMensualPorDefecto() {
        String priceId = ReflectionTestUtils.invokeMethod(stripeService, "resolveCheckoutPriceId", (Object) null);

        assertThat(priceId).isEqualTo("price_monthly");
    }

    @Test
    void resolveCheckoutPriceId_fallaSiSeSolicitaYearlySinPrecioAnual() {
        ReflectionTestUtils.setField(stripeService, "priceIdYearly", " ");

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                stripeService, "resolveCheckoutPriceId", SubscriptionBillingPeriod.YEARLY))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("STRIPE_PRICE_YEARLY");
    }
}
