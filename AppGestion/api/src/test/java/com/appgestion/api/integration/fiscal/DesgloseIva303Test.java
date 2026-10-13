package com.appgestion.api.integration.fiscal;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.constant.PresupuestoEstado;
import com.appgestion.api.integration.facturacion.FacturaIntegrationTestSupport;
import com.appgestion.api.integration.facturacion.FacturacionAuth;
import com.appgestion.api.integration.presupuesto.PresupuestoIntegrationAuth;
import com.appgestion.api.integration.presupuesto.PresupuestoIntegrationTestSupport;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Transactional
class DesgloseIva303Test {

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
    private FacturaIntegrationTestSupport.Scenario facturas;
    private PresupuestoIntegrationTestSupport.Scenario presupuestos;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        facturas = FacturaIntegrationTestSupport.seed(
                organizationRepository, usuarioRepository, clienteRepository, empresaRepository);
        presupuestos = PresupuestoIntegrationTestSupport.seed(
                organizationRepository, usuarioRepository, clienteRepository, empresaRepository);
    }

    @Test
    void devengoCajaYSoloPagadas_elDesgloseCuadraConElIvaRepercutido() throws Exception {
        JsonNode pagada = crearFactura(facturas.clienteCompletoId(), LocalDate.of(2026, 2, 10), "100.00", 21);
        cobrar(pagada.get("id").asLong(), pagada.get("total").asText(), LocalDate.of(2026, 2, 10));
        crearFactura(facturas.clienteCompletoId(), LocalDate.of(2026, 2, 11), "50.00", 21);

        JsonNode devengo = modelo303Facturas(2026, 1, "DEVENGO", false);
        JsonNode soloPagadas = modelo303Facturas(2026, 1, "DEVENGO", true);
        JsonNode caja = modelo303Facturas(2026, 1, "CAJA", false);

        assertThat(money(devengo, "ivaRepercutido")).isEqualByComparingTo("31.50");
        assertThat(money(soloPagadas, "ivaRepercutido")).isEqualByComparingTo("21.00");
        assertThat(money(caja, "ivaRepercutido")).isEqualByComparingTo("21.00");
        assertCuadra(devengo);
        assertCuadra(soloPagadas);
        assertCuadra(caja);
    }

    @Test
    void ajusteNegativo_cuadraConEl303() throws Exception {
        crearFactura(facturas.clienteCompletoId(), LocalDate.of(2026, 2, 12), "0.03", 21, 3);

        JsonNode root = modelo303Facturas(2026, 1, "DEVENGO", false);

        assertThat(money(root, "ivaRepercutido")).isEqualByComparingTo("0.02");
        assertThat(money(root, "ajusteRedondeo")).isEqualByComparingTo("-0.01");
        assertCuadra(root);
    }

    @Test
    void anticipo_elDescuentoNoRestaElIvaYElDesgloseCuadraEnAmbosCriterios() throws Exception {
        String presBody = PresupuestoIntegrationTestSupport.presupuestoJson(
                presupuestos.clienteCompletoId(), PresupuestoEstado.ACEPTADO, 1000.0, 1.0);
        String presJson = mockMvc.perform(post("/presupuestos")
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(presBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long presId = objectMapper.readTree(presJson).get("id").asLong();

        mockMvc.perform(post("/presupuestos/{id}/anticipo", presId)
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"importeAnticipo\": 300, \"fechaAnticipo\": \"2026-01-15\"}"))
                .andExpect(status().isOk());

        String anticipoJson = mockMvc.perform(post("/presupuestos/{id}/factura-anticipo", presId)
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        BigDecimal ivaAnticipo = money(objectMapper.readTree(anticipoJson), "iva");

        String finalJson = mockMvc.perform(post("/presupuestos/{id}/factura-final", presId)
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode facturaFinal = objectMapper.readTree(finalJson);
        BigDecimal ivaFinal = money(facturaFinal, "iva");
        BigDecimal bruto = money(facturaFinal, "subtotal").add(ivaFinal);
        assertThat(money(facturaFinal, "total")).isLessThan(bruto);

        int trimestreFinal = (LocalDate.now().getMonthValue() - 1) / 3 + 1;
        int anioFinal = LocalDate.now().getYear();

        JsonNode t1Devengo = modelo303Presupuestos(2026, 1, "DEVENGO", false);
        JsonNode t1Caja = modelo303Presupuestos(2026, 1, "CAJA", false);
        JsonNode t1SoloPagadas = modelo303Presupuestos(2026, 1, "DEVENGO", true);
        JsonNode finalDevengo = modelo303Presupuestos(anioFinal, trimestreFinal, "DEVENGO", false);
        JsonNode finalSoloPagadas = modelo303Presupuestos(anioFinal, trimestreFinal, "DEVENGO", true);
        JsonNode finalCaja = modelo303Presupuestos(anioFinal, trimestreFinal, "CAJA", false);

        assertThat(money(t1Devengo, "ivaRepercutido")).isEqualByComparingTo(ivaAnticipo);
        assertThat(money(t1Caja, "ivaRepercutido")).isEqualByComparingTo(ivaAnticipo);
        assertThat(money(t1SoloPagadas, "ivaRepercutido")).isEqualByComparingTo(ivaAnticipo);
        assertThat(money(finalDevengo, "ivaRepercutido")).isEqualByComparingTo(ivaFinal);
        if (trimestreFinal != 1 || anioFinal != 2026) {
            assertThat(money(finalSoloPagadas, "ivaRepercutido")).isEqualByComparingTo("0.00");
            assertThat(money(finalCaja, "ivaRepercutido")).isEqualByComparingTo("0.00");
        }
        assertCuadra(t1Devengo);
        assertCuadra(t1Caja);
        assertCuadra(t1SoloPagadas);
        assertCuadra(finalDevengo);
        assertCuadra(finalSoloPagadas);
        assertCuadra(finalCaja);
    }

    private JsonNode crearFactura(long clienteId, LocalDate fecha, String precio, int tipo) throws Exception {
        return crearFactura(clienteId, fecha, precio, tipo, 1);
    }

    private JsonNode crearFactura(long clienteId, LocalDate fecha, String precio, int tipo, int repeticiones) throws Exception {
        StringBuilder items = new StringBuilder();
        for (int i = 0; i < repeticiones; i++) {
            if (i > 0) {
                items.append(',');
            }
            items.append("""
                    {"materialId": null, "tareaManual": "L%d", "cantidad": 1.0, "precioUnitario": %s, "aplicaIva": %s, "tipoIva": %d}
                    """.formatted(i + 1, precio, tipo != 0, tipo));
        }
        String body = """
                {
                  "clienteId": %d,
                  "items": [%s],
                  "fechaExpedicion": "%s",
                  "metodoPago": "Transferencia",
                  "ivaHabilitado": true
                }
                """.formatted(clienteId, items, fecha);
        String json = mockMvc.perform(post("/facturas")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json);
    }

    private void cobrar(long facturaId, String importe, LocalDate fecha) throws Exception {
        mockMvc.perform(post("/facturas/{id}/cobros", facturaId)
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"importe": %s, "fecha": "%s", "metodo": "Transferencia"}
                                """.formatted(importe, fecha)))
                .andExpect(status().isOk());
    }

    private JsonNode modelo303Facturas(int year, int trimestre, String criterio, boolean soloPagadas) throws Exception {
        return modelo303(year, trimestre, criterio, soloPagadas, FacturacionAuth.asUsuarioFacturacion(userDetailsService));
    }

    private JsonNode modelo303Presupuestos(int year, int trimestre, String criterio, boolean soloPagadas) throws Exception {
        return modelo303(year, trimestre, criterio, soloPagadas, PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService));
    }

    private JsonNode modelo303(int year, int trimestre, String criterio, boolean soloPagadas, RequestPostProcessor auth) throws Exception {
        String content = mockMvc.perform(get("/fiscal/modelo303")
                        .param("year", String.valueOf(year))
                        .param("trimestre", String.valueOf(trimestre))
                        .param("criterio", criterio)
                        .param("soloPagadas", String.valueOf(soloPagadas))
                        .with(auth))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(content);
    }

    private static void assertCuadra(JsonNode root) {
        BigDecimal suma = BigDecimal.ZERO;
        for (JsonNode fila : root.get("desgloseIva")) {
            suma = suma.add(money(fila, "cuota"));
        }
        assertThat(suma.add(money(root, "ajusteRedondeo"))).isEqualByComparingTo(money(root, "ivaRepercutido"));
    }

    private static BigDecimal money(JsonNode node, String field) {
        return new BigDecimal(node.get(field).asText());
    }
}
