package com.appgestion.api.integration.presupuesto;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.constant.PresupuestoEstado;
import com.appgestion.api.domain.entity.Presupuesto;
import com.appgestion.api.domain.entity.PresupuestoItem;
import com.appgestion.api.repository.ClienteRepository;
import com.appgestion.api.repository.EmpresaRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Transactional
class EstadoPresupuestoNoRecalculaTest {

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
    private PresupuestoRepository presupuestoRepository;
    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private PresupuestoIntegrationTestSupport.Scenario scenario;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        scenario = PresupuestoIntegrationTestSupport.seed(
                organizationRepository, usuarioRepository, clienteRepository, empresaRepository);
    }

    @Test
    void cambiarEstado_noTocaSubtotalIvaNiTotal() throws Exception {
        String body = PresupuestoIntegrationTestSupport.presupuestoJson(
                scenario.clienteCompletoId(), PresupuestoEstado.PENDIENTE, 100.0, 1.0);
        String creada = mockMvc.perform(post("/presupuestos")
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(creada).get("id").asLong();

        Presupuesto guardado = presupuestoRepository.findById(id).orElseThrow();
        double subtotal = guardado.getSubtotal();
        double iva = guardado.getIva();
        PresupuestoItem linea = guardado.getItems().get(0);
        double subtotalLinea = linea.getSubtotal();
        linea.setDescuentoPorcentaje(12.5);
        guardado.setTotal(77.77);
        presupuestoRepository.saveAndFlush(guardado);

        mockMvc.perform(patch("/presupuestos/{id}/estado", id)
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\": \"Aceptado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value(PresupuestoEstado.ACEPTADO))
                .andExpect(jsonPath("$.subtotal").value(subtotal))
                .andExpect(jsonPath("$.iva").value(iva))
                .andExpect(jsonPath("$.total").value(77.77));

        Presupuesto recargado = presupuestoRepository.findById(id).orElseThrow();
        assertThat(recargado.getEstado()).isEqualTo(PresupuestoEstado.ACEPTADO);
        assertThat(recargado.getSubtotal()).isEqualTo(subtotal);
        assertThat(recargado.getIva()).isEqualTo(iva);
        assertThat(recargado.getTotal()).isEqualTo(77.77);
        assertThat(recargado.getItems()).hasSize(1);
        PresupuestoItem lineaRecargada = recargado.getItems().get(0);
        assertThat(lineaRecargada.getSubtotal()).isEqualTo(subtotalLinea);
        assertThat(lineaRecargada.getDescuentoPorcentaje()).isEqualTo(12.5);
        assertThat(lineaRecargada.getPrecioUnitario()).isEqualTo(100.0);
    }
}
