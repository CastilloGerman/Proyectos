import sqlite3 
import os
class ConexionDB:
    def __init__(self):
        base_dir = os.path.dirname(os.path.dirname(__file__))
        db_dir = os.path.join(base_dir, 'database')
        if not os.path.isdir(db_dir):
            os.makedirs(db_dir, exist_ok=True)
        self.base_datos = os.path.join(db_dir, 'peliculas.db')
        self.conexion = sqlite3.connect(self.base_datos)
        self.cursor = self.conexion.cursor()

    def cerrar_conexion(self):
        self.conexion.commit()
        self.conexion.close()
