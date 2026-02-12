package appgestion.controller;

import appgestion.model.Cliente;
import appgestion.model.Material;
import appgestion.model.PresupuestoItemRow;
import appgestion.service.ClienteService;
import appgestion.service.MaterialService;
import appgestion.service.PresupuestoService;
import appgestion.util.FxAlerts;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controlador JavaFX para la pestaña de creación de presupuestos.
 * Implementa un flujo similar al de Tkinter: selección de cliente,
 * selección de materiales/tareas, items, descuentos globales e IVA.
 */
public class PresupuestosController {

    private static final Logger log = Logger.getLogger(PresupuestosController.class.getName());
    private final ClienteService clienteService;
    private final MaterialService materialService;
    private final PresupuestoService presupuestoService;

    private final ObservableList<Cliente> clientes = FXCollections.observableArrayList();
    private final ObservableList<Material> materiales = FXCollections.observableArrayList();
    private final ObservableList<PresupuestoItemRow> items = FXCollections.observableArrayList();

    private ComboBox<Cliente> clienteCombo;
    private ComboBox<Material> materialCombo;
    private TextField cantidadField;
    private TextField tareaDescripcionField;
    private TextField tareaCantidadField;
    private TextField tareaPrecioField;
    private TableView<PresupuestoItemRow> itemsTable;

    private TextField descuentoPorcentajeField;
    private TextField descuentoFijoField;
    private CheckBox descuentoAntesIvaCheck;
    private CheckBox ivaHabilitadoCheck;

    private Label subtotalLabel;
    private Label ivaLabel;
    private Label totalLabel;

    public PresupuestosController(ClienteService clienteService,
                                  MaterialService materialService,
                                  PresupuestoService presupuestoService) {
        this.clienteService = clienteService;
        this.materialService = materialService;
        this.presupuestoService = presupuestoService;
    }

    public Node createContent() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);

        VBox mainBox = new VBox(10);
        mainBox.setPadding(new Insets(10));

        // Selección de cliente
        TitledPane clientePane = new TitledPane();
        clientePane.setText("Seleccionar Cliente");
        HBox clienteBox = new HBox(10);
        clienteCombo = new ComboBox<>();
        clienteCombo.setPrefWidth(350);
        Button refrescarListasBtn = new Button("🔄 Refrescar listas");
        refrescarListasBtn.setOnAction(e -> refreshDatos());
        clienteBox.getChildren().addAll(new Label("Cliente:"), clienteCombo, refrescarListasBtn);
        clientePane.setContent(clienteBox);
        clientePane.setCollapsible(false);

        // Sección de materiales
        TitledPane materialPane = new TitledPane();
        materialPane.setText("Agregar Material");
        HBox materialBox = new HBox(10);
        materialCombo = new ComboBox<>();
        materialCombo.setPrefWidth(300);
        cantidadField = new TextField();
        cantidadField.setPromptText("Cantidad");
        Button agregarMaterialBtn = new Button("➕ Agregar Material");
        agregarMaterialBtn.setOnAction(e -> onAgregarMaterial());
        materialBox.getChildren().addAll(
                new Label("Material:"), materialCombo,
                new Label("Cantidad:"), cantidadField,
                agregarMaterialBtn
        );
        materialPane.setContent(materialBox);
        materialPane.setCollapsible(false);

        // Sección de tareas manuales
        TitledPane tareaPane = new TitledPane();
        tareaPane.setText("Agregar Tarea Manual");
        VBox tareaBox = new VBox(8);
        HBox tareaFila1 = new HBox(10);
        tareaDescripcionField = new TextField();
        tareaDescripcionField.setPrefWidth(350);
        tareaFila1.getChildren().addAll(new Label("Descripción:"), tareaDescripcionField);
        HBox tareaFila2 = new HBox(10);
        tareaCantidadField = new TextField();
        tareaCantidadField.setPromptText("Cantidad");
        tareaPrecioField = new TextField();
        tareaPrecioField.setPromptText("Precio Unit.");
        Button agregarTareaBtn = new Button("➕ Agregar Tarea");
        agregarTareaBtn.setOnAction(e -> onAgregarTarea());
        tareaFila2.getChildren().addAll(
                new Label("Cantidad:"), tareaCantidadField,
                new Label("Precio Unit.:"), tareaPrecioField,
                agregarTareaBtn
        );
        tareaBox.getChildren().addAll(tareaFila1, tareaFila2);
        tareaPane.setContent(tareaBox);
        tareaPane.setCollapsible(false);

        // Tabla de items
        TitledPane itemsPane = new TitledPane();
        itemsPane.setText("Items del Presupuesto");
        itemsTable = new TableView<>(items);
        itemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<PresupuestoItemRow, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().isEsTareaManual() ? "Tarea" : "Material"
        ));
        colTipo.setMaxWidth(90);

        TableColumn<PresupuestoItemRow, String> colDesc = new TableColumn<>("Descripción");
        colDesc.setCellValueFactory(data -> data.getValue().descripcionProperty());

        TableColumn<PresupuestoItemRow, Number> colCant = new TableColumn<>("Cantidad");
        colCant.setCellValueFactory(data -> data.getValue().cantidadProperty());

        TableColumn<PresupuestoItemRow, Number> colPrecio = new TableColumn<>("Precio Unit.");
        colPrecio.setCellValueFactory(data -> data.getValue().precioUnitarioProperty());

        TableColumn<PresupuestoItemRow, Number> colDescPct = new TableColumn<>("Desc. %");
        colDescPct.setCellValueFactory(data -> data.getValue().descuentoPorcentajeProperty());

        TableColumn<PresupuestoItemRow, Number> colDescFijo = new TableColumn<>("Desc. €");
        colDescFijo.setCellValueFactory(data -> data.getValue().descuentoFijoProperty());

        TableColumn<PresupuestoItemRow, Boolean> colIva = new TableColumn<>("IVA");
        colIva.setCellValueFactory(data -> data.getValue().aplicaIvaProperty());

        TableColumn<PresupuestoItemRow, Number> colSubtotal = new TableColumn<>("Subtotal");
        colSubtotal.setCellValueFactory(data -> data.getValue().subtotalProperty());

        itemsTable.getColumns().add(colTipo);
        itemsTable.getColumns().add(colDesc);
        itemsTable.getColumns().add(colCant);
        itemsTable.getColumns().add(colPrecio);
        itemsTable.getColumns().add(colDescPct);
        itemsTable.getColumns().add(colDescFijo);
        itemsTable.getColumns().add(colIva);
        itemsTable.getColumns().add(colSubtotal);

        ContextMenu ctxMenu = new ContextMenu();
        MenuItem eliminarItem = new MenuItem("Eliminar item");
        eliminarItem.setOnAction(e -> onEliminarItem());
        ctxMenu.getItems().add(eliminarItem);
        itemsTable.setContextMenu(ctxMenu);

        VBox itemsBox = new VBox(itemsTable);
        VBox.setVgrow(itemsTable, Priority.ALWAYS);
        itemsPane.setContent(itemsBox);
        itemsPane.setCollapsible(false);

        // Totales y descuentos globales
        TitledPane totalesPane = new TitledPane();
        totalesPane.setText("Totales");
        VBox totalesBox = new VBox(8);

        HBox descRow = new HBox(10);
        descuentoPorcentajeField = new TextField("0");
        descuentoFijoField = new TextField("0");
        descuentoAntesIvaCheck = new CheckBox("Descuento antes de IVA");
        descuentoAntesIvaCheck.setSelected(true);
        descRow.getChildren().addAll(
                new Label("Descuento %:"), descuentoPorcentajeField,
                new Label("Descuento €:"), descuentoFijoField,
                descuentoAntesIvaCheck
        );

        HBox ivaRow = new HBox(10);
        ivaHabilitadoCheck = new CheckBox("Incluir IVA (21%)");
        ivaHabilitadoCheck.setSelected(true);
        ivaRow.getChildren().addAll(ivaHabilitadoCheck);

        HBox totalsRow = new HBox(20);
        subtotalLabel = new Label("Subtotal: €0.00");
        ivaLabel = new Label("IVA: €0.00");
        totalLabel = new Label("Total: €0.00");
        totalsRow.getChildren().addAll(subtotalLabel, ivaLabel, totalLabel);

        Button recalcularBtn = new Button("Recalcular totales");
        recalcularBtn.setOnAction(e -> recalcularTotales());

        Button guardarBtn = new Button("💾 Guardar Presupuesto");
        guardarBtn.setOnAction(e -> onGuardarPresupuesto());

        HBox actionsRow = new HBox(10, recalcularBtn, guardarBtn);

        totalesBox.getChildren().addAll(descRow, ivaRow, totalsRow, actionsRow);
        totalesPane.setContent(totalesBox);
        totalesPane.setCollapsible(false);

        mainBox.getChildren().addAll(
                clientePane,
                materialPane,
                tareaPane,
                itemsPane,
                totalesPane
        );

        scrollPane.setContent(mainBox);
        root.setCenter(scrollPane);

        cargarDatosIniciales();
        recalcularTotales();

        return root;
    }

    private void cargarDatosIniciales() {
        clientes.setAll(clienteService.obtenerTodos());
        materiales.setAll(materialService.obtenerTodos());

        clienteCombo.setItems(clientes);
        clienteCombo.setConverter(new StringConverterCliente());

        materialCombo.setItems(materiales);
        materialCombo.setConverter(new StringConverterMaterial());
    }

    /** Recarga clientes y materiales desde la BD. Llamar al entrar en la pestaña o al pulsar Refrescar. */
    public void refreshDatos() {
        cargarDatosIniciales();
    }

    private void onAgregarMaterial() {
        Material m = materialCombo.getValue();
        if (m == null) {
            FxAlerts.showInfo("Validación", "Selecciona un material.");
            return;
        }
        double cantidad;
        try {
            cantidad = Double.parseDouble(cantidadField.getText().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            FxAlerts.showInfo("Validación", "Cantidad inválida.");
            return;
        }
        if (cantidad <= 0) {
            FxAlerts.showInfo("Validación", "La cantidad debe ser mayor que cero.");
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
        items.add(row);
        cantidadField.clear();
        recalcularTotales();
    }

    private void onAgregarTarea() {
        String desc = tareaDescripcionField.getText().trim();
        if (desc.isEmpty()) {
            FxAlerts.showInfo("Validación", "La descripción de la tarea es obligatoria.");
            return;
        }
        double cantidad;
        double precio;
        try {
            cantidad = Double.parseDouble(tareaCantidadField.getText().trim().replace(',', '.'));
            precio = Double.parseDouble(tareaPrecioField.getText().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            FxAlerts.showInfo("Validación", "Cantidad y precio deben ser números válidos.");
            return;
        }
        if (cantidad <= 0 || precio < 0) {
            FxAlerts.showInfo("Validación", "Cantidad debe ser > 0 y precio >= 0.");
            return;
        }
        PresupuestoItemRow row = new PresupuestoItemRow();
        row.setMaterialId(0);
        row.setDescripcion(desc);
        row.setEsTareaManual(true);
        row.setCantidad(cantidad);
        row.setPrecioUnitario(precio);
        row.setAplicaIva(true);
        row.setVisiblePdf(true);
        items.add(row);

        tareaDescripcionField.clear();
        tareaCantidadField.clear();
        tareaPrecioField.clear();
        recalcularTotales();
    }

    private void onEliminarItem() {
        PresupuestoItemRow selected = itemsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            items.remove(selected);
            recalcularTotales();
        }
    }

    private void recalcularTotales() {
        double descPct = parseDoubleOrZero(descuentoPorcentajeField.getText());
        double descFijo = parseDoubleOrZero(descuentoFijoField.getText());
        boolean descAntesIva = descuentoAntesIvaCheck.isSelected();
        boolean ivaHab = ivaHabilitadoCheck.isSelected();

        PresupuestoService.TotalesPresupuesto t = presupuestoService.calcularTotalesCompleto(
                items,
                descPct,
                descFijo,
                descAntesIva,
                ivaHab
        );

        subtotalLabel.setText(String.format("Subtotal: €%.2f", t.subtotal));
        ivaLabel.setText(String.format("IVA: €%.2f", t.iva));
        totalLabel.setText(String.format("Total: €%.2f", t.total));
    }

    private void onGuardarPresupuesto() {
        Cliente c = clienteCombo.getValue();
        if (c == null) {
            FxAlerts.showInfo("Validación", "Selecciona un cliente.");
            return;
        }
        if (items.isEmpty()) {
            FxAlerts.showInfo("Validación", "Añade al menos un item al presupuesto.");
            return;
        }
        double descPct = parseDoubleOrZero(descuentoPorcentajeField.getText());
        double descFijo = parseDoubleOrZero(descuentoFijoField.getText());
        boolean descAntesIva = descuentoAntesIvaCheck.isSelected();
        boolean ivaHab = ivaHabilitadoCheck.isSelected();

        try {
            int id = presupuestoService.crearPresupuesto(
                    c.getId(),
                    items,
                    ivaHab,
                    descPct,
                    descFijo,
                    descAntesIva
            );
            FxAlerts.showInfo("Presupuesto guardado", "Presupuesto creado con ID " + id + ".");
            items.clear();
            recalcularTotales();
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Error en operación", e);
            FxAlerts.showError("Error", "No se pudo guardar el presupuesto: " + e.getMessage());
        }
    }

    private double parseDoubleOrZero(String text) {
        if (text == null || text.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(text.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static class StringConverterCliente extends javafx.util.StringConverter<Cliente> {
        @Override
        public String toString(Cliente cliente) {
            return cliente == null ? "" : cliente.getNombre();
        }

        @Override
        public Cliente fromString(String s) {
            return null;
        }
    }

    private static class StringConverterMaterial extends javafx.util.StringConverter<Material> {
        @Override
        public String toString(Material material) {
            if (material == null) return "";
            return material.getNombre() + " (" + material.getUnidadMedida() + ") - €" + material.getPrecioUnitario();
        }

        @Override
        public Material fromString(String s) {
            return null;
        }
    }
}

