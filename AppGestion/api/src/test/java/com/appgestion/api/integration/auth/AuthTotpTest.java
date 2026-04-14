package com.appgestion.api.integration.auth;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.repository.OrganizationRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.security.JwtService;
import com.appgestion.api.security.TotpService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warrenstrange.googleauth.GoogleAuthenticator;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 2FA en login: sin código HTTP distinto para “TOTP expirado” frente a “incorrecto” en el flujo password
 * (ambos fallan verificación). El setup con secreto pendiente caducado es otro endpoint.
 */
@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Transactional
class AuthTotpTest {

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private TotpService totpService;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private Usuario usuarioTotp;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        usuarioTotp = AuthLoginSeedSupport.seedTotpUser(
                organizationRepository, usuarioRepository, passwordEncoder, totpService);
    }

    @Test
    void login_totpCorrecto_devuelve200yJwt() throws Exception {
        String code = String.format("%06d", new GoogleAuthenticator().getTotpPassword(usuarioTotp.getTotpSecret()));
        String body = """
                {"email":"%s","password":"%s","totpCode":"%s","clientInfo":null}
                """.formatted(AuthLoginSeedSupport.TOTP_USER_EMAIL, AuthLoginSeedSupport.TOTP_USER_PASSWORD, code);

        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        assertThat(root.path("token").asText()).isNotBlank();
        assertThat(jwtService.validateToken(root.path("token").asText())).isTrue();
    }

    @Test
    void login_totpIncorrecto_devuelve400() throws Exception {
        String body = """
                {"email":"%s","password":"%s","totpCode":"000000","clientInfo":null}
                """.formatted(AuthLoginSeedSupport.TOTP_USER_EMAIL, AuthLoginSeedSupport.TOTP_USER_PASSWORD);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_totpRequeridoSinCodigo_devuelve400() throws Exception {
        String body = """
                {"email":"%s","password":"%s","clientInfo":null}
                """.formatted(AuthLoginSeedSupport.TOTP_USER_EMAIL, AuthLoginSeedSupport.TOTP_USER_PASSWORD);

        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).contains("TOTP_REQUERIDO");
    }
}
