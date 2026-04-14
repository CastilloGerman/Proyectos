package com.appgestion.api.integration.multitenancy;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.dto.request.PresupuestoItemRequest;
import com.appgestion.api.dto.request.PresupuestoRequest;
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

import java.util.List;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Transactional
class MultitenancyPresupuestosTest {

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
    void getPresupuestoDeOtroUsuario_devuelve404() throws Exception {
        mockMvc.perform(get("/presupuestos/{id}", scenario.presupuestoIdB())
                        .with(MultitenancyAuth.asUsuarioA(userDetailsService)))
                .andExpect(status().isNotFound());
    }

    @Test
    void putPresupuestoDeOtroUsuario_devuelve404() throws Exception {
        PresupuestoRequest body = new PresupuestoRequest(
                scenario.clienteIdA(),
                List.of(new PresupuestoItemRequest(null, "Ítem", 1.0, 10.0, true, null, null, null)),
                true,
                "Pendiente",
                0.0,
                0.0,
                true,
                null,
                null
        );
        mockMvc.perform(put("/presupuestos/{id}", scenario.presupuestoIdB())
                        .with(MultitenancyAuth.asUsuarioA(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletePresupuestoDeOtroUsuario_devuelve404() throws Exception {
        mockMvc.perform(delete("/presupuestos/{id}", scenario.presupuestoIdB())
                        .with(MultitenancyAuth.asUsuarioA(userDetailsService)))
                .andExpect(status().isNotFound());
    }
}
