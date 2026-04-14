package com.appgestion.api.integration.validation;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.integration.facturacion.FacturaIntegrationTestSupport;
import com.appgestion.api.integration.facturacion.FacturacionAuth;
import com.appgestion.api.repository.ClienteRepository;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.repository.OrganizationRepository;
import com.appgestion.api.repository.UsuarioRepository;
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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Validación de entrada en flujos de cliente y datos relacionados (empresa/cobro).
 * <p>
 * El formato de NIF del cliente se valida al emitir factura
 * ({@link com.appgestion.api.service.FacturaService#validarDatosFactura}), no en {@code PUT .../completar}.
 */
@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Transactional
class ValidacionClienteTest {

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
    void crearProvisional_sinNombre_devuelve400() throws Exception {
        mockMvc.perform(post("/clientes/provisional")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearCliente_sinNombre_devuelve400() throws Exception {
        mockMvc.perform(post("/clientes")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "   ",
                                  "telefono": null,
                                  "email": null,
                                  "direccion": null,
                                  "codigoPostal": null,
                                  "provincia": null,
                                  "pais": null,
                                  "dni": null
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearCliente_emailNoValido_devuelve400() throws Exception {
        mockMvc.perform(post("/clientes")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Test email formato",
                                  "telefono": null,
                                  "email": "no-es-un-email",
                                  "direccion": null,
                                  "codigoPostal": null,
                                  "provincia": null,
                                  "pais": null,
                                  "dni": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("El email no tiene formato válido")));
    }

    @Test
    void emitirFactura_conNifClienteInvalido_devuelve400() throws Exception {
        mockMvc.perform(put("/clientes/{id}", scenario.clienteCompletoId())
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Cliente NIF mal",
                                  "telefono": null,
                                  "email": null,
                                  "direccion": "Calle X 1",
                                  "codigoPostal": "28001",
                                  "provincia": "Madrid",
                                  "pais": "España",
                                  "dni": "XXXX"
                                }
                                """))
                .andExpect(status().isOk());

        String body = facturaJson(scenario.clienteCompletoId(), java.time.LocalDate.now());
        mockMvc.perform(post("/facturas")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("NIF del cliente no es válido")));
    }

    @Test
    void patchMetodosCobro_ibanInvalido_devuelve400() throws Exception {
        mockMvc.perform(patch("/config/empresa/metodos-cobro")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ibanCuenta\": \"ES00INVALID\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("IBAN")));
    }

    @Test
    void completarCliente_codigoPostalAlfanumerico_devuelve400() throws Exception {
        String created = mockMvc.perform(post("/clientes/provisional")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Prov CP test\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long id = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(put("/clientes/{id}/completar", id)
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Prov CP test",
                                  "dni": "00000001R",
                                  "direccion": "Calle Y 2",
                                  "codigoPostal": "ABC",
                                  "telefono": null,
                                  "email": null,
                                  "pais": "España",
                                  "provincia": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("El código postal debe tener 5 dígitos")));
    }

    private static String facturaJson(long clienteId, java.time.LocalDate fechaExpedicion) {
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
                  "montoCobrado": null,
                  "notas": null,
                  "ivaHabilitado": true
                }
                """.formatted(clienteId, fechaExpedicion);
    }
}
