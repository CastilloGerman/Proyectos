package com.appgestion.api.integration.validation;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.constant.PresupuestoEstado;
import com.appgestion.api.integration.presupuesto.PresupuestoIntegrationAuth;
import com.appgestion.api.integration.presupuesto.PresupuestoIntegrationTestSupport;
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

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Transactional
class ValidacionPresupuestoTest {

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
    private PresupuestoIntegrationTestSupport.Scenario scenario;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        scenario = PresupuestoIntegrationTestSupport.seed(
                organizationRepository, usuarioRepository, clienteRepository, empresaRepository);
    }

    @Test
    void crearPresupuesto_sinLineas_devuelve400() throws Exception {
        mockMvc.perform(post("/presupuestos")
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clienteId": %d,
                                  "items": [],
                                  "ivaHabilitado": true,
                                  "estado": "%s",
                                  "descuentoGlobalPorcentaje": 0.0,
                                  "descuentoGlobalFijo": 0.0,
                                  "descuentoAntesIva": true,
                                  "condicionesActivas": [],
                                  "notaAdicional": null
                                }
                                """.formatted(scenario.clienteCompletoId(), PresupuestoEstado.PENDIENTE)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearPresupuesto_cantidadNegativa_devuelve400() throws Exception {
        mockMvc.perform(post("/presupuestos")
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(presupuestoUnItem(scenario.clienteCompletoId(), -1.0, 10.0)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearPresupuesto_precioUnitarioNegativo_devuelve400() throws Exception {
        mockMvc.perform(post("/presupuestos")
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(presupuestoUnItem(scenario.clienteCompletoId(), 1.0, -5.0)))
                .andExpect(status().isBadRequest());
    }

    private static String presupuestoUnItem(long clienteId, double cantidad, double precioUnitario) {
        return """
                {
                  "clienteId": %d,
                  "items": [{"materialId": null, "tareaManual": "Concepto", "cantidad": %s, "precioUnitario": %s, "aplicaIva": true}],
                  "ivaHabilitado": true,
                  "estado": "%s",
                  "descuentoGlobalPorcentaje": 0.0,
                  "descuentoGlobalFijo": 0.0,
                  "descuentoAntesIva": true,
                  "condicionesActivas": [],
                  "notaAdicional": null
                }
                """.formatted(clienteId, Double.toString(cantidad), Double.toString(precioUnitario), PresupuestoEstado.PENDIENTE);
    }
}
