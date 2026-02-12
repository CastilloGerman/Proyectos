# AppGestion

Aplicación de gestión de presupuestos y facturación desarrollada en **Java 21 + JavaFX**. Reimplementa la funcionalidad del proyecto original en Python/Tkinter (`AppPresupuestos`), reutilizando la misma base de datos SQLite.

## Requisitos

- **Java 21** o superior
- **Maven 3.9** o superior

## Ejecución

### Desde Maven

```bash
mvn clean javafx:run
```

### Desde el IDE

- **VS Code / Cursor**: Abre "Run and Debug" (Ctrl+Shift+D), selecciona **"AppGestion (JavaFX)"** y ejecuta.
- **IntelliJ IDEA**: Configura el run con módulos JavaFX (`--add-modules javafx.controls,javafx.fxml,javafx.graphics`).

## Estructura del proyecto

```
AppGestion/
├── config/
│   ├── plantilla_config.json       # Configuración empresa/logo (generada si no existe)
│   └── plantilla_config.json.example
├── output/
│   ├── presupuestos/               # PDFs de presupuestos
│   └── facturas/                   # PDFs de facturas
├── src/main/java/appgestion/
│   ├── AppGestionApplication.java  # Punto de entrada
│   ├── config/
│   │   └── PlantillaConfig.java    # Modelo de configuración JSON
│   ├── controller/
│   │   ├── ClientesController.java
│   │   ├── MaterialesController.java
│   │   ├── PresupuestosController.java
│   │   ├── VerPresupuestosController.java
│   │   ├── FacturacionController.java
│   │   └── MetricasController.java
│   ├── model/
│   │   ├── Cliente.java
│   │   ├── Material.java
│   │   └── PresupuestoItemRow.java
│   ├── service/
│   │   ├── Database.java           # Conexión SQLite
│   │   ├── ConfigService.java
│   │   ├── ClienteService.java
│   │   ├── MaterialService.java
│   │   ├── PresupuestoService.java
│   │   ├── FacturaService.java
│   │   ├── MetricasService.java
│   │   └── PdfGeneratorService.java
│   └── util/
│       ├── FxAlerts.java           # Alertas JavaFX
│       └── StringUtils.java
└── src/main/resources/appgestion/
    └── styles.css
```

## Funcionalidades

| Pestaña | Descripción |
|---------|-------------|
| **Gestión de Clientes** | CRUD de clientes |
| **Gestión de Materiales** | CRUD de materiales/servicios |
| **Gestión de Presupuestos** | Crear presupuestos con items, descuentos e IVA |
| **Ver Presupuestos** | Listar, filtrar, ver detalle, generar PDF, marcar Aprobado/Rechazado |
| **Facturación** | Crear facturas manuales o desde presupuesto, marcar Pagada/No Pagada |
| **Métricas** | Dashboard financiero, gráficos de presupuestos/facturas, top clientes/materiales |

## Base de datos

Utiliza `presupuestos.db` (SQLite) en el directorio de trabajo. Compatible con la app Python: los datos se comparten entre ambas versiones.

## Configuración

- **config/plantilla_config.json**: Datos de empresa, logo y carpetas de salida para PDFs.
- Si no existe, se crea con valores por defecto al iniciar.
- Usa `plantilla_config.json.example` como referencia para rutas relativas.

## Dependencias (pom.xml)

- JavaFX Controls, FXML, Graphics
- SQLite JDBC
- OpenPDF (generación de PDFs)
- Gson (configuración JSON)
