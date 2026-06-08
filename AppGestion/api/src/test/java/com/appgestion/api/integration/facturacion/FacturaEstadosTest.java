package com.appgestion.api.integration.facturacion;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.constant.FacturaEstadoPago;
import com.appgestion.api.constant.PresupuestoEstado;
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

import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Transactional
class FacturaEstadosTest {

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
    private FacturaIntegrationTestSupport.Scenario scenario;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        scenario = FacturaIntegrationTestSupport.seed(
                organizationRepository, usuarioRepository, clienteRepository, empresaRepository);
    }

    @Test
    void facturaNueva_tieneEstadoNoPagada() throws Exception {
        String body = facturaJson(scenario.clienteCompletoId(), 100.0);
        mockMvc.perform(post("/facturas")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoPago").value(FacturaEstadoPago.NO_PAGADA));
    }

    @Test
    void registrarCobroTotal_marcaPagada() throws Exception {
        String body = facturaJson(scenario.clienteCompletoId(), 100.0);
        String content = mockMvc.perform(post("/facturas")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoPago").value(FacturaEstadoPago.NO_PAGADA))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(content);
        long id = root.get("id").asLong();
        double total = root.get("total").asDouble();

        String cobro = """
                {"importe": %s, "fecha": null, "metodo": "Transferencia", "notas": null}
                """.formatted(total);

        mockMvc.perform(post("/facturas/{id}/cobros", id)
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cobro))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPago").value(FacturaEstadoPago.PAGADA));
    }

    @Test
    void registrarCobroParcial_marcaParcial() throws Exception {
        String body = facturaJson(scenario.clienteCompletoId(), 100.0);
        String content = mockMvc.perform(post("/facturas")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = objectMapper.readTree(content).get("id").asLong();

        String cobro = """
                {"importe": 40.0, "fecha": null, "metodo": "Efectivo", "notas": null}
                """;

        mockMvc.perform(post("/facturas/{id}/cobros", id)
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cobro))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPago").value(FacturaEstadoPago.PARCIAL));
    }

    @Test
    void patchEstadoPago_actualizaEstadoSinReescribirDocumento() throws Exception {
        String body = facturaJson(scenario.clienteCompletoId(), 100.0);
        String content = mockMvc.perform(post("/facturas")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = objectMapper.readTree(content).get("id").asLong();

        mockMvc.perform(patch("/facturas/{id}/estado-pago", id)
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estadoPago\":\"Parcial\",\"montoCobrado\":40.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPago").value(FacturaEstadoPago.PARCIAL))
                .andExpect(jsonPath("$.montoCobrado").value(40.0))
                .andExpect(jsonPath("$.subtotal").value(100.0))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void actualizarFacturaVinculada_noSobrescribeEstadoActualDelPresupuesto() throws Exception {
        String presupuesto = presupuestoJson(scenario.clienteCompletoId(), PresupuestoEstado.PENDIENTE, 100.0);
        String presupuestoContent = mockMvc.perform(post("/presupuestos")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(presupuesto))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long presupuestoId = objectMapper.readTree(presupuestoContent).get("id").asLong();

        String facturaContent = mockMvc.perform(post("/facturas")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(facturaJson(scenario.clienteCompletoId(), presupuestoId, 100.0)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long facturaId = objectMapper.readTree(facturaContent).get("id").asLong();

        mockMvc.perform(patch("/presupuestos/{id}/estado", presupuestoId)
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"" + PresupuestoEstado.EN_EJECUCION + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value(PresupuestoEstado.EN_EJECUCION));

        mockMvc.perform(put("/facturas/{id}", facturaId)
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(facturaJson(scenario.clienteCompletoId(), presupuestoId, 120.0)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/presupuestos/{id}", presupuestoId)
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value(PresupuestoEstado.EN_EJECUCION));
    }

    private static String facturaJson(long clienteId, double precioUnitario) {
        return facturaJson(clienteId, null, precioUnitario);
    }

    private static String facturaJson(long clienteId, Long presupuestoId, double precioUnitario) {
        String presupuestoJson = presupuestoId == null ? "null" : presupuestoId.toString();
        return """
                {
                  "clienteId": %d,
                  "presupuestoId": %s,
                  "items": [{"materialId": null, "tareaManual": "Línea", "cantidad": 1.0, "precioUnitario": %s, "aplicaIva": true}],
                  "numeroFactura": null,
                  "fechaExpedicion": "%s",
                  "fechaOperacion": null,
                  "fechaVencimiento": null,
                  "regimenFiscal": null,
                  "condicionesPago": null,
                  "metodoPago": "Transferencia",
                  "montoCobrado": null,
                  "notas": null,
                  "ivaHabilitado": true
                }
                """.formatted(clienteId, presupuestoJson, precioUnitario, LocalDate.now());
    }

    private static String presupuestoJson(long clienteId, String estado, double precioUnitario) {
        return """
                {
                  "clienteId": %d,
                  "items": [{"materialId": null, "tareaManual": "Concepto presupuesto", "cantidad": 1.0, "precioUnitario": %s, "aplicaIva": true}],
                  "ivaHabilitado": true,
                  "estado": "%s",
                  "descuentoGlobalPorcentaje": 0.0,
                  "descuentoGlobalFijo": 0.0,
                  "descuentoAntesIva": true,
                  "condicionesActivas": [],
                  "notaAdicional": null
                }
                """.formatted(clienteId, precioUnitario, estado);
    }
}
