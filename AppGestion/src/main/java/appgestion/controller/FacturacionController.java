package appgestion.controller;

import appgestion.model.Cliente;
import appgestion.model.Material;
import appgestion.model.PresupuestoItemRow;
import appgestion.service.ClienteService;
import appgestion.service.ConfigService;
import appgestion.service.FacturaService;
import appgestion.service.FacturaService.FacturaDetalle;
import appgestion.service.FacturaService.FacturaItemDetalle;
import appgestion.service.FacturaService.FacturaResumen;
import appgestion.service.MaterialService;
import appgestion.service.PdfGeneratorService;
import appgestion.service.PresupuestoService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import com.lowagie.text.DocumentException;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pestaña de facturación:
 * - Crear factura manual o importar desde presupuesto
 * - Lista de facturas con filtros y detalle/PDF
 */
public class FacturacionController {

    private static final Logger log = Logger.getLogger(FacturacionController.class.getName());
    private final FacturaService facturaService;
    private final ClienteService clienteService;
    private final PresupuestoService presupuestoService;
    private final MaterialService materialService;
    private final ConfigService configService = new ConfigService();
    private final PdfGeneratorService pdfGenerator = new PdfGeneratorService();
    private final ObservableList<FacturaResumen> facturas = FXCollections.observableArrayList();
    private final ObservableList<Cliente> clientesFactura = FXCollections.observableArrayList();
    private final ObservableList<Material> materialesFactura = FXCollections.observableArrayList();
    private final ObservableList<PresupuestoItemRow> itemsFactura = FXCollections.observableArrayList();

    private TableView<FacturaResumen> facturasTable;
    private TextField presupuestoIdField;
    private TextField buscarField;
    private ComboBox<Integer> anioCombo;
    private ComboBox<Integer> mesCombo;
    private ComboBox<String> estadoCombo;
    private Label resumenLabel;

    // Formulario crear factura manual
    private ComboBox<Cliente> clienteFacturaCombo;
    private TextField numeroFacturaField;
    private javafx.scene.control.DatePicker fechaVencimientoPicker;
    private ComboBox<String> metodoPagoCombo;
    private ComboBox<String> estadoPagoCombo;
    private CheckBox ivaHabilitadoCheck;
    private TextArea notasFacturaArea;
    private ComboBox<Material> materialFacturaCombo;
    private TextField cantidadFacturaField;
    private TextField tareaDescripcionField;
    private TextField tareaCantidadField;
    private TextField tareaPrecioField;
    private TableView<PresupuestoItemRow> itemsFacturaTable;
    private Label subtotalFacturaLabel;
    private Label ivaFacturaLabel;
    private Label totalFacturaLabel;

    public FacturacionController(FacturaService facturaService,
                                 ClienteService clienteService,
                                 PresupuestoService presupuestoService,
                                 MaterialService materialService) {
        this.facturaService = facturaService;
        this.clienteService = clienteService;
        this.presupuestoService = presupuestoService;
        this.materialService = materialService;
    }

    public Node createContent() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        VBox topBox = new VBox(8);

        // --- Formulario Crear factura (manual o tras importar) ---
        ScrollPane formScroll = new ScrollPane();
        formScroll.setFitToWidth(true);
        formScroll.setMaxHeight(420);
        VBox formBox = new VBox(8);
        formBox.setPadding(new Insets(6));

        TitledPane datosPane = new TitledPane();
        datosPane.setText("Crear factura");
        datosPane.setCollapsible(false);
        VBox datosBox = new VBox(8);
        HBox fila1 = new HBox(10);
        clienteFacturaCombo = new ComboBox<>();
        clienteFacturaCombo.setPrefWidth(280);
        numeroFacturaField = new TextField();
        numeroFacturaField.setPromptText("Nº factura");
        Button autoNumBtn = new Button("Auto");
        autoNumBtn.setOnAction(e -> onAutoNumeroFactura());
        fechaVencimientoPicker = new javafx.scene.control.DatePicker();
        fechaVencimientoPicker.setValue(LocalDate.now().plusDays(30));
        fila1.getChildren().addAll(
                new Label("Cliente:"), clienteFacturaCombo,
                new Label("Nº factura:"), numeroFacturaField, autoNumBtn,
                new Label("Vencimiento:"), fechaVencimientoPicker
        );
        HBox fila2 = new HBox(10);
        metodoPagoCombo = new ComboBox<>();
        metodoPagoCombo.getItems().addAll("Transferencia", "Efectivo", "Tarjeta", "Bizum", "Otro");
        metodoPagoCombo.getSelectionModel().selectFirst();
        estadoPagoCombo = new ComboBox<>();
        estadoPagoCombo.getItems().addAll("No Pagada", "Pagada", "Parcial");
        estadoPagoCombo.getSelectionModel().selectFirst();
        ivaHabilitadoCheck = new CheckBox("Incluir IVA (21%)");
        ivaHabilitadoCheck.setSelected(true);
        ivaHabilitadoCheck.setOnAction(e -> recalcularTotalesFactura());
        fila2.getChildren().addAll(
                new Label("Método pago:"), metodoPagoCombo,
                new Label("Estado:"), estadoPagoCombo,
                ivaHabilitadoCheck
        );
        notasFacturaArea = new TextArea();
        notasFacturaArea.setPromptText("Notas (opcional)");
        notasFacturaArea.setPrefRowCount(2);
        datosBox.getChildren().addAll(fila1, fila2, new Label("Notas:"), notasFacturaArea);
        datosPane.setContent(datosBox);
        formBox.getChildren().add(datosPane);

        TitledPane materialPane = new TitledPane();
        materialPane.setText("Agregar Material");
        materialPane.setCollapsible(false);
        HBox materialBox = new HBox(10);
        materialFacturaCombo = new ComboBox<>();
        materialFacturaCombo.setPrefWidth(300);
        cantidadFacturaField = new TextField();
        cantidadFacturaField.setPromptText("Cantidad");
        Button agregarMaterialBtn = new Button("➕ Agregar Material");
        agregarMaterialBtn.setOnAction(e -> onAgregarMaterialFactura());
        materialBox.getChildren().addAll(
                new Label("Material:"), materialFacturaCombo,
                new Label("Cantidad:"), cantidadFacturaField,
                agregarMaterialBtn
        );
        materialPane.setContent(materialBox);
        formBox.getChildren().add(materialPane);

        TitledPane tareaPane = new TitledPane();
        tareaPane.setText("Agregar Tarea Manual");
        tareaPane.setCollapsible(false);
        VBox tareaBox = new VBox(8);
        HBox tareaFila1 = new HBox(10);
        tareaDescripcionField = new TextField();
        tareaDescripcionField.setPrefWidth(350);
        tareaDescripcionField.setPromptText("Descripción");
        tareaFila1.getChildren().addAll(new Label("Descripción:"), tareaDescripcionField);
        HBox tareaFila2 = new HBox(10);
        tareaCantidadField = new TextField();
        tareaCantidadField.setPromptText("Cantidad");
        tareaPrecioField = new TextField();
        tareaPrecioField.setPromptText("Precio Unit.");
        Button agregarTareaBtn = new Button("➕ Agregar Tarea");
        agregarTareaBtn.setOnAction(e -> onAgregarTareaFactura());
        tareaFila2.getChildren().addAll(
                new Label("Cantidad:"), tareaCantidadField,
                new Label("Precio Unit.:"), tareaPrecioField,
                agregarTareaBtn
        );
        tareaBox.getChildren().addAll(tareaFila1, tareaFila2);
        tareaPane.setContent(tareaBox);
        formBox.getChildren().add(tareaPane);

        TitledPane itemsPane = new TitledPane();
        itemsPane.setText("Items de la factura");
        itemsPane.setCollapsible(false);
        itemsFacturaTable = new TableView<>(itemsFactura);
        itemsFacturaTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<PresupuestoItemRow, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().isEsTareaManual() ? "Tarea" : "Material"));
        colTipo.setMaxWidth(90);
        TableColumn<PresupuestoItemRow, String> colDesc = new TableColumn<>("Descripción");
        colDesc.setCellValueFactory(data -> data.getValue().descripcionProperty());
        TableColumn<PresupuestoItemRow, Number> colCant = new TableColumn<>("Cantidad");
        colCant.setCellValueFactory(data -> data.getValue().cantidadProperty());
        TableColumn<PresupuestoItemRow, Number> colPrecio = new TableColumn<>("Precio Unit.");
        colPrecio.setCellValueFactory(data -> data.getValue().precioUnitarioProperty());
        TableColumn<PresupuestoItemRow, Number> colSub = new TableColumn<>("Subtotal");
        colSub.setCellValueFactory(data -> data.getValue().subtotalProperty());
        itemsFacturaTable.getColumns().add(colTipo);
        itemsFacturaTable.getColumns().add(colDesc);
        itemsFacturaTable.getColumns().add(colCant);
        itemsFacturaTable.getColumns().add(colPrecio);
        itemsFacturaTable.getColumns().add(colSub);
        ContextMenu ctxMenu = new ContextMenu();
        MenuItem eliminarItem = new MenuItem("Eliminar item");
        eliminarItem.setOnAction(e -> onEliminarItemFactura());
        ctxMenu.getItems().add(eliminarItem);
        itemsFacturaTable.setContextMenu(ctxMenu);
        VBox itemsBox = new VBox(itemsFacturaTable);
        VBox.setVgrow(itemsFacturaTable, Priority.ALWAYS);
        itemsPane.setContent(itemsBox);
        formBox.getChildren().add(itemsPane);

        HBox totalesRow = new HBox(20);
        subtotalFacturaLabel = new Label("Subtotal: €0.00");
        ivaFacturaLabel = new Label("IVA: €0.00");
        totalFacturaLabel = new Label("Total: €0.00");
        totalesRow.getChildren().addAll(subtotalFacturaLabel, ivaFacturaLabel, totalFacturaLabel);

        Button guardarFacturaBtn = new Button("Guardar factura");
        guardarFacturaBtn.setOnAction(e -> onGuardarFacturaManual());
        Button limpiarFacturaBtn = new Button("Limpiar");
        limpiarFacturaBtn.setOnAction(e -> onLimpiarFormularioFactura());
        Button importarPresupuestoBtn = new Button("Importar desde presupuesto");
        importarPresupuestoBtn.setOnAction(e -> onImportarDesdePresupuesto());
        HBox botonesFactura = new HBox(10, guardarFacturaBtn, limpiarFacturaBtn, importarPresupuestoBtn);
        formBox.getChildren().addAll(totalesRow, botonesFactura);
        formScroll.setContent(formBox);
        topBox.getChildren().add(formScroll);

        // Crear factura desde presupuesto (por ID)
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
        cargarDatosFormularioFactura();
        recargar();
        recalcularTotalesFactura();

        return root;
    }

    private void cargarDatosFormularioFactura() {
        clientesFactura.setAll(clienteService.obtenerTodos());
        materialesFactura.setAll(materialService.obtenerTodos());
        clienteFacturaCombo.setItems(clientesFactura);
        clienteFacturaCombo.setConverter(new javafx.util.StringConverter<Cliente>() {
            @Override
            public String toString(Cliente c) { return c == null ? "" : c.getNombre(); }
            @Override
            public Cliente fromString(String s) { return null; }
        });
        materialFacturaCombo.setItems(materialesFactura);
        materialFacturaCombo.setConverter(new javafx.util.StringConverter<Material>() {
            @Override
            public String toString(Material m) {
                if (m == null) return "";
                return m.getNombre() + " (" + m.getUnidadMedida() + ") - €" + m.getPrecioUnitario();
            }
            @Override
            public Material fromString(String s) { return null; }
        });
    }

    private void onAutoNumeroFactura() {
        try {
            numeroFacturaField.setText(facturaService.generarNumeroFactura());
        } catch (SQLException e) {
            mostrarAlertaError("Error", "No se pudo generar el número: " + e.getMessage());
        }
    }

    private void onAgregarMaterialFactura() {
        Material m = materialFacturaCombo.getValue();
        if (m == null) {
            mostrarAlerta("Validación", "Selecciona un material.");
            return;
        }
        double cantidad = parseDoubleOrZero(cantidadFacturaField.getText());
        if (cantidad <= 0) {
            mostrarAlerta("Validación", "La cantidad debe ser mayor que cero.");
            return;
        }
        PresupuestoItemRow row = new PresupuestoItemRow();
        row.setMaterialId(m.getId());
        row.setDescripcion(m.getNombre());
        row.setEsTareaManual(false);
        row.setCantidad(cantidad);
        row.setPrecioUnitario(m.getPrecioUnitario());
        row.setAplicaIva(true);
        row.setVisiblePdf(true);
        itemsFactura.add(row);
        cantidadFacturaField.clear();
        recalcularTotalesFactura();
    }

    private void onAgregarTareaFactura() {
        String desc = tareaDescripcionField.getText();
        if (desc == null || desc.trim().isEmpty()) {
            mostrarAlerta("Validación", "La descripción de la tarea es obligatoria.");
            return;
        }
        double cantidad = parseDoubleOrZero(tareaCantidadField.getText());
        double precio = parseDoubleOrZero(tareaPrecioField.getText());
        if (cantidad <= 0 || precio < 0) {
            mostrarAlerta("Validación", "Cantidad debe ser > 0 y precio >= 0.");
            return;
        }
        PresupuestoItemRow row = new PresupuestoItemRow();
        row.setMaterialId(0);
        row.setDescripcion(desc.trim());
        row.setEsTareaManual(true);
        row.setCantidad(cantidad);
        row.setPrecioUnitario(precio);
        row.setAplicaIva(true);
        row.setVisiblePdf(true);
        itemsFactura.add(row);
        tareaDescripcionField.clear();
        tareaCantidadField.clear();
        tareaPrecioField.clear();
        recalcularTotalesFactura();
    }

    private void onEliminarItemFactura() {
        PresupuestoItemRow sel = itemsFacturaTable.getSelectionModel().getSelectedItem();
        if (sel != null) {
            itemsFactura.remove(sel);
            recalcularTotalesFactura();
        }
    }

    private void recalcularTotalesFactura() {
        if (itemsFactura.isEmpty()) {
            subtotalFacturaLabel.setText("Subtotal: €0.00");
            ivaFacturaLabel.setText("IVA: €0.00");
            totalFacturaLabel.setText("Total: €0.00");
            return;
        }
        PresupuestoService.TotalesPresupuesto t = presupuestoService.calcularTotalesCompleto(
                itemsFactura, 0, 0, true, ivaHabilitadoCheck != null && ivaHabilitadoCheck.isSelected());
        subtotalFacturaLabel.setText(String.format(Locale.US, "Subtotal: €%.2f", t.subtotal));
        ivaFacturaLabel.setText(String.format(Locale.US, "IVA: €%.2f", t.iva));
        totalFacturaLabel.setText(String.format(Locale.US, "Total: €%.2f", t.total));
    }

    private void onGuardarFacturaManual() {
        Cliente c = clienteFacturaCombo.getValue();
        if (c == null) {
            mostrarAlerta("Validación", "Selecciona un cliente.");
            return;
        }
        if (itemsFactura.isEmpty()) {
            mostrarAlerta("Validación", "Añade al menos un item a la factura.");
            return;
        }
        String numero = numeroFacturaField.getText();
        String fechaVenc = fechaVencimientoPicker.getValue() != null
                ? fechaVencimientoPicker.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE)
                : "";
        String metodoPago = metodoPagoCombo.getValue() != null ? metodoPagoCombo.getValue() : "Transferencia";
        String estadoPago = estadoPagoCombo.getValue() != null ? estadoPagoCombo.getValue() : "No Pagada";
        String notas = notasFacturaArea.getText();
        if (notas == null) notas = "";
        boolean ivaHab = ivaHabilitadoCheck.isSelected();
        List<PresupuestoItemRow> itemsList = new ArrayList<>(itemsFactura);
        try {
            int facturaId = facturaService.crearFacturaManual(
                    c.getId(), itemsList, numero.isEmpty() ? null : numero, fechaVenc,
                    metodoPago, estadoPago, notas, ivaHab);
            mostrarAlerta("Factura creada", "Factura creada con ID " + facturaId + ".");
            onLimpiarFormularioFactura();
            recargar();
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Error al guardar factura", e);
            mostrarAlertaError("Error", "No se pudo guardar la factura: " + e.getMessage());
        }
    }

    private void onLimpiarFormularioFactura() {
        clienteFacturaCombo.getSelectionModel().clearSelection();
        numeroFacturaField.clear();
        fechaVencimientoPicker.setValue(LocalDate.now().plusDays(30));
        metodoPagoCombo.getSelectionModel().selectFirst();
        estadoPagoCombo.getSelectionModel().selectFirst();
        ivaHabilitadoCheck.setSelected(true);
        notasFacturaArea.clear();
        itemsFactura.clear();
        recalcularTotalesFactura();
    }

    private void onImportarDesdePresupuesto() {
        abrirDialogoImportarPresupuesto();
    }

    private void abrirDialogoImportarPresupuesto() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Importar desde presupuesto");
        ObservableList<PresupuestoService.PresupuestoResumen> items = FXCollections.observableArrayList();
        TableView<PresupuestoService.PresupuestoResumen> table = new TableView<>(items);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<PresupuestoService.PresupuestoResumen, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().id).asObject());
        TableColumn<PresupuestoService.PresupuestoResumen, String> colCliente = new TableColumn<>("Cliente");
        colCliente.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().clienteNombre));
        TableColumn<PresupuestoService.PresupuestoResumen, String> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().fechaCreacion));
        TableColumn<PresupuestoService.PresupuestoResumen, Double> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(c -> new javafx.beans.property.SimpleDoubleProperty(c.getValue().total).asObject());
        table.getColumns().add(colId);
        table.getColumns().add(colCliente);
        table.getColumns().add(colFecha);
        table.getColumns().add(colTotal);
        Label avisoVacio = new Label("No hay presupuestos. Crea uno en la pestaña \"Gestión de Presupuestos\".");
        avisoVacio.setVisible(false);
        Button importarBtn = new Button("Importar");
        importarBtn.setOnAction(e -> {
            PresupuestoService.PresupuestoResumen sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) {
                mostrarAlerta("Aviso", "Selecciona un presupuesto.");
                return;
            }
            rellenarFormularioDesdePresupuesto(sel.id);
            stage.close();
        });
        Button cancelarBtn = new Button("Cancelar");
        cancelarBtn.setOnAction(e -> stage.close());
        Button refrescarBtn = new Button("Actualizar lista");
        refrescarBtn.setOnAction(e -> {
            items.setAll(presupuestoService.obtenerPresupuestos());
            avisoVacio.setVisible(items.isEmpty());
        });
        VBox root = new VBox(10, table, avisoVacio, new HBox(10, importarBtn, refrescarBtn, cancelarBtn));
        root.setPadding(new Insets(10));
        stage.setScene(new Scene(root, 560, 400));
        stage.setOnShown(e -> {
            items.setAll(presupuestoService.obtenerPresupuestos());
            avisoVacio.setVisible(items.isEmpty());
        });
        stage.show();
    }

    private void rellenarFormularioDesdePresupuesto(int presupuestoId) {
        PresupuestoService.PresupuestoDetalle d = presupuestoService.obtenerPresupuestoPorId(presupuestoId);
        if (d == null) {
            mostrarAlertaError("Error", "No se pudo cargar el presupuesto.");
            return;
        }
        itemsFactura.clear();
        Cliente clienteSel = null;
        for (Cliente c : clientesFactura) {
            if (c.getId() == d.clienteId) {
                clienteSel = c;
                break;
            }
        }
        if (clienteSel != null) clienteFacturaCombo.setValue(clienteSel);
        try {
            numeroFacturaField.setText(facturaService.generarNumeroFactura());
        } catch (SQLException ignored) { }
        fechaVencimientoPicker.setValue(LocalDate.now().plusDays(30));
        estadoPagoCombo.getSelectionModel().selectFirst();
        ivaHabilitadoCheck.setSelected(d.ivaHabilitado);
        if (d.items != null) {
            for (PresupuestoService.PresupuestoItemDetalle it : d.items) {
                PresupuestoItemRow row = new PresupuestoItemRow();
                row.setMaterialId(Optional.ofNullable(it.materialId).orElse(0));
                String desc = it.esTareaManual && it.tareaManual != null && !it.tareaManual.isEmpty()
                        ? it.tareaManual
                        : (it.materialNombre != null ? it.materialNombre : "");
                row.setDescripcion(desc);
                row.setEsTareaManual(it.esTareaManual);
                row.setCantidad(it.cantidad);
                row.setPrecioUnitario(it.precioUnitario);
                row.setAplicaIva(true);
                row.setVisiblePdf(true);
                itemsFactura.add(row);
            }
        }
        recalcularTotalesFactura();
    }

    private double parseDoubleOrZero(String text) {
        if (text == null || text.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(text.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return 0.0;
        }
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

