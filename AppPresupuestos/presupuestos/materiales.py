from .utils import db
from typing import List, Dict, Any, Optional

class MaterialManager:
    def __init__(self):
        self.db = db
    
    def crear_material(self, nombre: str, unidad_medida: str, precio_unitario: float) -> int:
        """Crea un nuevo material en la base de datos"""
        query = """
            INSERT INTO materiales (nombre, unidad_medida, precio_unitario)
            VALUES (?, ?, ?)
        """
        return self.db.execute_update(query, (nombre, unidad_medida, precio_unitario))
    
    def obtener_materiales(self) -> List[Dict[str, Any]]:
        """Obtiene todos los materiales de la base de datos"""
        query = "SELECT * FROM materiales ORDER BY nombre"
        return self.db.execute_query(query)
    
    def obtener_material_por_id(self, material_id: int) -> Optional[Dict[str, Any]]:
        """Obtiene un material específico por su ID"""
        query = "SELECT * FROM materiales WHERE id = ?"
        results = self.db.execute_query(query, (material_id,))
        return results[0] if results else None
    
    def buscar_materiales(self, termino: str) -> List[Dict[str, Any]]:
        """Busca materiales por nombre"""
        query = """
            SELECT * FROM materiales 
            WHERE nombre LIKE ?
            ORDER BY nombre
        """
        termino_busqueda = f"%{termino}%"
        return self.db.execute_query(query, (termino_busqueda,))
    
    def actualizar_material(self, material_id: int, nombre: str, unidad_medida: str, 
                           precio_unitario: float) -> bool:
        """Actualiza un material existente"""
        query = """
            UPDATE materiales 
            SET nombre = ?, unidad_medida = ?, precio_unitario = ?
            WHERE id = ?
        """
        try:
            self.db.execute_update(query, (nombre, unidad_medida, precio_unitario, material_id))
            return True
        except:
            return False
    
    def eliminar_material(self, material_id: int) -> bool:
        """Elimina un material de la base de datos"""
        query = "DELETE FROM materiales WHERE id = ?"
        try:
            self.db.execute_update(query, (material_id,))
            return True
        except:
            return False
    
    def obtener_top_materiales(self, fecha_inicio: str = None, fecha_fin: str = None, limite: int = 10) -> Dict[str, List[Dict[str, Any]]]:
        """Obtiene top materiales por cantidad e ingresos"""
        where_clauses = []
        params = []
        
        if fecha_inicio:
            where_clauses.append("DATE(f.fecha_creacion) >= ?")
            params.append(fecha_inicio)
        
        if fecha_fin:
            where_clauses.append("DATE(f.fecha_creacion) <= ?")
            params.append(fecha_fin)
        
        # Construir WHERE clause
        if where_clauses:
            where_sql = "WHERE " + " AND ".join(where_clauses)
        else:
            where_sql = ""
        
        # Top por cantidad vendida
        base_query_cantidad = """
            SELECT 
                m.id as material_id,
                m.nombre as material_nombre,
                m.unidad_medida,
                COALESCE(SUM(fi.cantidad), 0) as cantidad_total,
                COUNT(DISTINCT fi.factura_id) as veces_usado
            FROM materiales m
            INNER JOIN factura_items fi ON fi.material_id = m.id
            INNER JOIN facturas f ON f.id = fi.factura_id
        """
        query_cantidad = base_query_cantidad + where_sql + """
            GROUP BY m.id, m.nombre, m.unidad_medida
            HAVING cantidad_total > 0
            ORDER BY cantidad_total DESC
            LIMIT ?
        """
        params_cantidad = tuple(list(params) + [limite])
        top_por_cantidad = self.db.execute_query(query_cantidad, params_cantidad)
        
        # Top por ingresos generados
        base_query_ingresos = """
            SELECT 
                m.id as material_id,
                m.nombre as material_nombre,
                m.unidad_medida,
                COALESCE(SUM(fi.subtotal), 0) as ingresos_total,
                COALESCE(SUM(fi.cantidad), 0) as cantidad_total,
                COUNT(DISTINCT fi.factura_id) as veces_usado
            FROM materiales m
            INNER JOIN factura_items fi ON fi.material_id = m.id
            INNER JOIN facturas f ON f.id = fi.factura_id
        """
        query_ingresos = base_query_ingresos + where_sql + """
            GROUP BY m.id, m.nombre, m.unidad_medida
            HAVING ingresos_total > 0
            ORDER BY ingresos_total DESC
            LIMIT ?
        """
        params_ingresos = tuple(list(params) + [limite])
        top_por_ingresos = self.db.execute_query(query_ingresos, params_ingresos)
        
        return {
            'por_cantidad': top_por_cantidad,
            'por_ingresos': top_por_ingresos
        }

# Instancia global del manager de materiales
material_manager = MaterialManager()
