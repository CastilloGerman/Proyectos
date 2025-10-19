from .utils import db
from typing import List, Dict, Any, Optional

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

# Instancia global del manager de clientes
cliente_manager = ClienteManager()
