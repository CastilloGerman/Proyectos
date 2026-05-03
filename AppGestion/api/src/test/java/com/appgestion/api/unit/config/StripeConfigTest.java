package com.appgestion.api.unit.config;

import com.appgestion.api.config.StripeConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StripeConfigTest {

    @Test
    void init_prodAllowsMonthlyOnlyStripeConfiguration() {
        StripeConfig config = prodConfig();
        setValidRequiredStripeSettings(config);

        assertThatCode(config::init).doesNotThrowAnyException();
    }

    @Test
    void init_prodStillRequiresMonthlyPrice() {
        StripeConfig config = prodConfig();
        setValidRequiredStripeSettings(config);
        ReflectionTestUtils.setField(config, "priceIdMonthly", "");

        assertThatThrownBy(config::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("STRIPE_PRICE_MONTHLY");
    }

    private static StripeConfig prodConfig() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        return new StripeConfig(environment);
    }

    private static void setValidRequiredStripeSettings(StripeConfig config) {
        ReflectionTestUtils.setField(config, "stripeSecretKey", "sk_live_valid");
        ReflectionTestUtils.setField(config, "stripeWebhookSecret", "whsec_valid");
        ReflectionTestUtils.setField(config, "priceIdMonthly", "price_monthly");
        ReflectionTestUtils.setField(config, "successUrl", "https://app.example.com/subscription/success");
        ReflectionTestUtils.setField(config, "cancelUrl", "https://app.example.com/subscription/cancel");
    }
}
