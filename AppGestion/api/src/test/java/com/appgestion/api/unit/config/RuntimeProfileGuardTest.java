package com.appgestion.api.unit.config;

import com.appgestion.api.config.RuntimeProfileGuard;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** SEC-20: el perfil local no puede arrancar en la nube y la nube exige el perfil prod. */
class RuntimeProfileGuardTest {

    private static void validate(MockEnvironment env) {
        ReflectionTestUtils.invokeMethod(new RuntimeProfileGuard(env), "validate");
    }

    @Test
    void perfilLocalEnRailway_noArranca() {
        MockEnvironment env = new MockEnvironment().withProperty("RAILWAY_ENVIRONMENT", "production");
        env.setActiveProfiles("local");
        assertThatThrownBy(() -> validate(env)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("local");
    }

    @Test
    void nubeSinPerfilProd_noArranca() {
        MockEnvironment env = new MockEnvironment().withProperty("VERCEL", "1");
        assertThatThrownBy(() -> validate(env)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("prod");
    }

    @Test
    void nubeConPerfilProd_arranca() {
        MockEnvironment env = new MockEnvironment().withProperty("RAILWAY_ENVIRONMENT", "production");
        env.setActiveProfiles("prod", "serverless");
        assertThatCode(() -> validate(env)).doesNotThrowAnyException();
    }

    @Test
    void desarrolloLocalSinMarcadoresCloud_noSeRestringe() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("local");
        assertThatCode(() -> validate(env)).doesNotThrowAnyException();
        assertThatCode(() -> validate(new MockEnvironment())).doesNotThrowAnyException();
    }

    @Test
    void sec40_detectaBdRemotaSinTlsEstricto() {
        assertThat(RuntimeProfileGuard.isRemotePostgresWithoutStrictTls("jdbc:postgresql://db.neon.tech/app")).isTrue();
        assertThat(RuntimeProfileGuard.isRemotePostgresWithoutStrictTls("jdbc:postgresql://db.neon.tech/app?sslmode=prefer")).isTrue();
        assertThat(RuntimeProfileGuard.isRemotePostgresWithoutStrictTls("jdbc:postgresql://db.neon.tech/app?sslmode=require")).isFalse();
        assertThat(RuntimeProfileGuard.isRemotePostgresWithoutStrictTls("jdbc:postgresql://db.neon.tech/app?sslmode=verify-full")).isFalse();
        assertThat(RuntimeProfileGuard.isRemotePostgresWithoutStrictTls("jdbc:postgresql://localhost:5432/app")).isFalse();
        assertThat(RuntimeProfileGuard.isRemotePostgresWithoutStrictTls("jdbc:h2:mem:test")).isFalse();
        assertThat(RuntimeProfileGuard.isRemotePostgresWithoutStrictTls(null)).isFalse();
    }

    @Test
    void marcadorVacio_noCuentaComoNube() {
        MockEnvironment env = new MockEnvironment().withProperty("RAILWAY_ENVIRONMENT", " ");
        env.setActiveProfiles("local");
        assertThatCode(() -> validate(env)).doesNotThrowAnyException();
    }
}
