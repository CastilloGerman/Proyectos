package com.appgestion.api.integration.presupuesto;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.constant.PresupuestoEstado;
import com.appgestion.api.domain.enums.TipoFactura;
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
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Transactional
class PresupuestoFlujosTest {

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
    void crearPresupuesto_conClienteProvisionalRapido_devuelve201yClienteProvisional() throws Exception {
        String cli = mockMvc.perform(post("/clientes/provisional")
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Cliente rápido B4\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoCliente").value("PROVISIONAL"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long clienteId = objectMapper.readTree(cli).get("id").asLong();

        String pres = PresupuestoIntegrationTestSupport.presupuestoJson(clienteId, PresupuestoEstado.PENDIENTE, 50.0, 1.0);
        mockMvc.perform(post("/presupuestos")
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pres))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clienteEstado").value("PROVISIONAL"));
    }

    @Test
    void facturar_presupuestoPendiente_clienteCompleto_devuelve201() throws Exception {
        String body = PresupuestoIntegrationTestSupport.presupuestoJson(
                scenario.clienteCompletoId(), PresupuestoEstado.PENDIENTE, 100.0, 1.0);
        String res = mockMvc.perform(post("/presupuestos")
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value(PresupuestoEstado.PENDIENTE))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long presId = objectMapper.readTree(res).get("id").asLong();

        mockMvc.perform(post("/presupuestos/{id}/factura", presId)
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoFactura").value(TipoFactura.NORMAL.name()));
    }

    @Test
    void facturar_presupuestoRechazado_devuelve400() throws Exception {
        String body = PresupuestoIntegrationTestSupport.presupuestoJson(
                scenario.clienteCompletoId(), PresupuestoEstado.RECHAZADO, 50.0, 1.0);
        String res = mockMvc.perform(post("/presupuestos")
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long presId = objectMapper.readTree(res).get("id").asLong();

        mockMvc.perform(post("/presupuestos/{id}/factura", presId)
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("rechazado")));
    }

    @Test
    void facturar_presupuestoAceptado_devuelve201() throws Exception {
        String body = PresupuestoIntegrationTestSupport.presupuestoJson(
                scenario.clienteCompletoId(), PresupuestoEstado.ACEPTADO, 200.0, 1.0);
        String res = mockMvc.perform(post("/presupuestos")
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long presId = objectMapper.readTree(res).get("id").asLong();

        mockMvc.perform(post("/presupuestos/{id}/factura", presId)
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoFactura").value(TipoFactura.NORMAL.name()));
    }

    @Test
    void actualizarEstado_noReemplazaLineasNiPierdeDescuentos() throws Exception {
        String body = """
                {
                  "clienteId": %d,
                  "items": [{"materialId": null, "tareaManual": "Concepto con descuento", "cantidad": 2.0, "precioUnitario": 100.0, "aplicaIva": false, "descuentoPorcentaje": 25.0, "descuentoFijo": 10.0}],
                  "ivaHabilitado": true,
                  "estado": "%s",
                  "descuentoGlobalPorcentaje": 0.0,
                  "descuentoGlobalFijo": 0.0,
                  "descuentoAntesIva": true,
                  "condicionesActivas": [],
                  "notaAdicional": null
                }
                """.formatted(scenario.clienteCompletoId(), PresupuestoEstado.PENDIENTE);
        String res = mockMvc.perform(post("/presupuestos")
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(140.0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long presId = objectMapper.readTree(res).get("id").asLong();

        mockMvc.perform(patch("/presupuestos/{id}/estado", presId)
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"Aceptado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value(PresupuestoEstado.ACEPTADO))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.total").value(140.0));

        var presupuesto = presupuestoRepository.findById(presId).orElseThrow();
        assertThat(presupuesto.getItems()).hasSize(1);
        var item = presupuesto.getItems().get(0);
        assertThat(item.getDescuentoPorcentaje()).isEqualTo(25.0);
        assertThat(item.getDescuentoFijo()).isEqualTo(10.0);
        assertThat(item.getAplicaIva()).isFalse();
        assertThat(presupuesto.getTotal()).isEqualTo(140.0);
    }
}
