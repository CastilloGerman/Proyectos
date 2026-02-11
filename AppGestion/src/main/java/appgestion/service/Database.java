package appgestion.service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gestor muy simple de conexiones SQLite.
 * Reutiliza el mismo fichero presupuestos.db que la app Python.
 */
public class Database {

    private static final String DB_FILENAME = "presupuestos.db";
    private static Connection sharedConnection;

    public static Connection getConnection() throws SQLException {
        if (sharedConnection == null || sharedConnection.isClosed()) {
            Path dbPath = Paths.get(DB_FILENAME).toAbsolutePath();
            String url = "jdbc:sqlite:" + dbPath;
            sharedConnection = DriverManager.getConnection(url);
            initSchemaIfNeeded(sharedConnection);
        }
        return sharedConnection;
    }

    private static boolean schemaInitialized = false;

    private static void initSchemaIfNeeded(Connection conn) throws SQLException {
        if (schemaInitialized) return;
        try (java.sql.Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS clientes (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL, telefono TEXT, email TEXT, direccion TEXT, dni TEXT, fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE IF NOT EXISTS materiales (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL, unidad_medida TEXT NOT NULL, precio_unitario REAL NOT NULL, fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE IF NOT EXISTS presupuestos (id INTEGER PRIMARY KEY AUTOINCREMENT, cliente_id INTEGER NOT NULL, fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP, subtotal REAL NOT NULL, iva REAL NOT NULL, total REAL NOT NULL, iva_habilitado INTEGER DEFAULT 1, estado TEXT DEFAULT 'Pendiente', descuento_global_porcentaje REAL DEFAULT 0, descuento_global_fijo REAL DEFAULT 0, descuento_antes_iva INTEGER DEFAULT 1, FOREIGN KEY (cliente_id) REFERENCES clientes(id))");
            st.execute("CREATE TABLE IF NOT EXISTS presupuesto_items (id INTEGER PRIMARY KEY AUTOINCREMENT, presupuesto_id INTEGER NOT NULL, material_id INTEGER, tarea_manual TEXT, cantidad REAL NOT NULL, precio_unitario REAL NOT NULL, subtotal REAL NOT NULL, visible_pdf INTEGER DEFAULT 1, es_tarea_manual INTEGER DEFAULT 0, aplica_iva INTEGER DEFAULT 1, descuento_porcentaje REAL DEFAULT 0, descuento_fijo REAL DEFAULT 0, FOREIGN KEY (presupuesto_id) REFERENCES presupuestos(id), FOREIGN KEY (material_id) REFERENCES materiales(id))");
            st.execute("CREATE TABLE IF NOT EXISTS facturas (id INTEGER PRIMARY KEY AUTOINCREMENT, numero_factura TEXT UNIQUE NOT NULL, cliente_id INTEGER NOT NULL, presupuesto_id INTEGER, fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP, fecha_vencimiento DATE, subtotal REAL NOT NULL, iva REAL NOT NULL, total REAL NOT NULL, iva_habilitado INTEGER DEFAULT 1, metodo_pago TEXT DEFAULT 'Transferencia', estado_pago TEXT DEFAULT 'No Pagada', notas TEXT, descuento_global_porcentaje REAL DEFAULT 0, descuento_global_fijo REAL DEFAULT 0, descuento_antes_iva INTEGER DEFAULT 1, retencion_irpf REAL, FOREIGN KEY (cliente_id) REFERENCES clientes(id), FOREIGN KEY (presupuesto_id) REFERENCES presupuestos(id))");
            st.execute("CREATE TABLE IF NOT EXISTS factura_items (id INTEGER PRIMARY KEY AUTOINCREMENT, factura_id INTEGER NOT NULL, material_id INTEGER, tarea_manual TEXT, cantidad REAL NOT NULL, precio_unitario REAL NOT NULL, subtotal REAL NOT NULL, visible_pdf INTEGER DEFAULT 1, es_tarea_manual INTEGER DEFAULT 0, aplica_iva INTEGER DEFAULT 1, descuento_porcentaje REAL DEFAULT 0, descuento_fijo REAL DEFAULT 0, cuota_iva REAL DEFAULT 0, FOREIGN KEY (factura_id) REFERENCES facturas(id), FOREIGN KEY (material_id) REFERENCES materiales(id))");
        }
        schemaInitialized = true;
    }
}

