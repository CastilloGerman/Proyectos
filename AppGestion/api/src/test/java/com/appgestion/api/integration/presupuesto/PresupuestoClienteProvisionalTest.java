package com.appgestion.api.integration.presupuesto;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.constant.PresupuestoEstado;
import com.appgestion.api.repository.ClienteRepository;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.repository.OrganizationRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.service.ClienteService;
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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Transactional
class PresupuestoClienteProvisionalTest {

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
        PresupuestoIntegrationTestSupport.seed(
                organizationRepository, usuarioRepository, clienteRepository, empresaRepository);
    }

    @Test
    void provisional_factura400_completar_factura201() throws Exception {
        String cliJson = mockMvc.perform(post("/clientes/provisional")
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Solo nombre B4\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoCliente").value("PROVISIONAL"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long clienteId = objectMapper.readTree(cliJson).get("id").asLong();

        String presBody = PresupuestoIntegrationTestSupport.presupuestoJson(
                clienteId, PresupuestoEstado.ACEPTADO, 80.0, 1.0);
        String presJson = mockMvc.perform(post("/presupuestos")
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(presBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clienteEstado").value("PROVISIONAL"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long presId = objectMapper.readTree(presJson).get("id").asLong();

        mockMvc.perform(post("/presupuestos/{id}/factura", presId)
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString(ClienteService.MSG_FACTURA_CLIENTE_INCOMPLETO)));

        String completar = """
                {
                  "nombre": "Cliente completado B4",
                  "dni": "00000002W",
                  "direccion": "Calle Completar 9",
                  "codigoPostal": "28004"
                }
                """;

        mockMvc.perform(put("/clientes/{id}/completar", clienteId)
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(completar))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoCliente").value("COMPLETO"));

        mockMvc.perform(post("/presupuestos/{id}/factura", presId)
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService)))
                .andExpect(status().isCreated());
    }
}
