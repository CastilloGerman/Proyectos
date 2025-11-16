from .utils import db
from typing import List, Dict, Any, Optional
from datetime import datetime, timedelta

class ClienteManager:
    def __init__(self):
        self.db = db
    
    def crear_cliente(self, nombre: str, telefono: str = "", email: str = "", direccion: str = "", dni: str = "") -> int:
        """Crea un nuevo cliente en la base de datos"""
        query = """
            INSERT INTO clientes (nombre, telefono, email, direccion, dni)
            VALUES (?, ?, ?, ?, ?)
        """
        return self.db.execute_update(query, (nombre, telefono, email, direccion, dni))
    
    def obtener_clientes(self) -> List[Dict[str, Any]]:
        """Obtiene todos los clientes de la base de datos"""
        query = "SELECT * FROM clientes ORDER BY nombre"
        return self.db.execute_query(query)
    
    def obtener_cliente_por_id(self, cliente_id: int) -> Optional[Dict[str, Any]]:
        """Obtiene un cliente específico por su ID"""
        query = "SELECT * FROM clientes WHERE id = ?"
        results = self.db.execute_query(query, (cliente_id,))
        return results[0] if results else None
    
    def buscar_clientes(self, termino: str) -> List[Dict[str, Any]]:
        """Busca clientes por nombre, teléfono, email o DNI"""
        query = """
            SELECT * FROM clientes 
            WHERE nombre LIKE ? OR telefono LIKE ? OR email LIKE ? OR dni LIKE ?
            ORDER BY nombre
        """
        termino_busqueda = f"%{termino}%"
        return self.db.execute_query(query, (termino_busqueda, termino_busqueda, termino_busqueda, termino_busqueda))
    
    def actualizar_cliente(self, cliente_id: int, nombre: str, telefono: str = "", 
                          email: str = "", direccion: str = "", dni: str = "") -> bool:
        """Actualiza un cliente existente"""
        query = """
            UPDATE clientes 
            SET nombre = ?, telefono = ?, email = ?, direccion = ?, dni = ?
            WHERE id = ?
        """
        try:
            self.db.execute_update(query, (nombre, telefono, email, direccion, dni, cliente_id))
            return True
        except:
            return False
    
    def eliminar_cliente(self, cliente_id: int) -> bool:
        """Elimina un cliente de la base de datos"""
        query = "DELETE FROM clientes WHERE id = ?"
        try:
            self.db.execute_update(query, (cliente_id,))
            return True
        except:
            return False
    
    def obtener_estadisticas_clientes(self) -> Dict[str, Any]:
        """Obtiene estadísticas de clientes"""
        # Total de clientes activos (con al menos una factura o presupuesto)
        query_activos = """
            SELECT COUNT(DISTINCT c.id) as total_activos
            FROM clientes c
            WHERE EXISTS (
                SELECT 1 FROM facturas f WHERE f.cliente_id = c.id
            ) OR EXISTS (
                SELECT 1 FROM presupuestos p WHERE p.cliente_id = c.id
            )
        """
        result_activos = self.db.execute_query(query_activos)
        total_activos = result_activos[0]['total_activos'] if result_activos else 0
        
        # Clientes nuevos este mes
        fecha_inicio_mes = datetime.now().replace(day=1).strftime("%Y-%m-%d")
        query_nuevos = """
            SELECT COUNT(*) as total_nuevos
            FROM clientes
            WHERE DATE(fecha_creacion) >= DATE(?)
        """
        result_nuevos = self.db.execute_query(query_nuevos, (fecha_inicio_mes,))
        clientes_nuevos_mes = result_nuevos[0]['total_nuevos'] if result_nuevos else 0
        
        # Clientes con facturas pendientes
        query_pendientes = """
            SELECT COUNT(DISTINCT c.id) as total_con_pendientes
            FROM clientes c
            INNER JOIN facturas f ON f.cliente_id = c.id
            WHERE f.estado_pago = 'No Pagada'
        """
        result_pendientes = self.db.execute_query(query_pendientes)
        clientes_con_pendientes = result_pendientes[0]['total_con_pendientes'] if result_pendientes else 0
        
        # Promedio de facturación por cliente
        query_promedio = """
            SELECT 
                COUNT(DISTINCT c.id) as clientes_con_facturas,
                COALESCE(SUM(CASE WHEN f.estado_pago = 'Pagada' THEN f.total ELSE 0 END), 0) as total_facturado
            FROM clientes c
            LEFT JOIN facturas f ON f.cliente_id = c.id
            WHERE EXISTS (SELECT 1 FROM facturas f2 WHERE f2.cliente_id = c.id)
        """
        result_promedio = self.db.execute_query(query_promedio)
        if result_promedio:
            clientes_con_facturas = result_promedio[0].get('clientes_con_facturas', 0) or 0
            total_facturado = result_promedio[0].get('total_facturado', 0) or 0
            promedio_facturacion = (total_facturado / clientes_con_facturas) if clientes_con_facturas > 0 else 0
        else:
            promedio_facturacion = 0
        
        return {
            'total_activos': total_activos,
            'clientes_nuevos_mes': clientes_nuevos_mes,
            'clientes_con_pendientes': clientes_con_pendientes,
            'promedio_facturacion_por_cliente': promedio_facturacion
        }

# Instancia global del manager de clientes
cliente_manager = ClienteManager()
