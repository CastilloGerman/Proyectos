package com.appgestion.api.integration.auth;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.entity.UsuarioSesion;
import com.appgestion.api.repository.OrganizationRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.repository.UsuarioSesionRepository;
import com.appgestion.api.security.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Transactional
class AuthJwtTest {

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
    private UsuarioSesionRepository usuarioSesionRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        AuthLoginSeedSupport.seedPasswordLoginUser(
                organizationRepository, usuarioRepository, passwordEncoder);
    }

    private String tokenConSesion(boolean revocada) {
        Usuario u = usuarioRepository.findByEmail(AuthLoginSeedSupport.LOGIN_USER_EMAIL).orElseThrow();
        Instant now = Instant.now();
        UsuarioSesion s = new UsuarioSesion();
        s.setId(UUID.randomUUID().toString());
        s.setUsuario(u);
        s.setCreatedAt(now);
        s.setLastActivityAt(now);
        s.setExpiresAt(now.plusSeconds(3600));
        if (revocada) {
            s.setRevokedAt(now);
        }
        usuarioSesionRepository.saveAndFlush(s);
        return jwtService.generateToken(u.getEmail(), u.getRol(), s.getId());
    }

    @Test
    void me_conJwtValido_devuelve200() throws Exception {
        String token = tokenConSesion(false);
        assertThat(jwtService.validateToken(token)).isTrue();

        mockMvc.perform(get("/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /** SEC-10: un JWT firmado pero sin claim sid no se puede revocar, así que se rechaza. */
    @Test
    void me_conJwtSinSid_devuelve401() throws Exception {
        String token = JwtTestTokens.validToken(AuthLoginSeedSupport.LOGIN_USER_EMAIL, "USER");
        assertThat(jwtService.validateToken(token)).isTrue();

        mockMvc.perform(get("/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_conSesionRevocada_devuelve401() throws Exception {
        String token = tokenConSesion(true);

        mockMvc.perform(get("/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    /** SEC-10: el emisor (iss) debe coincidir con el configurado. */
    @Test
    void jwtConEmisorAjeno_noEsValido() {
        String ajeno = Jwts.builder()
                .subject(AuthLoginSeedSupport.LOGIN_USER_EMAIL)
                .issuer("otra-app")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .claim("sid", "x")
                .signWith(Keys.hmacShaKeyFor(JwtTestTokens.TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
        assertThat(jwtService.validateToken(ajeno)).isFalse();
    }

    @Test
    void me_sinJwt_devuelve401() throws Exception {
        mockMvc.perform(get("/auth/me").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_conJwtExpirado_devuelve401() throws Exception {
        String token = JwtTestTokens.expiredToken(AuthLoginSeedSupport.LOGIN_USER_EMAIL, "USER");
        assertThat(jwtService.validateToken(token)).isFalse();

        mockMvc.perform(get("/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_conJwtManipulado_devuelve401() throws Exception {
        String token = JwtTestTokens.tamperedToken(AuthLoginSeedSupport.LOGIN_USER_EMAIL, "USER");
        assertThat(jwtService.validateToken(token)).isFalse();

        mockMvc.perform(get("/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
