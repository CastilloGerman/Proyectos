package com.appgestion.api.unit.service;

import com.appgestion.api.dto.SubscriptionBillingPeriod;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.service.StripeService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class StripeServiceTest {

    @Test
    void resolveCheckoutPriceId_honorsYearlyWhenBothPricesAreConfigured() {
        StripeService service = stripeServiceWithPrices("price_monthly", "price_yearly");

        String priceId = ReflectionTestUtils.invokeMethod(
                service, "resolveCheckoutPriceId", SubscriptionBillingPeriod.YEARLY);

        assertThat(priceId).isEqualTo("price_yearly");
    }

    @Test
    void resolveCheckoutPriceId_defaultsToMonthlyWhenNoPeriodIsRequested() {
        StripeService service = stripeServiceWithPrices("price_monthly", "price_yearly");

        String priceId = ReflectionTestUtils.invokeMethod(service, "resolveCheckoutPriceId", (Object) null);

        assertThat(priceId).isEqualTo("price_monthly");
    }

    private static StripeService stripeServiceWithPrices(String monthly, String yearly) {
        StripeService service = new StripeService(mock(UsuarioRepository.class));
        ReflectionTestUtils.setField(service, "priceIdMonthly", monthly);
        ReflectionTestUtils.setField(service, "priceIdYearly", yearly);
        return service;
    }
}
