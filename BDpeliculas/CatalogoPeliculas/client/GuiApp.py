import tkinter as tk
from tkinter import ttk
from tkinter import messagebox
import sqlite3
import requests
import webbrowser
try:
    from ..model.pelicula_dao import crear_tabla, borrar_tabla, Pelicula, listar
except ImportError:
    import os
    import sys
    sys.path.append(os.path.dirname(os.path.dirname(__file__)))
    from model.pelicula_dao import crear_tabla, borrar_tabla, Pelicula, listar

def BarraMenu(root):
    # Modo oscuro para menú
    menu_bg = '#0b0f19'
    menu_fg = '#e5e7eb'
    menu_active_bg = '#1f2937'
    menu_active_fg = '#ffffff'
    BarraMenu = tk.Menu(root, bg=menu_bg, fg=menu_fg, activebackground=menu_active_bg, activeforeground=menu_active_fg, tearoff=0, bd=0)
    root.config(menu=BarraMenu, width=300, height=300)
    MenuInicio = tk.Menu(BarraMenu, tearoff=0, bg=menu_bg, fg=menu_fg, activebackground=menu_active_bg, activeforeground=menu_active_fg, bd=0)
    BarraMenu.add_cascade(label="Inicio", menu=MenuInicio)

    MenuInicio.add_command(label="Ingresar Registro", command=crear_tabla)
    MenuInicio.add_command(label="Eliminar Registro", command=borrar_tabla)
    MenuInicio.add_command(label="Salir", command=root.destroy)

    # Cascadas sin submenús (estilo oscuro)
    BarraMenu.add_cascade(label="Consultas")
    BarraMenu.add_cascade(label="Configuración")
    BarraMenu.add_cascade(label="Ayuda")

class Frame(tk.Frame):
    def __init__(self, root=None):
        super().__init__(root)
        self.root = root
        self.pack(fill="both", expand=True)
        self.config(width=700, height=600)
        self.setup_styles()
        self.CamposBusqueda()
        self.CamposPeliculas()
        self.tabla_peliculas()  # Llamada al método para crear la tabla

    def CamposPeliculas(self):
#nombre
        self.labelnombre = ttk.LabelFrame(self, text="Nombre", style='Dark.TLabelframe')
        # ttk maneja el estilo del título vía 'Dark.TLabelframe.Label'
        self.labelnombre.pack(fill="x", padx=20, pady=10)
        
        self.entry_nombre = ttk.Entry(self.labelnombre, justify="center", state="disabled", width=30)
        self.entry_nombre.pack(padx=10, pady=5)
#duracion
        self.labelduracion = ttk.LabelFrame(self, text="Duracion", style='Dark.TLabelframe')
        self.labelduracion.pack(fill="x", padx=20, pady=10)
        
        self.entry_duracion = ttk.Entry(self.labelduracion, justify="center", state="disabled", width=30)
        self.entry_duracion.pack(padx=10, pady=5)
#genero
        self.labelgenero = ttk.LabelFrame(self, text="Genero", style='Dark.TLabelframe')
        self.labelgenero.pack(fill="x", padx=20, pady=10)

        self.entry_genero = ttk.Entry(self.labelgenero, justify="center", state="disabled", width=30)
        self.entry_genero.pack(padx=10, pady=5)

# Frame contenedor para los botones
        self.frame_botones = tk.Frame(self)
        self.frame_botones.pack(pady=10)
        try:
            # Fondo oscuro para el contenedor de botones para evitar bordes claros aparentes
            self.frame_botones.configure(bg='#111827', highlightthickness=0, bd=0)
        except Exception:
            pass
#boton nuevo
        self.boton_nuevo = ttk.Button(self.frame_botones, text="🆕 Nuevo", command=self.habilitar_campos, style='Accent.TButton', takefocus=False)
        self.boton_nuevo.config(width=20)
        self.boton_nuevo.pack(side="left", padx=10)
#boton guardar
        self.boton_guardar = ttk.Button(self.frame_botones, text="💾 Guardar", command=self.guardar_datos, style='Accent.TButton', takefocus=False)
        self.boton_guardar.config(width=20)
        self.boton_guardar.pack(side="left", padx=10)
#boton cancelar
        self.boton_cancelar = ttk.Button(self.frame_botones, text="✖ Cancelar", command=self.boton_cancelar_func, style='Accent.TButton', takefocus=False)
        self.boton_cancelar.config(width=20)
        self.boton_cancelar.pack(side="left", padx=10)
#boton Editar
        self.boton_editar = ttk.Button(self.frame_botones, text="✏️ Editar", command=self.boton_guardar_func, style='Accent.TButton', takefocus=False)
        self.boton_editar.config(width=20)
        self.boton_editar.pack(side="left", padx=10)
#boton eliminar
        self.boton_eliminar = ttk.Button(self.frame_botones, text="🗑️ Eliminar", command=self.boton_eliminar_func, style='Accent.TButton', takefocus=False)
        self.boton_eliminar.config(width=20)
        self.boton_eliminar.pack(side="left", padx=10)

    def CamposBusqueda(self):
        # Barra de búsqueda para API externa
        self.frame_busqueda = ttk.LabelFrame(self, text="Buscar en API (TMDB)", style='Dark.TLabelframe')
        self.frame_busqueda.pack(fill="x", padx=20, pady=10)

        self.entry_busqueda = ttk.Entry(self.frame_busqueda, justify="left", width=40)
        self.entry_busqueda.pack(side="left", padx=10, pady=5)

        self.boton_buscar_api = ttk.Button(self.frame_busqueda, text="🔎 Buscar en API", command=self.buscar_en_api, style='Accent.TButton')
        self.boton_buscar_api.config(width=15)
        self.boton_buscar_api.pack(side="left", padx=10)

        # Contenedor de resultados
        self.frame_resultados_api = tk.Frame(self)
        self.frame_resultados_api.pack(fill="both", padx=20, pady=5)
        # modo oscuro para contenedor
        try:
            self.frame_resultados_api.configure(bg='#0b0f19', highlightthickness=1, highlightbackground='#374151', highlightcolor='#374151')
        except Exception:
            pass

        self.scroll_api = ttk.Scrollbar(self.frame_resultados_api, orient='vertical', style='Dark.Vertical.TScrollbar')
        self.scroll_api.pack(side="right", fill="y")

        self.listbox_api = tk.Listbox(self.frame_resultados_api, height=8, yscrollcommand=self.scroll_api.set)
        self.listbox_api.pack(side="left", fill="both", expand=True)
        # modo oscuro para listbox
        try:
            self.listbox_api.configure(bg='#111827', fg='#e5e7eb', selectbackground='#3730a3', selectforeground='#ffffff', highlightthickness=0, bd=0, relief='flat')
        except Exception:
            pass
        self.scroll_api.config(command=self.listbox_api.yview)

        # Botón abrir enlace selección
        self.boton_abrir_api = ttk.Button(self, text="🔗 Abrir selección", command=self.abrir_seleccion_api, style='Accent.TButton')
        self.boton_abrir_api.config(width=20)
        self.boton_abrir_api.pack(padx=20, pady=5)

        # Bind doble clic
        self.listbox_api.bind("<Double-Button-1>", lambda e: self.abrir_seleccion_api())

        # Almacén de resultados
        self.api_results = []
        self.api_result_urls = []

    def buscar_en_api(self):
        try:
            titulo = self.entry_busqueda.get().strip()
            if not titulo:
                messagebox.showinfo("Búsqueda", "Ingresa un título para buscar.")
                return
            resultados = buscar_peliculas(titulo)
            # Limpiar lista y almacenes
            self.listbox_api.delete(0, tk.END)
            self.api_results = []
            self.api_result_urls = []
            if not resultados:
                messagebox.showinfo("Resultados", "No se encontraron resultados.")
                return
            # Tomar hasta 20 resultados
            for item in resultados[:20]:
                nombre = item.get("title") or item.get("name") or "(Sin título)"
                fecha = item.get("release_date") or item.get("first_air_date") or ""
                etiqueta = f"{nombre} {f'({fecha})' if fecha else ''}"
                self.listbox_api.insert(tk.END, etiqueta)
                # Construir URL de TMDB (búsqueda actual es de 'movie')
                movie_id = item.get("id")
                url = f"https://www.themoviedb.org/movie/{movie_id}" if movie_id is not None else None
                self.api_results.append(item)
                self.api_result_urls.append(url)
        except Exception as e:
            messagebox.showerror("Error", str(e))

    def abrir_seleccion_api(self):
        try:
            seleccion = self.listbox_api.curselection()
            if not seleccion:
                messagebox.showinfo("Abrir", "Selecciona un resultado de la lista.")
                return
            idx = seleccion[0]
            url = self.api_result_urls[idx] if idx < len(self.api_result_urls) else None
            if not url:
                messagebox.showwarning("Abrir", "No hay URL disponible para este elemento.")
                return
            webbrowser.open(url)
        except Exception as e:
            messagebox.showerror("Error", str(e))

    def habilitar_campos(self):
        # Ejemplo: habilita los campos de texto
        self.entry_nombre.config(state="normal")
        self.entry_duracion.config(state="normal")
        self.entry_genero.config(state="normal")
   
    def guardar_datos(self):
        nombre = self.entry_nombre.get()
        duracion = self.entry_duracion.get()
        genero = self.entry_genero.get()
        # Intentar tomar link desde la selección actual de la lista de API
        link = ""
        nombre_api = ""
        detalles = None
        try:
            seleccion = self.listbox_api.curselection()
            if seleccion:
                idx = seleccion[0]
                if idx < len(self.api_result_urls):
                    link = self.api_result_urls[idx] or ""
                # Tomar nombre desde resultados API si disponible
                if idx < len(self.api_results):
                    item = self.api_results[idx]
                    nombre_api = item.get("title") or item.get("name") or ""
                    # Completar detalles (duración/género) si faltan
                    movie_id = item.get("id")
                    if movie_id is not None:
                        detalles = obtener_detalles_pelicula(movie_id)
        except Exception:
            link = ""
            nombre_api = ""
            detalles = None
        # Si el campo nombre está vacío, usar el nombre de la API (si existe)
        if (not nombre or not nombre.strip()) and nombre_api:
            nombre = nombre_api
        # Si duración o género están vacíos, intentar completar con detalles
        if detalles:
            if (not duracion or not duracion.strip()) and detalles.get("duracion"):
                duracion = detalles["duracion"]
            if (not genero or not genero.strip()) and detalles.get("genero"):
                genero = detalles["genero"]
        try:
            pelicula = Pelicula(nombre=nombre, duracion=duracion, genero=genero, calificacion="", link=link)
            pelicula.guardar()
            print(f"Guardando datos: Nombre={nombre}, Duracion={duracion}, Genero={genero}")
            # refrescar tabla
            self.cargar_tabla()
            self.entry_nombre.delete(0, tk.END)
            self.entry_duracion.delete(0, tk.END)
            self.entry_genero.delete(0, tk.END)
            self.entry_nombre.config(state="disabled")
            self.entry_duracion.config(state="disabled")
            self.entry_genero.config(state="disabled")
        except sqlite3.OperationalError as e:
            if "no such table" in str(e).lower():
                messagebox.showerror("Error", "La tabla 'peliculas' no existe. Cree la tabla desde Inicio > Ingresar Registro y vuelva a intentar.")
            else:
                messagebox.showerror("Error de base de datos", str(e))
        except Exception as e:
            messagebox.showerror("Error", str(e))
    #boton cancelar
    def boton_cancelar_func(self):
        self.entry_nombre.delete(0, tk.END)
        self.entry_duracion.delete(0, tk.END)
        self.entry_genero.delete(0, tk.END)
        self.entry_nombre.config(state="disabled")
        self.entry_duracion.config(state="disabled")
        self.entry_genero.config(state="disabled")

    def tabla_peliculas(self):
        # Crear un frame para la tabla
        self.peliculas = listar()
        self.frame_tabla = tk.Frame(self)
        self.frame_tabla.pack(fill="x", padx=20, pady=10)  # Cambia fill="both", expand=True por fill="x"

        # Scrollbar vertical
        scrollbar = ttk.Scrollbar(self.frame_tabla, orient='vertical', style='Dark.Vertical.TScrollbar')
        scrollbar.pack(side="right", fill="y")

        # Crear la tabla con menos altura
        self.tabla = ttk.Treeview(
            self.frame_tabla,
            columns=("Nombre", "Duración", "Género"),
            show="headings",
            yscrollcommand=scrollbar.set,
            height=10  # <-- Cambia este valor para menos filas visibles
            )
        
        # Definir los encabezados de las columnas
        self.tabla.heading("Nombre", text="Nombre")
        self.tabla.heading("Duración", text="Duración")
        self.tabla.heading("Género", text="Género")
        self.tabla.pack(fill="x")  # Cambia fill="both", expand=True por fill="x"
        scrollbar.config(command=self.tabla.yview) # Configurar la scrollbar

        # Cargar datos iniciales
        self.cargar_tabla()
        self.tabla.bind("<ButtonRelease-1>", self.seleccionar_pelicula)
        self.tabla.bind("<Double-Button-1>", self.abrir_link_registro)

    def cargar_tabla(self):
        # vaciar la tabla y volver a cargar desde la BD
        for item in self.tabla.get_children():
            self.tabla.delete(item)
        self.peliculas = listar()
        for idx, pelicula in enumerate(self.peliculas):
            # pelicula = (id, nombre, duracion, genero, calificacion)
            tag = 'even' if idx % 2 == 0 else 'odd'
            self.tabla.insert("", "end", iid=str(pelicula[0]), values=(pelicula[1], pelicula[2], pelicula[3]), tags=(tag,))
        # Zebra styling (modo oscuro)
        self.tabla.tag_configure('even', background='#0f172a')
        self.tabla.tag_configure('odd', background='#111827')

    def setup_styles(self):
        try:
            style = ttk.Style()
            # Usar un tema claro moderno
            if 'clam' in style.theme_names():
                style.theme_use('clam')
            # Paleta modo oscuro con acento violeta
            bg = '#0b0f19'
            surface = '#111827'
            header = '#1f2937'
            border = '#374151'
            accent = '#8b5cf6'        # Violet 500/600
            accent_hover = '#7c3aed'   # Violet 600/700
            text = '#e5e7eb'
            subtle = '#9ca3af'
            selection = '#3730a3'     # Indigo 800 como selección

            # Fondo ventana principal
            try:
                self.root.configure(bg=bg)
                self.configure(bg=bg)
            except Exception:
                pass

            # Botones base sin bordes/blanco
            style.configure('TButton', padding=10, relief='flat', foreground=text, background=surface, bordercolor=border, borderwidth=0, focusthickness=0, focuscolor=surface)
            style.map('TButton',
                      foreground=[('disabled', subtle), ('active', text)],
                      background=[('pressed', surface), ('active', surface)],
                      highlightcolor=[('!focus', surface)],
                      focuscolor=[('!focus', surface)])
            # Layout para TButton para eliminar halos/bordes claros
            style.layout('TButton', [
                ('Button.border', {
                    'sticky': 'nswe',
                    'border': 0,
                    'children': [
                        ('Button.padding', {
                            'sticky': 'nswe',
                            'children': [
                                ('Button.label', {'sticky': 'nswe'})
                            ]
                        })
                    ]
                })
            ])

            # Botón acento (violeta) sin bordes/blanco
            style.configure('Accent.TButton', padding=10, relief='flat', foreground='#ffffff', background=accent, borderwidth=0, focusthickness=0, focuscolor=accent)
            style.map('Accent.TButton',
                      foreground=[('disabled', '#9ca3af'), ('active', '#ffffff')],
                      background=[('pressed', accent_hover), ('active', accent_hover)],
                      highlightcolor=[('!focus', accent)],
                      focuscolor=[('!focus', accent)])
            # Layout para Accent.TButton para eliminar halos/bordes claros
            style.layout('Accent.TButton', [
                ('Button.border', {
                    'sticky': 'nswe',
                    'border': 0,
                    'children': [
                        ('Button.padding', {
                            'sticky': 'nswe',
                            'children': [
                                ('Button.label', {'sticky': 'nswe'})
                            ]
                        })
                    ]
                })
            ])

            # Entradas
            style.configure('TEntry', padding=8, fieldbackground=surface, foreground=text)

            # LabelFrames oscuros
            style.configure('Dark.TLabelframe', background=surface, bordercolor=border, relief='groove')
            style.configure('Dark.TLabelframe.Label', background=surface, foreground=text, padding=4)

            # Treeview y encabezados con bordes sutiles
            style.configure('Treeview', background=surface, fieldbackground=surface, foreground=text, rowheight=28, bordercolor=border, borderwidth=1)
            style.configure('Treeview.Heading', background=header, foreground=text, padding=8, bordercolor=border, borderwidth=1)
            style.map('Treeview', background=[('selected', selection)], foreground=[('selected', '#ffffff')])

            # Fondo oscuro para frames ttk
            style.configure('TFrame', background=bg)
            style.configure('TLabelframe', background=surface)
            style.configure('TLabelframe.Label', background=surface, foreground=text)

            # Scrollbar oscuro (vertical)
            style.configure('Dark.Vertical.TScrollbar', gripcount=0, background=header, darkcolor=border, lightcolor=header, troughcolor=bg, bordercolor=border, arrowcolor=text)
            style.map('Dark.Vertical.TScrollbar', background=[('active', header)], arrowcolor=[('active', '#ffffff')])

            # Ajustes visuales de listbox de API
            try:
                self.frame_resultados_api.configure(bg=bg)
                self.listbox_api.configure(bg=surface, fg=text, selectbackground=selection, selectforeground='#ffffff', highlightthickness=0, bd=0)
            except Exception:
                pass
        except Exception:
            pass

    def seleccionar_pelicula(self, event):
        iid = self.tabla.selection()[0]
        valores = self.tabla.item(iid, 'values')
        self.entry_nombre.config(state="normal")
        self.entry_duracion.config(state="normal")
        self.entry_genero.config(state="normal")
        self.entry_nombre.delete(0, tk.END)
        self.entry_duracion.delete(0, tk.END)
        self.entry_genero.delete(0, tk.END)
        self.entry_nombre.insert(0, valores[0])
        self.entry_duracion.insert(0, valores[1])
        self.entry_genero.insert(0, valores[2])

    def abrir_link_registro(self, event=None):
        try:
            seleccion = self.tabla.selection()
            if not seleccion:
                return
            iid = seleccion[0]
            registro_id = int(iid)
            # Buscar link en self.peliculas (SELECT *: id, nombre, duracion, genero, calificacion, link)
            link = None
            for fila in self.peliculas:
                if fila[0] == registro_id:
                    # Link suele estar en la  f3ltima posición
                    link = fila[-1] if len(fila) >= 6 else None
                    break
            if not link:
                messagebox.showwarning("Abrir", "Este registro no tiene link guardado.")
                return
            webbrowser.open(link)
        except Exception as e:
            messagebox.showerror("Error", str(e))
    def boton_editar_func(self):
        iid = self.tabla.selection()[0]
        self.entry_nombre.config(state="normal")
        self.entry_duracion.config(state="normal")
        self.entry_genero.config(state="normal")
        self.entry_nombre.delete(0, tk.END)
        self.entry_duracion.delete(0, tk.END)
        self.entry_genero.delete(0, tk.END)
        valores = self.tabla.item(iid, 'values')
        self.entry_nombre.insert(0, valores[0])
        self.entry_duracion.insert(0, valores[1])
        self.entry_genero.insert(0, valores[2])
    def boton_eliminar_func(self):
        try:
            iid = self.tabla.selection()[0]
            pelicula = Pelicula(id=int(iid), nombre="", duracion="", genero="", calificacion="")
            pelicula.eliminar()
            self.cargar_tabla()
        except IndexError:
            messagebox.showwarning("Atención", "Selecciona un registro antes de eliminar.")
        except sqlite3.Error as e:
            messagebox.showerror("Error de base de datos", str(e))
        except Exception as e:
            messagebox.showerror("Error", str(e))
    def boton_guardar_func(self):
        try:
            iid = self.tabla.selection()[0]
            # En edición, mantener link si existe y permitir sobrescribir con selección API
            link_actual = ""
            try:
                # No tenemos el link en la tabla visible; podríamos consultarlo si es necesario
                link_actual = ""
            except Exception:
                link_actual = ""
            link_nuevo = link_actual
            nombre_api = ""
            detalles = None
            try:
                seleccion = self.listbox_api.curselection()
                if seleccion:
                    idx = seleccion[0]
                    if idx < len(self.api_result_urls):
                        link_nuevo = self.api_result_urls[idx] or link_actual
                    if idx < len(self.api_results):
                        item = self.api_results[idx]
                        nombre_api = item.get("title") or item.get("name") or ""
                        movie_id = item.get("id")
                        if movie_id is not None:
                            detalles = obtener_detalles_pelicula(movie_id)
            except Exception:
                pass
            nombre_final = self.entry_nombre.get()
            if (not nombre_final or not nombre_final.strip()) and nombre_api:
                nombre_final = nombre_api
            duracion_final = self.entry_duracion.get()
            genero_final = self.entry_genero.get()
            if detalles:
                if (not duracion_final or not duracion_final.strip()) and detalles.get("duracion"):
                    duracion_final = detalles["duracion"]
                if (not genero_final or not genero_final.strip()) and detalles.get("genero"):
                    genero_final = detalles["genero"]
            pelicula = Pelicula(
                id=int(iid),
                nombre=nombre_final,
                duracion=duracion_final,
                genero=genero_final,
                calificacion="",
                link=link_nuevo
            )
            pelicula.editar()
            self.cargar_tabla()
        except IndexError:
            messagebox.showwarning("Atención", "Selecciona un registro antes de guardar cambios.")
        except sqlite3.Error as e:
            messagebox.showerror("Error de base de datos", str(e))
        except Exception as e:
            messagebox.showerror("Error", str(e))
    def boton_cancelar_func(self):
        try:
            self.entry_nombre.delete(0, tk.END)
            self.entry_duracion.delete(0, tk.END)
            self.entry_genero.delete(0, tk.END)
            self.entry_nombre.config(state="disabled")
            self.entry_duracion.config(state="disabled")
            self.entry_genero.config(state="disabled")
        except Exception as e:
            messagebox.showerror("Error", str(e))

API_KEY = "c92831df528ed768a74613af650cd187"
BASE_URL = "https://api.themoviedb.org/3"

def buscar_peliculas(titulo):
    url = f"{BASE_URL}/search/movie?api_key={API_KEY}&query={titulo}"
    response = requests.get(url, timeout=10)
    if response.status_code == 200:
        return response.json()["results"]
    else:
        return []

def obtener_detalles_pelicula(movie_id):
    try:
        url = f"{BASE_URL}/movie/{movie_id}?api_key={API_KEY}"
        response = requests.get(url, timeout=10)
        if response.status_code != 200:
            return None
        data = response.json()
        runtime = data.get("runtime")  # minutos
        generos = data.get("genres") or []  # lista de dicts con 'name'
        nombres_generos = ", ".join([g.get("name") for g in generos if g.get("name")])
        duracion_str = f"{runtime} min" if isinstance(runtime, int) and runtime > 0 else ""
        return {"duracion": duracion_str, "genero": nombres_generos}
    except Exception:
        return None

        
        