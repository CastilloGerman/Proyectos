package appgestion.controller;

import appgestion.service.ConfigService;
import appgestion.service.FacturaService;
import appgestion.service.FacturaService.FacturaDetalle;
import appgestion.service.FacturaService.FacturaItemDetalle;
import appgestion.service.FacturaService.FacturaResumen;
import appgestion.service.PdfGeneratorService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import com.lowagie.text.DocumentException;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pestaña de facturación básica:
 * - Lista las facturas
 * - Permite generar una nueva factura a partir de un ID de presupuesto existente
 */
public class FacturacionController {

    private static final Logger log = Logger.getLogger(FacturacionController.class.getName());
    private final FacturaService facturaService;
    private final ConfigService configService = new ConfigService();
    private final PdfGeneratorService pdfGenerator = new PdfGeneratorService();
    private final ObservableList<FacturaResumen> facturas = FXCollections.observableArrayList();

    private TableView<FacturaResumen> facturasTable;
    private TextField presupuestoIdField;
    private TextField buscarField;
    private ComboBox<Integer> anioCombo;
    private ComboBox<Integer> mesCombo;
    private ComboBox<String> estadoCombo;
    private Label resumenLabel;

    public FacturacionController(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    public Node createContent() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        VBox topBox = new VBox(8);

        // Crear factura desde presupuesto
        HBox crearBox = new HBox(10);
        presupuestoIdField = new TextField();
        presupuestoIdField.setPromptText("ID de presupuesto");
        Button crearBtn = new Button("Crear factura desde presupuesto");
        crearBtn.setOnAction(e -> onCrearFactura());
        crearBox.getChildren().addAll(new Label("Presupuesto ID:"), presupuestoIdField, crearBtn);

        // Filtros avanzados
        HBox filtrosBox = new HBox(10);
        buscarField = new TextField();
        buscarField.setPromptText("Buscar por cliente o número");
        anioCombo = new ComboBox<>();
        anioCombo.setPromptText("Año");
        mesCombo = new ComboBox<>();
        mesCombo.setPromptText("Mes");
        estadoCombo = new ComboBox<>();
        estadoCombo.getItems().addAll("TODAS", "No Pagada", "Pagada", "Parcial");
        estadoCombo.getSelectionModel().selectFirst();
        Button aplicarFiltrosBtn = new Button("Filtrar");
        aplicarFiltrosBtn.setOnAction(e -> recargar());
        Button limpiarFiltrosBtn = new Button("Limpiar");
        limpiarFiltrosBtn.setOnAction(e -> {
            buscarField.clear();
            anioCombo.getSelectionModel().clearSelection();
            mesCombo.getSelectionModel().clearSelection();
            estadoCombo.getSelectionModel().selectFirst();
            recargar();
        });
        Button verDetalleBtn = new Button("Ver detalle");
        verDetalleBtn.setOnAction(e -> verDetalleFactura());
        filtrosBox.getChildren().addAll(
                new Label("Buscar:"), buscarField,
                new Label("Año:"), anioCombo,
                new Label("Mes:"), mesCombo,
                new Label("Estado:"), estadoCombo,
                aplicarFiltrosBtn, limpiarFiltrosBtn, verDetalleBtn
        );

        topBox.getChildren().addAll(crearBox, filtrosBox);
        root.setTop(topBox);

        facturasTable = new TableView<>(facturas);
        facturasTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        facturasTable.setOnMouseClicked(ev -> { if (ev.getClickCount() == 2) verDetalleFactura(); });

        TableColumn<FacturaResumen, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<FacturaResumen, String> colNum = new TableColumn<>("Número");
        colNum.setCellValueFactory(new PropertyValueFactory<>("numeroFactura"));

        TableColumn<FacturaResumen, String> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaCreacion"));

        TableColumn<FacturaResumen, String> colCliente = new TableColumn<>("Cliente");
        colCliente.setCellValueFactory(new PropertyValueFactory<>("clienteNombre"));

        TableColumn<FacturaResumen, Double> colSubtotal = new TableColumn<>("Subtotal");
        colSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));

        TableColumn<FacturaResumen, Double> colIva = new TableColumn<>("IVA");
        colIva.setCellValueFactory(new PropertyValueFactory<>("iva"));

        TableColumn<FacturaResumen, Double> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));

        TableColumn<FacturaResumen, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estadoPago"));

        facturasTable.getColumns().add(colId);
        facturasTable.getColumns().add(colNum);
        facturasTable.getColumns().add(colFecha);
        facturasTable.getColumns().add(colCliente);
        facturasTable.getColumns().add(colSubtotal);
        facturasTable.getColumns().add(colIva);
        facturasTable.getColumns().add(colTotal);
        facturasTable.getColumns().add(colEstado);

        resumenLabel = new Label();

        VBox centerBox = new VBox(8, facturasTable, resumenLabel);
        root.setCenter(centerBox);

        cargarCombos();
        recargar();

        return root;
    }

    private void onCrearFactura() {
        String txt = presupuestoIdField.getText();
        if (txt == null || txt.trim().isEmpty()) {
            mostrarAlerta("Validación", "Introduce un ID de presupuesto.");
            return;
        }
        int presupuestoId;
        try {
            presupuestoId = Integer.parseInt(txt.trim());
        } catch (NumberFormatException e) {
            mostrarAlerta("Validación", "ID de presupuesto inválido.");
            return;
        }
        try {
            int facturaId = facturaService.crearFacturaDesdePresupuesto(presupuestoId);
            mostrarAlerta("Factura creada", "Factura creada con ID " + facturaId + ".");
            presupuestoIdField.clear();
            recargar();
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Error en operación", e);
            mostrarAlerta("Error", "No se pudo crear la factura: " + e.getMessage());
        }
    }

    public void recargar() {
        String termino = buscarField != null ? buscarField.getText() : null;
        Integer anio = anioCombo != null ? anioCombo.getValue() : null;
        Integer mes = mesCombo != null ? mesCombo.getValue() : null;
        String estado = estadoCombo != null ? estadoCombo.getValue() : "TODAS";
        facturas.setAll(facturaService.buscarFacturas(termino, anio, mes, estado));
        actualizarResumen();
    }

    private void cargarCombos() {
        anioCombo.getItems().setAll(facturaService.obtenerAniosDisponibles());
        mesCombo.getItems().clear();
        mesCombo.getItems().addAll(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
    }

    private void actualizarResumen() {
        double subtotal = 0;
        double iva = 0;
        double total = 0;
        double pendienteCobro = 0;
        for (FacturaResumen r : facturas) {
            subtotal += r.subtotal;
            iva += r.iva;
            total += r.total;
            if (r.estadoPago != null && !"Pagada".equalsIgnoreCase(r.estadoPago)) {
                pendienteCobro += r.total;
            }
        }
        resumenLabel.setText(String.format(
                "Facturas: %d | Subtotal: €%.2f | IVA: €%.2f | Total: €%.2f | Pendiente cobro: €%.2f",
                facturas.size(), subtotal, iva, total, pendienteCobro
        ));
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarAlertaError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void verDetalleFactura() {
        FacturaResumen sel = facturasTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            mostrarAlerta("Aviso", "Seleccione una factura para ver el detalle.");
            return;
        }
        FacturaDetalle detalle = facturaService.obtenerFacturaPorId(sel.id);
        if (detalle == null) {
            mostrarAlerta("Error", "No se pudo cargar la factura.");
            return;
        }
        mostrarDialogoDetalleFactura(detalle);
    }

    private void mostrarDialogoDetalleFactura(FacturaDetalle d) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Detalle Factura " + nullToEmpty(d.numeroFactura));

        VBox infoBox = new VBox(4);
        infoBox.setPadding(new Insets(10));
        infoBox.getChildren().addAll(
                new Label("Número: " + nullToEmpty(d.numeroFactura)),
                new Label("Fecha: " + nullToEmpty(d.fechaCreacion)),
                new Label("Vencimiento: " + nullToEmpty(d.fechaVencimiento)),
                new Label("Método de pago: " + nullToEmpty(d.metodoPago)),
                new Label("Estado: " + nullToEmpty(d.estadoPago))
        );
        TitledPane infoPane = new TitledPane("Información de la factura", infoBox);
        infoPane.setExpanded(true);

        VBox clientBox = new VBox(4);
        clientBox.setPadding(new Insets(10));
        clientBox.getChildren().addAll(
                new Label("Cliente: " + nullToEmpty(d.clienteNombre)),
                new Label("Teléfono: " + nullToEmpty(d.telefono)),
                new Label("Email: " + nullToEmpty(d.email)),
                new Label("Dirección: " + nullToEmpty(d.direccion))
        );
        TitledPane clientePane = new TitledPane("Cliente", clientBox);

        ObservableList<FacturaItemDetalleRow> itemsRows = FXCollections.observableArrayList();
        if (d.items != null) {
            for (FacturaItemDetalle it : d.items) {
                FacturaItemDetalleRow row = new FacturaItemDetalleRow();
                row.tipo = it.esTareaManual ? "Tarea" : "Material";
                row.descripcion = it.esTareaManual ? nullToEmpty(it.tareaManual)
                        : nullToEmpty(it.materialNombre) + (it.unidadMedida != null && !it.unidadMedida.isEmpty() ? " (" + it.unidadMedida + ")" : "");
                row.cantidad = it.cantidad;
                row.precioUnitario = it.precioUnitario;
                row.subtotal = it.subtotal;
                itemsRows.add(row);
            }
        }
        TableView<FacturaItemDetalleRow> itemsTable = new TableView<>(itemsRows);
        TableColumn<FacturaItemDetalleRow, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().tipo));
        TableColumn<FacturaItemDetalleRow, String> colDesc = new TableColumn<>("Descripción");
        colDesc.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().descripcion));
        TableColumn<FacturaItemDetalleRow, Number> colCant = new TableColumn<>("Cant.");
        colCant.setCellValueFactory(c -> new javafx.beans.property.SimpleDoubleProperty(c.getValue().cantidad));
        TableColumn<FacturaItemDetalleRow, Number> colPrecio = new TableColumn<>("Precio unit.");
        colPrecio.setCellValueFactory(c -> new javafx.beans.property.SimpleDoubleProperty(c.getValue().precioUnitario));
        TableColumn<FacturaItemDetalleRow, Number> colSub = new TableColumn<>("Subtotal");
        colSub.setCellValueFactory(c -> new javafx.beans.property.SimpleDoubleProperty(c.getValue().subtotal));
        itemsTable.getColumns().add(colTipo);
        itemsTable.getColumns().add(colDesc);
        itemsTable.getColumns().add(colCant);
        itemsTable.getColumns().add(colPrecio);
        itemsTable.getColumns().add(colSub);
        itemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        VBox totalesBox = new VBox(4);
        totalesBox.setPadding(new Insets(10));
        totalesBox.getChildren().addAll(
                new Label(String.format(Locale.US, "Subtotal: €%.2f", d.subtotal)),
                new Label(d.ivaHabilitado ? String.format(Locale.US, "IVA (21%%): €%.2f", d.iva) : "IVA: No incluido"),
                new Label(String.format(Locale.US, "Total: €%.2f", d.total))
        );
        if (d.notas != null && !d.notas.isEmpty()) {
            totalesBox.getChildren().add(new Label("Notas: " + d.notas));
        }
        TitledPane totalesPane = new TitledPane("Totales", totalesBox);

        Button generarPdfBtn = new Button("Generar PDF");
        generarPdfBtn.setOnAction(e -> generarPdfFactura(d, stage));
        Button cerrarBtn = new Button("Cerrar");
        cerrarBtn.setOnAction(e -> stage.close());
        HBox botones = new HBox(10, generarPdfBtn, cerrarBtn);
        botones.setPadding(new Insets(10));

        VBox root = new VBox(10, infoPane, clientePane, new TitledPane("Items de la factura", itemsTable), totalesPane, botones);
        root.setPadding(new Insets(10));
        Scene scene = new Scene(root, 720, 580);
        stage.setScene(scene);
        stage.show();
    }

    private void generarPdfFactura(FacturaDetalle d, Stage parentStage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar PDF de la factura");
        fc.setInitialDirectory(configService.getCarpetaFacturasPdf().toFile());
        String nombreSeguro = d.numeroFactura != null ? d.numeroFactura.replaceAll("[\\\\/:*?\"<>|]", "_") : "factura";
        fc.setInitialFileName(nombreSeguro + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File file = fc.showSaveDialog(parentStage);
        if (file == null) return;
        try {
            java.nio.file.Path path = file.toPath();
            pdfGenerator.generateFacturaPdf(d, path);
            mostrarAlerta("Éxito", "PDF generado correctamente:\n" + path.toAbsolutePath());
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file);
            }
        } catch (IOException | DocumentException ex) {
            mostrarAlertaError("Error", "No se pudo generar el PDF: " + ex.getMessage());
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static class FacturaItemDetalleRow {
        String tipo;
        String descripcion;
        double cantidad;
        double precioUnitario;
        double subtotal;
    }
}

