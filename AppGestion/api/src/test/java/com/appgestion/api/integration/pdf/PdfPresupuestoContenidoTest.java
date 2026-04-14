package com.appgestion.api.integration.pdf;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.constant.PresupuestoEstado;
import com.appgestion.api.domain.presupuesto.PresupuestoCondicionCatalogo;
import com.appgestion.api.integration.presupuesto.PresupuestoIntegrationAuth;
import com.appgestion.api.integration.presupuesto.PresupuestoIntegrationTestSupport;
import com.appgestion.api.repository.ClienteRepository;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.repository.OrganizationRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Transactional
class PdfPresupuestoContenidoTest {

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

    @Test
    void pdfPresupuesto_contieneSoloCondicionesActivas() throws Exception {
        String body = """
                {
                  "clienteId": %d,
                  "items": [{"materialId": null, "tareaManual": "Concepto PDF Presupuesto", "cantidad": 1.0, "precioUnitario": 100.0, "aplicaIva": true}],
                  "ivaHabilitado": true,
                  "estado": "%s",
                  "descuentoGlobalPorcentaje": 0.0,
                  "descuentoGlobalFijo": 0.0,
                  "descuentoAntesIva": true,
                  "condicionesActivas": ["validez_30_dias", "garantia_1_anio"],
                  "notaAdicional": null
                }
                """.formatted(scenario.clienteCompletoId(), PresupuestoEstado.PENDIENTE);

        String created = mockMvc.perform(post("/presupuestos")
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = objectMapper.readTree(created).get("id").asLong();
        String text = descargarTextoPresupuestoPdf(id);

        String tValidez = PresupuestoCondicionCatalogo.porClave("validez_30_dias").textoPdf();
        String tGarantia = PresupuestoCondicionCatalogo.porClave("garantia_1_anio").textoPdf();
        String tNoActiva = PresupuestoCondicionCatalogo.porClave("materiales_no_incluidos").textoPdf();

        assertThat(text).contains("Condiciones");
        assertThat(text).contains(tValidez);
        assertThat(text).contains(tGarantia);
        assertThat(text).doesNotContain(tNoActiva);
    }

    @Test
    void pdfPresupuesto_conNotaAdicional_muestraSeccionNotas() throws Exception {
        String body = """
                {
                  "clienteId": %d,
                  "items": [{"materialId": null, "tareaManual": "Concepto con nota", "cantidad": 1.0, "precioUnitario": 90.0, "aplicaIva": true}],
                  "ivaHabilitado": true,
                  "estado": "%s",
                  "descuentoGlobalPorcentaje": 0.0,
                  "descuentoGlobalFijo": 0.0,
                  "descuentoAntesIva": true,
                  "condicionesActivas": ["validez_30_dias"],
                  "notaAdicional": "Trabajos sujetos a disponibilidad de acceso."
                }
                """.formatted(scenario.clienteCompletoId(), PresupuestoEstado.PENDIENTE);

        String created = mockMvc.perform(post("/presupuestos")
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = objectMapper.readTree(created).get("id").asLong();
        String text = descargarTextoPresupuestoPdf(id);

        assertThat(text).contains("Notas");
        assertThat(text).contains("Trabajos sujetos a disponibilidad de acceso.");
    }

    @Test
    void pdfPresupuesto_sinNotaAdicional_noMuestraSeccionNotas() throws Exception {
        String body = """
                {
                  "clienteId": %d,
                  "items": [{"materialId": null, "tareaManual": "Concepto sin nota", "cantidad": 1.0, "precioUnitario": 90.0, "aplicaIva": true}],
                  "ivaHabilitado": true,
                  "estado": "%s",
                  "descuentoGlobalPorcentaje": 0.0,
                  "descuentoGlobalFijo": 0.0,
                  "descuentoAntesIva": true,
                  "condicionesActivas": ["validez_30_dias"],
                  "notaAdicional": ""
                }
                """.formatted(scenario.clienteCompletoId(), PresupuestoEstado.PENDIENTE);

        String created = mockMvc.perform(post("/presupuestos")
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = objectMapper.readTree(created).get("id").asLong();
        String text = descargarTextoPresupuestoPdf(id);

        assertThat(text).doesNotContain("Notas");
    }

    private String descargarTextoPresupuestoPdf(long presupuestoId) throws Exception {
        MvcResult result = mockMvc.perform(get("/presupuestos/{id}/pdf", presupuestoId)
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService)))
                .andExpect(status().isOk())
                .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        return normalize(extractPdfText(pdfBytes));
    }

    private static String extractPdfText(byte[] pdfBytes) throws Exception {
        try (PdfReader reader = new PdfReader(pdfBytes)) {
            StringBuilder out = new StringBuilder();
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                out.append(extractor.getTextFromPage(i)).append('\n');
            }
            return out.toString();
        }
    }

    private static String normalize(String text) {
        return new String(text.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }
}
