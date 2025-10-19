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

# Instancia global del manager de materiales
material_manager = MaterialManager()
