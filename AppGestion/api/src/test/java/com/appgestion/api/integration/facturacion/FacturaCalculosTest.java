package com.appgestion.api.integration.facturacion;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.constant.TaxConstants;
import com.appgestion.api.repository.ClienteRepository;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.repository.OrganizationRepository;
import com.appgestion.api.repository.UsuarioRepository;
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

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cálculos de factura según {@link com.appgestion.api.service.FacturaService#calcularTotales}.
 * <p>
 * El tipo reducido IVA 10% no está modelado: solo existe {@link TaxConstants#IVA_RATE} (21%).
 */
@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Transactional
class FacturaCalculosTest {

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
    void facturaIva21_porcentaje_baseMasIvaIgualTotal() throws Exception {
        String body = facturaUnaLinea(scenario.clienteCompletoId(), 100.0, true, true);
        mockMvc.perform(post("/facturas")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(100.0))
                .andExpect(jsonPath("$.iva").value(21.0))
                .andExpect(jsonPath("$.total").value(121.0));
    }

    @Test
    void facturaIvaDesactivado_totalIgualSubtotal() throws Exception {
        String body = facturaUnaLinea(scenario.clienteCompletoId(), 100.0, true, false);
        mockMvc.perform(post("/facturas")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(100.0))
                .andExpect(jsonPath("$.iva").value(0.0))
                .andExpect(jsonPath("$.total").value(100.0));
    }

    @Test
    void facturaMultiplesLineas_sumaCoherenteConTotales() throws Exception {
        String body = """
                {
                  "clienteId": %d,
                  "presupuestoId": null,
                  "items": [
                    {"materialId": null, "tareaManual": "A", "cantidad": 1.0, "precioUnitario": 50.0, "aplicaIva": true},
                    {"materialId": null, "tareaManual": "B", "cantidad": 1.0, "precioUnitario": 30.0, "aplicaIva": true}
                  ],
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
                """.formatted(scenario.clienteCompletoId(), LocalDate.now());

        BigDecimal sub = BigDecimal.valueOf(80.0).setScale(2, RoundingMode.HALF_UP);
        BigDecimal iva = sub.multiply(TaxConstants.IVA_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = sub.add(iva).setScale(2, RoundingMode.HALF_UP);

        mockMvc.perform(post("/facturas")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(sub.doubleValue()))
                .andExpect(jsonPath("$.iva").value(iva.doubleValue()))
                .andExpect(jsonPath("$.total").value(total.doubleValue()));
    }

    private static String facturaUnaLinea(long clienteId, double precioUnitario, boolean aplicaIva, boolean ivaHabilitado) {
        return """
                {
                  "clienteId": %d,
                  "presupuestoId": null,
                  "items": [{"materialId": null, "tareaManual": "Línea", "cantidad": 1.0, "precioUnitario": %s, "aplicaIva": %s}],
                  "numeroFactura": null,
                  "fechaExpedicion": "%s",
                  "fechaOperacion": null,
                  "fechaVencimiento": null,
                  "regimenFiscal": null,
                  "condicionesPago": null,
                  "metodoPago": "Transferencia",
                  "montoCobrado": null,
                  "notas": null,
                  "ivaHabilitado": %s
                }
                """.formatted(clienteId, precioUnitario, aplicaIva, LocalDate.now(), ivaHabilitado);
    }
}
