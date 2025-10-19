from .conexion_db import ConexionDB
from tkinter import messagebox
#crear tabla peliculas
def crear_tabla():
    conexion = ConexionDB()
    sql = '''CREATE TABLE IF NOT EXISTS peliculas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                duracion TEXT NOT NULL,
                genero TEXT NOT NULL,
                calificacion TEXT NOT NULL,
                link TEXT
            )'''
    try:
        conexion.cursor.execute(sql)
        # Migraci 3n segura: a dadir columna link si la tabla ya exist 3da sin esa columna
        try:
            conexion.cursor.execute('ALTER TABLE peliculas ADD COLUMN link TEXT')
        except Exception:
            # Si ya existe la columna, ignorar el error
            pass
        conexion.cerrar_conexion()
        messagebox.showinfo("Éxito", "Tabla 'peliculas' creada correctamente.")
    except Exception as e:
        messagebox.showerror("Error", f"No se pudo crear la tabla: {e}")
        conexion.cerrar_conexion()
#borrar tabla peliculas
def borrar_tabla():
    conexion = ConexionDB()
    sql = 'DROP TABLE IF EXISTS peliculas'
    conexion.cursor.execute(sql)
    conexion.cerrar_conexion()
    try:
        messagebox.showinfo("Éxito", "Tabla 'peliculas' borrada correctamente.")
    except Exception as e:
        messagebox.showerror("Error", f"No hay tabla para borrar: {e}")        

class Pelicula:
    def __init__(self, nombre, duracion, genero, calificacion, link="", id=None):
        self.id = id
        self.nombre = nombre
        self.duracion = duracion
        self.genero = genero
        self.calificacion = calificacion
        self.link = link

    def __str__(self):
        return f'Pelicula({self.id}, {self.nombre}, {self.duracion}, {self.genero}, {self.calificacion})'

    def guardar(self):
        conexion = ConexionDB()
        # Asegurar que la columna link existe
        try:
            conexion.cursor.execute('ALTER TABLE peliculas ADD COLUMN link TEXT')
        except Exception:
            pass
        sql = 'INSERT INTO peliculas (nombre, duracion, genero, calificacion, link) VALUES (?, ?, ?, ?, ?)'
        valores = (self.nombre, self.duracion, self.genero, self.calificacion, self.link)
        conexion.cursor.execute(sql, valores)
        conexion.cerrar_conexion()
        try:
            messagebox.showinfo("Éxito", f"Pelicula '{self.nombre}' guardada correctamente.")
        except Exception as e:
            messagebox.showerror("Error", f"No se pudo guardar la pelicula: {e}")

    def editar(self):
        conexion = ConexionDB()
        # Asegurar que la columna link existe
        try:
            conexion.cursor.execute('ALTER TABLE peliculas ADD COLUMN link TEXT')
        except Exception:
            pass
        sql = 'UPDATE peliculas SET nombre = ?, duracion = ?, genero = ?, calificacion = ?, link = ? WHERE id = ?'
        valores = (self.nombre, self.duracion, self.genero, self.calificacion, self.link, self.id)
        conexion.cursor.execute(sql, valores)
        conexion.cerrar_conexion()
        try:
            messagebox.showinfo("Éxito", f"Pelicula '{self.nombre}' actualizada correctamente.")
        except Exception as e:
            messagebox.showerror("Error", f"No se pudo actualizar la pelicula: {e}")

    def eliminar(self):
        conexion = ConexionDB()
        sql = 'DELETE FROM peliculas WHERE id = ?'
        valores = (self.id,)
        conexion.cursor.execute(sql, valores)
        conexion.cerrar_conexion()
        try:
            messagebox.showinfo("Éxito", f"Pelicula '{self.nombre}' eliminada correctamente.")
        except Exception as e:
            messagebox.showerror("Error", f"No se pudo eliminar la pelicula: {e}")

def listar():
    conexion = ConexionDB()
    peliculas = []
    sql = 'SELECT * FROM peliculas'
    try:
        conexion.cursor.execute(sql)
        peliculas = conexion.cursor.fetchall()
        conexion.cerrar_conexion()
        return peliculas
    except Exception as e:
        mensaje = "No se pudo listar las peliculas, crea la tabla primero"
        messagebox.showerror("Error", f"{mensaje}: {e}")
        conexion.cerrar_conexion()
        return []

def buscar_por_nombre(texto):
    conexion = ConexionDB()
    resultados = []
    try:
        consulta = f"%{texto}%"
        # Seleccionar campos necesarios, incluido link
        conexion.cursor.execute('SELECT id, nombre, link FROM peliculas WHERE nombre LIKE ? ORDER BY nombre ASC', (consulta,))
        resultados = conexion.cursor.fetchall()
        conexion.cerrar_conexion()
        return resultados
    except Exception as e:
        messagebox.showerror("Error", f"No se pudo buscar en la base de datos: {e}")
        conexion.cerrar_conexion()
        return []
