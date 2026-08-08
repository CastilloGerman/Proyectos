package com.appgestion.api.integration.fiscal;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.domain.entity.Gasto;
import com.appgestion.api.domain.enums.GastoCategoria;
import com.appgestion.api.integration.facturacion.FacturaIntegrationTestSupport;
import com.appgestion.api.integration.facturacion.FacturacionAuth;
import com.appgestion.api.repository.ClienteRepository;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.repository.GastoRepository;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * IVA soportado real en el resumen orientativo 303 a partir de {@code gastos}.
 */
@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Transactional
class GastoIvaSoportado303Test {

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
    private GastoRepository gastoRepository;
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
    void trimestreConGastos_ivaSoportadoMayorQueCero() throws Exception {
        int year = Year.now().getValue();
        persistGasto(scenario.usuarioId(), LocalDate.of(year, 2, 15), 100.0, 21.0);
        persistGasto(scenario.usuarioId(), LocalDate.of(year, 2, 20), 50.0, 10.0);

        BigDecimal ivaEsperado = BigDecimal.valueOf(21.0 + 5.0).setScale(2, RoundingMode.HALF_UP);

        JsonNode root = getModelo303(year, 1, "DEVENGO");
        assertThat(new BigDecimal(root.get("ivaSoportado").asText())).isEqualByComparingTo(ivaEsperado);
        assertThat(root.get("ivaSoportadoCalculado").asBoolean()).isTrue();
        assertThat(root.get("ivaSoportadoNota").asText()).contains("gastos registrados");
    }

    @Test
    void trimestreSinGastos_ivaSoportadoCero_peroCalculado() throws Exception {
        JsonNode root = getModelo303(ANIO_VACIO, 2, "DEVENGO");
        assertThat(new BigDecimal(root.get("ivaSoportado").asText()))
                .isEqualByComparingTo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        assertThat(root.get("ivaSoportadoCalculado").asBoolean()).isTrue();
        assertThat(root.get("ivaSoportadoNota").asText()).contains("no está registrado");
    }

    @Test
    void trimestreConGastos_resultadoRestaIvaSoportado() throws Exception {
        int year = Year.now().getValue();
        mockMvc.perform(post("/facturas")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(facturaJson(scenario.clienteCompletoId(), LocalDate.of(year, 1, 10), 100.0)))
                .andExpect(status().isCreated());
        persistGasto(scenario.usuarioId(), LocalDate.of(year, 1, 12), 100.0, 21.0);

        JsonNode root = getModelo303(year, 1, "DEVENGO");
        BigDecimal ivaRep = new BigDecimal(root.get("ivaRepercutido").asText());
        BigDecimal ivaSop = new BigDecimal(root.get("ivaSoportado").asText());
        BigDecimal resultado = new BigDecimal(root.get("resultadoIva").asText());
        assertThat(resultado).isEqualByComparingTo(ivaRep.subtract(ivaSop).setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    void gastoEnPrimerDiaTrimestre_entraEnT1() throws Exception {
        int year = Year.now().getValue();
        persistGasto(scenario.usuarioId(), FiscalService.fechaInicioTrimestre(year, 1), 100.0, 21.0);

        JsonNode root = getModelo303(year, 1, "DEVENGO");
        assertThat(new BigDecimal(root.get("ivaSoportado").asText()))
                .isEqualByComparingTo(BigDecimal.valueOf(21.0).setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    void gastoEnUltimoDiaTrimestre_entraEnT1() throws Exception {
        int year = Year.now().getValue();
        persistGasto(scenario.usuarioId(), FiscalService.fechaFinTrimestre(year, 1), 200.0, 21.0);

        JsonNode root = getModelo303(year, 1, "DEVENGO");
        assertThat(new BigDecimal(root.get("ivaSoportado").asText()))
                .isEqualByComparingTo(BigDecimal.valueOf(42.0).setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    void gastoFueraDeRango_noSumaEnTrimestre() throws Exception {
        int year = Year.now().getValue();
        persistGasto(scenario.usuarioId(), FiscalService.fechaFinTrimestre(year, 1).plusDays(1), 100.0, 21.0);

        JsonNode root = getModelo303(year, 1, "DEVENGO");
        assertThat(new BigDecimal(root.get("ivaSoportado").asText()))
                .isEqualByComparingTo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    void criterioCaja_sumaGastosDelTrimestrePorFecha() throws Exception {
        int year = Year.now().getValue();
        persistGasto(scenario.usuarioId(), LocalDate.of(year, 5, 10), 100.0, 21.0);

        JsonNode root = getModelo303(year, 2, "CAJA");
        assertThat(new BigDecimal(root.get("ivaSoportado").asText()))
                .isEqualByComparingTo(BigDecimal.valueOf(21.0).setScale(2, RoundingMode.HALF_UP));
        assertThat(root.get("ivaSoportadoCalculado").asBoolean()).isTrue();
    }

    @Test
    void avisoLegal303_noSeModifica() throws Exception {
        JsonNode root = getModelo303(ANIO_VACIO, 1, "DEVENGO");
        assertThat(root.get("avisoLegal").asText()).contains("No sustituye el Modelo 303 oficial");
    }

    private void persistGasto(Long usuarioId, LocalDate fecha, double base, double tipoIva) {
        var usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        Gasto gasto = new Gasto();
        gasto.setUsuario(usuario);
        gasto.setProveedor("Proveedor test");
        gasto.setConcepto("Compra test");
        gasto.setFecha(fecha);
        gasto.setBaseImponible(base);
        gasto.setTipoIva(tipoIva);
        gasto.setCuotaIva(com.appgestion.api.service.GastoService.calcularCuotaIva(base, tipoIva));
        gasto.setCategoria(GastoCategoria.MATERIAL);
        gastoRepository.save(gasto);
    }

    private JsonNode getModelo303(int year, int trimestre, String criterio) throws Exception {
        String content = mockMvc.perform(get("/fiscal/modelo303")
                        .param("year", String.valueOf(year))
                        .param("trimestre", String.valueOf(trimestre))
                        .param("criterio", criterio)
                        .param("soloPagadas", "false")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(content);
    }

    private static String facturaJson(long clienteId, LocalDate fechaExpedicion, double precioUnitario) {
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
                """.formatted(clienteId, Double.toString(precioUnitario), fechaExpedicion);
    }
}
