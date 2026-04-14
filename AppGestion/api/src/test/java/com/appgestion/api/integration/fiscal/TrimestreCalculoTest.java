package com.appgestion.api.integration.fiscal;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.integration.facturacion.FacturaIntegrationTestSupport;
import com.appgestion.api.integration.facturacion.FacturacionAuth;
import com.appgestion.api.repository.ClienteRepository;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.repository.OrganizationRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.service.FiscalService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Imputación por trimestre (criterio devengo por fecha de expedición) del resumen orientativo 303.
 */
@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Transactional
class TrimestreCalculoTest {

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
    void facturaDeEnero_vaAlT1() throws Exception {
        int year = Year.now().getValue();
        crearFacturaEnFecha(LocalDate.of(year, 1, 14));
        assertResumenTrimestre(year, 1, true);
    }

    @Test
    void facturaDeMarzo_vaAlT1() throws Exception {
        int year = Year.now().getValue();
        crearFacturaEnFecha(LocalDate.of(year, 3, 20));
        assertResumenTrimestre(year, 1, true);
    }

    @Test
    void facturaDeAbril_vaAlT2() throws Exception {
        int year = Year.now().getValue();
        crearFacturaEnFecha(LocalDate.of(year, 4, 1));
        assertResumenTrimestre(year, 2, true);
    }

    @Test
    void facturaDeDiciembre_vaAlT4() throws Exception {
        int year = Year.now().getValue();
        crearFacturaEnFecha(LocalDate.of(year, 12, 15));
        assertResumenTrimestre(year, 4, true);
    }

    @Test
    void factura31Diciembre_vaAlT4_mismoAnio() throws Exception {
        int year = Year.now().getValue();
        crearFacturaEnFecha(LocalDate.of(year, 12, 31));
        assertResumenTrimestre(year, 4, true);
    }

    /**
     * El 31 de diciembre pertenece al T4 del mismo año fiscal, no al T1 del año siguiente.
     */
    @Test
    void factura31Diciembre_noApareceEnT1AnioSiguiente() throws Exception {
        int yFactura = Year.now().getValue() - 1;
        if (yFactura < 2000) {
            yFactura = Year.now().getValue();
        }
        crearFacturaEnFecha(LocalDate.of(yFactura, 12, 31));

        JsonNode t1Siguiente = getModelo303Json(yFactura + 1, 1);
        assertThat(t1Siguiente.get("numeroFacturas").asLong()).isZero();
        assertThat(new BigDecimal(t1Siguiente.get("baseImponibleTotal").asText()))
                .isEqualByComparingTo(BigDecimal.ZERO.setScale(2));

        JsonNode t4AnioFactura = getModelo303Json(yFactura, 4);
        assertThat(t4AnioFactura.get("numeroFacturas").asLong()).isGreaterThanOrEqualTo(1);
    }

    private void crearFacturaEnFecha(LocalDate fechaExpedicion) throws Exception {
        mockMvc.perform(post("/facturas")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(facturaJson(scenario.clienteCompletoId(), fechaExpedicion, null)))
                .andExpect(status().isCreated());
    }

    private void assertResumenTrimestre(int year, int trimestre, boolean esperaDatos) throws Exception {
        JsonNode root = getModelo303Json(year, trimestre);
        LocalDate desde = FiscalService.fechaInicioTrimestre(year, trimestre);
        LocalDate hasta = FiscalService.fechaFinTrimestre(year, trimestre);
        assertThat(root.get("fechaDesde").asText()).isEqualTo(desde.toString());
        assertThat(root.get("fechaHasta").asText()).isEqualTo(hasta.toString());

        long num = root.get("numeroFacturas").asLong();
        BigDecimal base = new BigDecimal(root.get("baseImponibleTotal").asText());
        if (esperaDatos) {
            assertThat(num).isGreaterThanOrEqualTo(1);
            assertThat(base).isGreaterThan(BigDecimal.ZERO);
        }
    }

    private JsonNode getModelo303Json(int year, int trimestre) throws Exception {
        String content = mockMvc.perform(get("/fiscal/modelo303")
                        .param("year", String.valueOf(year))
                        .param("trimestre", String.valueOf(trimestre))
                        .param("criterio", "DEVENGO")
                        .param("soloPagadas", "false")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(content);
    }

    private static String facturaJson(long clienteId, LocalDate fechaExpedicion, String numeroFactura) {
        String num = numeroFactura == null ? "null" : "\"" + numeroFactura + "\"";
        return """
                {
                  "clienteId": %d,
                  "presupuestoId": null,
                  "items": [{"materialId": null, "tareaManual": "Línea", "cantidad": 1.0, "precioUnitario": 10.0, "aplicaIva": true}],
                  "numeroFactura": %s,
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
                """.formatted(clienteId, num, fechaExpedicion);
    }
}
