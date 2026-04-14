package com.appgestion.api.integration.auth;

import com.appgestion.api.domain.entity.Organization;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.SubscriptionStatus;
import com.appgestion.api.repository.OrganizationRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.security.TotpService;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Usuarios mínimos para tests de login / JWT / TOTP (perfil {@code test}, H2).
 */
public final class AuthLoginSeedSupport {

    public static final String LOGIN_USER_EMAIL = "auth-block2-login@test.local";
    public static final String LOGIN_USER_PASSWORD = "TestPassword123!";

    public static final String TOTP_USER_EMAIL = "auth-block2-totp@test.local";
    public static final String TOTP_USER_PASSWORD = "TotpPassword123!";

    private AuthLoginSeedSupport() {
    }

    public static Usuario seedPasswordLoginUser(
            OrganizationRepository organizationRepository,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder
    ) {
        return seedUser(
                organizationRepository,
                usuarioRepository,
                passwordEncoder,
                LOGIN_USER_EMAIL,
                LOGIN_USER_PASSWORD,
                false,
                null
        );
    }

    /**
     * Usuario con 2FA activo y secreto TOTP persistido (Base32).
     */
    public static Usuario seedTotpUser(
            OrganizationRepository organizationRepository,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            TotpService totpService
    ) {
        var key = totpService.generateKey();
        String secretBase32 = key.getKey();
        return seedUser(
                organizationRepository,
                usuarioRepository,
                passwordEncoder,
                TOTP_USER_EMAIL,
                TOTP_USER_PASSWORD,
                true,
                secretBase32
        );
    }

    private static Usuario seedUser(
            OrganizationRepository organizationRepository,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            String email,
            String plainPassword,
            boolean totpEnabled,
            String totpSecretBase32
    ) {
        Organization org = new Organization();
        org.setName("Org auth block2");
        org = organizationRepository.save(org);

        Usuario u = new Usuario();
        u.setNombre("Usuario block2");
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(plainPassword));
        u.setRol("USER");
        u.setActivo(true);
        u.setOrganization(org);
        u.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        if (totpEnabled && totpSecretBase32 != null) {
            u.setTotpEnabled(true);
            u.setTotpSecret(totpSecretBase32);
        } else {
            u.setTotpEnabled(false);
        }
        return usuarioRepository.save(u);
    }
}
