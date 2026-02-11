package appgestion.controller;

import appgestion.model.Cliente;
import appgestion.service.ClienteService;
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
 * Controlador JavaFX para la pestaña de Gestión de Clientes.
 * Equivalente a create_clientes_tab y métodos asociados en Tkinter.
 */
public class ClientesController {

    private final ClienteService clienteService;
    private final ObservableList<Cliente> clientes = FXCollections.observableArrayList();

    private TextField nombreField;
    private TextField telefonoField;
    private TextField emailField;
    private TextField direccionField;
    private TextField dniField;
    private TextField buscarField;
    private TableView<Cliente> tablaClientes;

    public ClientesController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    public Node createContent() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        VBox topBox = new VBox(10);

        // Formulario de cliente
        TitledPane formPane = new TitledPane();
        formPane.setText("Datos del Cliente");

        VBox formBox = new VBox(8);
        formBox.setPadding(new Insets(10));

        HBox fila1 = new HBox(10);
        nombreField = new TextField();
        telefonoField = new TextField();
        fila1.getChildren().addAll(
                new Label("Nombre:"), nombreField,
                new Label("Teléfono:"), telefonoField
        );

        HBox fila2 = new HBox(10);
        emailField = new TextField();
        direccionField = new TextField();
        fila2.getChildren().addAll(
                new Label("Email:"), emailField,
                new Label("Dirección:"), direccionField
        );

        HBox fila3 = new HBox(10);
        dniField = new TextField();
        fila3.getChildren().addAll(
                new Label("NIF/NIE/IVA Intracomunitario:"), dniField
        );

        HBox botones = new HBox(10);
        Button agregarBtn = new Button("➕ Agregar Cliente");
        Button actualizarBtn = new Button("✏️ Actualizar");
        Button eliminarBtn = new Button("🗑️ Eliminar");
        Button limpiarBtn = new Button("🧹 Limpiar");

        agregarBtn.setOnAction(e -> onAgregar());
        actualizarBtn.setOnAction(e -> onActualizar());
        eliminarBtn.setOnAction(e -> onEliminar());
        limpiarBtn.setOnAction(e -> limpiarFormulario());

        botones.getChildren().addAll(agregarBtn, actualizarBtn, eliminarBtn, limpiarBtn);

        formBox.getChildren().addAll(fila1, fila2, fila3, botones);
        formPane.setContent(formBox);
        formPane.setCollapsible(false);

        // Búsqueda
        HBox searchBox = new HBox(10);
        buscarField = new TextField();
        buscarField.setPromptText("Buscar por nombre, teléfono, email o DNI");
        Button buscarBtn = new Button("🔍 Buscar");
        buscarBtn.setOnAction(e -> cargarClientesFiltrados());
        searchBox.getChildren().addAll(new Label("🔍 Buscar:"), buscarField, buscarBtn);

        topBox.getChildren().addAll(formPane, searchBox);
        root.setTop(topBox);

        // Tabla de clientes
        tablaClientes = new TableView<>(clientes);
        tablaClientes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<Cliente, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(data -> data.getValue().idProperty().asObject());
        colId.setMaxWidth(80);

        TableColumn<Cliente, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(data -> data.getValue().nombreProperty());

        TableColumn<Cliente, String> colTelefono = new TableColumn<>("Teléfono");
        colTelefono.setCellValueFactory(data -> data.getValue().telefonoProperty());

        TableColumn<Cliente, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(data -> data.getValue().emailProperty());

        TableColumn<Cliente, String> colDireccion = new TableColumn<>("Dirección");
        colDireccion.setCellValueFactory(data -> data.getValue().direccionProperty());

        TableColumn<Cliente, String> colDni = new TableColumn<>("DNI");
        colDni.setCellValueFactory(data -> data.getValue().dniProperty());

        tablaClientes.getColumns().add(colId);
        tablaClientes.getColumns().add(colNombre);
        tablaClientes.getColumns().add(colTelefono);
        tablaClientes.getColumns().add(colEmail);
        tablaClientes.getColumns().add(colDireccion);
        tablaClientes.getColumns().add(colDni);

        tablaClientes.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                cargarFormularioDesdeSeleccion(newSel);
            }
        });

        VBox centerBox = new VBox(tablaClientes);
        VBox.setVgrow(tablaClientes, Priority.ALWAYS);
        root.setCenter(centerBox);

        cargarClientes();

        return root;
    }

    private void cargarClientes() {
        clientes.setAll(clienteService.obtenerTodos());
    }

    private void cargarClientesFiltrados() {
        String termino = buscarField.getText() != null ? buscarField.getText().trim() : "";
        if (termino.isEmpty()) {
            cargarClientes();
        } else {
            clientes.setAll(clienteService.buscar(termino));
        }
    }

    private void cargarFormularioDesdeSeleccion(Cliente c) {
        nombreField.setText(c.getNombre());
        telefonoField.setText(c.getTelefono());
        emailField.setText(c.getEmail());
        direccionField.setText(c.getDireccion());
        dniField.setText(c.getDni());
    }

    private void limpiarFormulario() {
        nombreField.clear();
        telefonoField.clear();
        emailField.clear();
        direccionField.clear();
        dniField.clear();
        tablaClientes.getSelectionModel().clearSelection();
    }

    private void onAgregar() {
        String nombre = nombreField.getText().trim();
        if (nombre.isEmpty()) {
            mostrarAlerta("Validación", "El nombre es obligatorio.");
            return;
        }
        Cliente nuevo = clienteService.crear(
                nombre,
                telefonoField.getText().trim(),
                emailField.getText().trim(),
                direccionField.getText().trim(),
                dniField.getText().trim()
        );
        if (nuevo != null) {
            clientes.add(nuevo);
            limpiarFormulario();
        }
    }

    private void onActualizar() {
        Cliente seleccionado = tablaClientes.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta("Selección requerida", "Selecciona un cliente para actualizar.");
            return;
        }
        String nombre = nombreField.getText().trim();
        if (nombre.isEmpty()) {
            mostrarAlerta("Validación", "El nombre es obligatorio.");
            return;
        }
        Cliente actualizado = new Cliente(
                seleccionado.getId(),
                nombre,
                telefonoField.getText().trim(),
                emailField.getText().trim(),
                direccionField.getText().trim(),
                dniField.getText().trim()
        );
        if (clienteService.actualizar(actualizado)) {
            int idx = clientes.indexOf(seleccionado);
            if (idx >= 0) {
                clientes.set(idx, actualizado);
            }
            limpiarFormulario();
        }
    }

    private void onEliminar() {
        Cliente seleccionado = tablaClientes.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta("Selección requerida", "Selecciona un cliente para eliminar.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar eliminación");
        confirm.setHeaderText("Eliminar cliente");
        confirm.setContentText("¿Seguro que deseas eliminar el cliente seleccionado?");
        confirm.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                if (clienteService.eliminar(seleccionado.getId())) {
                    clientes.remove(seleccionado);
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

