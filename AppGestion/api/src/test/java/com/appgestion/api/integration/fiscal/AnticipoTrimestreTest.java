package com.appgestion.api.integration.fiscal;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.constant.PresupuestoEstado;
import com.appgestion.api.domain.enums.TipoFactura;
import com.appgestion.api.integration.presupuesto.PresupuestoIntegrationAuth;
import com.appgestion.api.integration.presupuesto.PresupuestoIntegrationTestSupport;
import com.appgestion.api.repository.ClienteRepository;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.repository.OrganizationRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
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
import java.time.Month;

import org.assertj.core.data.Offset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Transactional
class AnticipoTrimestreTest {

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
    private PresupuestoIntegrationTestSupport.Scenario scenario;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        scenario = PresupuestoIntegrationTestSupport.seed(
                organizationRepository, usuarioRepository, clienteRepository, empresaRepository);
    }

    /**
     * La factura final toma la fecha de expedición de hoy; el anticipo queda en T1 (enero).
     * Solo si hoy cae en T2 (abr–jun) el remanente va a un trimestre distinto sin tocar BD.
     */
    @Test
    void ivaAnticipoEnT1_ivaFinalEnT2_sinDuplicarAnticipoEnT2() throws Exception {
        LocalDate hoy = LocalDate.now();
        Assumptions.assumeTrue(
                hoy.getMonthValue() >= Month.APRIL.getValue()
                        && hoy.getMonthValue() <= Month.JUNE.getValue(),
                "Reparto T1/T2: requiere ejecutar en abr–jun (factura final con fecha de hoy en T2).");

        int year = hoy.getYear();
        LocalDate fechaAnticipo = LocalDate.of(year, 1, 15);

        FlujoAnticipo f = ejecutarFlujoAnticipoYFinal(fechaAnticipo);
        double ivaT1 = ivaModelo303(year, 1);
        double ivaT2 = ivaModelo303(year, 2);
        double ivaT3 = ivaModelo303(year, 3);
        double ivaT4 = ivaModelo303(year, 4);

        assertThat(ivaT1 + ivaT2 + ivaT3 + ivaT4).isCloseTo(f.ivaPresupuesto(), Offset.offset(0.05));
        assertThat(ivaT3).isZero();
        assertThat(ivaT4).isZero();

        assertThat(ivaT1).isCloseTo(f.ivaFacturaAnticipo(), Offset.offset(0.05));
        assertThat(ivaT2).isCloseTo(f.ivaFacturaFinal(), Offset.offset(0.05));
        assertThat(ivaT1 + ivaT2).isCloseTo(f.ivaPresupuesto(), Offset.offset(0.05));
    }

    @Test
    void sumaIvaCuatroTrimestres_coincideConIvaTotalPresupuesto() throws Exception {
        LocalDate hoy = LocalDate.now();
        int year = hoy.getYear();
        LocalDate fechaAnticipo = LocalDate.of(year, 1, 15);

        FlujoAnticipo f = ejecutarFlujoAnticipoYFinal(fechaAnticipo);

        double suma = 0.0;
        for (int t = 1; t <= 4; t++) {
            suma += ivaModelo303(year, t);
        }
        assertThat(suma).isCloseTo(f.ivaPresupuesto(), Offset.offset(0.05));
    }

    private record FlujoAnticipo(double ivaPresupuesto, double ivaFacturaAnticipo, double ivaFacturaFinal) {
    }

    /**
     * Ejecuta presupuesto → anticipo (fecha en T1) → factura anticipo → factura final (fecha hoy).
     */
    private FlujoAnticipo ejecutarFlujoAnticipoYFinal(LocalDate fechaAnticipo) throws Exception {
        String presBody = PresupuestoIntegrationTestSupport.presupuestoJson(
                scenario.clienteCompletoId(), PresupuestoEstado.ACEPTADO, 1000.0, 1.0);
        String presJson = mockMvc.perform(post("/presupuestos")
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(presBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode presNode = objectMapper.readTree(presJson);
        long presId = presNode.get("id").asLong();
        double ivaPres = presNode.get("iva").asDouble();

        String anticipoBody = """
                {"importeAnticipo": 300, "fechaAnticipo": "%s"}
                """.formatted(fechaAnticipo);

        mockMvc.perform(post("/presupuestos/{id}/anticipo", presId)
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(anticipoBody))
                .andExpect(status().isOk());

        String faJson = mockMvc.perform(post("/presupuestos/{id}/factura-anticipo", presId)
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoFactura").value(TipoFactura.ANTICIPO.name()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        double ivaAnt = objectMapper.readTree(faJson).get("iva").asDouble();

        String ffJson = mockMvc.perform(post("/presupuestos/{id}/factura-final", presId)
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoFactura").value(TipoFactura.FINAL_CON_ANTICIPO.name()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        double ivaFinal = objectMapper.readTree(ffJson).get("iva").asDouble();

        BigDecimal esperado = BigDecimal.valueOf(ivaAnt + ivaFinal).setScale(2, RoundingMode.HALF_UP);
        assertThat(BigDecimal.valueOf(ivaPres).setScale(2, RoundingMode.HALF_UP)).isEqualByComparingTo(esperado);

        return new FlujoAnticipo(ivaPres, ivaAnt, ivaFinal);
    }

    private double ivaModelo303(int year, int trimestre) throws Exception {
        String m303 = mockMvc.perform(get("/fiscal/modelo303")
                        .param("year", String.valueOf(year))
                        .param("trimestre", String.valueOf(trimestre))
                        .param("criterio", "DEVENGO")
                        .param("soloPagadas", "false")
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return new BigDecimal(objectMapper.readTree(m303).get("ivaRepercutido").asText())
                .doubleValue();
    }
}
