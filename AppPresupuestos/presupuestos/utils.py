import sqlite3
import os
from typing import List, Dict, Any

class DatabaseManager:
    def __init__(self, db_path: str = "presupuestos.db"):
        self.db_path = db_path
        self.init_database()
    
    def init_database(self):
        """Inicializa la base de datos con las tablas necesarias"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        
        # Tabla de clientes
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS clientes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                telefono TEXT,
                email TEXT,
                direccion TEXT,
                dni TEXT,
                fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        ''')
        
        # Tabla de materiales
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS materiales (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                unidad_medida TEXT NOT NULL,
                precio_unitario REAL NOT NULL,
                fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        ''')
        
        # Tabla de presupuestos
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS presupuestos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                cliente_id INTEGER NOT NULL,
                fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                subtotal REAL NOT NULL,
                iva REAL NOT NULL,
                total REAL NOT NULL,
                iva_habilitado INTEGER DEFAULT 1,
                estado TEXT DEFAULT 'Pendiente',
                descuento_global_porcentaje REAL DEFAULT 0,
                descuento_global_fijo REAL DEFAULT 0,
                descuento_antes_iva INTEGER DEFAULT 1,
                FOREIGN KEY (cliente_id) REFERENCES clientes (id)
            )
        ''')
        
        # Tabla de items de presupuesto
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS presupuesto_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                presupuesto_id INTEGER NOT NULL,
                material_id INTEGER,
                tarea_manual TEXT,
                cantidad REAL NOT NULL,
                precio_unitario REAL NOT NULL,
                subtotal REAL NOT NULL,
                visible_pdf INTEGER DEFAULT 1,
                es_tarea_manual INTEGER DEFAULT 0,
                aplica_iva INTEGER DEFAULT 1,
                descuento_porcentaje REAL DEFAULT 0,
                descuento_fijo REAL DEFAULT 0,
                FOREIGN KEY (presupuesto_id) REFERENCES presupuestos (id),
                FOREIGN KEY (material_id) REFERENCES materiales (id)
            )
        ''')
        
        # Tabla de facturas
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS facturas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                numero_factura TEXT UNIQUE NOT NULL,
                cliente_id INTEGER NOT NULL,
                presupuesto_id INTEGER,
                fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                fecha_vencimiento DATE,
                subtotal REAL NOT NULL,
                iva REAL NOT NULL,
                total REAL NOT NULL,
                iva_habilitado INTEGER DEFAULT 1,
                metodo_pago TEXT DEFAULT 'Transferencia',
                estado_pago TEXT DEFAULT 'No Pagada',
                notas TEXT,
                descuento_global_porcentaje REAL DEFAULT 0,
                descuento_global_fijo REAL DEFAULT 0,
                descuento_antes_iva INTEGER DEFAULT 1,
                FOREIGN KEY (cliente_id) REFERENCES clientes (id),
                FOREIGN KEY (presupuesto_id) REFERENCES presupuestos (id)
            )
        ''')
        
        # Tabla de items de factura
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS factura_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                factura_id INTEGER NOT NULL,
                material_id INTEGER,
                tarea_manual TEXT,
                cantidad REAL NOT NULL,
                precio_unitario REAL NOT NULL,
                subtotal REAL NOT NULL,
                visible_pdf INTEGER DEFAULT 1,
                es_tarea_manual INTEGER DEFAULT 0,
                aplica_iva INTEGER DEFAULT 1,
                descuento_porcentaje REAL DEFAULT 0,
                descuento_fijo REAL DEFAULT 0,
                FOREIGN KEY (factura_id) REFERENCES facturas (id),
                FOREIGN KEY (material_id) REFERENCES materiales (id)
            )
        ''')
        
        # Actualizar tabla existente si no tiene las nuevas columnas
        self.update_existing_tables(cursor)
        
        conn.commit()
        conn.close()
    
    def update_existing_tables(self, cursor):
        """Actualiza tablas existentes para agregar nuevas columnas"""
        try:
            # Verificar si la tabla presupuesto_items existe y tiene las columnas necesarias
            cursor.execute("PRAGMA table_info(presupuesto_items)")
            columns = [column[1] for column in cursor.fetchall()]
            
            # Verificar si material_id permite NULL
            cursor.execute("PRAGMA table_info(presupuesto_items)")
            column_info = cursor.fetchall()
            material_id_nullable = True
            for col in column_info:
                if col[1] == 'material_id':
                    material_id_nullable = col[3] == 0  # 0 = nullable, 1 = not null
            
            # Si material_id no permite NULL, necesitamos recrear la tabla
            if not material_id_nullable or 'tarea_manual' not in columns or 'visible_pdf' not in columns or 'es_tarea_manual' not in columns or 'aplica_iva' not in columns:
                print("Recreando tabla presupuesto_items para soportar nuevas funcionalidades...")
                
                # Crear tabla temporal con la estructura correcta
                cursor.execute('''
                    CREATE TABLE presupuesto_items_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        presupuesto_id INTEGER NOT NULL,
                        material_id INTEGER,
                        tarea_manual TEXT,
                        cantidad REAL NOT NULL,
                        precio_unitario REAL NOT NULL,
                        subtotal REAL NOT NULL,
                        visible_pdf INTEGER DEFAULT 1,
                        es_tarea_manual INTEGER DEFAULT 0,
                        aplica_iva INTEGER DEFAULT 1,
                        descuento_porcentaje REAL DEFAULT 0,
                        descuento_fijo REAL DEFAULT 0,
                        FOREIGN KEY (presupuesto_id) REFERENCES presupuestos (id),
                        FOREIGN KEY (material_id) REFERENCES materiales (id)
                    )
                ''')
                
                # Copiar datos existentes
                cursor.execute('''
                    INSERT INTO presupuesto_items_new 
                    (id, presupuesto_id, material_id, cantidad, precio_unitario, subtotal, visible_pdf, es_tarea_manual, aplica_iva, descuento_porcentaje, descuento_fijo)
                    SELECT id, presupuesto_id, material_id, cantidad, precio_unitario, subtotal, 
                           COALESCE(visible_pdf, 1), COALESCE(es_tarea_manual, 0), 1, 0, 0
                    FROM presupuesto_items
                ''')
                
                # Eliminar tabla antigua y renombrar la nueva
                cursor.execute("DROP TABLE presupuesto_items")
                cursor.execute("ALTER TABLE presupuesto_items_new RENAME TO presupuesto_items")
                
                print("Tabla presupuesto_items actualizada correctamente")
            
            # Verificar si la tabla presupuestos tiene la columna iva_habilitado
            cursor.execute("PRAGMA table_info(presupuestos)")
            presupuestos_columns = [column[1] for column in cursor.fetchall()]
            
            if 'iva_habilitado' not in presupuestos_columns:
                print("Agregando columna iva_habilitado a la tabla presupuestos...")
                cursor.execute("ALTER TABLE presupuestos ADD COLUMN iva_habilitado INTEGER DEFAULT 1")
                print("Columna iva_habilitado agregada correctamente")
            
            # Verificar si la tabla clientes tiene la columna dni
            cursor.execute("PRAGMA table_info(clientes)")
            clientes_columns = [column[1] for column in cursor.fetchall()]
            
            if 'dni' not in clientes_columns:
                print("Agregando columna dni a la tabla clientes...")
                cursor.execute("ALTER TABLE clientes ADD COLUMN dni TEXT")
                print("Columna dni agregada correctamente")
            
            # Verificar si la tabla presupuestos tiene la columna estado
            if 'estado' not in presupuestos_columns:
                print("Agregando columna estado a la tabla presupuestos...")
                cursor.execute("ALTER TABLE presupuestos ADD COLUMN estado TEXT DEFAULT 'Pendiente'")
                print("Columna estado agregada correctamente")
            
            # Verificar y agregar nuevas columnas de descuentos a presupuestos
            if 'descuento_global_porcentaje' not in presupuestos_columns:
                print("Agregando columnas de descuentos a la tabla presupuestos...")
                cursor.execute("ALTER TABLE presupuestos ADD COLUMN descuento_global_porcentaje REAL DEFAULT 0")
                cursor.execute("ALTER TABLE presupuestos ADD COLUMN descuento_global_fijo REAL DEFAULT 0")
                cursor.execute("ALTER TABLE presupuestos ADD COLUMN descuento_antes_iva INTEGER DEFAULT 1")
                print("Columnas de descuentos agregadas correctamente")
            
            # Verificar si la tabla factura_items existe y tiene las columnas necesarias
            cursor.execute("PRAGMA table_info(factura_items)")
            factura_items_columns = [column[1] for column in cursor.fetchall()]
            
            if factura_items_columns and ('aplica_iva' not in factura_items_columns or 'descuento_porcentaje' not in factura_items_columns):
                print("Agregando nuevas columnas a la tabla factura_items...")
                try:
                    cursor.execute("ALTER TABLE factura_items ADD COLUMN aplica_iva INTEGER DEFAULT 1")
                except:
                    pass  # Columna ya existe
                try:
                    cursor.execute("ALTER TABLE factura_items ADD COLUMN descuento_porcentaje REAL DEFAULT 0")
                except:
                    pass  # Columna ya existe
                try:
                    cursor.execute("ALTER TABLE factura_items ADD COLUMN descuento_fijo REAL DEFAULT 0")
                except:
                    pass  # Columna ya existe
                print("Columnas agregadas a factura_items correctamente")
            
            # Verificar y agregar nuevas columnas de descuentos a facturas
            cursor.execute("PRAGMA table_info(facturas)")
            facturas_columns = [column[1] for column in cursor.fetchall()]
            
            if 'descuento_global_porcentaje' not in facturas_columns:
                print("Agregando columnas de descuentos a la tabla facturas...")
                try:
                    cursor.execute("ALTER TABLE facturas ADD COLUMN descuento_global_porcentaje REAL DEFAULT 0")
                except:
                    pass
                try:
                    cursor.execute("ALTER TABLE facturas ADD COLUMN descuento_global_fijo REAL DEFAULT 0")
                except:
                    pass
                try:
                    cursor.execute("ALTER TABLE facturas ADD COLUMN descuento_antes_iva INTEGER DEFAULT 1")
                except:
                    pass
                print("Columnas de descuentos agregadas a facturas correctamente")
            
        except Exception as e:
            print(f"Error actualizando tabla: {e}")
            # Si hay error, recrear la tabla
            try:
                cursor.execute("DROP TABLE IF EXISTS presupuesto_items")
                cursor.execute('''
                    CREATE TABLE presupuesto_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        presupuesto_id INTEGER NOT NULL,
                        material_id INTEGER,
                        tarea_manual TEXT,
                        cantidad REAL NOT NULL,
                        precio_unitario REAL NOT NULL,
                        subtotal REAL NOT NULL,
                        visible_pdf INTEGER DEFAULT 1,
                        es_tarea_manual INTEGER DEFAULT 0,
                        aplica_iva INTEGER DEFAULT 1,
                        descuento_porcentaje REAL DEFAULT 0,
                        descuento_fijo REAL DEFAULT 0,
                        FOREIGN KEY (presupuesto_id) REFERENCES presupuestos (id),
                        FOREIGN KEY (material_id) REFERENCES materiales (id)
                    )
                ''')
                print("Tabla presupuesto_items recreada")
            except Exception as e2:
                print(f"Error recreando tabla: {e2}")
    
    def get_connection(self):
        """Obtiene una conexión a la base de datos"""
        return sqlite3.connect(self.db_path)
    
    def execute_query(self, query: str, params: tuple = ()) -> List[Dict[str, Any]]:
        """Ejecuta una consulta y devuelve los resultados como lista de diccionarios"""
        conn = self.get_connection()
        conn.row_factory = sqlite3.Row
        cursor = conn.cursor()
        cursor.execute(query, params)
        results = [dict(row) for row in cursor.fetchall()]
        conn.close()
        return results
    
    def execute_update(self, query: str, params: tuple = ()) -> int:
        """Ejecuta una actualización y devuelve el ID del último registro insertado"""
        conn = self.get_connection()
        cursor = conn.cursor()
        cursor.execute(query, params)
        conn.commit()
        last_id = cursor.lastrowid
        conn.close()
        return last_id

# Instancia global de la base de datos
db = DatabaseManager()
