package appgestion;

import javafx.application.Application;
import appgestion.controller.ClientesController;
import appgestion.controller.MaterialesController;
import appgestion.controller.PresupuestosController;
import appgestion.controller.VerPresupuestosController;
import appgestion.controller.FacturacionController;
import appgestion.service.ClienteService;
import appgestion.service.MaterialService;
import appgestion.service.PresupuestoService;
import appgestion.service.FacturaService;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Punto de entrada principal de la aplicación JavaFX.
 * Equivalente a main.py + ui.app.AppPresupuestos en la versión Tkinter.
 */
public class AppGestionApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Sistema de Gestión de Presupuestos - JavaFX");

        // Contenedor raíz
        BorderPane root = new BorderPane();

        // Servicios compartidos
        ClienteService clienteService = new ClienteService();
        MaterialService materialService = new MaterialService();
        PresupuestoService presupuestoService = new PresupuestoService();
        FacturaService facturaService = new FacturaService();

        // Notebook / pestañas principal (equivalente a ttk.Notebook)
        TabPane tabPane = new TabPane();

        // Pestañas: Clientes ya migrada parcialmente
        ClientesController clientesController = new ClientesController(clienteService);
        tabPane.getTabs().add(createTab("Gestión de Clientes", clientesController.createContent()));

        // Pestaña de materiales migrada (CRUD completo)
        MaterialesController materialesController = new MaterialesController(materialService);
        tabPane.getTabs().add(createTab("Gestión de Materiales", materialesController.createContent()));

        // Pestaña de creación de presupuestos (refrescar listas al seleccionar la pestaña)
        PresupuestosController presupuestosController = new PresupuestosController(
                clienteService,
                materialService,
                presupuestoService
        );
        Tab tabPresupuestos = createTab("Gestión de Presupuestos", presupuestosController.createContent());
        tabPresupuestos.selectedProperty().addListener((o, prev, selected) -> {
            if (Boolean.TRUE.equals(selected)) presupuestosController.refreshDatos();
        });
        tabPane.getTabs().add(tabPresupuestos);

        // Pestaña de listado de presupuestos (refrescar al seleccionar para ver presupuestos recién creados)
        VerPresupuestosController verPresupuestosController = new VerPresupuestosController(presupuestoService);
        Tab tabVerPresupuestos = createTab("Ver Presupuestos", verPresupuestosController.createContent());
        tabVerPresupuestos.selectedProperty().addListener((o, prev, selected) -> {
            if (Boolean.TRUE.equals(selected)) verPresupuestosController.recargar();
        });
        tabPane.getTabs().add(tabVerPresupuestos);

        // Pestaña de facturación (refrescar al seleccionar para ver facturas recién creadas)
        FacturacionController facturacionController = new FacturacionController(facturaService);
        Tab tabFacturacion = createTab("Facturación", facturacionController.createContent());
        tabFacturacion.selectedProperty().addListener((o, prev, selected) -> {
            if (Boolean.TRUE.equals(selected)) facturacionController.recargar();
        });
        tabPane.getTabs().add(tabFacturacion);
        tabPane.getTabs().add(createTab("Métricas"));

        root.setCenter(tabPane);

        Scene scene = new Scene(root, 1200, 800);
        java.net.URL cssUrl = getClass().getResource("/appgestion/styles.css");
        if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Tab createTab(String title) {
        return createTab(title, new Label("Contenido pendiente de migrar para: " + title));
    }

    private Tab createTab(String title, javafx.scene.Node content) {
        Tab tab = new Tab(title);
        tab.setClosable(false);
        tab.setContent(content);
        return tab;
    }

    public static void main(String[] args) {
        launch(args);
    }
}

