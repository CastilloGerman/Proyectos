package com.appgestion.api.integration.presupuesto;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.constant.PresupuestoEstado;
import com.appgestion.api.domain.enums.TipoFactura;
import com.appgestion.api.repository.ClienteRepository;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.repository.OrganizationRepository;
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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
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
    void facturar_presupuestoConDescuentoDeLinea_conservaBaseEIva() throws Exception {
        // 100 - 10% = 90 base → IVA 18.90 → total 108.90 (no 121.00 sin descuento)
        String body = """
                {
                  "clienteId": %d,
                  "items": [{
                    "materialId": null,
                    "tareaManual": "Línea con dto",
                    "cantidad": 1.0,
                    "precioUnitario": 100.0,
                    "aplicaIva": true,
                    "descuentoPorcentaje": 10.0,
                    "descuentoFijo": 0.0
                  }],
                  "ivaHabilitado": true,
                  "estado": "%s",
                  "descuentoGlobalPorcentaje": 0.0,
                  "descuentoGlobalFijo": 0.0,
                  "descuentoAntesIva": true,
                  "condicionesActivas": [],
                  "notaAdicional": null
                }
                """.formatted(scenario.clienteCompletoId(), PresupuestoEstado.ACEPTADO);

        String res = mockMvc.perform(post("/presupuestos")
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(90.0))
                .andExpect(jsonPath("$.iva").value(18.9))
                .andExpect(jsonPath("$.total").value(108.9))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long presId = objectMapper.readTree(res).get("id").asLong();

        mockMvc.perform(post("/presupuestos/{id}/factura", presId)
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(90.0))
                .andExpect(jsonPath("$.iva").value(18.9))
                .andExpect(jsonPath("$.total").value(108.9));
    }

    @Test
    void facturar_presupuestoConDescuentoGlobal_conservaBaseEIva() throws Exception {
        // 200 - 20 fijo = 180 base → IVA 37.80 → total 217.80
        String body = """
                {
                  "clienteId": %d,
                  "items": [{
                    "materialId": null,
                    "tareaManual": "Línea",
                    "cantidad": 2.0,
                    "precioUnitario": 100.0,
                    "aplicaIva": true,
                    "descuentoPorcentaje": 0.0,
                    "descuentoFijo": 0.0
                  }],
                  "ivaHabilitado": true,
                  "estado": "%s",
                  "descuentoGlobalPorcentaje": 0.0,
                  "descuentoGlobalFijo": 20.0,
                  "descuentoAntesIva": true,
                  "condicionesActivas": [],
                  "notaAdicional": null
                }
                """.formatted(scenario.clienteCompletoId(), PresupuestoEstado.ACEPTADO);

        String res = mockMvc.perform(post("/presupuestos")
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(180.0))
                .andExpect(jsonPath("$.iva").value(37.8))
                .andExpect(jsonPath("$.total").value(217.8))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long presId = objectMapper.readTree(res).get("id").asLong();

        mockMvc.perform(post("/presupuestos/{id}/factura", presId)
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(180.0))
                .andExpect(jsonPath("$.iva").value(37.8))
                .andExpect(jsonPath("$.total").value(217.8));
    }
}
