package com.appgestion.api.integration.multitenancy;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.dto.request.ClienteRequest;
import com.appgestion.api.repository.ClienteRepository;
import com.appgestion.api.repository.FacturaRepository;
import com.appgestion.api.repository.OrganizationRepository;
import com.appgestion.api.repository.PresupuestoRepository;
import com.appgestion.api.repository.UsuarioRepository;
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

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Transactional
class MultitenancyClientesTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private FacturaRepository facturaRepository;
    @Autowired
    private PresupuestoRepository presupuestoRepository;

    private MultitenancyIntegrationTestSupport.Scenario scenario;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        scenario = MultitenancyIntegrationTestSupport.seed(
                organizationRepository,
                usuarioRepository,
                clienteRepository,
                facturaRepository,
                presupuestoRepository);
    }

    @Test
    void getClienteDeOtroUsuario_devuelve404() throws Exception {
        mockMvc.perform(get("/clientes/{id}", scenario.clienteIdB())
                        .with(MultitenancyAuth.asUsuarioA(userDetailsService)))
                .andExpect(status().isNotFound());
    }

    @Test
    void putClienteDeOtroUsuario_devuelve404() throws Exception {
        ClienteRequest body = new ClienteRequest(
                "Nombre actualizado",
                "",
                "",
                "Dir",
                "28001",
                null,
                null,
                null
        );
        mockMvc.perform(put("/clientes/{id}", scenario.clienteIdB())
                        .with(MultitenancyAuth.asUsuarioA(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteClienteDeOtroUsuario_devuelve404() throws Exception {
        mockMvc.perform(delete("/clientes/{id}", scenario.clienteIdB())
                        .with(MultitenancyAuth.asUsuarioA(userDetailsService)))
                .andExpect(status().isNotFound());
    }
}
