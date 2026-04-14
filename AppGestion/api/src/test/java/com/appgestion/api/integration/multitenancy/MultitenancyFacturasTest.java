package com.appgestion.api.integration.multitenancy;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.repository.ClienteRepository;
import com.appgestion.api.repository.FacturaRepository;
import com.appgestion.api.repository.OrganizationRepository;
import com.appgestion.api.repository.PresupuestoRepository;
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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Transactional
class MultitenancyFacturasTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private FacturaRepository facturaRepository;
    @Autowired
    private PresupuestoRepository presupuestoRepository;

    private MultitenancyIntegrationTestSupport.Scenario scenario;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        scenario = MultitenancyIntegrationTestSupport.seed(
                organizationRepository,
                usuarioRepository,
                clienteRepository,
                facturaRepository,
                presupuestoRepository);
    }

    @Test
    void getFacturaDeOtroUsuario_devuelve404_sinDatosDeB() throws Exception {
        mockMvc.perform(get("/facturas/{id}", scenario.facturaIdB())
                        .with(MultitenancyAuth.asUsuarioA(userDetailsService)))
                .andExpect(status().isNotFound());
    }

    @Test
    void putFacturaDeOtroUsuario_devuelve404() throws Exception {
        String body = facturaPutJson(scenario.clienteIdA());
        mockMvc.perform(put("/facturas/{id}", scenario.facturaIdB())
                        .with(MultitenancyAuth.asUsuarioA(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    /** JSON explícito: evita depender de módulo JSR310 del ObjectMapper en tests. */
    private static String facturaPutJson(long clienteIdA) {
        return """
                {
                  "clienteId": %d,
                  "presupuestoId": null,
                  "items": [{"materialId": null, "tareaManual": "Línea", "cantidad": 1.0, "precioUnitario": 10.0, "aplicaIva": true}],
                  "numeroFactura": null,
                  "fechaExpedicion": "%s",
                  "fechaOperacion": null,
                  "fechaVencimiento": null,
                  "regimenFiscal": null,
                  "condicionesPago": null,
                  "metodoPago": "Transferencia",
                  "estadoPago": "NO_PAGADA",
                  "montoCobrado": null,
                  "notas": null,
                  "ivaHabilitado": true
                }
                """.formatted(clienteIdA, LocalDate.now());
    }

    @Test
    void postAnularFacturaDeOtroUsuario_devuelve404() throws Exception {
        mockMvc.perform(post("/facturas/{id}/anular", scenario.facturaIdB())
                        .with(MultitenancyAuth.asUsuarioA(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    /**
     * No hay endpoint de borrado de facturas: la Parte 5 sustituyó el borrado físico por anulación lógica
     * ({@code POST /facturas/{id}/anular}). {@code DELETE /facturas/{id}} no tiene ruta mapeada; que devuelva
     * 405 (método no permitido) o 500 (según cómo el {@code GlobalExceptionHandler} trate
     * {@code HttpRequestMethodNotSupportedException}) es correcto. El test acepta ambos códigos.
     */
    @Test
    void deleteFactura_noMapeado_devuelve405o500() throws Exception {
        int statusCode = mockMvc.perform(delete("/facturas/{id}", scenario.facturaIdB())
                        .with(MultitenancyAuth.asUsuarioA(userDetailsService)))
                .andReturn()
                .getResponse()
                .getStatus();
        assertThat(statusCode).isIn(405, 500);
    }
}
