package com.appgestion.api.integration.validation;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.integration.facturacion.FacturaIntegrationTestSupport;
import com.appgestion.api.integration.facturacion.FacturacionAuth;
import com.appgestion.api.repository.ClienteRepository;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.repository.OrganizationRepository;
import com.appgestion.api.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Transactional
class ValidacionFacturaTest {

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
    void crearFactura_cantidadNegativa_devuelve400() throws Exception {
        mockMvc.perform(post("/facturas")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(facturaJson(scenario.clienteCompletoId(), LocalDate.now(), -1.0, 10.0)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearFactura_precioUnitarioNegativo_devuelve400() throws Exception {
        mockMvc.perform(post("/facturas")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(facturaJson(scenario.clienteCompletoId(), LocalDate.now(), 1.0, -5.0)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearFactura_cantidadCero_devuelve400() throws Exception {
        mockMvc.perform(post("/facturas")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(facturaJson(scenario.clienteCompletoId(), LocalDate.now(), 0.0, 10.0)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearFactura_precioUnitarioCero_seAcepta() throws Exception {
        mockMvc.perform(post("/facturas")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(facturaJson(scenario.clienteCompletoId(), LocalDate.now(), 1.0, 0.0)))
                .andExpect(status().isCreated());
    }

    @Test
    void crearFactura_vencimientoAnteriorAExpedicion_devuelve400() throws Exception {
        LocalDate exp = LocalDate.of(2026, 6, 15);
        LocalDate ven = LocalDate.of(2026, 1, 10);
        mockMvc.perform(post("/facturas")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(facturaJsonConVencimiento(scenario.clienteCompletoId(), exp, ven)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("La fecha de vencimiento no puede ser")));
    }

    @Disabled("No hay campo de tipo IVA en FacturaRequest; el tipo impositivo lo fija el servicio (21 %).")
    @Test
    void crearFactura_ivaFueraDeValoresPermitidos_noAplicaEnApiActual() {
    }

    private static String facturaJson(long clienteId, LocalDate fechaExpedicion, double cantidad, double precio) {
        return """
                {
                  "clienteId": %d,
                  "presupuestoId": null,
                  "items": [{"materialId": null, "tareaManual": "Línea", "cantidad": %s, "precioUnitario": %s, "aplicaIva": true}],
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
                """.formatted(clienteId, Double.toString(cantidad), Double.toString(precio), fechaExpedicion);
    }

    private static String facturaJsonConVencimiento(long clienteId, LocalDate fechaExpedicion, LocalDate fechaVencimiento) {
        return """
                {
                  "clienteId": %d,
                  "presupuestoId": null,
                  "items": [{"materialId": null, "tareaManual": "Línea", "cantidad": 1.0, "precioUnitario": 10.0, "aplicaIva": true}],
                  "numeroFactura": null,
                  "fechaExpedicion": "%s",
                  "fechaOperacion": null,
                  "fechaVencimiento": "%s",
                  "regimenFiscal": null,
                  "condicionesPago": null,
                  "metodoPago": "Transferencia",
                  "montoCobrado": null,
                  "notas": null,
                  "ivaHabilitado": true
                }
                """.formatted(clienteId, fechaExpedicion, fechaVencimiento);
    }
}
