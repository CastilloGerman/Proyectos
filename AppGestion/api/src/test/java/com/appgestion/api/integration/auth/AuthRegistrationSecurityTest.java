package com.appgestion.api.integration.auth;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.util.OpaqueTokens;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** SEC-01 (rol no asignable desde el cliente) y SEC-05 (verificación de email / pre-hijacking). */
@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Transactional
class AuthRegistrationSecurityTest {

    private static final String EMAIL = "registro-seguro@test.local";

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }

    private void register(String email, String password, String extraJson) throws Exception {
        String body = """
                {"nombre":"Ana","email":"%s","password":"%s"%s}
                """.formatted(email, password, extraJson);
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        String body = """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
        return mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void registro_ignoraRolEnviadoPorElCliente_yNoDevuelveJwt() throws Exception {
        String body = """
                {"nombre":"Mallory","email":"%s","password":"Password123!","rol":"ADMIN"}
                """.formatted(EMAIL);

        String response = mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailVerificationRequired").value(true))
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        assertThat(json.has("token")).isFalse();
        Usuario u = usuarioRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(u.getRol()).isEqualTo("USER");
        assertThat(u.isEmailVerified()).isFalse();
        // El token de verificación se guarda como hash SHA-256, nunca en claro.
        assertThat(u.getEmailVerificationTokenHash()).matches("[0-9a-f]{64}");
        assertThat(u.getEmailVerificationExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void login_sinEmailVerificado_devuelve400EmailNoVerificado() throws Exception {
        register(EMAIL, "Password123!", "");

        login(EMAIL, "Password123!")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("EMAIL_NO_VERIFICADO"));
    }

    @Test
    void login_sinEmailVerificado_conPasswordIncorrecta_noRevelaElEstadoDeLaCuenta() throws Exception {
        register(EMAIL, "Password123!", "");

        login(EMAIL, "otra-password")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
    }

    @Test
    void verificarEmail_conTokenValido_permiteIniciarSesion() throws Exception {
        register(EMAIL, "Password123!", "");
        Usuario u = usuarioRepository.findByEmail(EMAIL).orElseThrow();
        u.setEmailVerificationTokenHash(OpaqueTokens.sha256Hex("token-conocido"));
        u.setEmailVerificationExpiresAt(LocalDateTime.now().plusHours(1));
        usuarioRepository.saveAndFlush(u);

        mockMvc.perform(post("/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"token-conocido\"}"))
                .andExpect(status().isNoContent());

        Usuario verificado = usuarioRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(verificado.isEmailVerified()).isTrue();
        assertThat(verificado.getEmailVerificationTokenHash()).isNull();
        login(EMAIL, "Password123!").andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());

        // El enlace es de un solo uso.
        mockMvc.perform(post("/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"token-conocido\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verificarEmail_tokenCaducadoOInexistente_devuelve400() throws Exception {
        register(EMAIL, "Password123!", "");
        Usuario u = usuarioRepository.findByEmail(EMAIL).orElseThrow();
        u.setEmailVerificationTokenHash(OpaqueTokens.sha256Hex("caducado"));
        u.setEmailVerificationExpiresAt(LocalDateTime.now().minusMinutes(1));
        usuarioRepository.saveAndFlush(u);

        mockMvc.perform(post("/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"caducado\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"no-existe\"}"))
                .andExpect(status().isBadRequest());
        assertThat(usuarioRepository.findByEmail(EMAIL).orElseThrow().isEmailVerified()).isFalse();
    }

    @Test
    void preHijacking_quienRegistraDespuesDelTitular_reclamaLaCuentaPendiente() throws Exception {
        // 1) El atacante registra el email de la víctima con su contraseña.
        register(EMAIL, "PasswordAtacante1!", "");
        Long idCuenta = usuarioRepository.findByEmail(EMAIL).orElseThrow().getId();
        String tokenAtacante = usuarioRepository.findByEmail(EMAIL).orElseThrow().getEmailVerificationTokenHash();

        // 2) El titular real se registra con el mismo email: no hay "ya existe" y la contraseña del atacante deja de valer.
        register(EMAIL, "PasswordTitular1!", "");

        Usuario u = usuarioRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(u.getId()).isEqualTo(idCuenta);
        assertThat(passwordEncoder.matches("PasswordAtacante1!", u.getPasswordHash())).isFalse();
        assertThat(passwordEncoder.matches("PasswordTitular1!", u.getPasswordHash())).isTrue();
        assertThat(u.getEmailVerificationTokenHash()).isNotEqualTo(tokenAtacante); // el enlace anterior queda invalidado
        login(EMAIL, "PasswordAtacante1!").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
    }

    @Test
    void registro_conEmailYaVerificado_respondeIgualQueUnAltaNuevaYNoCambiaLaCuenta() throws Exception {
        register(EMAIL, "Password123!", "");
        Usuario u = usuarioRepository.findByEmail(EMAIL).orElseThrow();
        u.setEmailVerified(true);
        usuarioRepository.saveAndFlush(u);

        String body = """
                {"nombre":"Otro","email":"%s","password":"OtraPassword1!"}
                """.formatted(EMAIL);
        // SEC-22: misma respuesta (201) que un alta nueva; no se revela que el email ya existe.
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        assertThat(passwordEncoder.matches("Password123!", usuarioRepository.findByEmail(EMAIL).orElseThrow().getPasswordHash()))
                .isTrue();
    }

    @Test
    void reenviarVerificacion_nuncaRevelaSiElEmailExiste_yRespetaElCooldown() throws Exception {
        mockMvc.perform(post("/auth/resend-verification").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"no-existe@test.local\"}"))
                .andExpect(status().isNoContent());

        register(EMAIL, "Password123!", "");
        String hashInicial = usuarioRepository.findByEmail(EMAIL).orElseThrow().getEmailVerificationTokenHash();

        // Dentro del cooldown (60 s) no se genera un token nuevo.
        mockMvc.perform(post("/auth/resend-verification").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\"}".formatted(EMAIL)))
                .andExpect(status().isNoContent());
        assertThat(usuarioRepository.findByEmail(EMAIL).orElseThrow().getEmailVerificationTokenHash()).isEqualTo(hashInicial);

        // Pasado el cooldown sí se emite uno nuevo (y el anterior queda invalidado).
        Usuario u = usuarioRepository.findByEmail(EMAIL).orElseThrow();
        u.setEmailVerificationSentAt(LocalDateTime.now().minusMinutes(5));
        usuarioRepository.saveAndFlush(u);
        mockMvc.perform(post("/auth/resend-verification").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\"}".formatted(EMAIL)))
                .andExpect(status().isNoContent());
        assertThat(usuarioRepository.findByEmail(EMAIL).orElseThrow().getEmailVerificationTokenHash()).isNotEqualTo(hashInicial);
    }
}
