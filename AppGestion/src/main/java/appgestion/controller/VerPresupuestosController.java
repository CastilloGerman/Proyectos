package appgestion.controller;

import appgestion.service.ConfigService;
import appgestion.service.PdfGeneratorService;
import com.lowagie.text.DocumentException;
import appgestion.service.PresupuestoService;
import appgestion.service.PresupuestoService.PresupuestoDetalle;
import appgestion.service.PresupuestoService.PresupuestoItemDetalle;
import appgestion.service.PresupuestoService.PresupuestoResumen;
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

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Pestaña simple para listar presupuestos existentes.
 */
public class VerPresupuestosController {

    private final PresupuestoService presupuestoService;
    private final ConfigService configService = new ConfigService();
    private final PdfGeneratorService pdfGenerator = new PdfGeneratorService();
    private final ObservableList<PresupuestoResumen> presupuestos = FXCollections.observableArrayList();

    private TableView<PresupuestoResumen> table;
    private TextField buscarField;
    private ComboBox<Integer> anioCombo;
    private ComboBox<Integer> mesCombo;
    private Label resumenLabel;

    public VerPresupuestosController(PresupuestoService presupuestoService) {
        this.presupuestoService = presupuestoService;
    }

    public Node createContent() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Filtros avanzados
        HBox filtros = new HBox(10);
        buscarField = new TextField();
        buscarField.setPromptText("Buscar por cliente, ID o fecha");
        anioCombo = new ComboBox<>();
        anioCombo.setPromptText("Año");
        mesCombo = new ComboBox<>();
        mesCombo.setPromptText("Mes");
        Button aplicarBtn = new Button("Filtrar");
        aplicarBtn.setOnAction(e -> recargar());
        Button limpiarBtn = new Button("Limpiar");
        limpiarBtn.setOnAction(e -> {
            buscarField.clear();
            anioCombo.getSelectionModel().clearSelection();
            mesCombo.getSelectionModel().clearSelection();
            recargar();
        });
        Button verDetalleBtn = new Button("Ver detalle");
        verDetalleBtn.setOnAction(e -> verDetallePresupuesto());
        Button marcarAprobadoBtn = new Button("✓ Marcar Aprobado");
        marcarAprobadoBtn.getStyleClass().add("success-button");
        marcarAprobadoBtn.setOnAction(e -> marcarEstadoPresupuesto("Aprobado"));
        Button marcarRechazadoBtn = new Button("✗ Marcar Rechazado");
        marcarRechazadoBtn.getStyleClass().add("danger-button");
        marcarRechazadoBtn.setOnAction(e -> marcarEstadoPresupuesto("Rechazado"));
        filtros.getChildren().addAll(
                new Label("Buscar:"), buscarField,
                new Label("Año:"), anioCombo,
                new Label("Mes:"), mesCombo,
                aplicarBtn, limpiarBtn, verDetalleBtn, marcarAprobadoBtn, marcarRechazadoBtn
        );
        root.setTop(filtros);

        table = new TableView<>(presupuestos);
        table.setOnMouseClicked(e -> { if (e.getClickCount() == 2) verDetallePresupuesto(); });
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<PresupuestoResumen, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<PresupuestoResumen, String> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaCreacion"));

        TableColumn<PresupuestoResumen, String> colCliente = new TableColumn<>("Cliente");
        colCliente.setCellValueFactory(new PropertyValueFactory<>("clienteNombre"));

        TableColumn<PresupuestoResumen, Double> colSubtotal = new TableColumn<>("Subtotal");
        colSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));

        TableColumn<PresupuestoResumen, Double> colIva = new TableColumn<>("IVA");
        colIva.setCellValueFactory(new PropertyValueFactory<>("iva"));

        TableColumn<PresupuestoResumen, Double> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        TableColumn<PresupuestoResumen, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        table.getColumns().add(colId);
        table.getColumns().add(colFecha);
        table.getColumns().add(colCliente);
        table.getColumns().add(colSubtotal);
        table.getColumns().add(colIva);
        table.getColumns().add(colTotal);
        table.getColumns().add(colEstado);

        resumenLabel = new Label();

        VBox box = new VBox(8, table, resumenLabel);
        root.setCenter(box);

        cargarCombos();
        recargar();

        return root;
    }

    private void cargarCombos() {
        anioCombo.getItems().setAll(presupuestoService.obtenerAniosDisponibles());
        mesCombo.getItems().clear();
        mesCombo.getItems().addAll(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
    }

    public void recargar() {
        String termino = buscarField != null ? buscarField.getText() : null;
        Integer anio = anioCombo != null ? anioCombo.getValue() : null;
        Integer mes = mesCombo != null ? mesCombo.getValue() : null;
        try {
            presupuestos.setAll(presupuestoService.buscarPresupuestos(termino, anio, mes));
            actualizarResumen();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void actualizarResumen() {
        double subtotal = 0;
        double iva = 0;
        double total = 0;
        for (PresupuestoResumen r : presupuestos) {
            subtotal += r.subtotal;
            iva += r.iva;
            total += r.total;
        }
        resumenLabel.setText(String.format(
                "Presupuestos: %d | Subtotal: €%.2f | IVA: €%.2f | Total: €%.2f",
                presupuestos.size(), subtotal, iva, total
        ));
    }

    private void marcarEstadoPresupuesto(String estado) {
        PresupuestoResumen sel = table == null ? null : table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            mostrarAlerta("Aviso", "Seleccione un presupuesto para cambiar su estado.");
            return;
        }
        if (presupuestoService.actualizarEstadoPresupuesto(sel.id, estado)) {
            mostrarAlerta("Éxito", "Presupuesto #" + sel.id + " marcado como " + estado + ".");
            recargar();
        } else {
            mostrarAlertaError("Error", "No se pudo actualizar el estado.");
        }
    }

    private void verDetallePresupuesto() {
        PresupuestoResumen sel = table == null ? null : table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            mostrarAlerta("Aviso", "Seleccione un presupuesto para ver el detalle.");
            return;
        }
        PresupuestoDetalle detalle = presupuestoService.obtenerPresupuestoPorId(sel.id);
        if (detalle == null) {
            mostrarAlerta("Error", "No se pudo cargar el presupuesto.");
            return;
        }
        mostrarDialogoDetallePresupuesto(detalle);
    }

    private void mostrarDialogoDetallePresupuesto(PresupuestoDetalle d) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Detalle Presupuesto #" + d.id);

        VBox clientBox = new VBox(4);
        clientBox.setPadding(new Insets(10));
        clientBox.getChildren().addAll(
                new Label("Estado: " + nullToEmpty(d.estado)),
                new Label("Cliente: " + nullToEmpty(d.clienteNombre)),
                new Label("Teléfono: " + nullToEmpty(d.telefono)),
                new Label("Email: " + nullToEmpty(d.email)),
                new Label("Dirección: " + nullToEmpty(d.direccion))
        );
        TitledPane clientePane = new TitledPane("Información del cliente", clientBox);
        clientePane.setExpanded(true);

        ObservableList<ItemDetalleRow> itemsRows = FXCollections.observableArrayList();
        if (d.items != null) {
            for (PresupuestoItemDetalle it : d.items) {
                ItemDetalleRow row = new ItemDetalleRow();
                row.tipo = it.esTareaManual ? "Tarea" : "Material";
                row.descripcion = it.esTareaManual ? nullToEmpty(it.tareaManual)
                        : nullToEmpty(it.materialNombre) + (it.unidadMedida != null && !it.unidadMedida.isEmpty() ? " (" + it.unidadMedida + ")" : "");
                row.cantidad = it.cantidad;
                row.precioUnitario = it.precioUnitario;
                row.subtotal = it.subtotal;
                itemsRows.add(row);
            }
        }
        TableView<ItemDetalleRow> itemsTable = new TableView<>(itemsRows);
        TableColumn<ItemDetalleRow, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().tipo));
        TableColumn<ItemDetalleRow, String> colDesc = new TableColumn<>("Descripción");
        colDesc.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().descripcion));
        TableColumn<ItemDetalleRow, Number> colCant = new TableColumn<>("Cant.");
        colCant.setCellValueFactory(c -> new javafx.beans.property.SimpleDoubleProperty(c.getValue().cantidad));
        TableColumn<ItemDetalleRow, Number> colPrecio = new TableColumn<>("Precio unit.");
        colPrecio.setCellValueFactory(c -> new javafx.beans.property.SimpleDoubleProperty(c.getValue().precioUnitario));
        TableColumn<ItemDetalleRow, Number> colSub = new TableColumn<>("Subtotal");
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
        TitledPane totalesPane = new TitledPane("Totales", totalesBox);

        Button marcarAprobadoBtn = new Button("✓ Marcar Aprobado");
        marcarAprobadoBtn.getStyleClass().add("success-button");
        marcarAprobadoBtn.setOnAction(e -> { marcarEstadoPresupuestoDesdeDetalle(d.id, "Aprobado", stage); });
        Button marcarRechazadoBtn = new Button("✗ Marcar Rechazado");
        marcarRechazadoBtn.getStyleClass().add("danger-button");
        marcarRechazadoBtn.setOnAction(e -> { marcarEstadoPresupuestoDesdeDetalle(d.id, "Rechazado", stage); });
        Button generarPdfBtn = new Button("Generar PDF");
        generarPdfBtn.setOnAction(e -> generarPdfPresupuesto(d, stage));
        Button cerrarBtn = new Button("Cerrar");
        cerrarBtn.setOnAction(e -> stage.close());
        HBox botones = new HBox(10, marcarAprobadoBtn, marcarRechazadoBtn, generarPdfBtn, cerrarBtn);
        botones.setPadding(new Insets(10));

        VBox root = new VBox(10, clientePane, new TitledPane("Items del presupuesto", itemsTable), totalesPane, botones);
        root.setPadding(new Insets(10));
        Scene scene = new Scene(root, 700, 550);
        stage.setScene(scene);
        stage.show();
    }

    private void marcarEstadoPresupuestoDesdeDetalle(int presupuestoId, String estado, Stage stage) {
        if (presupuestoService.actualizarEstadoPresupuesto(presupuestoId, estado)) {
            mostrarAlerta("Éxito", "Presupuesto #" + presupuestoId + " marcado como " + estado + ".");
            recargar();
            stage.close();
        } else {
            mostrarAlertaError("Error", "No se pudo actualizar el estado.");
        }
    }

    private void generarPdfPresupuesto(PresupuestoDetalle d, Stage parentStage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar PDF del presupuesto");
        fc.setInitialDirectory(configService.getCarpetaPresupuestosPdf().toFile());
        fc.setInitialFileName("presupuesto_" + d.id + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File file = fc.showSaveDialog(parentStage);
        if (file == null) return;
        try {
            Path path = file.toPath();
            pdfGenerator.generatePresupuestoPdf(d, path);
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

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(mensaje);
        a.showAndWait();
    }

    private void mostrarAlertaError(String titulo, String mensaje) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(mensaje);
        a.showAndWait();
    }

    private static class ItemDetalleRow {
        String tipo;
        String descripcion;
        double cantidad;
        double precioUnitario;
        double subtotal;
    }
}

