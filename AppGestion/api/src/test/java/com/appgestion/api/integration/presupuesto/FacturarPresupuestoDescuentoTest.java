package com.appgestion.api.integration.presupuesto;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.constant.PresupuestoEstado;
import com.appgestion.api.repository.ClienteRepository;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.repository.OrganizationRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Transactional
class FacturarPresupuestoDescuentoTest {

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
    private long clienteId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        clienteId = PresupuestoIntegrationTestSupport.seed(
                organizationRepository, usuarioRepository, clienteRepository, empresaRepository).clienteCompletoId();
    }

    @Test
    void descuentoDeLinea_laFacturaCuadraAlCentimo() throws Exception {
        String body = presupuesto(
                linea("Trabajo", "1.0", "100.00", true, "10.0", "0.0"),
                "0.0", "0.0", true);
        Emitido emitido = emitir(body);

        assertEuros(emitido.presupuesto(), "90.00", "18.90", "108.90");
        assertFactura(emitido.factura(), "90.00", "18.90", "108.90");
        assertLinea(emitido.factura(), 0, "90.00", "90.00", 21);
        assertThat(euros(emitido.factura(), "total")).isEqualByComparingTo(euros(emitido.presupuesto(), "total"));
    }

    @Test
    void descuentoGlobalPorcentajeAntesDeIva_laFacturaCuadraAlCentimo() throws Exception {
        String body = presupuesto(
                linea("Trabajo", "1.0", "200.00", true, "0.0", "0.0"),
                "10.0", "0.0", true);
        Emitido emitido = emitir(body);

        assertEuros(emitido.presupuesto(), "180.00", "37.80", "217.80");
        assertFactura(emitido.factura(), "180.00", "37.80", "217.80");
        assertLinea(emitido.factura(), 0, "180.00", "180.00", 21);
    }

    @Test
    void descuentoGlobalFijoAntesDeIva_elSobrantePasaALasLineasAlCero() throws Exception {
        String body = presupuesto(
                linea("Gravado", "1.0", "40.00", true, "0.0", "0.0")
                        + "," + linea("Exento", "1.0", "100.00", false, "0.0", "0.0"),
                "0.0", "50.00", true);
        Emitido emitido = emitir(body);

        assertEuros(emitido.presupuesto(), "90.00", "0.00", "90.00");
        assertFactura(emitido.factura(), "90.00", "0.00", "90.00");
        assertLinea(emitido.factura(), 0, "0.00", "0.00", 21);
        assertLinea(emitido.factura(), 1, "90.00", "90.00", 0);
    }

    /**
     * Sigue omitido. Pendiente de decisión fiscal: si el descuento es posterior al IVA,
     * la base y la cuota no cambian y solo resta del total.
     */
    @Test
    @Disabled("Pendiente de decisión fiscal: si el descuento es posterior al IVA, la base y la cuota no cambian y solo resta del total")
    void descuentoPosteriorAlIva_noCambiaBaseNiCuota() throws Exception {
        String body = presupuesto(
                linea("Trabajo", "1.0", "100.00", true, "0.0", "0.0"),
                "10.0", "0.0", false);
        Emitido emitido = emitir(body);

        assertEuros(emitido.presupuesto(), "90.00", "21.00", "111.00");
        assertFactura(emitido.factura(), "100.00", "21.00", "111.00");
        assertLinea(emitido.factura(), 0, "100.00", "100.00", 21);
        assertThat(euros(emitido.factura(), "total")).isEqualByComparingTo(euros(emitido.presupuesto(), "total"));
        assertThat(euros(emitido.factura(), "subtotal")).isEqualByComparingTo("100.00");
        assertThat(euros(emitido.factura(), "iva")).isEqualByComparingTo("21.00");
    }

    @Test
    void presupuestoConLineasAl21YAl0_reparteElPorcentaje() throws Exception {
        String body = presupuesto(
                linea("Gravado", "1.0", "100.00", true, "0.0", "0.0")
                        + "," + linea("Exento", "1.0", "50.00", false, "0.0", "0.0"),
                "10.0", "0.0", true);
        Emitido emitido = emitir(body);

        assertEuros(emitido.presupuesto(), "135.00", "18.90", "153.90");
        assertFactura(emitido.factura(), "135.00", "18.90", "153.90");
        assertLinea(emitido.factura(), 0, "90.00", "90.00", 21);
        assertLinea(emitido.factura(), 1, "45.00", "45.00", 0);
    }

    @Test
    void descuentoFijoMayorQueLaLineaPequena_noDejaBaseNegativa() throws Exception {
        String body = presupuesto(
                linea("Pequeña", "1.0", "5.00", true, "0.0", "0.0")
                        + "," + linea("Grande", "1.0", "100.00", true, "0.0", "0.0"),
                "0.0", "30.00", true);
        Emitido emitido = emitir(body);

        assertEuros(emitido.presupuesto(), "75.00", "15.75", "90.75");
        assertFactura(emitido.factura(), "75.00", "15.75", "90.75");
        assertLinea(emitido.factura(), 0, "3.57", "3.57", 21);
        assertLinea(emitido.factura(), 1, "71.43", "71.43", 21);
        assertThat(euros(emitido.factura().get("items").get(0), "subtotal")).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(euros(emitido.factura().get("items").get(1), "subtotal")).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    /**
     * precio_unitario es DOUBLE PRECISION, sin escala de céntimos. El subtotal se guarda;
     * calcularTotales lo lee y no lo vuelve a sacar de cantidad × precio.
     * 3 × 33,33 = 99,99 y el fijo de 1,00 deja 98,99, que no es divisible por 3 a céntimo
     * (3 × 32,99 = 98,97; 3 × 33,00 = 99,00). Se conserva el precio original y la base neta
     * queda solo en el subtotal.
     */
    @Test
    void cantidad3Precio3333YFijo1_conservaElPrecioYGuardaLaBaseNeta() throws Exception {
        String body = presupuesto(
                linea("Trabajo", "3.0", "33.33", true, "0.0", "0.0"),
                "0.0", "1.00", true);
        Emitido emitido = emitir(body);

        assertEurosACentimo(emitido.presupuesto(), "98.99", "20.79", "119.78");
        assertFactura(emitido.factura(), "98.99", "20.79", "119.78");
        JsonNode linea = emitido.factura().get("items").get(0);
        assertThat(linea.get("cantidad").decimalValue()).isEqualByComparingTo("3");
        assertThat(euros(linea, "precioUnitario")).isEqualByComparingTo("33.33");
        assertThat(euros(linea, "subtotal")).isEqualByComparingTo("98.99");
        assertThat(linea.get("tipoIva").asInt()).isEqualTo(21);
    }

    @Test
    void descuentoDeLineaYGlobal_laFacturaCuadraAlCentimo() throws Exception {
        String body = presupuesto(
                linea("Trabajo", "1.0", "100.00", true, "10.0", "0.0"),
                "10.0", "0.0", true);
        Emitido emitido = emitir(body);

        assertEurosACentimo(emitido.presupuesto(), "81.00", "17.01", "98.01");
        assertFactura(emitido.factura(), "81.00", "17.01", "98.01");
        assertLinea(emitido.factura(), 0, "81.00", "81.00", 21);
    }

    @Test
    void fijoConTodasLasLineasAlCero_laFacturaNoFalla() throws Exception {
        String body = presupuesto(
                linea("Exento A", "1.0", "40.00", false, "0.0", "0.0")
                        + "," + linea("Exento B", "1.0", "60.00", false, "0.0", "0.0"),
                "0.0", "10.00", true);
        Emitido emitido = emitir(body);

        assertEuros(emitido.presupuesto(), "90.00", "0.00", "90.00");
        assertFactura(emitido.factura(), "90.00", "0.00", "90.00");
        assertLinea(emitido.factura(), 0, "36.00", "36.00", 0);
        assertLinea(emitido.factura(), 1, "54.00", "54.00", 0);
    }

    @Test
    void descuentoFijoMayorQueTodaLaBase_seLimitaACero() throws Exception {
        String body = presupuesto(
                linea("Trabajo", "1.0", "100.00", true, "0.0", "0.0"),
                "0.0", "150.00", true);
        Emitido emitido = emitir(body);

        assertEuros(emitido.presupuesto(), "0.00", "0.00", "0.00");
        assertFactura(emitido.factura(), "0.00", "0.00", "0.00");
        assertLinea(emitido.factura(), 0, "0.00", "0.00", 21);
    }

    /**
     * 3 × 10,05 al 10 % antes de IVA. El presupuesto guarda 27,135 y un total de
     * 32,83335, que a céntimo es 32,83. Opción B, a la espera de que el gestor la
     * confirme: la factura guarda base 27,14 y cuota 5,70, y el total es su suma, 32,84.
     * Se admite hasta 0,01 € de diferencia con el presupuesto.
     */
    @Test
    void tresLineasDe1005AlDiezPorCiento_laSumaDeBasesCuadraConLaBaseNeta() throws Exception {
        String body = presupuesto(
                linea("A", "1.0", "10.05", true, "0.0", "0.0")
                        + "," + linea("B", "1.0", "10.05", true, "0.0", "0.0")
                        + "," + linea("C", "1.0", "10.05", true, "0.0", "0.0"),
                "10.0", "0.0", true);
        Emitido emitido = emitir(body);

        assertEurosACentimo(emitido.presupuesto(), "27.14", "5.70", "32.83");
        assertFactura(emitido.factura(), "27.14", "5.70", "32.84");
        assertThat(euros(emitido.factura(), "total"))
                .isEqualByComparingTo(euros(emitido.factura(), "subtotal").add(euros(emitido.factura(), "iva")));
        assertDentroDeUnCentimo(emitido.factura(), emitido.presupuesto());
        assertLinea(emitido.factura(), 0, "9.04", "9.04", 21);
        assertLinea(emitido.factura(), 1, "9.05", "9.05", 21);
        assertLinea(emitido.factura(), 2, "9.05", "9.05", 21);
        BigDecimal sumaLineas = BigDecimal.ZERO;
        for (JsonNode item : emitido.factura().get("items")) {
            sumaLineas = sumaLineas.add(euros(item, "subtotal"));
        }
        assertThat(sumaLineas.setScale(2, RoundingMode.HALF_UP))
                .isEqualByComparingTo(euros(emitido.presupuesto(), "subtotal").setScale(2, RoundingMode.HALF_UP));
    }

    private String presupuesto(String items, String porcentaje, String fijo, boolean antesDeIva) {
        return """
                {
                  "clienteId": %d,
                  "items": [%s],
                  "ivaHabilitado": true,
                  "estado": "%s",
                  "descuentoGlobalPorcentaje": %s,
                  "descuentoGlobalFijo": %s,
                  "descuentoAntesIva": %s,
                  "condicionesActivas": [],
                  "notaAdicional": null
                }
                """.formatted(clienteId, items, PresupuestoEstado.PENDIENTE, porcentaje, fijo, antesDeIva);
    }

    private static String linea(String nombre, String cantidad, String precio, boolean aplicaIva,
                                String descuentoPorcentaje, String descuentoFijo) {
        return """
                {"materialId": null, "tareaManual": "%s", "cantidad": %s, "precioUnitario": %s, "aplicaIva": %s, "descuentoPorcentaje": %s, "descuentoFijo": %s}
                """.formatted(nombre, cantidad, precio, aplicaIva, descuentoPorcentaje, descuentoFijo);
    }

    private Emitido emitir(String body) throws Exception {
        String creado = mockMvc.perform(post("/presupuestos")
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode presupuesto = objectMapper.readTree(creado);
        String facturaJson = mockMvc.perform(post("/presupuestos/{id}/factura", presupuesto.get("id").asLong())
                        .with(PresupuestoIntegrationAuth.asUsuarioPresupuestos(userDetailsService)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return new Emitido(presupuesto, objectMapper.readTree(facturaJson));
    }

    private static void assertEuros(JsonNode presupuesto, String subtotal, String iva, String total) {
        assertThat(euros(presupuesto, "subtotal")).isEqualByComparingTo(subtotal);
        assertThat(euros(presupuesto, "iva")).isEqualByComparingTo(iva);
        assertThat(euros(presupuesto, "total")).isEqualByComparingTo(total);
    }

    private static void assertEurosACentimo(JsonNode node, String subtotal, String iva, String total) {
        assertThat(euros(node, "subtotal").setScale(2, RoundingMode.HALF_UP)).isEqualByComparingTo(subtotal);
        assertThat(euros(node, "iva").setScale(2, RoundingMode.HALF_UP)).isEqualByComparingTo(iva);
        assertThat(euros(node, "total").setScale(2, RoundingMode.HALF_UP)).isEqualByComparingTo(total);
    }

    private static void assertFactura(JsonNode factura, String subtotal, String iva, String total) {
        assertEuros(factura, subtotal, iva, total);
    }

    /** Opción B: el total del presupuesto puede quedar 0,01 € por debajo del de la factura. */
    private static void assertDentroDeUnCentimo(JsonNode factura, JsonNode presupuesto) {
        assertThat(separacion(factura, presupuesto, "subtotal")).isLessThanOrEqualTo(new BigDecimal("0.01"));
        assertThat(separacion(factura, presupuesto, "iva")).isLessThanOrEqualTo(new BigDecimal("0.01"));
        assertThat(separacion(factura, presupuesto, "total")).isLessThanOrEqualTo(new BigDecimal("0.01"));
    }

    private static BigDecimal separacion(JsonNode factura, JsonNode presupuesto, String campo) {
        return euros(factura, campo).setScale(2, RoundingMode.HALF_UP)
                .subtract(euros(presupuesto, campo).setScale(2, RoundingMode.HALF_UP))
                .abs();
    }

    private static void assertLinea(JsonNode factura, int indice, String subtotal, String precioUnitario, int tipoIva) {
        JsonNode linea = factura.get("items").get(indice);
        assertThat(euros(linea, "subtotal")).isEqualByComparingTo(subtotal);
        assertThat(euros(linea, "precioUnitario")).isEqualByComparingTo(precioUnitario);
        assertThat(linea.get("cantidad").decimalValue()).isEqualByComparingTo("1.0");
        assertThat(linea.get("tipoIva").asInt()).isEqualTo(tipoIva);
        assertThat(euros(linea, "subtotal")).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    private static BigDecimal euros(JsonNode node, String field) {
        return node.get(field).decimalValue();
    }

    private record Emitido(JsonNode presupuesto, JsonNode factura) {
    }
}
