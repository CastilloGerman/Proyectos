package com.appgestion.api.integration.gastos;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.domain.entity.Organization;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.SubscriptionStatus;
import com.appgestion.api.dto.internal.GeminiGastoExtraction;
import com.appgestion.api.repository.OrganizationRepository;
import com.appgestion.api.repository.GastoRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.service.GeminiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Transactional
class GastoIaEndpointTest {

    private static final String EMAIL = "gasto-ia-endpoint@test.local";

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private GastoRepository gastoRepository;
    @Autowired
    private UserDetailsService userDetailsService;

    @MockitoBean
    private GeminiClient geminiClient;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        Organization organization = new Organization();
        organization.setName("Org test gastos IA");
        organization = organizationRepository.save(organization);

        Usuario usuario = new Usuario();
        usuario.setNombre("Usuario gastos IA");
        usuario.setEmail(EMAIL);
        usuario.setPasswordHash("$2a$10$dummyhashfordummytestsxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        usuario.setRol("USER");
        usuario.setActivo(true);
        usuario.setOrganization(organization);
        usuario.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        usuarioRepository.save(usuario);
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(multipart("/gastos/ia/extraer").file(validImage()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidContentType() throws Exception {
        MockMultipartFile invalid = new MockMultipartFile(
                "archivo", "ticket.jpg", "image/jpeg", "contenido falso".getBytes());

        mockMvc.perform(multipart("/gastos/ia/extraer").file(invalid).with(asCurrentUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Tipo de archivo")));
    }

    @Test
    void rejectsFilesAboveFiveMegabytes() throws Exception {
        byte[] content = new byte[(5 * 1024 * 1024) + 1];
        content[0] = (byte) 0xFF;
        content[1] = (byte) 0xD8;
        content[2] = (byte) 0xFF;
        MockMultipartFile large = new MockMultipartFile("archivo", "ticket.jpg", "image/jpeg", content);

        mockMvc.perform(multipart("/gastos/ia/extraer").file(large).with(asCurrentUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El archivo supera el límite permitido."));
    }

    @Test
    void returnsDraftWithoutPersistingWhenGeminiIsMocked() throws Exception {
        when(geminiClient.generate(anyString(), anyString(), any(), anyString(), any(),
                eq(GeminiGastoExtraction.class)))
                .thenReturn(new GeminiGastoExtraction(
                        "Proveedor test", "Material", "01/09/2026", new BigDecimal("100"),
                        new BigDecimal("21"), "MATERIAL", new BigDecimal("121"),
                        List.of(new BigDecimal("21")), List.of(), true, true));

        mockMvc.perform(multipart("/gastos/ia/extraer").file(validImage()).with(asCurrentUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proveedor").value("Proveedor test"))
                .andExpect(jsonPath("$.baseImponible").value(100.00))
                .andExpect(jsonPath("$.categoria").value("MATERIAL"))
                .andExpect(jsonPath("$.tieneNif").value(true))
                .andExpect(jsonPath("$.nif").doesNotExist())
                .andExpect(jsonPath("$.numeroDocumento").doesNotExist())
                .andExpect(jsonPath("$.esDocumentoValido").value(true));
        assertEquals(0, gastoRepository.count());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor asCurrentUser() {
        UserDetails userDetails = userDetailsService.loadUserByUsername(EMAIL);
        return authentication(new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()));
    }

    private static MockMultipartFile validImage() {
        return new MockMultipartFile("archivo", "ticket.jpg", MediaType.IMAGE_JPEG_VALUE,
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01});
    }
}
