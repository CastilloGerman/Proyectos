package com.appgestion.api.unit.validation;

import com.appgestion.api.validation.PasswordPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/** SEC-13: política única de contraseñas nuevas. */
class PasswordPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {"Password123!", "correct horse battery staple", "N0emi-Gestion-2026", "xK9#mP2$vL7@qR4!"})
    void aceptaContrasenasLargasYNoTriviales(String pwd) {
        assertThat(PasswordPolicy.violation(pwd)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"corta1!", "12345678901", "abcdefghijk"})
    void rechazaMenosDe12Caracteres(String pwd) {
        assertThat(PasswordPolicy.violation(pwd)).contains("entre 12 y 128");
    }

    @ParameterizedTest
    @ValueSource(strings = {"aaaaaaaaaaaa", "abababababab", "111111111111"})
    void rechazaContrasenasDeUnoODosCaracteres(String pwd) {
        assertThat(PasswordPolicy.violation(pwd)).contains("demasiado simple");
    }

    @ParameterizedTest
    @ValueSource(strings = {"password1234", "PASSWORD1234", "123456789012", "qwertyuiopas", "abcdefghijkl"})
    void rechazaContrasenasComunesYSecuencias(String pwd) {
        assertThat(PasswordPolicy.violation(pwd)).contains("demasiado común");
    }

    @Test
    void rechazaMasDe128Caracteres() {
        assertThat(PasswordPolicy.violation("aB3".repeat(50))).contains("entre 12 y 128");
    }

    @Test
    void rechazaNuloOVacio() {
        assertThat(PasswordPolicy.violation(null)).isNotNull();
        assertThat(PasswordPolicy.violation("   ")).isNotNull();
    }
}
