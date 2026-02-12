package appgestion.controller;

import appgestion.service.MetricasService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Controlador para la pestaña de métricas con dashboard, gráficos y estadísticas.
 * Equivalente a create_metricas_tab en la app Python.
 */
@SuppressWarnings("unchecked")
public class MetricasController {

    private final MetricasService metricasService;

    private ComboBox<String> mesComboPresupuestos;
    private ComboBox<String> mesComboFacturas;

    private Label kpiFacturacionTotal;
    private Label kpiPendienteCobro;
    private Label kpiPromedio;
    private Label kpiComparacion;

    private Label totalEmitidosLabel;
    private Label pendientesLabel;
    private Label aprobadosLabel;
    private Label rechazadosLabel;
    private Label valorTotalEmitidosLabel;
    private Label valorAprobadosLabel;
    private Label valorPendientesLabel;
    private Label promedioPresupuestoLabel;
    private Label tasaConversionLabel;

    private Label totalEmitidasLabel;
    private Label noPagadasLabel;
    private Label pagadasLabel;
    private Label totalFacturadoLabel;
    private Label pendienteCobroLabel;
    private Label promedioFacturaLabel;
    private Label diasPromedioCobroLabel;

    private PieChart piePresupuestos;
    private PieChart pieFacturas;
    private BarChart<String, Number> barClientes;
    private BarChart<String, Number> barMateriales;
    private BarChart<String, Number> barEvolucion;

    private TableView<FacturaVencidaRow> tableVencidas;
    private TableView<FacturaProximaRow> tableProximas;
    private TableView<TopClienteRow> tableTopClientes;
    private TableView<TopMaterialRow> tableTopMateriales;

    private Label montoTotalVencidoLabel;
    private Label montoTotalProximasLabel;

    public MetricasController(MetricasService metricasService) {
        this.metricasService = metricasService;
    }

    public Node createContent() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #ecf0f1;");
        VBox main = new VBox(15);
        main.setPadding(new Insets(15));
        main.setStyle("-fx-background-color: #ecf0f1;");

        // Dashboard KPI
        main.getChildren().add(crearDashboardKPI());

        // Presupuestos
        main.getChildren().add(crearSeccionPresupuestos());

        // Facturas
        main.getChildren().add(crearSeccionFacturas());

        // Análisis cobranza
        main.getChildren().add(crearSeccionCobranza());

        // Top clientes
        main.getChildren().add(crearSeccionTopClientes());

        // Top materiales
        main.getChildren().add(crearSeccionTopMateriales());

        // Evolución mensual
        main.getChildren().add(crearSeccionEvolucion());

        scroll.setContent(main);
        actualizarTodo();
        return scroll;
    }

    private TitledPane crearDashboardKPI() {
        kpiFacturacionTotal = crearKPICard("€0.00", "#27ae60");
        kpiPendienteCobro = crearKPICard("€0.00", "#e74c3c");
        kpiPromedio = crearKPICard("€0.00", "#3498db");
        kpiComparacion = crearKPICard("+0.0%", "#9b59b6");

        VBox box1 = crearKPIBox("Facturación Total", kpiFacturacionTotal, "#27ae60");
        VBox box2 = crearKPIBox("Pendiente de Cobro", kpiPendienteCobro, "#e74c3c");
        VBox box3 = crearKPIBox("Facturación Promedio", kpiPromedio, "#3498db");
        VBox box4 = crearKPIBox("vs Mes Anterior", kpiComparacion, "#9b59b6");

        HBox kpiRow = new HBox(15);
        kpiRow.getChildren().addAll(box1, box2, box3, box4);
        kpiRow.setAlignment(Pos.CENTER);

        TitledPane pane = new TitledPane("📈 Dashboard Financiero", kpiRow);
        pane.setExpanded(true);
        return pane;
    }

    private VBox crearKPIBox(String titulo, Label valor, String colorHex) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(15));
        box.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-width: 2;");
        Label tit = new Label(titulo);
        tit.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        valor.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        valor.setStyle("-fx-text-fill: " + colorHex + ";");
        box.getChildren().addAll(tit, valor);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private Label crearKPICard(String valor, String color) {
        Label l = new Label(valor);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        l.setStyle("-fx-text-fill: " + color + ";");
        return l;
    }

    private TitledPane crearSeccionPresupuestos() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(15));

        HBox filterRow = new HBox(10);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        filterRow.getChildren().add(new Label("Filtrar por mes:"));
        mesComboPresupuestos = new ComboBox<>();
        mesComboPresupuestos.getItems().add("Todos");
        mesComboPresupuestos.getItems().addAll(metricasService.obtenerMesesDisponiblesPresupuestos());
        mesComboPresupuestos.setValue("Todos");
        mesComboPresupuestos.setMaxWidth(250);
        mesComboPresupuestos.setOnAction(e -> actualizarPresupuestos());
        filterRow.getChildren().add(mesComboPresupuestos);

        HBox statsRow = new HBox(20);
        VBox statsLeft = new VBox(8);
        totalEmitidosLabel = new Label("Total Emitidos: 0");
        totalEmitidosLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        pendientesLabel = new Label("Pendientes: 0 (0%)");
        aprobadosLabel = new Label("Aprobados: 0 (0%)");
        rechazadosLabel = new Label("Rechazados: 0 (0%)");
        Separator sep = new Separator();
        valorTotalEmitidosLabel = new Label("Valor Total Emitido: €0.00");
        valorTotalEmitidosLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        valorAprobadosLabel = new Label("Valor Aprobado: €0.00");
        valorPendientesLabel = new Label("Valor Pendiente: €0.00");
        promedioPresupuestoLabel = new Label("Promedio por Presupuesto: €0.00");
        tasaConversionLabel = new Label("Tasa Conversión: 0.0%");
        tasaConversionLabel.setStyle("-fx-text-fill: #9b59b6;");
        statsLeft.getChildren().addAll(totalEmitidosLabel, pendientesLabel, aprobadosLabel, rechazadosLabel,
                sep, valorTotalEmitidosLabel, valorAprobadosLabel, valorPendientesLabel,
                promedioPresupuestoLabel, tasaConversionLabel);

        piePresupuestos = new PieChart();
        piePresupuestos.setTitle("Distribución por Estado");
        piePresupuestos.setPrefSize(280, 220);

        statsRow.getChildren().addAll(statsLeft, piePresupuestos);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(statsLeft, Priority.ALWAYS);

        content.getChildren().addAll(filterRow, statsRow);
        TitledPane pane = new TitledPane("📊 Estadísticas de Presupuestos", content);
        pane.setExpanded(true);
        return pane;
    }

    private TitledPane crearSeccionFacturas() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(15));

        HBox filterRow = new HBox(10);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        filterRow.getChildren().add(new Label("Filtrar por mes:"));
        mesComboFacturas = new ComboBox<>();
        mesComboFacturas.getItems().add("Todos");
        mesComboFacturas.getItems().addAll(metricasService.obtenerMesesDisponiblesFacturas());
        mesComboFacturas.setValue("Todos");
        mesComboFacturas.setMaxWidth(250);
        mesComboFacturas.setOnAction(e -> actualizarFacturasYDerivados());
        filterRow.getChildren().add(mesComboFacturas);

        HBox statsRow = new HBox(20);
        VBox statsLeft = new VBox(8);
        totalEmitidasLabel = new Label("Total Emitidas: 0");
        totalEmitidasLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        noPagadasLabel = new Label("No Pagadas: 0 (0%)");
        pagadasLabel = new Label("Pagadas: 0 (0%)");
        Separator sep = new Separator();
        totalFacturadoLabel = new Label("Total Facturado: €0.00");
        totalFacturadoLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        totalFacturadoLabel.setStyle("-fx-text-fill: #27ae60;");
        pendienteCobroLabel = new Label("Pendiente de Cobro: €0.00");
        pendienteCobroLabel.setStyle("-fx-text-fill: #e74c3c;");
        promedioFacturaLabel = new Label("Promedio por Factura: €0.00");
        diasPromedioCobroLabel = new Label("Días Promedio de Cobro: 0");
        statsLeft.getChildren().addAll(totalEmitidasLabel, noPagadasLabel, pagadasLabel, sep,
                totalFacturadoLabel, pendienteCobroLabel, promedioFacturaLabel, diasPromedioCobroLabel);

        pieFacturas = new PieChart();
        pieFacturas.setTitle("Distribución por Estado de Pago");
        pieFacturas.setPrefSize(280, 220);

        statsRow.getChildren().addAll(statsLeft, pieFacturas);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(statsLeft, Priority.ALWAYS);

        content.getChildren().addAll(filterRow, statsRow);
        TitledPane pane = new TitledPane("💰 Estadísticas de Facturas", content);
        pane.setExpanded(true);
        return pane;
    }

    private TitledPane crearSeccionCobranza() {
        HBox content = new HBox(15);
        content.setPadding(new Insets(15));

        VBox vencidas = new VBox(8);
        TitledPane tpVencidas = new TitledPane("Facturas Vencidas", new Label());
        tableVencidas = new TableView<>();
        TableColumn<FacturaVencidaRow, String> colNum = new TableColumn<>("Número");
        colNum.setCellValueFactory(c -> c.getValue().numeroProperty());
        TableColumn<FacturaVencidaRow, String> colCliente = new TableColumn<>("Cliente");
        colCliente.setCellValueFactory(c -> c.getValue().clienteProperty());
        TableColumn<FacturaVencidaRow, String> colMonto = new TableColumn<>("Monto");
        colMonto.setCellValueFactory(c -> c.getValue().montoProperty());
        TableColumn<FacturaVencidaRow, String> colDias = new TableColumn<>("Días Vencidos");
        colDias.setCellValueFactory(c -> c.getValue().diasProperty());
        tableVencidas.getColumns().addAll(colNum, colCliente, colMonto, colDias);
        tableVencidas.setPrefHeight(180);
        montoTotalVencidoLabel = new Label("Monto Total Vencido: €0.00");
        montoTotalVencidoLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #e74c3c;");
        vencidas.getChildren().addAll(tableVencidas, montoTotalVencidoLabel);
        tpVencidas.setContent(vencidas);

        VBox proximas = new VBox(8);
        TitledPane tpProximas = new TitledPane("Próximas a Vencer (30 días)", new Label());
        tableProximas = new TableView<>();
        TableColumn<FacturaProximaRow, String> colNumP = new TableColumn<>("Número");
        colNumP.setCellValueFactory(c -> c.getValue().numeroProperty());
        TableColumn<FacturaProximaRow, String> colClienteP = new TableColumn<>("Cliente");
        colClienteP.setCellValueFactory(c -> c.getValue().clienteProperty());
        TableColumn<FacturaProximaRow, String> colMontoP = new TableColumn<>("Monto");
        colMontoP.setCellValueFactory(c -> c.getValue().montoProperty());
        TableColumn<FacturaProximaRow, String> colDiasP = new TableColumn<>("Días Restantes");
        colDiasP.setCellValueFactory(c -> c.getValue().diasProperty());
        tableProximas.getColumns().addAll(colNumP, colClienteP, colMontoP, colDiasP);
        tableProximas.setPrefHeight(180);
        montoTotalProximasLabel = new Label("Monto Total Próximo: €0.00");
        montoTotalProximasLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #f39c12;");
        proximas.getChildren().addAll(tableProximas, montoTotalProximasLabel);
        tpProximas.setContent(proximas);

        content.getChildren().addAll(tpVencidas, tpProximas);
        HBox.setHgrow(tpVencidas, Priority.ALWAYS);
        HBox.setHgrow(tpProximas, Priority.ALWAYS);

        TitledPane pane = new TitledPane("⚠️ Análisis de Cobranza", content);
        pane.setExpanded(true);
        return pane;
    }

    private TitledPane crearSeccionTopClientes() {
        HBox content = new HBox(15);
        content.setPadding(new Insets(15));

        tableTopClientes = new TableView<>();
        TableColumn<TopClienteRow, String> colC = new TableColumn<>("Cliente");
        colC.setCellValueFactory(c -> c.getValue().clienteProperty());
        TableColumn<TopClienteRow, String> colF = new TableColumn<>("Facturas");
        colF.setCellValueFactory(c -> c.getValue().facturasProperty());
        TableColumn<TopClienteRow, String> colT = new TableColumn<>("Total Facturado");
        colT.setCellValueFactory(c -> c.getValue().totalProperty());
        TableColumn<TopClienteRow, String> colP = new TableColumn<>("Promedio");
        colP.setCellValueFactory(c -> c.getValue().promedioProperty());
        tableTopClientes.getColumns().addAll(colC, colF, colT, colP);
        tableTopClientes.setPrefHeight(200);
        tableTopClientes.setPrefWidth(400);

        javafx.scene.chart.CategoryAxis xAxisC = new javafx.scene.chart.CategoryAxis();
        javafx.scene.chart.NumberAxis yAxisC = new javafx.scene.chart.NumberAxis();
        barClientes = new BarChart<>(xAxisC, yAxisC);
        barClientes.setTitle("Top 5 Clientes por Facturación");
        barClientes.setPrefSize(350, 200);
        barClientes.setLegendVisible(false);

        content.getChildren().addAll(tableTopClientes, barClientes);
        HBox.setHgrow(tableTopClientes, Priority.ALWAYS);
        HBox.setHgrow(barClientes, Priority.ALWAYS);

        TitledPane pane = new TitledPane("👥 Top Clientes", content);
        pane.setExpanded(true);
        return pane;
    }

    private TitledPane crearSeccionTopMateriales() {
        HBox content = new HBox(15);
        content.setPadding(new Insets(15));

        tableTopMateriales = new TableView<>();
        TableColumn<TopMaterialRow, String> colM = new TableColumn<>("Material");
        colM.setCellValueFactory(c -> c.getValue().materialProperty());
        TableColumn<TopMaterialRow, String> colI = new TableColumn<>("Ingresos");
        colI.setCellValueFactory(c -> c.getValue().ingresosProperty());
        TableColumn<TopMaterialRow, String> colC = new TableColumn<>("Cantidad");
        colC.setCellValueFactory(c -> c.getValue().cantidadProperty());
        TableColumn<TopMaterialRow, String> colV = new TableColumn<>("Veces Usado");
        colV.setCellValueFactory(c -> c.getValue().vecesProperty());
        tableTopMateriales.getColumns().addAll(colM, colI, colC, colV);
        tableTopMateriales.setPrefHeight(200);
        tableTopMateriales.setPrefWidth(400);

        javafx.scene.chart.CategoryAxis xAxisM = new javafx.scene.chart.CategoryAxis();
        javafx.scene.chart.NumberAxis yAxisM = new javafx.scene.chart.NumberAxis();
        barMateriales = new BarChart<>(xAxisM, yAxisM);
        barMateriales.setTitle("Top 5 Materiales por Ingresos");
        barMateriales.setPrefSize(350, 200);
        barMateriales.setLegendVisible(false);

        content.getChildren().addAll(tableTopMateriales, barMateriales);
        HBox.setHgrow(tableTopMateriales, Priority.ALWAYS);
        HBox.setHgrow(barMateriales, Priority.ALWAYS);

        TitledPane pane = new TitledPane("📦 Top Materiales/Servicios", content);
        pane.setExpanded(true);
        return pane;
    }

    private TitledPane crearSeccionEvolucion() {
        javafx.scene.chart.CategoryAxis xAxis = new javafx.scene.chart.CategoryAxis();
        javafx.scene.chart.NumberAxis yAxis = new javafx.scene.chart.NumberAxis();
        barEvolucion = new BarChart<>(xAxis, yAxis);
        barEvolucion.setTitle("Evolución Mensual de Facturación (Últimos 12 meses)");
        barEvolucion.setPrefHeight(280);
        barEvolucion.setLegendVisible(true);

        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        content.getChildren().add(barEvolucion);

        TitledPane pane = new TitledPane("📊 Comparaciones Temporales", content);
        pane.setExpanded(true);
        return pane;
    }

    private void actualizarTodo() {
        actualizarDashboard();
        actualizarPresupuestos();
        actualizarFacturasYDerivados();
        actualizarCobranza();
        actualizarTopClientes();
        actualizarTopMateriales();
        actualizarEvolucion();
    }

    public void recargar() {
        mesComboPresupuestos.getItems().clear();
        mesComboPresupuestos.getItems().add("Todos");
        mesComboPresupuestos.getItems().addAll(metricasService.obtenerMesesDisponiblesPresupuestos());
        mesComboPresupuestos.setValue("Todos");
        mesComboFacturas.getItems().clear();
        mesComboFacturas.getItems().add("Todos");
        mesComboFacturas.getItems().addAll(metricasService.obtenerMesesDisponiblesFacturas());
        mesComboFacturas.setValue("Todos");
        actualizarTodo();
    }

    private void actualizarDashboard() {
        LocalDate ahora = LocalDate.now();
        String fechaInicioMes = ahora.withDayOfMonth(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String fechaFinMes = ahora.format(DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDate primerDiaAnterior = ahora.withDayOfMonth(1).minusMonths(1);
        LocalDate ultimoDiaAnterior = ahora.withDayOfMonth(1).minusDays(1);
        String fechaInicioAnterior = primerDiaAnterior.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String fechaFinAnterior = ultimoDiaAnterior.format(DateTimeFormatter.ISO_LOCAL_DATE);

        MetricasService.StatsFacturas statsActual = metricasService.obtenerEstadisticasFacturas(fechaInicioMes, fechaFinMes);
        MetricasService.StatsFacturas statsAnterior = metricasService.obtenerEstadisticasFacturas(fechaInicioAnterior, fechaFinAnterior);

        kpiFacturacionTotal.setText(String.format("€%.2f", statsActual.totalFacturado));
        kpiPendienteCobro.setText(String.format("€%.2f", statsActual.totalPendienteCobro));
        kpiPromedio.setText(String.format("€%.2f", statsActual.promedioFactura));

        if (statsAnterior.totalFacturado > 0) {
            double cambio = ((statsActual.totalFacturado - statsAnterior.totalFacturado) / statsAnterior.totalFacturado) * 100;
            String signo = cambio >= 0 ? "+" : "";
            kpiComparacion.setText(signo + String.format("%.1f%%", cambio));
            kpiComparacion.setStyle("-fx-text-fill: " + (cambio >= 0 ? "#27ae60" : "#e74c3c") + ";");
        } else {
            kpiComparacion.setText("N/A");
        }
    }

    private String[] parsearMesSeleccionado(String seleccion) {
        if (seleccion == null || "Todos".equals(seleccion)) return null;
        Matcher m = Pattern.compile("\\((\\d{4}-\\d{2})\\)").matcher(seleccion);
        if (!m.find()) return null;
        String ym = m.group(1);
        String[] p = ym.split("-");
        int mes = Integer.parseInt(p[1]);
        int anio = Integer.parseInt(p[0]);
        String inicio = ym + "-01";
        int ultimoDia = ultimoDiaMes(anio, mes);
        String fin = String.format("%04d-%02d-%02d", anio, mes, ultimoDia);
        return new String[]{inicio, fin};
    }

    private int ultimoDiaMes(int anio, int mes) {
        if (mes == 2) return (anio % 4 == 0) ? 29 : 28;
        if (mes == 4 || mes == 6 || mes == 9 || mes == 11) return 30;
        return 31;
    }

    private void actualizarPresupuestos() {
        String[] rango = parsearMesSeleccionado(mesComboPresupuestos.getValue());
        String inicio = rango != null ? rango[0] : null;
        String fin = rango != null ? rango[1] : null;

        MetricasService.StatsPresupuestos s = metricasService.obtenerEstadisticasPresupuestos(inicio, fin);
        int total = s.totalEmitidos;
        double pPend = total > 0 ? (s.pendientes * 100.0 / total) : 0;
        double pApr = total > 0 ? (s.aprobados * 100.0 / total) : 0;
        double pRech = total > 0 ? (s.rechazados * 100.0 / total) : 0;
        double tasa = metricasService.obtenerTasaConversionPresupuestos(inicio, fin);

        totalEmitidosLabel.setText("Total Emitidos: " + total);
        pendientesLabel.setText(String.format("Pendientes: %d (%.1f%%)", s.pendientes, pPend));
        aprobadosLabel.setText(String.format("Aprobados: %d (%.1f%%)", s.aprobados, pApr));
        rechazadosLabel.setText(String.format("Rechazados: %d (%.1f%%)", s.rechazados, pRech));
        valorTotalEmitidosLabel.setText(String.format("Valor Total Emitido: €%.2f", s.totalValorEmitidos));
        valorAprobadosLabel.setText(String.format("Valor Aprobado: €%.2f", s.totalValorAprobados));
        valorPendientesLabel.setText(String.format("Valor Pendiente: €%.2f", s.totalValorPendientes));
        promedioPresupuestoLabel.setText(String.format("Promedio por Presupuesto: €%.2f", s.promedioPresupuesto));
        tasaConversionLabel.setText(String.format("Tasa Conversión: %.1f%%", tasa));

        piePresupuestos.getData().clear();
        if (total > 0) {
            if (s.pendientes > 0) piePresupuestos.getData().add(new javafx.scene.chart.PieChart.Data("Pendientes", s.pendientes));
            if (s.aprobados > 0) piePresupuestos.getData().add(new javafx.scene.chart.PieChart.Data("Aprobados", s.aprobados));
            if (s.rechazados > 0) piePresupuestos.getData().add(new javafx.scene.chart.PieChart.Data("Rechazados", s.rechazados));
        }
    }

    private void actualizarFacturasYDerivados() {
        actualizarFacturas();
        actualizarTopClientes();
        actualizarTopMateriales();
        actualizarEvolucion();
    }

    private void actualizarFacturas() {
        String[] rango = parsearMesSeleccionado(mesComboFacturas.getValue());
        String inicio = rango != null ? rango[0] : null;
        String fin = rango != null ? rango[1] : null;

        MetricasService.StatsFacturas s = metricasService.obtenerEstadisticasFacturas(inicio, fin);
        int total = s.totalEmitidas;
        double pNoPag = total > 0 ? (s.noPagadas * 100.0 / total) : 0;
        double pPag = total > 0 ? (s.pagadas * 100.0 / total) : 0;
        double diasPromedio = metricasService.obtenerDiasPromedioCobro(inicio, fin);

        totalEmitidasLabel.setText("Total Emitidas: " + total);
        noPagadasLabel.setText(String.format("No Pagadas: %d (%.1f%%)", s.noPagadas, pNoPag));
        pagadasLabel.setText(String.format("Pagadas: %d (%.1f%%)", s.pagadas, pPag));
        totalFacturadoLabel.setText(String.format("Total Facturado: €%.2f", s.totalFacturado));
        pendienteCobroLabel.setText(String.format("Pendiente de Cobro: €%.2f", s.totalPendienteCobro));
        promedioFacturaLabel.setText(String.format("Promedio por Factura: €%.2f", s.promedioFactura));
        diasPromedioCobroLabel.setText(String.format("Días Promedio de Cobro: %.0f", diasPromedio));

        pieFacturas.getData().clear();
        if (total > 0) {
            if (s.noPagadas > 0) pieFacturas.getData().add(new javafx.scene.chart.PieChart.Data("No Pagadas", s.noPagadas));
            if (s.pagadas > 0) pieFacturas.getData().add(new javafx.scene.chart.PieChart.Data("Pagadas", s.pagadas));
        }
    }

    private void actualizarCobranza() {
        List<MetricasService.FacturaVencida> vencidas = metricasService.obtenerFacturasVencidas();
        ObservableList<FacturaVencidaRow> rowsV = FXCollections.observableArrayList();
        double totalV = 0;
        for (MetricasService.FacturaVencida f : vencidas) {
            rowsV.add(new FacturaVencidaRow(f));
            totalV += f.total;
        }
        tableVencidas.setItems(rowsV);
        montoTotalVencidoLabel.setText(String.format("Monto Total Vencido: €%.2f", totalV));

        List<MetricasService.FacturaProxima> proximas = metricasService.obtenerFacturasProximasVencer(30);
        ObservableList<FacturaProximaRow> rowsP = FXCollections.observableArrayList();
        double totalP = 0;
        for (MetricasService.FacturaProxima f : proximas) {
            rowsP.add(new FacturaProximaRow(f));
            totalP += f.total;
        }
        tableProximas.setItems(rowsP);
        montoTotalProximasLabel.setText(String.format("Monto Total Próximo: €%.2f", totalP));
    }

    private void actualizarTopClientes() {
        String[] rango = parsearMesSeleccionado(mesComboFacturas.getValue());
        String inicio = rango != null ? rango[0] : null;
        String fin = rango != null ? rango[1] : null;

        List<MetricasService.TopCliente> lista = metricasService.obtenerTopClientes(inicio, fin, 10);
        ObservableList<TopClienteRow> rows = FXCollections.observableArrayList();
        for (MetricasService.TopCliente c : lista) {
            rows.add(new TopClienteRow(c));
        }
        tableTopClientes.setItems(rows);

        barClientes.getData().clear();
        if (!lista.isEmpty()) {
            javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
            int limit = Math.min(5, lista.size());
            for (int i = 0; i < limit; i++) {
                MetricasService.TopCliente c = lista.get(i);
                String nombre = c.clienteNombre != null && c.clienteNombre.length() > 20 ? c.clienteNombre.substring(0, 20) : c.clienteNombre;
                series.getData().add(new javafx.scene.chart.XYChart.Data<>(nombre, c.totalPagado));
            }
            barClientes.getData().add(series);
        }
    }

    private void actualizarTopMateriales() {
        String[] rango = parsearMesSeleccionado(mesComboFacturas.getValue());
        String inicio = rango != null ? rango[0] : null;
        String fin = rango != null ? rango[1] : null;

        List<MetricasService.TopMaterial> lista = metricasService.obtenerTopMaterialesPorIngresos(inicio, fin, 10);
        ObservableList<TopMaterialRow> rows = FXCollections.observableArrayList();
        for (MetricasService.TopMaterial m : lista) {
            rows.add(new TopMaterialRow(m));
        }
        tableTopMateriales.setItems(rows);

        barMateriales.getData().clear();
        if (!lista.isEmpty()) {
            javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
            int limit = Math.min(5, lista.size());
            for (int i = 0; i < limit; i++) {
                MetricasService.TopMaterial m = lista.get(i);
                String nombre = m.materialNombre != null && m.materialNombre.length() > 15 ? m.materialNombre.substring(0, 15) : m.materialNombre;
                series.getData().add(new javafx.scene.chart.XYChart.Data<>(nombre, m.ingresosTotal));
            }
            barMateriales.getData().add(series);
        }
    }

    private void actualizarEvolucion() {
        List<MetricasService.EvolucionMensual> evolucion = metricasService.obtenerEvolucionFacturacionMensual(12);
        String[] abr = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};

        barEvolucion.getData().clear();
        if (!evolucion.isEmpty()) {
            javafx.scene.chart.XYChart.Series<String, Number> seriesPagada = new javafx.scene.chart.XYChart.Series<>();
            seriesPagada.setName("Pagada");
            javafx.scene.chart.XYChart.Series<String, Number> seriesPendiente = new javafx.scene.chart.XYChart.Series<>();
            seriesPendiente.setName("Pendiente");
            for (MetricasService.EvolucionMensual e : evolucion) {
                String label = "Sin datos";
                if (e.mes != null && e.mes.length() >= 7) {
                    String[] p = e.mes.split("-");
                    if (p.length == 2) {
                        label = abr[Integer.parseInt(p[1]) - 1] + " " + p[0].substring(2);
                    }
                }
                seriesPagada.getData().add(new javafx.scene.chart.XYChart.Data<>(label, e.facturacionPagada));
                seriesPendiente.getData().add(new javafx.scene.chart.XYChart.Data<>(label, e.facturacionPendiente));
            }
            barEvolucion.getData().addAll(seriesPagada, seriesPendiente);
        }
    }

    // Clases auxiliares para TableView
    private static class FacturaVencidaRow {
        private final SimpleStringProperty numero = new SimpleStringProperty();
        private final SimpleStringProperty cliente = new SimpleStringProperty();
        private final SimpleStringProperty monto = new SimpleStringProperty();
        private final SimpleStringProperty dias = new SimpleStringProperty();

        FacturaVencidaRow(MetricasService.FacturaVencida f) {
            numero.set(f.numeroFactura);
            cliente.set(f.clienteNombre);
            monto.set(String.format("€%.2f", f.total));
            dias.set(f.diasVencidos + " días");
        }
        SimpleStringProperty numeroProperty() { return numero; }
        SimpleStringProperty clienteProperty() { return cliente; }
        SimpleStringProperty montoProperty() { return monto; }
        SimpleStringProperty diasProperty() { return dias; }
    }

    private static class FacturaProximaRow {
        private final SimpleStringProperty numero = new SimpleStringProperty();
        private final SimpleStringProperty cliente = new SimpleStringProperty();
        private final SimpleStringProperty monto = new SimpleStringProperty();
        private final SimpleStringProperty dias = new SimpleStringProperty();

        FacturaProximaRow(MetricasService.FacturaProxima f) {
            numero.set(f.numeroFactura);
            cliente.set(f.clienteNombre);
            monto.set(String.format("€%.2f", f.total));
            dias.set(f.diasRestantes + " días");
        }
        SimpleStringProperty numeroProperty() { return numero; }
        SimpleStringProperty clienteProperty() { return cliente; }
        SimpleStringProperty montoProperty() { return monto; }
        SimpleStringProperty diasProperty() { return dias; }
    }

    private static class TopClienteRow {
        private final SimpleStringProperty cliente = new SimpleStringProperty();
        private final SimpleStringProperty facturas = new SimpleStringProperty();
        private final SimpleStringProperty total = new SimpleStringProperty();
        private final SimpleStringProperty promedio = new SimpleStringProperty();

        TopClienteRow(MetricasService.TopCliente c) {
            cliente.set(c.clienteNombre);
            facturas.set(String.valueOf(c.cantidadFacturas));
            total.set(String.format("€%.2f", c.totalPagado));
            promedio.set(String.format("€%.2f", c.promedioFactura));
        }
        SimpleStringProperty clienteProperty() { return cliente; }
        SimpleStringProperty facturasProperty() { return facturas; }
        SimpleStringProperty totalProperty() { return total; }
        SimpleStringProperty promedioProperty() { return promedio; }
    }

    private static class TopMaterialRow {
        private final SimpleStringProperty material = new SimpleStringProperty();
        private final SimpleStringProperty ingresos = new SimpleStringProperty();
        private final SimpleStringProperty cantidad = new SimpleStringProperty();
        private final SimpleStringProperty veces = new SimpleStringProperty();

        TopMaterialRow(MetricasService.TopMaterial m) {
            material.set(m.materialNombre);
            ingresos.set(String.format("€%.2f", m.ingresosTotal));
            cantidad.set(String.format("%.2f", m.cantidadTotal));
            veces.set(String.valueOf(m.vecesUsado));
        }
        SimpleStringProperty materialProperty() { return material; }
        SimpleStringProperty ingresosProperty() { return ingresos; }
        SimpleStringProperty cantidadProperty() { return cantidad; }
        SimpleStringProperty vecesProperty() { return veces; }
    }
}
