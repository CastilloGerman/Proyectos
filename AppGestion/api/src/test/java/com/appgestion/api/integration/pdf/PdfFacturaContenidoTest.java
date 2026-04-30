package com.appgestion.api.integration.pdf;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.constant.PresupuestoEstado;
import com.appgestion.api.integration.facturacion.FacturaIntegrationTestSupport;
import com.appgestion.api.integration.facturacion.FacturacionAuth;
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
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Transactional
class PdfFacturaContenidoTest {

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
    private FacturaIntegrationTestSupport.Scenario factScenario;
    private PresupuestoIntegrationTestSupport.Scenario presScenario;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        factScenario = FacturaIntegrationTestSupport.seed(
                organizationRepository, usuarioRepository, clienteRepository, empresaRepository);
        presScenario = PresupuestoIntegrationTestSupport.seed(
                organizationRepository, usuarioRepository, clienteRepository, empresaRepository);
    }

    @Test
    void pdfFactura_contieneFechaExpedicion_formatoDdMmYyyy() throws Exception {
        LocalDate fecha = LocalDate.of(2026, 4, 14);
        long facturaId = crearFacturaBasica(factScenario.clienteCompletoId(), fecha);

        String text = descargarTextoFacturaPdf(facturaId);

        assertThat(text).contains("Fecha de expedición:");
        assertThat(text).contains("14/04/2026");
    }

    @Test
    void pdfFactura_noContieneEstadoNiNoPagada() throws Exception {
        long facturaId = crearFacturaBasica(factScenario.clienteCompletoId(), LocalDate.of(2026, 5, 5));

        String text = descargarTextoFacturaPdf(facturaId);

        assertThat(text).doesNotContain("Estado:");
        assertThat(text).doesNotContain("No Pagada");
    }

    @Test
    void pdfFactura_conIban_muestraSeccionDatosBancarios() throws Exception {
        mockMvc.perform(patch("/config/empresa/metodos-cobro")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ibanCuenta": "ES9121000418450200051332",
                                  "titularCuenta": "Autonomo B8",
                                  "nombreBanco": "Banco Demo"
                                }
                                """))
                .andExpect(status().isOk());

        long facturaId = crearFacturaBasica(factScenario.clienteCompletoId(), LocalDate.now());

        String text = descargarTextoFacturaPdf(facturaId);

        assertThat(text).contains("DATOS BANCARIOS");
        assertThat(text).contains("IBAN");
    }

    @Test
    void pdfFactura_sinIban_noMuestraSeccionDatosBancarios() throws Exception {
        long facturaId = crearFacturaBasica(factScenario.clienteCompletoId(), LocalDate.now());

        String text = descargarTextoFacturaPdf(facturaId);

        assertThat(text).doesNotContain("DATOS BANCARIOS");
    }

    @Test
    void pdfFacturaFinalConAnticipo_contieneReferenciaAnticipo() throws Exception {
        String presBody = PresupuestoIntegrationTestSupport.presupuestoJson(
                presScenario.clienteCompletoId(), PresupuestoEstado.ACEPTADO, 1000.0, 1.0);
        String presJson = mockMvc.perform(post("/presupuestos")
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(presBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long presId = objectMapper.readTree(presJson).get("id").asLong();

        mockMvc.perform(post("/presupuestos/{id}/anticipo", presId)
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"importeAnticipo": 300, "fechaAnticipo": "2026-04-10"}
                                """))
                .andExpect(status().isOk());

        String faJson = mockMvc.perform(post("/presupuestos/{id}/factura-anticipo", presId)
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String referenciaAnticipo = objectMapper.readTree(faJson).path("numeroFactura").asText("");

        String ffJson = mockMvc.perform(post("/presupuestos/{id}/factura-final", presId)
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long facturaFinalId = objectMapper.readTree(ffJson).get("id").asLong();

        String text = descargarTextoFacturaPdfConAuthPresupuestos(facturaFinalId);

        assertThat(text).contains("Anticipo ya facturado");
        assertThat(text).contains(referenciaAnticipo);
    }

    private long crearFacturaBasica(long clienteId, LocalDate fechaExpedicion) throws Exception {
        String response = mockMvc.perform(post("/facturas")
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(facturaJson(clienteId, fechaExpedicion)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private String descargarTextoFacturaPdf(long facturaId) throws Exception {
        MvcResult result = mockMvc.perform(get("/facturas/{id}/pdf", facturaId)
                        .with(FacturacionAuth.asUsuarioFacturacion(userDetailsService)))
                .andExpect(status().isOk())
                .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        return normalize(extractPdfText(pdfBytes));
    }

    private String descargarTextoFacturaPdfConAuthPresupuestos(long facturaId) throws Exception {
        MvcResult result = mockMvc.perform(get("/facturas/{id}/pdf", facturaId)
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService)))
                .andExpect(status().isOk())
                .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        return normalize(extractPdfText(pdfBytes));
    }

    private static String facturaJson(long clienteId, LocalDate fechaExpedicion) {
        return """
                {
                  "clienteId": %d,
                  "presupuestoId": null,
                  "items": [{"materialId": null, "tareaManual": "Servicio PDF B8", "cantidad": 1.0, "precioUnitario": 200.0, "aplicaIva": true}],
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
