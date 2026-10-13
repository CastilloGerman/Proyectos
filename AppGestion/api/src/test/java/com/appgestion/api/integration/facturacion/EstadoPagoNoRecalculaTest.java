package com.appgestion.api.integration.facturacion;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.domain.entity.Factura;
import com.appgestion.api.domain.entity.FacturaItem;
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
class EstadoPagoNoRecalculaTest {

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
    void cambiarEstado_noTocaSubtotalIvaNiTotal() throws Exception {
        String creada = mockMvc.perform(post("/facturas")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(facturaDeCien(scenario.clienteCompletoId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(creada).get("id").asLong();

        Factura guardada = facturaRepository.findById(id).orElseThrow();
        double cuotaLinea = guardada.getItems().get(0).getCuotaIva();
        guardada.setSubtotal(50.0);
        guardada.setIva(77.77);
        guardada.setTotal(177.77);
        facturaRepository.saveAndFlush(guardada);

        String body = """
                {"estadoPago": "Pagada", "fechaCobro": "2026-01-15"}
                """;
        String actualizada = mockMvc.perform(patch("/facturas/{id}/estado-pago", id)
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPago").value("Pagada"))
                .andExpect(jsonPath("$.subtotal").value(50.0))
                .andExpect(jsonPath("$.iva").value(77.77))
                .andExpect(jsonPath("$.total").value(177.77))
                .andExpect(jsonPath("$.cobros[0].fecha").value("2026-01-15"))
                .andReturn().getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(actualizada);
        assertThat(root.get("items")).hasSize(1);
        assertThat(root.get("cobros").get(0).get("importe").asDouble()).isEqualTo(177.77);

        Factura recargada = facturaRepository.findById(id).orElseThrow();
        assertThat(recargada.getSubtotal()).isEqualTo(50.0);
        assertThat(recargada.getIva()).isEqualTo(77.77);
        assertThat(recargada.getTotal()).isEqualTo(177.77);
        FacturaItem linea = recargada.getItems().get(0);
        assertThat(linea.getCuotaIva()).isEqualTo(cuotaLinea);
        assertThat(linea.getSubtotal()).isEqualTo(100.0);
    }

    private static String facturaDeCien(long clienteId) {
        return """
                {
                  "clienteId": %d,
                  "items": [{"materialId": null, "tareaManual": "Línea", "cantidad": 1.0, "precioUnitario": 100.0, "aplicaIva": true}],
                  "fechaExpedicion": "%s",
                  "metodoPago": "Transferencia",
                  "ivaHabilitado": true
                }
                """.formatted(clienteId, LocalDate.of(2026, 2, 10));
    }
}
