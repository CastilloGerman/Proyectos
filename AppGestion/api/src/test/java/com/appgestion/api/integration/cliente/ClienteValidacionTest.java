package com.appgestion.api.integration.cliente;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.integration.facturacion.FacturaIntegrationTestSupport;
import com.appgestion.api.integration.facturacion.FacturacionAuth;
import com.appgestion.api.repository.ClienteRepository;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.repository.OrganizationRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Transactional
class ClienteValidacionTest {

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private EmpresaRepository empresaRepository;
    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        FacturaIntegrationTestSupport.seed(
                organizationRepository, usuarioRepository, clienteRepository, empresaRepository);
    }

    @Test
    void crearDosClientes_mismoDni_mismoUsuario_devuelve400() throws Exception {
        String body = """
                {
                  "nombre": "Cliente DNI repetido",
                  "telefono": null,
                  "email": null,
                  "direccion": "Calle Unica 1",
                  "codigoPostal": "28001",
                  "provincia": "Madrid",
                  "pais": "España",
                  "dni": "11111111H"
                }
                """;

        mockMvc.perform(post("/clientes")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/clientes")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listar_conBusquedaParcialPorNombre_devuelveCoincidenciasFlexibles() throws Exception {
        mockMvc.perform(post("/clientes/provisional")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Reformas Integrales Norte\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/clientes/provisional")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Servicio Reforma Express\"}"))
                .andExpect(status().isCreated());

        String listado = mockMvc.perform(get("/clientes")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .param("q", "refo"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode arr = objectMapper.readTree(listado);
        assertThat(arr.isArray()).isTrue();
        assertThat(arr.size()).isGreaterThanOrEqualTo(2);

        boolean contieneNorte = false;
        boolean contieneExpress = false;
        for (JsonNode n : arr) {
            String nombre = n.path("nombre").asText("").toLowerCase();
            if (nombre.contains("integrales norte")) {
                contieneNorte = true;
            }
            if (nombre.contains("reforma express")) {
                contieneExpress = true;
            }
        }
        assertThat(contieneNorte).isTrue();
        assertThat(contieneExpress).isTrue();
    }
}
