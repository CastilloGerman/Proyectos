package com.appgestion.api.integration.facturacion;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.constant.FacturaEstadoPago;
import com.appgestion.api.repository.ClienteRepository;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.repository.FacturaRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    private FacturaRepository facturaRepository;
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
    void actualizarEstadoPago_noReemplazaLineasNiRecalculaTotales() throws Exception {
        String body = facturaJson(scenario.clienteCompletoId(), 100.0);
        String content = mockMvc.perform(post("/facturas")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(content);
        long id = root.get("id").asLong();
        double totalAntes = root.get("total").asDouble();

        mockMvc.perform(patch("/facturas/{id}/estado-pago", id)
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estadoPago\":\"Parcial\",\"montoCobrado\":40.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPago").value(FacturaEstadoPago.PARCIAL))
                .andExpect(jsonPath("$.montoCobrado").value(40.0))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.total").value(totalAntes));

        var factura = facturaRepository.findById(id).orElseThrow();
        assertThat(factura.getItems()).hasSize(1);
        assertThat(factura.getTotal()).isEqualTo(totalAntes);
    }

    private static String facturaJson(long clienteId, double precioUnitario) {
        return """
                {
                  "clienteId": %d,
                  "presupuestoId": null,
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
                """.formatted(clienteId, precioUnitario, LocalDate.now());
    }
}
