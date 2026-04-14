package com.appgestion.api.integration.fiscal;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.constant.TaxConstants;
import com.appgestion.api.integration.facturacion.FacturaIntegrationTestSupport;
import com.appgestion.api.integration.facturacion.FacturacionAuth;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Agregación DEVENGO del resumen orientativo 303.
 * <p>
 * La API actual solo calcula IVA al 21 % ({@link TaxConstants#IVA_RATE}); no existe desglose por tipos
 * 10 % / 4 % en el resumen. Los tests con bases distintas verifican la suma global.
 */
@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Transactional
class Modelo303ResumenTest {

    private static final int ANIO_VACIO = 2099;

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
    void resumenT1_tresFacturas_sumaBasesEIva() throws Exception {
        int year = Year.now().getValue();
        double p1 = 100.0;
        double p2 = 250.0;
        double p3 = 75.0;
        mockMvc.perform(post("/facturas")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(facturaJson(scenario.clienteCompletoId(), LocalDate.of(year, 1, 5), null, p1)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/facturas")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(facturaJson(scenario.clienteCompletoId(), LocalDate.of(year, 2, 10), null, p2)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/facturas")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(facturaJson(scenario.clienteCompletoId(), LocalDate.of(year, 3, 20), null, p3)))
                .andExpect(status().isCreated());

        BigDecimal baseEsperada = BigDecimal.valueOf(p1 + p2 + p3).setScale(2, RoundingMode.HALF_UP);
        BigDecimal ivaEsperado = baseEsperada.multiply(TaxConstants.IVA_RATE).setScale(2, RoundingMode.HALF_UP);

        JsonNode root = getModelo303(year, 1);
        assertThat(new BigDecimal(root.get("baseImponibleTotal").asText())).isEqualByComparingTo(baseEsperada);
        assertThat(new BigDecimal(root.get("ivaRepercutido").asText())).isEqualByComparingTo(ivaEsperado);
        assertThat(root.get("numeroFacturas").asLong()).isEqualTo(3L);
    }

    /**
     * Tres bases distintas con el mismo tipo efectivo (21 %): el resumen sigue siendo un único agregado.
     */
    @Test
    void resumenT1_tresBasesDistintas_mismoIva21_agregadoUnico() throws Exception {
        int year = Year.now().getValue();
        mockMvc.perform(post("/facturas")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(facturaJson(scenario.clienteCompletoId(), LocalDate.of(year, 2, 1), null, 400.0)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/facturas")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(facturaJson(scenario.clienteCompletoId(), LocalDate.of(year, 2, 2), null, 100.0)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/facturas")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(facturaJson(scenario.clienteCompletoId(), LocalDate.of(year, 2, 3), null, 50.0)))
                .andExpect(status().isCreated());

        BigDecimal baseEsperada = BigDecimal.valueOf(550.0).setScale(2, RoundingMode.HALF_UP);
        BigDecimal ivaEsperado = baseEsperada.multiply(TaxConstants.IVA_RATE).setScale(2, RoundingMode.HALF_UP);

        JsonNode root = getModelo303(year, 1);
        assertThat(new BigDecimal(root.get("baseImponibleTotal").asText())).isEqualByComparingTo(baseEsperada);
        assertThat(new BigDecimal(root.get("ivaRepercutido").asText())).isEqualByComparingTo(ivaEsperado);
    }

    @Test
    void trimestreSinFacturas_devuelveCerosSinError() throws Exception {
        JsonNode root = getModelo303(ANIO_VACIO, 3);
        assertThat(new BigDecimal(root.get("baseImponibleTotal").asText()))
                .isEqualByComparingTo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        assertThat(new BigDecimal(root.get("ivaRepercutido").asText()))
                .isEqualByComparingTo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        assertThat(root.get("numeroFacturas").asLong()).isZero();
    }

    private JsonNode getModelo303(int year, int trimestre) throws Exception {
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

    private static String facturaJson(long clienteId, LocalDate fechaExpedicion, String numeroFactura, double precioUnitario) {
        String num = numeroFactura == null ? "null" : "\"" + numeroFactura + "\"";
        return """
                {
                  "clienteId": %d,
                  "presupuestoId": null,
                  "items": [{"materialId": null, "tareaManual": "Línea", "cantidad": 1.0, "precioUnitario": %s, "aplicaIva": true}],
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
                """.formatted(clienteId, Double.toString(precioUnitario), num, fechaExpedicion);
    }
}
