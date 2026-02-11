# AppGestion (Java + JavaFX)

Aplicación de gestión de presupuestos reimplementada en Java + JavaFX a partir del proyecto original en Python + Tkinter (`AppPresupuestos`).

## Estructura del proyecto

- `pom.xml`: Configuración Maven con dependencias de JavaFX y SQLite.
- `src/main/java/appgestion/AppGestionApplication.java`: Punto de entrada principal de la aplicación JavaFX.
- `src/main/java/appgestion/controller`:
  - `ClientesController`: Gestión CRUD de clientes.
  - `MaterialesController`: Gestión CRUD de materiales.
  - `PresupuestosController`: Creación de presupuestos (items, descuentos, IVA).
  - `VerPresupuestosController`: Listado de presupuestos.
  - `FacturacionController`: Listado de facturas y creación desde presupuesto.
- `src/main/java/appgestion/model`:
  - `Cliente`, `Material`, `PresupuestoItemRow`.
- `src/main/java/appgestion/service`:
  - `Database`: Conexión SQLite contra `presupuestos.db`.
  - `ClienteService`, `MaterialService`, `PresupuestoService`, `FacturaService`.

## Requisitos

- Java 21 o superior.
- Maven 3.9 o superior.

## Ejecución

**Desde el IDE (Cursor/VS Code):**  
Abre "Run and Debug" (Ctrl+Shift+D), elige la configuración **"AppGestion (JavaFX)"** y pulsa F5 o el botón de ejecutar. (La configuración incluye los módulos de JavaFX necesarios.)

**Desde terminal con Maven** (desde la carpeta `AppGestion`):

```bash
mvn clean javafx:run
```

Esto lanzará la ventana principal de JavaFX con las pestañas base:

- Gestión de Clientes (CRUD conectado a la BD existente).
- Gestión de Materiales (CRUD conectado a la BD existente).
- Gestión de Presupuestos (creación de presupuestos con descuentos e IVA).
- Ver Presupuestos (listado de presupuestos creados).
- Facturación (listado de facturas y creación desde un presupuesto).

La aplicación utiliza el mismo fichero `presupuestos.db` que la versión original en Python/Tkinter, por lo que puedes reutilizar los datos existentes.

