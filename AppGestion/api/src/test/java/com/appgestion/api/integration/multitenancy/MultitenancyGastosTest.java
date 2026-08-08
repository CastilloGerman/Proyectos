package com.appgestion.api.integration.multitenancy;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.domain.entity.Gasto;
import com.appgestion.api.domain.enums.GastoCategoria;
import com.appgestion.api.repository.ClienteRepository;
import com.appgestion.api.repository.FacturaRepository;
import com.appgestion.api.repository.GastoRepository;
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

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Transactional
class MultitenancyGastosTest {

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
    @Autowired
    private GastoRepository gastoRepository;

    private MultitenancyIntegrationTestSupport.Scenario scenario;
    private Long gastoIdB;

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

        Gasto gastoB = new Gasto();
        gastoB.setUsuario(scenario.usuarioB());
        gastoB.setProveedor("Proveedor B");
        gastoB.setConcepto("Gasto de B");
        gastoB.setFecha(LocalDate.now());
        gastoB.setBaseImponible(200.0);
        gastoB.setTipoIva(21.0);
        gastoB.setCuotaIva(42.0);
        gastoB.setCategoria(GastoCategoria.SUMINISTROS);
        gastoB = gastoRepository.save(gastoB);
        gastoIdB = gastoB.getId();
    }

    @Test
    void getGastoDeOtroUsuario_devuelve404() throws Exception {
        mockMvc.perform(get("/gastos/{id}", gastoIdB)
                        .with(MultitenancyAuth.asUsuarioA(userDetailsService)))
                .andExpect(status().isNotFound());
    }

    @Test
    void putGastoDeOtroUsuario_devuelve404() throws Exception {
        mockMvc.perform(put("/gastos/{id}", gastoIdB)
                        .with(MultitenancyAuth.asUsuarioA(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gastoPutJson()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteGastoDeOtroUsuario_devuelve404() throws Exception {
        mockMvc.perform(delete("/gastos/{id}", gastoIdB)
                        .with(MultitenancyAuth.asUsuarioA(userDetailsService)))
                .andExpect(status().isNotFound());
    }

    private static String gastoPutJson() {
        return """
                {
                  "proveedor": "Proveedor edit",
                  "concepto": "Concepto edit",
                  "fecha": "%s",
                  "baseImponible": 150.0,
                  "tipoIva": 21.0,
                  "categoria": "MATERIAL"
                }
                """.formatted(LocalDate.now());
    }
}
