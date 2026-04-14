package com.appgestion.api.integration.facturacion;

import com.appgestion.api.AppGestionApiApplication;
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
import java.time.Year;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Transactional
class FacturaNumeroCorrelativoTest {

    private static final Pattern FAC_FORMAT = Pattern.compile("^FAC-\\d{4}-\\d{4}$");

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
    void tresFacturasConsecutivas_tienenNumerosCorrelativosYFormatoFac() throws Exception {
        int anio = Year.now().getValue();
        LocalDate fecha = LocalDate.of(anio, 6, 15);
        String body = facturaJson(scenario.clienteCompletoId(), fecha, null);

        int[] secuenciales = new int[3];
        for (int i = 0; i < 3; i++) {
            String content = mockMvc.perform(post("/facturas")
                            .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.numeroFactura").exists())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            JsonNode root = objectMapper.readTree(content);
            String numero = root.get("numeroFactura").asText();
            assertThat(numero).matches(FAC_FORMAT);
            String[] partes = numero.split("-");
            assertThat(partes[0]).isEqualTo("FAC");
            assertThat(Integer.parseInt(partes[1])).isEqualTo(anio);
            secuenciales[i] = Integer.parseInt(partes[2]);
        }
        assertThat(secuenciales[1]).isEqualTo(secuenciales[0] + 1);
        assertThat(secuenciales[2]).isEqualTo(secuenciales[1] + 1);
    }

    @Test
    void crearConNumeroManualDuplicado_devuelve400() throws Exception {
        int anio = Year.now().getValue();
        LocalDate fecha = LocalDate.of(anio, 3, 10);
        String manual = String.format("FAC-%d-0990", anio);

        String primera = facturaJson(scenario.clienteCompletoId(), fecha, manual);
        mockMvc.perform(post("/facturas")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(primera))
                .andExpect(status().isCreated());

        String segunda = facturaJson(scenario.clienteCompletoId(), fecha, manual);
        mockMvc.perform(post("/facturas")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(segunda))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Ya existe una factura")));
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
