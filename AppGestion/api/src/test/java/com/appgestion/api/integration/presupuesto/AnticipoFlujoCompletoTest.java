package com.appgestion.api.integration.presupuesto;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.constant.FacturaEstadoPago;
import com.appgestion.api.constant.PresupuestoEstado;
import com.appgestion.api.domain.enums.TipoFactura;
import com.appgestion.api.repository.ClienteRepository;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.repository.OrganizationRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

import org.assertj.core.data.Offset;

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
class AnticipoFlujoCompletoTest {

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
    void flujoAnticipo_facturaFinal_yModelo303SinDuplicarIva() throws Exception {
        LocalDate fechaAnticipo = LocalDate.now();

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

        long presId = objectMapper.readTree(presJson).get("id").asLong();
        double totalPres = objectMapper.readTree(presJson).get("total").asDouble();

        String anticipoBody = """
                {"importeAnticipo": 300, "fechaAnticipo": "%s"}
                """.formatted(fechaAnticipo);

        String presTrasAnticipo = mockMvc.perform(post("/presupuestos/{id}/anticipo", presId)
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(anticipoBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tieneAnticipo").value(true))
                .andExpect(jsonPath("$.importeAnticipo").value(300.0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(objectMapper.readTree(presTrasAnticipo).get("anticipoFacturado").asBoolean()).isFalse();

        String faJson = mockMvc.perform(post("/presupuestos/{id}/factura-anticipo", presId)
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoFactura").value(TipoFactura.ANTICIPO.name()))
                .andExpect(jsonPath("$.estadoPago").value(FacturaEstadoPago.PAGADA))
                .andReturn()
                .getResponse()
                .getContentAsString();

        double ivaAnt = objectMapper.readTree(faJson).get("iva").asDouble();

        mockMvc.perform(post("/presupuestos/{id}/factura-anticipo", presId)
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("ya fue generada")));

        String ffJson = mockMvc.perform(post("/presupuestos/{id}/factura-final", presId)
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoFactura").value(TipoFactura.FINAL_CON_ANTICIPO.name()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(ffJson);
        double totalFinal = root.get("total").asDouble();
        double ivaFinal = root.get("iva").asDouble();

        // Remanente bruto = total presupuesto − anticipo; total de la factura final = remanente − anticipo ya cobrado (pendiente de pago).
        assertThat(totalFinal + 300.0).isCloseTo(totalPres - 300.0, Offset.offset(0.02));

        LocalDate hoy = LocalDate.now();
        int year = hoy.getYear();
        int trimestre = (hoy.getMonthValue() - 1) / 3 + 1;

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

        BigDecimal ivaRep = new BigDecimal(objectMapper.readTree(m303).get("ivaRepercutido").asText())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal esperado = BigDecimal.valueOf(ivaAnt + ivaFinal).setScale(2, RoundingMode.HALF_UP);
        assertThat(ivaRep).isEqualByComparingTo(esperado);
    }

    /**
     * La factura final guarda el precio a 4 decimales. Editar solo las notas
     * compara cantidad y precio a esa escala y no vuelve a calcular la base.
     */
    @Test
    void editarSoloLasNotasDeLaFacturaFinal_conservaSubtotalIvaYTotal() throws Exception {
        String presJson = mockMvc.perform(post("/presupuestos")
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PresupuestoIntegrationTestSupport.presupuestoJson(
                                scenario.clienteCompletoId(), PresupuestoEstado.ACEPTADO, 100.0, 3.0)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long presId = objectMapper.readTree(presJson).get("id").asLong();

        mockMvc.perform(post("/presupuestos/{id}/anticipo", presId)
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"importeAnticipo\": 100, \"fechaAnticipo\": \"%s\"}".formatted(LocalDate.now())))
                .andExpect(status().isOk());

        JsonNode factura = objectMapper.readTree(mockMvc.perform(post("/presupuestos/{id}/factura-final", presId)
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());

        JsonNode linea = factura.get("items").get(0);
        BigDecimal precio = linea.get("precioUnitario").decimalValue();
        assertThat(precio.setScale(4, RoundingMode.HALF_UP))
                .isNotEqualByComparingTo(precio.setScale(2, RoundingMode.HALF_UP));

        ObjectNode body = objectMapper.createObjectNode();
        body.put("clienteId", factura.get("clienteId").asLong());
        body.put("presupuestoId", factura.get("presupuestoId").asLong());
        body.put("numeroFactura", factura.get("numeroFactura").asText());
        body.put("fechaExpedicion", factura.get("fechaExpedicion").asText());
        body.put("metodoPago", factura.path("metodoPago").asText("Transferencia"));
        body.put("estadoPago", factura.path("estadoPago").asText("No Pagada"));
        body.put("notas", "Nota sin tocar el anticipo");
        body.put("ivaHabilitado", true);
        ArrayNode items = body.putArray("items");
        ObjectNode item = items.addObject();
        item.put("id", linea.get("id").asLong());
        item.put("tareaManual", linea.path("descripcion").asText());
        item.put("cantidad", linea.get("cantidad").decimalValue());
        item.put("precioUnitario", precio);
        item.put("aplicaIva", true);
        item.put("tipoIva", linea.path("tipoIva").asInt(21));

        JsonNode actualizado = objectMapper.readTree(mockMvc.perform(put("/facturas/{id}", factura.get("id").asLong())
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

        assertThat(actualizado.get("notas").asText()).isEqualTo("Nota sin tocar el anticipo");
        assertThat(actualizado.get("items").get(0).get("subtotal").decimalValue())
                .isEqualByComparingTo(linea.get("subtotal").decimalValue());
        assertThat(actualizado.get("subtotal").decimalValue()).isEqualByComparingTo(factura.get("subtotal").decimalValue());
        assertThat(actualizado.get("iva").decimalValue()).isEqualByComparingTo(factura.get("iva").decimalValue());
        assertThat(actualizado.get("total").decimalValue()).isEqualByComparingTo(factura.get("total").decimalValue());
    }
}
