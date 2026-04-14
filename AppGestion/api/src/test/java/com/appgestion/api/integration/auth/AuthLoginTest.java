package com.appgestion.api.integration.auth;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.repository.OrganizationRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.security.JwtService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Transactional
class AuthLoginTest {

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        AuthLoginSeedSupport.seedPasswordLoginUser(
                organizationRepository, usuarioRepository, passwordEncoder);
    }

    @Test
    void login_credencialesCorrectas_devuelve200yJwtValido() throws Exception {
        String body = """
                {"email":"%s","password":"%s","totpCode":null,"clientInfo":null}
                """.formatted(AuthLoginSeedSupport.LOGIN_USER_EMAIL, AuthLoginSeedSupport.LOGIN_USER_PASSWORD);

        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        assertThat(root.has("token")).isTrue();
        String token = root.get("token").asText();
        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(root.path("type").asText()).isEqualTo("Bearer");
    }

    @Test
    void login_passwordIncorrecta_devuelve400() throws Exception {
        String body = """
                {"email":"%s","password":"wrong-password","totpCode":null,"clientInfo":null}
                """.formatted(AuthLoginSeedSupport.LOGIN_USER_EMAIL);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_usuarioInexistente_devuelve400() throws Exception {
        String body = """
                {"email":"no-existe-block2@test.local","password":"x","totpCode":null,"clientInfo":null}
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    /**
     * Objeto JSON vacío: faltan email/contraseña obligatorios → 400.
     * Un cuerpo literalmente vacío ("") puede producir 500 por fallo de parseo; no es el caso de uso típico del cliente.
     */
    @Test
    void login_bodyJsonVacio_sinCamposObligatorios_devuelve400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
