package appgestion.controller;

import appgestion.model.Material;
import appgestion.service.MaterialService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Controlador JavaFX para la pestaña de Gestión de Materiales.
 * Equivalente a create_materiales_tab y métodos asociados en Tkinter.
 */
public class MaterialesController {

    private final MaterialService materialService;
    private final ObservableList<Material> materiales = FXCollections.observableArrayList();

    private TextField nombreField;
    private TextField unidadField;
    private TextField precioField;
    private TextField buscarField;
    private TableView<Material> tablaMateriales;

    public MaterialesController(MaterialService materialService) {
        this.materialService = materialService;
    }

    public Node createContent() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        VBox topBox = new VBox(10);

        // Formulario
        TitledPane formPane = new TitledPane();
        formPane.setText("Datos del Material");
        VBox formBox = new VBox(8);
        formBox.setPadding(new Insets(10));

        HBox fila1 = new HBox(10);
        nombreField = new TextField();
        unidadField = new TextField();
        fila1.getChildren().addAll(
                new Label("Nombre:"), nombreField,
                new Label("Unidad:"), unidadField
        );

        HBox fila2 = new HBox(10);
        precioField = new TextField();
        fila2.getChildren().addAll(
                new Label("Precio Unitario:"), precioField
        );

        HBox botones = new HBox(10);
        Button agregarBtn = new Button("➕ Agregar Material");
        Button actualizarBtn = new Button("✏️ Actualizar");
        Button eliminarBtn = new Button("🗑️ Eliminar");
        Button limpiarBtn = new Button("🧹 Limpiar");

        agregarBtn.setOnAction(e -> onAgregar());
        actualizarBtn.setOnAction(e -> onActualizar());
        eliminarBtn.setOnAction(e -> onEliminar());
        limpiarBtn.setOnAction(e -> limpiarFormulario());

        botones.getChildren().addAll(agregarBtn, actualizarBtn, eliminarBtn, limpiarBtn);

        formBox.getChildren().addAll(fila1, fila2, botones);
        formPane.setContent(formBox);
        formPane.setCollapsible(false);

        // Búsqueda
        HBox searchBox = new HBox(10);
        buscarField = new TextField();
        buscarField.setPromptText("Buscar por nombre");
        Button buscarBtn = new Button("🔍 Buscar");
        buscarBtn.setOnAction(e -> cargarMaterialesFiltrados());
        searchBox.getChildren().addAll(new Label("🔍 Buscar:"), buscarField, buscarBtn);

        topBox.getChildren().addAll(formPane, searchBox);
        root.setTop(topBox);

        // Tabla
        tablaMateriales = new TableView<>(materiales);
        tablaMateriales.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<Material, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(data -> data.getValue().idProperty().asObject());
        colId.setMaxWidth(80);

        TableColumn<Material, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(data -> data.getValue().nombreProperty());

        TableColumn<Material, String> colUnidad = new TableColumn<>("Unidad");
        colUnidad.setCellValueFactory(data -> data.getValue().unidadMedidaProperty());

        TableColumn<Material, Number> colPrecio = new TableColumn<>("Precio Unitario");
        colPrecio.setCellValueFactory(data -> data.getValue().precioUnitarioProperty());

        tablaMateriales.getColumns().add(colId);
        tablaMateriales.getColumns().add(colNombre);
        tablaMateriales.getColumns().add(colUnidad);
        tablaMateriales.getColumns().add(colPrecio);

        tablaMateriales.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                cargarFormularioDesdeSeleccion(newSel);
            }
        });

        VBox centerBox = new VBox(tablaMateriales);
        VBox.setVgrow(tablaMateriales, Priority.ALWAYS);
        root.setCenter(centerBox);

        cargarMateriales();

        return root;
    }

    private void cargarMateriales() {
        materiales.setAll(materialService.obtenerTodos());
    }

    private void cargarMaterialesFiltrados() {
        String termino = buscarField.getText() != null ? buscarField.getText().trim() : "";
        if (termino.isEmpty()) {
            cargarMateriales();
        } else {
            materiales.setAll(materialService.buscar(termino));
        }
    }

    private void cargarFormularioDesdeSeleccion(Material m) {
        nombreField.setText(m.getNombre());
        unidadField.setText(m.getUnidadMedida());
        precioField.setText(Double.toString(m.getPrecioUnitario()));
    }

    private void limpiarFormulario() {
        nombreField.clear();
        unidadField.clear();
        precioField.clear();
        tablaMateriales.getSelectionModel().clearSelection();
    }

    private void onAgregar() {
        String nombre = nombreField.getText().trim();
        String unidad = unidadField.getText().trim();
        String precioTxt = precioField.getText().trim();
        if (nombre.isEmpty() || unidad.isEmpty() || precioTxt.isEmpty()) {
            mostrarAlerta("Validación", "Nombre, unidad y precio son obligatorios.");
            return;
        }
        double precio;
        try {
            precio = Double.parseDouble(precioTxt.replace(',', '.'));
        } catch (NumberFormatException e) {
            mostrarAlerta("Validación", "El precio debe ser un número válido.");
            return;
        }
        Material nuevo = materialService.crear(nombre, unidad, precio);
        if (nuevo != null) {
            materiales.add(nuevo);
            limpiarFormulario();
        }
    }

    private void onActualizar() {
        Material seleccionado = tablaMateriales.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta("Selección requerida", "Selecciona un material para actualizar.");
            return;
        }
        String nombre = nombreField.getText().trim();
        String unidad = unidadField.getText().trim();
        String precioTxt = precioField.getText().trim();
        if (nombre.isEmpty() || unidad.isEmpty() || precioTxt.isEmpty()) {
            mostrarAlerta("Validación", "Nombre, unidad y precio son obligatorios.");
            return;
        }
        double precio;
        try {
            precio = Double.parseDouble(precioTxt.replace(',', '.'));
        } catch (NumberFormatException e) {
            mostrarAlerta("Validación", "El precio debe ser un número válido.");
            return;
        }
        Material actualizado = new Material(seleccionado.getId(), nombre, unidad, precio);
        if (materialService.actualizar(actualizado)) {
            int idx = materiales.indexOf(seleccionado);
            if (idx >= 0) {
                materiales.set(idx, actualizado);
            }
            limpiarFormulario();
        }
    }

    private void onEliminar() {
        Material seleccionado = tablaMateriales.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta("Selección requerida", "Selecciona un material para eliminar.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar eliminación");
        confirm.setHeaderText("Eliminar material");
        confirm.setContentText("¿Seguro que deseas eliminar el material seleccionado?");
        confirm.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                if (materialService.eliminar(seleccionado.getId())) {
                    materiales.remove(seleccionado);
                    limpiarFormulario();
                }
            }
        });
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}

