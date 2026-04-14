package com.appgestion.api.integration.cliente;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.integration.facturacion.FacturaIntegrationTestSupport;
import com.appgestion.api.integration.facturacion.FacturacionAuth;
import com.appgestion.api.repository.ClienteRepository;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.repository.OrganizationRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.service.ClienteService;
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
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Transactional
class ClienteProvisionalFlujosTest {

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

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        FacturaIntegrationTestSupport.seed(
                organizationRepository, usuarioRepository, clienteRepository, empresaRepository);
    }

    @Test
    void crearProvisional_soloNombre_estadoProvisional() throws Exception {
        mockMvc.perform(post("/clientes/provisional")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Cliente Provisional B8\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoCliente").value("PROVISIONAL"));
    }

    @Test
    void listar_incluyeClienteProvisional_conEstadoProvisional() throws Exception {
        String created = mockMvc.perform(post("/clientes/provisional")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Badge fiscal B8\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long id = objectMapper.readTree(created).get("id").asLong();

        String listado = mockMvc.perform(get("/clientes")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode arr = objectMapper.readTree(listado);
        JsonNode match = null;
        for (JsonNode n : arr) {
            if (n.get("id").asLong() == id) {
                match = n;
                break;
            }
        }
        assertThat(match).isNotNull();
        if (match == null) {
            throw new AssertionError("No se encontro el cliente provisional en el listado");
        }
        assertThat(match.get("estadoCliente").asText()).isEqualTo("PROVISIONAL");
    }

    @Test
    void completarCliente_conDniYDireccion_pasaACompleto() throws Exception {
        String created = mockMvc.perform(post("/clientes/provisional")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Completar B8\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoCliente").value("PROVISIONAL"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long id = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(put("/clientes/{id}/completar", id)
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Completar B8",
                                  "dni": "00000003A",
                                  "direccion": "Calle Cliente 8",
                                  "codigoPostal": "28008",
                                  "telefono": null,
                                  "email": null,
                                  "pais": "España",
                                  "provincia": "Madrid"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoCliente").value("COMPLETO"));
    }

    @Test
    void facturarClienteProvisional_devuelve400() throws Exception {
        String created = mockMvc.perform(post("/clientes/provisional")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"No facturable B8\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long provisionalId = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(post("/facturas")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(facturaJson(provisionalId, LocalDate.now())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString(ClienteService.MSG_FACTURA_CLIENTE_INCOMPLETO)));
    }

    private static String facturaJson(long clienteId, LocalDate fechaExpedicion) {
        return """
                {
                  "clienteId": %d,
                  "presupuestoId": null,
                  "items": [{"materialId": null, "tareaManual": "Línea B8", "cantidad": 1.0, "precioUnitario": 10.0, "aplicaIva": true}],
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
                """.formatted(clienteId, fechaExpedicion);
    }
}
