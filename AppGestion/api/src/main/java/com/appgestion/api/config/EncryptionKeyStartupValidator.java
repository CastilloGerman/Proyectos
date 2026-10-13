package com.appgestion.api.config;

import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * SEC-06: el secreto TOTP, la contraseña SMTP y los tokens OAuth se cifran en reposo con
 * {@code app.email.token-encryption-key} (APP_EMAIL_TOKEN_SECRET). Fuera de los perfiles {@code local}/{@code test}
 * la aplicación no arranca sin una clave válida: es preferible un despliegue fallido a que los usuarios con 2FA no
 * puedan iniciar sesión.
 */
@Component
public class EncryptionKeyStartupValidator {

    /** Mismo mínimo que {@code TokenEncryptionService}; se recomiendan 32+ caracteres aleatorios. */
    static final int MIN_KEY_LENGTH = 16;

    private final AppEmailProperties props;
    private final Environment environment;

    public EncryptionKeyStartupValidator(AppEmailProperties props, Environment environment) {
        this.props = props;
        this.environment = environment;
    }

    @PostConstruct
    void validate() {
        if (environment.acceptsProfiles(Profiles.of("local", "test"))) {
            return;
        }
        String key = props.getTokenEncryptionKey();
        if (!StringUtils.hasText(key) || key.length() < MIN_KEY_LENGTH) {
            throw new IllegalStateException(
                    "Configura APP_EMAIL_TOKEN_SECRET (app.email.token-encryption-key) con al menos "
                            + MIN_KEY_LENGTH + " caracteres aleatorios: se usa para cifrar secretos 2FA, "
                            + "credenciales SMTP y tokens OAuth.");
        }
    }
}
