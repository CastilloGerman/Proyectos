package com.appgestion.api.unit.config;

import com.appgestion.api.config.GeminiConfiguration;
import com.appgestion.api.config.GeminiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiPropertiesContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(GeminiConfiguration.class)
            .withBean("configurationPropertiesValidator", LocalValidatorFactoryBean.class,
                    LocalValidatorFactoryBean::new);

    @Test
    void failsClearlyWhenEnabledWithoutApiKey() {
        contextRunner.withPropertyValues("app.ai.gemini.enabled=true").run(context -> {
            assertNotNull(context.getStartupFailure());
            String messages = exceptionMessages(context.getStartupFailure());
            assertTrue(messages.contains("GEMINI_API_KEY"), messages);
            assertTrue(messages.contains("app.ai.gemini.enabled=true"), messages);
        });
    }

    @Test
    void startsWhenDisabledWithoutApiKey() {
        contextRunner.withPropertyValues("app.ai.gemini.enabled=false").run(context -> {
            assertNull(context.getStartupFailure());
            assertFalse(context.getBean(GeminiProperties.class).isEnabled());
            assertEquals(10L * 1024 * 1024, context.getBean(GeminiProperties.class).getMaxAudioBytes());
        });
    }

    private static String exceptionMessages(Throwable throwable) {
        StringBuilder messages = new StringBuilder();
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            messages.append(current.getMessage()).append('\n');
        }
        return messages.toString();
    }
}
