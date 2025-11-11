import tkinter as tk
from tkinter import ttk, messagebox, filedialog, simpledialog
from datetime import datetime, timedelta
import os
import subprocess
import platform
import json
import ast
import copy
import re
from presupuestos.clientes import cliente_manager
from presupuestos.materiales import material_manager
from presupuestos.presupuestos import presupuesto_manager
from presupuestos.facturas import factura_manager
from presupuestos.utils import db
from presupuestos.pdf_generator import PDFGenerator
from presupuestos.email_sender import email_sender


MESES_NOMBRES = {
    '01': 'Enero',
    '02': 'Febrero',
    '03': 'Marzo',
    '04': 'Abril',
    '05': 'Mayo',
    '06': 'Junio',
    '07': 'Julio',
    '08': 'Agosto',
    '09': 'Septiembre',
    '10': 'Octubre',
    '11': 'Noviembre',
    '12': 'Diciembre'
}
try:
    from matplotlib.backends.backend_tkagg import FigureCanvasTkAgg
    from matplotlib.figure import Figure
    MATPLOTLIB_AVAILABLE = True
except ImportError as e:
    MATPLOTLIB_AVAILABLE = False
    import sys
    import os
    current_python = sys.executable
    
    # Verificar si estamos en el venv
    # __file__ está en ui/app.py, necesitamos ir dos niveles arriba
    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    venv_python = os.path.join(project_root, 'venv', 'Scripts', 'python.exe')
    is_venv = 'venv' in current_python.lower() or os.path.exists(venv_python)
    
    if not is_venv:
        print(f"⚠️ ADVERTENCIA: No estás usando el Python del entorno virtual.")
        print(f"Python actual: {current_python}")
        print(f"Para usar el venv, ejecuta desde la terminal:")
        print(f"  .\\venv\\Scripts\\python.exe main.py")
        print(f"O usa el script: .\\scripts\\iniciar_app.ps1")
    else:
        print(f"⚠️ Error: matplotlib no está disponible en este intérprete.")
        print(f"Python actual: {current_python}")
        print(f"Por favor ejecuta: .\\venv\\Scripts\\python.exe -m pip install matplotlib")
    
    print(f"Error detallado: {e}")
    
    # Crear clases dummy para evitar errores
    class FigureCanvasTkAgg:
        def __init__(self, *args, **kwargs):
            pass
        def get_tk_widget(self):
            return None
        def draw(self):
            pass
    class Figure:
        def __init__(self, *args, **kwargs):
            pass
        def add_subplot(self, *args, **kwargs):
            return None

class AppPresupuestos:
    def __init__(self, root):
        self.root = root
        self.root.title("Sistema de Gestión de Presupuestos")
        self.root.geometry("1200x800")
        self.root.configure(bg='#ecf0f1')
        
        # Configurar el estilo
        from .styles import setup_styles
        setup_styles()
        
        # Crear el notebook para las pestañas
        self.notebook = ttk.Notebook(root)
        self.notebook.pack(fill='both', expand=True, padx=10, pady=10)
        
        # Crear las pestañas
        self.create_clientes_tab()
        self.create_materiales_tab()
        self.create_presupuestos_tab()
        self.create_ver_presupuestos_tab()
        self.create_facturacion_tab()
        self.create_metricas_tab()
        
        # Cargar datos iniciales
        self.refresh_clientes()
        self.refresh_materiales()
        self.refresh_presupuestos()
        self.refresh_facturas()
        
        # Configurar evento de cierre para guardar configuración
        self.root.protocol("WM_DELETE_WINDOW", self.on_closing)
    
    def create_clientes_tab(self):
        """Crea la pestaña de gestión de clientes"""
        self.clientes_frame = ttk.Frame(self.notebook)
        self.notebook.add(self.clientes_frame, text="Gestión de Clientes")
        
        # Frame principal con scroll
        main_frame = ttk.Frame(self.clientes_frame)
        main_frame.pack(fill='both', expand=True, padx=10, pady=10)
        
        # Frame para formulario de cliente
        form_frame = ttk.LabelFrame(main_frame, text="Datos del Cliente", padding=10)
        form_frame.pack(fill='x', pady=(0, 10))
        
        # Campos del formulario
        ttk.Label(form_frame, text="Nombre:", style='TLabel').grid(row=0, column=0, sticky='w', padx=(0, 10), pady=8)
        self.nombre_entry = ttk.Entry(form_frame, width=25, style='TEntry')
        self.nombre_entry.grid(row=0, column=1, padx=(0, 20), pady=8)
        
        ttk.Label(form_frame, text="Teléfono:", style='TLabel').grid(row=0, column=2, sticky='w', padx=(0, 10), pady=8)
        self.telefono_entry = ttk.Entry(form_frame, width=20, style='TEntry')
        self.telefono_entry.grid(row=0, column=3, padx=(0, 20), pady=8)
        
        ttk.Label(form_frame, text="Email:", style='TLabel').grid(row=1, column=0, sticky='w', padx=(0, 10), pady=8)
        self.email_entry = ttk.Entry(form_frame, width=25, style='TEntry')
        self.email_entry.grid(row=1, column=1, padx=(0, 20), pady=8)
        
        ttk.Label(form_frame, text="Dirección:", style='TLabel').grid(row=1, column=2, sticky='w', padx=(0, 10), pady=8)
        self.direccion_entry = ttk.Entry(form_frame, width=25, style='TEntry')
        self.direccion_entry.grid(row=1, column=3, padx=(0, 20), pady=8)
        
        ttk.Label(form_frame, text="NIF/NIE/IVA Intracomunitario:", style='TLabel').grid(row=2, column=0, sticky='w', padx=(0, 10), pady=8)
        self.dni_entry = ttk.Entry(form_frame, width=20, style='TEntry')
        self.dni_entry.grid(row=2, column=1, padx=(0, 20), pady=8)
        
        # Botones del formulario
        button_frame = ttk.Frame(form_frame)
        button_frame.grid(row=3, column=0, columnspan=4, pady=10)
        
        ttk.Button(button_frame, text="➕ Agregar Cliente", command=self.agregar_cliente, style='Success.TButton').pack(side='left', padx=(0, 10))
        ttk.Button(button_frame, text="✏️ Actualizar", command=self.actualizar_cliente, style='Accent.TButton').pack(side='left', padx=(0, 10))
        ttk.Button(button_frame, text="🗑️ Eliminar", command=self.eliminar_cliente, style='Danger.TButton').pack(side='left', padx=(0, 10))
        ttk.Button(button_frame, text="🧹 Limpiar", command=self.limpiar_formulario_cliente).pack(side='left')
        
        # Frame para búsqueda
        search_frame = ttk.Frame(main_frame)
        search_frame.pack(fill='x', pady=(0, 10))
        
        ttk.Label(search_frame, text="🔍 Buscar:", style='TLabel').pack(side='left', padx=(0, 10))
        self.busqueda_entry = ttk.Entry(search_frame, width=30, style='TEntry')
        self.busqueda_entry.pack(side='left', padx=(0, 10))
        self.busqueda_entry.bind('<KeyRelease>', self.buscar_clientes)
        
        ttk.Button(search_frame, text="🔍 Buscar", command=self.buscar_clientes).pack(side='left')
        
        # Treeview para mostrar clientes
        columns = ('ID', 'Nombre', 'Teléfono', 'Email', 'Dirección', 'DNI')
        self.clientes_tree = ttk.Treeview(main_frame, columns=columns, show='headings', height=15)
        
        for col in columns:
            self.clientes_tree.heading(col, text=col)
            self.clientes_tree.column(col, width=150)
        
        # Scrollbar para el treeview
        scrollbar = ttk.Scrollbar(main_frame, orient='vertical', command=self.clientes_tree.yview)
        self.clientes_tree.configure(yscrollcommand=scrollbar.set)
        
        self.clientes_tree.pack(side='left', fill='both', expand=True)
        scrollbar.pack(side='right', fill='y')
        
        # Bind para selección
        self.clientes_tree.bind('<<TreeviewSelect>>', self.on_cliente_select)
    
    def create_materiales_tab(self):
        """Crea la pestaña de gestión de materiales"""
        self.materiales_frame = ttk.Frame(self.notebook)
        self.notebook.add(self.materiales_frame, text="Gestión de Materiales")
        
        # Frame principal
        main_frame = ttk.Frame(self.materiales_frame)
        main_frame.pack(fill='both', expand=True, padx=10, pady=10)
        
        # Frame para formulario de material
        form_frame = ttk.LabelFrame(main_frame, text="Datos del Material", padding=10)
        form_frame.pack(fill='x', pady=(0, 10))
        
        # Campos del formulario
        ttk.Label(form_frame, text="Nombre:", style='TLabel').grid(row=0, column=0, sticky='w', padx=(0, 10), pady=8)
        self.material_nombre_entry = ttk.Entry(form_frame, width=25, style='TEntry')
        self.material_nombre_entry.grid(row=0, column=1, padx=(0, 20), pady=8)
        
        ttk.Label(form_frame, text="Unidad:", style='TLabel').grid(row=0, column=2, sticky='w', padx=(0, 10), pady=8)
        self.unidad_entry = ttk.Entry(form_frame, width=15, style='TEntry')
        self.unidad_entry.grid(row=0, column=3, padx=(0, 20), pady=8)
        
        ttk.Label(form_frame, text="Precio Unitario:", style='TLabel').grid(row=1, column=0, sticky='w', padx=(0, 10), pady=8)
        self.precio_entry = ttk.Entry(form_frame, width=20, style='TEntry')
        self.precio_entry.grid(row=1, column=1, padx=(0, 20), pady=8)
        
        # Botones del formulario
        button_frame = ttk.Frame(form_frame)
        button_frame.grid(row=2, column=0, columnspan=4, pady=10)
        
        ttk.Button(button_frame, text="➕ Agregar Material", command=self.agregar_material, style='Success.TButton').pack(side='left', padx=(0, 10))
        ttk.Button(button_frame, text="✏️ Actualizar", command=self.actualizar_material, style='Accent.TButton').pack(side='left', padx=(0, 10))
        ttk.Button(button_frame, text="🗑️ Eliminar", command=self.eliminar_material, style='Danger.TButton').pack(side='left', padx=(0, 10))
        ttk.Button(button_frame, text="🧹 Limpiar", command=self.limpiar_formulario_material).pack(side='left')
        
        # Frame para búsqueda
        search_frame = ttk.Frame(main_frame)
        search_frame.pack(fill='x', pady=(0, 10))
        
        ttk.Label(search_frame, text="🔍 Buscar:", style='TLabel').pack(side='left', padx=(0, 10))
        self.material_busqueda_entry = ttk.Entry(search_frame, width=30, style='TEntry')
        self.material_busqueda_entry.pack(side='left', padx=(0, 10))
        self.material_busqueda_entry.bind('<KeyRelease>', self.buscar_materiales)
        
        ttk.Button(search_frame, text="🔍 Buscar", command=self.buscar_materiales).pack(side='left')
        
        # Treeview para mostrar materiales
        columns = ('ID', 'Nombre', 'Unidad', 'Precio Unitario')
        self.materiales_tree = ttk.Treeview(main_frame, columns=columns, show='headings', height=15)
        
        for col in columns:
            self.materiales_tree.heading(col, text=col)
            self.materiales_tree.column(col, width=200)
        
        # Scrollbar para el treeview
        material_scrollbar = ttk.Scrollbar(main_frame, orient='vertical', command=self.materiales_tree.yview)
        self.materiales_tree.configure(yscrollcommand=material_scrollbar.set)
        
        self.materiales_tree.pack(side='left', fill='both', expand=True)
        material_scrollbar.pack(side='right', fill='y')
        
        # Bind para selección
        self.materiales_tree.bind('<<TreeviewSelect>>', self.on_material_select)
    
    def create_presupuestos_tab(self):
        """Crea la pestaña de creación de presupuestos"""
        self.presupuestos_frame = ttk.Frame(self.notebook)
        self.notebook.add(self.presupuestos_frame, text="Crear Presupuesto")
        
        # Frame principal con scrollbar
        main_frame = ttk.Frame(self.presupuestos_frame)
        main_frame.pack(fill='both', expand=True, padx=10, pady=10)
        
        # Canvas y scrollbar para toda la ventana
        canvas = tk.Canvas(main_frame, bg='#ecf0f1')
        scrollbar = ttk.Scrollbar(main_frame, orient="vertical", command=canvas.yview)
        scrollable_frame = ttk.Frame(canvas)
        
        def configure_scroll_region(event=None):
            canvas.configure(scrollregion=canvas.bbox("all"))
        
        scrollable_frame.bind("<Configure>", configure_scroll_region)
        
        canvas.create_window((0, 0), window=scrollable_frame, anchor="nw")
        canvas.configure(yscrollcommand=scrollbar.set)
        
        # Configurar el canvas para que se expanda correctamente
        def configure_canvas(event):
            canvas_width = event.width
            canvas.itemconfig(canvas.find_all()[0], width=canvas_width)
        
        canvas.bind('<Configure>', configure_canvas)
        
        canvas.pack(side="left", fill="both", expand=True)
        scrollbar.pack(side="right", fill="y")
        
        # Configurar scroll con rueda del mouse para toda la ventana
        def _on_mousewheel(event):
            canvas.yview_scroll(int(-1*(event.delta/120)), "units")
        
        def _bind_to_mousewheel(event):
            canvas.bind_all("<MouseWheel>", _on_mousewheel)
        
        def _unbind_from_mousewheel(event):
            canvas.unbind_all("<MouseWheel>")
        
        # Bind en el frame principal para que funcione en toda la ventana
        main_frame.bind('<Enter>', _bind_to_mousewheel)
        main_frame.bind('<Leave>', _unbind_from_mousewheel)
        canvas.bind('<Enter>', _bind_to_mousewheel)
        canvas.bind('<Leave>', _unbind_from_mousewheel)
        
        # Frame para selección de cliente
        cliente_frame = ttk.LabelFrame(scrollable_frame, text="Seleccionar Cliente", padding=10)
        cliente_frame.pack(fill='x', pady=(0, 10))
        
        ttk.Label(cliente_frame, text="👤 Cliente:", style='TLabel').pack(side='left', padx=(0, 10))
        self.cliente_var = tk.StringVar()
        self.cliente_combo = ttk.Combobox(cliente_frame, textvariable=self.cliente_var, width=40, state='readonly', style='TCombobox')
        self.cliente_combo.pack(side='left', padx=(0, 10))
        
        # Frame para agregar materiales
        material_frame = ttk.LabelFrame(scrollable_frame, text="Agregar Material", padding=10)
        material_frame.pack(fill='x', pady=(0, 10))
        
        ttk.Label(material_frame, text="📦 Material:", style='TLabel').grid(row=0, column=0, sticky='w', padx=(0, 10), pady=8)
        self.material_var = tk.StringVar()
        self.material_combo = ttk.Combobox(material_frame, textvariable=self.material_var, width=30, state='readonly', style='TCombobox')
        self.material_combo.grid(row=0, column=1, padx=(0, 20), pady=8)
        
        ttk.Label(material_frame, text="🔢 Cantidad:", style='TLabel').grid(row=0, column=2, sticky='w', padx=(0, 10), pady=8)
        self.cantidad_entry = ttk.Entry(material_frame, width=10, style='TEntry')
        self.cantidad_entry.grid(row=0, column=3, padx=(0, 20), pady=8)
        
        ttk.Button(material_frame, text="➕ Agregar Material", command=self.agregar_item_presupuesto, style='Success.TButton').grid(row=0, column=4, padx=(10, 0), pady=8)
        
        # Frame para agregar tareas manuales
        tarea_frame = ttk.LabelFrame(scrollable_frame, text="Agregar Tarea Manual", padding=10)
        tarea_frame.pack(fill='x', pady=(0, 10))
        
        ttk.Label(tarea_frame, text="📝 Descripción:", style='TLabel').grid(row=0, column=0, sticky='w', padx=(0, 10), pady=8)
        self.tarea_descripcion_entry = ttk.Entry(tarea_frame, width=40, style='TEntry')
        self.tarea_descripcion_entry.grid(row=0, column=1, padx=(0, 20), pady=8)
        
        ttk.Label(tarea_frame, text="🔢 Cantidad:", style='TLabel').grid(row=0, column=2, sticky='w', padx=(0, 10), pady=8)
        self.tarea_cantidad_entry = ttk.Entry(tarea_frame, width=10, style='TEntry')
        self.tarea_cantidad_entry.grid(row=0, column=3, padx=(0, 20), pady=8)
        
        ttk.Label(tarea_frame, text="💰 Precio Unit.:", style='TLabel').grid(row=1, column=0, sticky='w', padx=(0, 10), pady=8)
        self.tarea_precio_entry = ttk.Entry(tarea_frame, width=15, style='TEntry')
        self.tarea_precio_entry.grid(row=1, column=1, padx=(0, 20), pady=8)
        
        ttk.Button(tarea_frame, text="➕ Agregar Tarea", command=self.agregar_tarea_manual, style='Success.TButton').grid(row=1, column=2, padx=(10, 0), pady=8)
        
        # Frame para items del presupuesto
        items_frame = ttk.LabelFrame(scrollable_frame, text="Items del Presupuesto", padding=10)
        items_frame.pack(fill='both', expand=True, pady=(0, 10))
        
        # Frame principal horizontal para items y botones
        items_main_frame = ttk.Frame(items_frame)
        items_main_frame.pack(fill='both', expand=True)
        
        # Frame para la lista de items (lado izquierdo)
        items_list_frame = ttk.Frame(items_main_frame)
        items_list_frame.pack(side='left', fill='both', expand=True, padx=(0, 10))
        
        # Treeview para items
        columns = ('Visible', 'IVA', 'Tipo', 'Descripción', 'Cantidad', 'Precio Unit.', 'Desc. %', 'Desc. €', 'Subtotal')
        self.items_tree = ttk.Treeview(items_list_frame, columns=columns, show='headings', height=10)
        
        for col in columns:
            self.items_tree.heading(col, text=col)
            if col == 'Visible' or col == 'IVA':
                self.items_tree.column(col, width=60)
            elif col == 'Tipo':
                self.items_tree.column(col, width=80)
            elif col == 'Desc. %' or col == 'Desc. €':
                self.items_tree.column(col, width=70)
            else:
                self.items_tree.column(col, width=120)
        
        # Scrollbar para items
        items_scrollbar = ttk.Scrollbar(items_list_frame, orient='vertical', command=self.items_tree.yview)
        self.items_tree.configure(yscrollcommand=items_scrollbar.set)
        
        self.items_tree.pack(side='left', fill='both', expand=True)
        items_scrollbar.pack(side='right', fill='y')
        
        # Bind para eliminar item y toggle visibilidad
        self.items_tree.bind('<Double-1>', self.on_item_double_click)
        self.items_tree.bind('<Button-1>', self.on_item_click)
        
        # Frame para botones de gestión (lado derecho)
        buttons_frame = ttk.LabelFrame(items_main_frame, text="Gestión", padding=10)
        buttons_frame.pack(side='right', fill='y', padx=(10, 0))
        buttons_frame.configure(width=200)
        
        # Botones de gestión de items
        ttk.Button(buttons_frame, text="✏️ Editar Item", command=self.editar_item_presupuesto, style='Accent.TButton').pack(fill='x', pady=(0, 10))
        ttk.Button(buttons_frame, text="🗑️ Eliminar Item", command=self.eliminar_item_presupuesto, style='Danger.TButton').pack(fill='x', pady=(0, 10))
        ttk.Button(buttons_frame, text="✅ Marcar Todos", command=self.marcar_todos_items).pack(fill='x', pady=(0, 10))
        ttk.Button(buttons_frame, text="❌ Desmarcar Todos", command=self.desmarcar_todos_items).pack(fill='x')
        
        # Frame para totales (abajo)
        totales_frame = ttk.LabelFrame(scrollable_frame, text="Totales", padding=10)
        totales_frame.pack(fill='x', pady=(0, 10))
        
        # Frame para controles de descuentos globales
        descuentos_frame = ttk.Frame(totales_frame)
        descuentos_frame.pack(fill='x', pady=(0, 10))
        
        # Descuentos globales
        ttk.Label(descuentos_frame, text="Descuento Global:", font=('Arial', 9, 'bold')).pack(side='left', padx=(0, 10))
        
        ttk.Label(descuentos_frame, text="%:").pack(side='left', padx=(0, 5))
        self.descuento_porcentaje_var = tk.StringVar(value="0")
        self.descuento_porcentaje_entry = ttk.Entry(descuentos_frame, textvariable=self.descuento_porcentaje_var, width=8)
        self.descuento_porcentaje_entry.pack(side='left', padx=(0, 10))
        self.descuento_porcentaje_entry.bind('<KeyRelease>', self.calcular_totales)
        
        ttk.Label(descuentos_frame, text="€:").pack(side='left', padx=(0, 5))
        self.descuento_fijo_var = tk.StringVar(value="0")
        self.descuento_fijo_entry = ttk.Entry(descuentos_frame, textvariable=self.descuento_fijo_var, width=10)
        self.descuento_fijo_entry.pack(side='left', padx=(0, 10))
        self.descuento_fijo_entry.bind('<KeyRelease>', self.calcular_totales)
        
        # Radio buttons para aplicar descuento antes o después de IVA
        self.descuento_antes_iva_var = tk.BooleanVar(value=True)
        ttk.Radiobutton(descuentos_frame, text="Antes de IVA", variable=self.descuento_antes_iva_var, 
                       value=True, command=self.calcular_totales).pack(side='left', padx=(10, 5))
        ttk.Radiobutton(descuentos_frame, text="Después de IVA", variable=self.descuento_antes_iva_var, 
                       value=False, command=self.calcular_totales).pack(side='left', padx=(5, 0))
        
        # Todos los elementos en una sola fila horizontal
        totales_linea_frame = ttk.Frame(totales_frame)
        totales_linea_frame.pack(fill='x')
        
        # Información de items
        self.items_info_label = ttk.Label(totales_linea_frame, text="Items: 0", font=('Arial', 9), foreground='gray')
        self.items_info_label.pack(side='left', padx=(0, 20))
        
        # Checkbox para activar/desactivar IVA
        self.iva_habilitado_var = tk.BooleanVar(value=True)
        self.iva_checkbox = ttk.Checkbutton(totales_linea_frame, text="Incluir IVA (21%)", 
                                          variable=self.iva_habilitado_var,
                                          command=self.calcular_totales)
        self.iva_checkbox.pack(side='left', padx=(0, 20))
        
        # Subtotal
        ttk.Label(totales_linea_frame, text="Subtotal:").pack(side='left', padx=(0, 5))
        self.subtotal_label = ttk.Label(totales_linea_frame, text="€0.00", font=('Arial', 10, 'bold'))
        self.subtotal_label.pack(side='left', padx=(0, 20))
        
        # Descuentos (si hay)
        self.descuento_label = ttk.Label(totales_linea_frame, text="", font=('Arial', 9), foreground='green')
        self.descuento_label.pack(side='left', padx=(0, 20))
        
        # IVA
        ttk.Label(totales_linea_frame, text="IVA (21%):").pack(side='left', padx=(0, 5))
        self.iva_label = ttk.Label(totales_linea_frame, text="€0.00", font=('Arial', 10, 'bold'))
        self.iva_label.pack(side='left', padx=(0, 20))
        
        # TOTAL (destacado)
        ttk.Label(totales_linea_frame, text="TOTAL:", font=('Arial', 12, 'bold')).pack(side='left', padx=(0, 5))
        self.total_label = ttk.Label(totales_linea_frame, text="€0.00", font=('Arial', 16, 'bold'), foreground='blue')
        self.total_label.pack(side='left', padx=(0, 20))
        
        # Botones principales
        button_frame = ttk.Frame(scrollable_frame)
        button_frame.pack(fill='x', pady=(10, 0))
        
        # Botón principal de guardar (más prominente)
        guardar_btn = ttk.Button(button_frame, text="💾 GUARDAR PRESUPUESTO", command=self.guardar_presupuesto, style='Accent.TButton')
        guardar_btn.pack(side='left', padx=(0, 15))
        
        # Botones secundarios
        ttk.Button(button_frame, text="🧹 Limpiar Todo", command=self.limpiar_presupuesto, style='Warning.TButton').pack(side='left', padx=(0, 10))
        ttk.Button(button_frame, text="👁️ Vista Previa PDF", command=self.vista_previa_pdf).pack(side='left', padx=(0, 10))
        ttk.Button(button_frame, text="🎨 Editar Plantilla PDF", command=self.editar_plantilla_pdf, style='Small.TButton').pack(side='left', padx=(0, 10))
        ttk.Button(button_frame, text="🧮 Calcular Totales", command=self.calcular_totales).pack(side='left')
        
        # Lista para almacenar items del presupuesto
        self.presupuesto_items = []
        self.materiales_data = {}  # Para almacenar datos de materiales
        
        # Carpeta para guardar PDFs (cargar desde configuración)
        self.config_file = "config/config.json"
        self.plantilla_config_file = "config/plantilla_config.json"
        config = self.cargar_configuracion()
        self.carpeta_pdfs = config.get('carpeta_pdfs', os.path.join(os.getcwd(), 'output', 'presupuestos'))
        self.carpeta_facturas = config.get('carpeta_facturas', os.path.join(os.getcwd(), 'output', 'facturas'))
        self.plantilla_config = self.cargar_configuracion_plantilla()
        self.pdf_generator = PDFGenerator(self.plantilla_config)
        self.actualizar_label_carpeta_pdfs()
    
    def create_ver_presupuestos_tab(self):
        """Crea la pestaña para ver presupuestos existentes"""
        self.ver_presupuestos_frame = ttk.Frame(self.notebook)
        self.notebook.add(self.ver_presupuestos_frame, text="Ver Presupuestos")
        
        # Canvas y scrollbar para permitir desplazamiento
        canvas = tk.Canvas(self.ver_presupuestos_frame, bg='#ecf0f1', highlightthickness=0)
        scrollbar = ttk.Scrollbar(self.ver_presupuestos_frame, orient='vertical', command=canvas.yview)
        scrollable_frame = ttk.Frame(canvas)

        scrollable_frame.bind(
            "<Configure>",
            lambda e: canvas.configure(scrollregion=canvas.bbox("all"))
        )

        canvas_window = canvas.create_window((0, 0), window=scrollable_frame, anchor='nw')
        canvas.configure(yscrollcommand=scrollbar.set)

        def _configure_canvas_width(event):
            canvas.itemconfig(canvas_window, width=event.width)

        canvas.bind('<Configure>', _configure_canvas_width)

        canvas.pack(side='left', fill='both', expand=True)
        scrollbar.pack(side='right', fill='y')

        # Frame principal dentro del área desplazable
        main_frame = ttk.Frame(scrollable_frame)
        main_frame.pack(fill='both', expand=True, padx=10, pady=10)
        
        # Frame para búsqueda y filtros
        search_filter_frame = ttk.LabelFrame(main_frame, text="Búsqueda y Filtros", padding=10)
        search_filter_frame.pack(fill='x', pady=(0, 10))
        
        # Primera fila: Búsqueda
        search_frame = ttk.Frame(search_filter_frame)
        search_frame.pack(fill='x', pady=(0, 10))
        
        ttk.Label(search_frame, text="🔍 Buscar:", style='TLabel').pack(side='left', padx=(0, 10))
        self.presupuesto_busqueda_entry = ttk.Entry(search_frame, width=40, style='TEntry')
        self.presupuesto_busqueda_entry.pack(side='left', padx=(0, 10))
        self.presupuesto_busqueda_entry.bind('<KeyRelease>', self.buscar_presupuestos)
        
        ttk.Button(search_frame, text="🔍 Buscar", command=self.buscar_presupuestos).pack(side='left', padx=(0, 20))
        ttk.Button(search_frame, text="🧹 Limpiar", command=self.limpiar_busqueda_presupuestos).pack(side='left')
        
        # Segunda fila: Filtros de estado
        filter_frame = ttk.Frame(search_filter_frame)
        filter_frame.pack(fill='x')
        
        ttk.Label(filter_frame, text="📊 Filtrar por estado:", style='TLabel').pack(side='left', padx=(0, 10))
        
        self.estado_filtro_var = tk.StringVar(value="Todos")
        self.estado_combo = ttk.Combobox(filter_frame, textvariable=self.estado_filtro_var, 
                                       values=["Todos", "Pendiente", "Aprobado", "Rechazado"], 
                                       state="readonly", width=15, style='TCombobox')
        self.estado_combo.pack(side='left', padx=(0, 10))
        self.estado_combo.bind('<<ComboboxSelected>>', self.filtrar_por_estado)
        
        # Botones de cambio de estado
        ttk.Button(filter_frame, text="✅ Marcar como Aprobado", command=self.marcar_aprobado, style='Success.TButton').pack(side='left', padx=(10, 5))
        ttk.Button(filter_frame, text="⏳ Marcar como Pendiente", command=self.marcar_pendiente, style='Warning.TButton').pack(side='left', padx=(5, 5))
        ttk.Button(filter_frame, text="❌ Marcar como Rechazado", command=self.marcar_rechazado, style='Danger.TButton').pack(side='left', padx=(5, 0))

        # Tercera fila: Filtros por fecha
        date_filter_frame = ttk.Frame(search_filter_frame)
        date_filter_frame.pack(fill='x', pady=(10, 0))

        ttk.Label(date_filter_frame, text="🗓️ Mes:", style='TLabel').pack(side='left', padx=(0, 10))
        self.presupuesto_mes_filtro_var = tk.StringVar(value="Todos")
        self.presupuesto_mes_combo = ttk.Combobox(
            date_filter_frame,
            textvariable=self.presupuesto_mes_filtro_var,
            values=["Todos"],
            state="readonly",
            width=20,
            style='TCombobox'
        )
        self.presupuesto_mes_combo.pack(side='left', padx=(0, 10))
        self.presupuesto_mes_combo.bind('<<ComboboxSelected>>', self.buscar_presupuestos)

        ttk.Label(date_filter_frame, text="Año:", style='TLabel').pack(side='left', padx=(0, 10))
        self.presupuesto_anio_filtro_var = tk.StringVar(value="Todos")
        self.presupuesto_anio_combo = ttk.Combobox(
            date_filter_frame,
            textvariable=self.presupuesto_anio_filtro_var,
            values=["Todos"],
            state="readonly",
            width=10,
            style='TCombobox'
        )
        self.presupuesto_anio_combo.pack(side='left', padx=(0, 10))
        self.presupuesto_anio_combo.bind('<<ComboboxSelected>>', self.on_presupuesto_anio_cambiado)

        # Inicializar valores de filtros de fecha
        self.cargar_filtros_fecha_presupuestos()
        
        # Treeview para presupuestos
        tree_container = ttk.Frame(main_frame)
        tree_container.pack(fill='both', expand=True)

        columns = ('ID', 'Cliente', 'Fecha', 'Subtotal', 'IVA', 'Total', 'Estado')
        self.presupuestos_tree = ttk.Treeview(tree_container, columns=columns, show='headings', height=18)
        
        for col in columns:
            self.presupuestos_tree.heading(col, text=col)
            if col == 'Estado':
                self.presupuestos_tree.column(col, width=100)
            else:
                self.presupuestos_tree.column(col, width=130)
        
        # Scrollbar
        presupuestos_scrollbar = ttk.Scrollbar(tree_container, orient='vertical', command=self.presupuestos_tree.yview)
        self.presupuestos_tree.configure(yscrollcommand=presupuestos_scrollbar.set)
        
        self.presupuestos_tree.pack(side='left', fill='both', expand=True)
        presupuestos_scrollbar.pack(side='right', fill='y')
        
        # Bind para ver detalles
        self.presupuestos_tree.bind('<Double-1>', self.ver_detalle_presupuesto)
        
        # Botones
        button_frame = ttk.Frame(main_frame)
        button_frame.pack(fill='x', pady=(0, 10))
        
        # Primera fila de botones
        button_row1 = ttk.Frame(button_frame)
        button_row1.pack(fill='x', pady=(0, 5))
        
        ttk.Button(button_row1, text="🔄 Actualizar", command=self.refresh_presupuestos, style='Small.TButton').pack(side='left', padx=(0, 8))
        ttk.Button(button_row1, text="👁️ Ver Detalle", command=self.ver_detalle_presupuesto, style='Small.TButton').pack(side='left', padx=(0, 8))
        ttk.Button(button_row1, text="📄 Exportar PDF", command=self.exportar_presupuesto_pdf, style='SmallAccent.TButton').pack(side='left', padx=(0, 8))
        ttk.Button(button_row1, text="📧 Enviar Email", command=self.enviar_presupuesto_email, style='Small.TButton').pack(side='left', padx=(0, 8))
        ttk.Button(button_row1, text="⚙️ Configurar Email", command=self.configurar_email, style='Small.TButton').pack(side='left', padx=(0, 8))
        ttk.Button(button_row1, text="🗑️ Eliminar", command=self.eliminar_presupuesto, style='SmallDanger.TButton').pack(side='left')
        
        # Segunda fila de botones para gestión de PDFs
        button_row2 = ttk.Frame(button_frame)
        button_row2.pack(fill='x')
        
        ttk.Button(button_row2, text="📁 Elegir Carpeta", command=self.elegir_carpeta_pdfs, style='Small.TButton').pack(side='left', padx=(0, 8))
        ttk.Button(button_row2, text="📂 Abrir Carpeta", command=self.abrir_carpeta_pdfs, style='Small.TButton').pack(side='left', padx=(0, 8))
        
        # Label para mostrar la carpeta actual
        self.carpeta_pdfs_label = ttk.Label(button_row2, text="Carpeta PDFs: No configurada", 
                                          font=('Arial', 8), foreground='gray')
        self.carpeta_pdfs_label.pack(side='left', padx=(15, 0))

        # Configurar desplazamiento con rueda del mouse en toda la vista
        def _on_mousewheel(event):
            canvas.yview_scroll(int(-1 * (event.delta / 120)), "units")

        def _bind_to_mousewheel(event=None):
            canvas.bind_all("<MouseWheel>", _on_mousewheel)

        def _unbind_from_mousewheel(event=None):
            canvas.unbind_all("<MouseWheel>")

        def _bind_scroll_events(widget):
            widget.bind('<Enter>', _bind_to_mousewheel)
            widget.bind('<Leave>', _unbind_from_mousewheel)
            for child in widget.winfo_children():
                _bind_scroll_events(child)

        _bind_scroll_events(scrollable_frame)
    
    # Métodos para gestión de clientes
    def agregar_cliente(self):
        nombre = self.nombre_entry.get().strip()
        if not nombre:
            messagebox.showerror("Error", "El nombre es obligatorio")
            return
        
        try:
            cliente_manager.crear_cliente(
                nombre,
                self.telefono_entry.get().strip(),
                self.email_entry.get().strip(),
                self.direccion_entry.get().strip(),
                self.dni_entry.get().strip()
            )
            messagebox.showinfo("Éxito", "Cliente agregado correctamente")
            self.limpiar_formulario_cliente()
            self.refresh_clientes()
        except Exception as e:
            messagebox.showerror("Error", f"Error al agregar cliente: {str(e)}")
    
    def actualizar_cliente(self):
        selection = self.clientes_tree.selection()
        if not selection:
            messagebox.showwarning("Advertencia", "Seleccione un cliente para actualizar")
            return
        
        cliente_id = self.clientes_tree.item(selection[0])['values'][0]
        nombre = self.nombre_entry.get().strip()
        
        if not nombre:
            messagebox.showerror("Error", "El nombre es obligatorio")
            return
        
        try:
            if cliente_manager.actualizar_cliente(
                cliente_id,
                nombre,
                self.telefono_entry.get().strip(),
                self.email_entry.get().strip(),
                self.direccion_entry.get().strip(),
                self.dni_entry.get().strip()
            ):
                messagebox.showinfo("Éxito", "Cliente actualizado correctamente")
                self.limpiar_formulario_cliente()
                self.refresh_clientes()
            else:
                messagebox.showerror("Error", "Error al actualizar cliente")
        except Exception as e:
            messagebox.showerror("Error", f"Error al actualizar cliente: {str(e)}")
    
    def eliminar_cliente(self):
        selection = self.clientes_tree.selection()
        if not selection:
            messagebox.showwarning("Advertencia", "Seleccione un cliente para eliminar")
            return
        
        if messagebox.askyesno("Confirmar", "¿Está seguro de que desea eliminar este cliente?"):
            cliente_id = self.clientes_tree.item(selection[0])['values'][0]
            try:
                if cliente_manager.eliminar_cliente(cliente_id):
                    messagebox.showinfo("Éxito", "Cliente eliminado correctamente")
                    self.limpiar_formulario_cliente()
                    self.refresh_clientes()
                else:
                    messagebox.showerror("Error", "Error al eliminar cliente")
            except Exception as e:
                messagebox.showerror("Error", f"Error al eliminar cliente: {str(e)}")
    
    def limpiar_formulario_cliente(self):
        self.nombre_entry.delete(0, tk.END)
        self.telefono_entry.delete(0, tk.END)
        self.email_entry.delete(0, tk.END)
        self.direccion_entry.delete(0, tk.END)
        self.dni_entry.delete(0, tk.END)
    
    def on_cliente_select(self, event):
        selection = self.clientes_tree.selection()
        if selection:
            item = self.clientes_tree.item(selection[0])
            values = item['values']
            self.nombre_entry.delete(0, tk.END)
            self.nombre_entry.insert(0, values[1])
            self.telefono_entry.delete(0, tk.END)
            self.telefono_entry.insert(0, values[2])
            self.email_entry.delete(0, tk.END)
            self.email_entry.insert(0, values[3])
            self.direccion_entry.delete(0, tk.END)
            self.direccion_entry.insert(0, values[4])
            self.dni_entry.delete(0, tk.END)
            self.dni_entry.insert(0, values[5] if len(values) > 5 else '')
    
    def buscar_clientes(self, event=None):
        termino = self.busqueda_entry.get().strip()
        if termino:
            clientes = cliente_manager.buscar_clientes(termino)
        else:
            clientes = cliente_manager.obtener_clientes()
        
        self.actualizar_tree_clientes(clientes)
    
    def refresh_clientes(self):
        clientes = cliente_manager.obtener_clientes()
        self.actualizar_tree_clientes(clientes)
        self.actualizar_combo_clientes()
    
    def actualizar_tree_clientes(self, clientes):
        for item in self.clientes_tree.get_children():
            self.clientes_tree.delete(item)
        
        for cliente in clientes:
            self.clientes_tree.insert('', 'end', values=(
                cliente['id'],
                cliente['nombre'],
                cliente['telefono'] or '',
                cliente['email'] or '',
                cliente['direccion'] or '',
                cliente['dni'] or ''
            ))
    
    def actualizar_combo_clientes(self):
        clientes = cliente_manager.obtener_clientes()
        cliente_names = [f"{c['id']} - {c['nombre']}" for c in clientes]
        self.cliente_combo['values'] = cliente_names
    
    # Métodos para gestión de materiales
    def agregar_material(self):
        nombre = self.material_nombre_entry.get().strip()
        unidad = self.unidad_entry.get().strip()
        precio_str = self.precio_entry.get().strip()
        
        if not nombre or not unidad or not precio_str:
            messagebox.showerror("Error", "Todos los campos son obligatorios")
            return
        
        try:
            precio = float(precio_str)
            material_manager.crear_material(nombre, unidad, precio)
            messagebox.showinfo("Éxito", "Material agregado correctamente")
            self.limpiar_formulario_material()
            self.refresh_materiales()
        except ValueError:
            messagebox.showerror("Error", "El precio debe ser un número válido")
        except Exception as e:
            messagebox.showerror("Error", f"Error al agregar material: {str(e)}")
    
    def actualizar_material(self):
        selection = self.materiales_tree.selection()
        if not selection:
            messagebox.showwarning("Advertencia", "Seleccione un material para actualizar")
            return
        
        material_id = self.materiales_tree.item(selection[0])['values'][0]
        nombre = self.material_nombre_entry.get().strip()
        unidad = self.unidad_entry.get().strip()
        precio_str = self.precio_entry.get().strip()
        
        if not nombre or not unidad or not precio_str:
            messagebox.showerror("Error", "Todos los campos son obligatorios")
            return
        
        try:
            precio = float(precio_str)
            if material_manager.actualizar_material(material_id, nombre, unidad, precio):
                messagebox.showinfo("Éxito", "Material actualizado correctamente")
                self.limpiar_formulario_material()
                self.refresh_materiales()
            else:
                messagebox.showerror("Error", "Error al actualizar material")
        except ValueError:
            messagebox.showerror("Error", "El precio debe ser un número válido")
        except Exception as e:
            messagebox.showerror("Error", f"Error al actualizar material: {str(e)}")
    
    def eliminar_material(self):
        selection = self.materiales_tree.selection()
        if not selection:
            messagebox.showwarning("Advertencia", "Seleccione un material para eliminar")
            return
        
        if messagebox.askyesno("Confirmar", "¿Está seguro de que desea eliminar este material?"):
            material_id = self.materiales_tree.item(selection[0])['values'][0]
            try:
                if material_manager.eliminar_material(material_id):
                    messagebox.showinfo("Éxito", "Material eliminado correctamente")
                    self.limpiar_formulario_material()
                    self.refresh_materiales()
                else:
                    messagebox.showerror("Error", "Error al eliminar material")
            except Exception as e:
                messagebox.showerror("Error", f"Error al eliminar material: {str(e)}")
    
    def limpiar_formulario_material(self):
        self.material_nombre_entry.delete(0, tk.END)
        self.unidad_entry.delete(0, tk.END)
        self.precio_entry.delete(0, tk.END)
    
    def on_material_select(self, event):
        selection = self.materiales_tree.selection()
        if selection:
            item = self.materiales_tree.item(selection[0])
            values = item['values']
            self.material_nombre_entry.delete(0, tk.END)
            self.material_nombre_entry.insert(0, values[1])
            self.unidad_entry.delete(0, tk.END)
            self.unidad_entry.insert(0, values[2])
            self.precio_entry.delete(0, tk.END)
            self.precio_entry.insert(0, values[3])
    
    def buscar_materiales(self, event=None):
        termino = self.material_busqueda_entry.get().strip()
        if termino:
            materiales = material_manager.buscar_materiales(termino)
        else:
            materiales = material_manager.obtener_materiales()
        
        self.actualizar_tree_materiales(materiales)
    
    def refresh_materiales(self):
        materiales = material_manager.obtener_materiales()
        self.actualizar_tree_materiales(materiales)
        self.actualizar_combo_materiales()
    
    def actualizar_tree_materiales(self, materiales):
        for item in self.materiales_tree.get_children():
            self.materiales_tree.delete(item)
        
        for material in materiales:
            self.materiales_tree.insert('', 'end', values=(
                material['id'],
                material['nombre'],
                material['unidad_medida'],
                f"€{material['precio_unitario']:.2f}"
            ))
    
    def actualizar_combo_materiales(self):
        materiales = material_manager.obtener_materiales()
        material_names = [f"{m['id']} - {m['nombre']} ({m['unidad_medida']})" for m in materiales]
        self.material_combo['values'] = material_names
        self.materiales_data = {m['id']: m for m in materiales}
    
    # Métodos para presupuestos
    def agregar_item_presupuesto(self):
        if not self.cliente_var.get():
            messagebox.showwarning("Advertencia", "Seleccione un cliente primero")
            return
        
        if not self.material_var.get():
            messagebox.showwarning("Advertencia", "Seleccione un material")
            return
        
        cantidad_str = self.cantidad_entry.get().strip()
        if not cantidad_str:
            messagebox.showwarning("Advertencia", "Ingrese la cantidad")
            return
        
        try:
            cantidad = float(cantidad_str)
            if cantidad <= 0:
                messagebox.showerror("Error", "La cantidad debe ser mayor a 0")
                return
            
            # Obtener ID del material seleccionado
            material_text = self.material_var.get()
            material_id = int(material_text.split(' - ')[0])
            material = self.materiales_data[material_id]
            
            # Crear item
            item = {
                'material_id': material_id,
                'tarea_manual': '',
                'material_nombre': material['nombre'],
                'unidad_medida': material['unidad_medida'],
                'cantidad': cantidad,
                'precio_unitario': material['precio_unitario'],
                'subtotal': cantidad * material['precio_unitario'],
                'visible_pdf': 1,
                'es_tarea_manual': 0,
                'aplica_iva': True,
                'descuento_porcentaje': 0,
                'descuento_fijo': 0
            }
            
            self.presupuesto_items.append(item)
            self.actualizar_tree_items()
            self.calcular_totales()
            
            # Limpiar campos
            self.material_var.set('')
            self.cantidad_entry.delete(0, tk.END)
            
        except ValueError:
            messagebox.showerror("Error", "La cantidad debe ser un número válido")
        except Exception as e:
            messagebox.showerror("Error", f"Error al agregar item: {str(e)}")
    
    def agregar_tarea_manual(self):
        if not self.cliente_var.get():
            messagebox.showwarning("Advertencia", "Seleccione un cliente primero")
            return
        
        descripcion = self.tarea_descripcion_entry.get().strip()
        cantidad_str = self.tarea_cantidad_entry.get().strip()
        precio_str = self.tarea_precio_entry.get().strip()
        
        if not descripcion or not cantidad_str or not precio_str:
            messagebox.showwarning("Advertencia", "Todos los campos son obligatorios")
            return
        
        try:
            cantidad = float(cantidad_str)
            precio = float(precio_str)
            
            if cantidad <= 0 or precio < 0:
                messagebox.showerror("Error", "La cantidad debe ser mayor a 0 y el precio no puede ser negativo")
                return
            
            # Crear tarea manual
            item = {
                'material_id': None,
                'tarea_manual': descripcion,
                'material_nombre': '',
                'unidad_medida': 'unidad',
                'cantidad': cantidad,
                'precio_unitario': precio,
                'subtotal': cantidad * precio,
                'visible_pdf': 1,
                'es_tarea_manual': 1,
                'aplica_iva': True,
                'descuento_porcentaje': 0,
                'descuento_fijo': 0
            }
            
            self.presupuesto_items.append(item)
            self.actualizar_tree_items()
            self.calcular_totales()
            
            # Limpiar campos
            self.tarea_descripcion_entry.delete(0, tk.END)
            self.tarea_cantidad_entry.delete(0, tk.END)
            self.tarea_precio_entry.delete(0, tk.END)
            
        except ValueError:
            messagebox.showerror("Error", "La cantidad y el precio deben ser números válidos")
        except Exception as e:
            messagebox.showerror("Error", f"Error al agregar tarea: {str(e)}")
    
    def eliminar_item_presupuesto(self, event=None):
        selection = self.items_tree.selection()
        if not selection:
            messagebox.showwarning("Advertencia", "Seleccione un item para eliminar")
            return
        
        if messagebox.askyesno("Confirmar", "¿Está seguro de que desea eliminar este item?"):
            index = self.items_tree.index(selection[0])
            del self.presupuesto_items[index]
            self.actualizar_tree_items()
            self.calcular_totales()
    
    def on_item_click(self, event):
        """Maneja el clic en los items para toggle de visibilidad"""
        region = self.items_tree.identify_region(event.x, event.y)
        if region == "cell":
            column = self.items_tree.identify_column(event.x)
            # Solo cambiar visibilidad si se hace click en la columna "Visible" (primera columna)
            if column == "#1":  # Columna de visibilidad
                self.toggle_visibilidad_item(event)
    
    def on_item_double_click(self, event):
        """Maneja el doble click en los items para abrir diálogo de edición"""
        region = self.items_tree.identify_region(event.x, event.y)
        if region == "cell":
            self.editar_item_presupuesto()
    
    def toggle_visibilidad_item(self, event=None):
        """Cambia la visibilidad de un item"""
        selection = self.items_tree.selection()
        if selection:
            index = self.items_tree.index(selection[0])
            item = self.presupuesto_items[index]
            item['visible_pdf'] = 0 if item.get('visible_pdf', 1) else 1
            self.actualizar_tree_items()
    
    def marcar_todos_items(self):
        """Marca todos los items como visibles"""
        for item in self.presupuesto_items:
            item['visible_pdf'] = 1
        self.actualizar_tree_items()
    
    def desmarcar_todos_items(self):
        """Desmarca todos los items como no visibles"""
        for item in self.presupuesto_items:
            item['visible_pdf'] = 0
        self.actualizar_tree_items()
    
    def actualizar_tree_items(self):
        for item in self.items_tree.get_children():
            self.items_tree.delete(item)
        
        for i, item in enumerate(self.presupuesto_items):
            # Determinar el tipo y descripción
            if item.get('es_tarea_manual', 0):
                tipo = "Tarea"
                descripcion = item.get('tarea_manual', 'Tarea manual')
            else:
                tipo = "Material"
                descripcion = f"{item['material_nombre']} ({item['unidad_medida']})"
            
            # Checkbox de visibilidad e IVA
            visible = "✓" if item.get('visible_pdf', 1) else "✗"
            aplica_iva = "✓" if item.get('aplica_iva', True) else "✗"
            
            # Calcular descuentos
            descuento_pct = item.get('descuento_porcentaje', 0)
            descuento_fijo = item.get('descuento_fijo', 0)
            
            descuento_pct_text = f"{descuento_pct:.1f}%" if descuento_pct > 0 else ""
            descuento_fijo_text = f"€{descuento_fijo:.2f}" if descuento_fijo > 0 else ""
            
            self.items_tree.insert('', 'end', values=(
                visible,
                aplica_iva,
                tipo,
                descripcion,
                f"{item['cantidad']:.2f}",
                f"€{item['precio_unitario']:.2f}",
                descuento_pct_text,
                descuento_fijo_text,
                f"€{item['subtotal']:.2f}"
            ))
        
        # Actualizar contador de items
        total_items = len(self.presupuesto_items)
        visible_items = sum(1 for item in self.presupuesto_items if item.get('visible_pdf', 1))
        self.items_info_label.config(text=f"Items: {total_items} (Visibles: {visible_items})")
    
    def calcular_totales(self):
        if not self.presupuesto_items:
            self.subtotal_label.config(text="€0.00")
            self.iva_label.config(text="€0.00")
            self.total_label.config(text="€0.00")
            self.descuento_label.config(text="")
            self.items_info_label.config(text="Items: 0")
            return
        
        # Obtener descuentos globales
        try:
            descuento_porcentaje = float(self.descuento_porcentaje_var.get() or 0)
        except ValueError:
            descuento_porcentaje = 0
        
        try:
            descuento_fijo = float(self.descuento_fijo_var.get() or 0)
        except ValueError:
            descuento_fijo = 0
        
        # Verificar si hay items con IVA habilitado
        items_con_iva = [item for item in self.presupuesto_items if item.get('aplica_iva', True)]
        iva_realmente_habilitado = len(items_con_iva) > 0 and self.iva_habilitado_var.get()
        
        # Calcular totales usando el nuevo método del backend
        totales = presupuesto_manager.calcular_totales_completo(
            self.presupuesto_items,
            descuento_porcentaje,
            descuento_fijo,
            self.descuento_antes_iva_var.get(),
            self.iva_habilitado_var.get()
        )
        
        # Actualizar labels
        self.subtotal_label.config(text=f"€{totales['subtotal']:.2f}")
        self.iva_label.config(text=f"€{totales['iva']:.2f}")
        self.total_label.config(text=f"€{totales['total']:.2f}")
        self.items_info_label.config(text=f"Items: {len(self.presupuesto_items)}")
        
        # Mostrar descuentos si los hay
        descuento_texto = ""
        if totales['descuentos_items'] > 0:
            descuento_texto += f"Desc. Items: -€{totales['descuentos_items']:.2f}"
        if totales['descuento_global'] > 0:
            if descuento_texto:
                descuento_texto += " | "
            descuento_texto += f"Desc. Global: -€{totales['descuento_global']:.2f}"
        
        self.descuento_label.config(text=descuento_texto)
        
        # Cambiar color del IVA según si está habilitado o no
        if iva_realmente_habilitado:
            self.iva_label.config(foreground='black')
            self.iva_checkbox.config(state='normal')
        else:
            self.iva_label.config(foreground='gray')
            # Si ningún item tiene IVA, desactivar el checkbox
            if len(items_con_iva) == 0 and self.presupuesto_items:
                self.iva_checkbox.config(state='disabled')
            else:
                self.iva_checkbox.config(state='normal')
    
    def editar_item_presupuesto(self):
        """Abre diálogo para editar un item del presupuesto"""
        selection = self.items_tree.selection()
        if not selection:
            messagebox.showwarning("Advertencia", "Seleccione un item para editar")
            return
        
        item = self.items_tree.item(selection[0])
        item_index = self.items_tree.index(selection[0])
        item_data = self.presupuesto_items[item_index]
        
        # Crear ventana de diálogo
        dialog = tk.Toplevel(self.root)
        dialog.title("Editar Item")
        dialog.geometry("500x400")
        dialog.resizable(False, False)
        dialog.transient(self.root)
        dialog.grab_set()
        
        # Centrar la ventana
        dialog.update_idletasks()
        x = (dialog.winfo_screenwidth() // 2) - (500 // 2)
        y = (dialog.winfo_screenheight() // 2) - (400 // 2)
        dialog.geometry(f"500x400+{x}+{y}")
        
        # Variables
        aplica_iva_var = tk.BooleanVar(value=item_data.get('aplica_iva', True))
        descuento_porcentaje_var = tk.StringVar(value=str(item_data.get('descuento_porcentaje', 0)))
        descuento_fijo_var = tk.StringVar(value=str(item_data.get('descuento_fijo', 0)))
        
        # Frame principal
        main_frame = ttk.Frame(dialog, padding=20)
        main_frame.pack(fill='both', expand=True)
        
        # Información del item
        ttk.Label(main_frame, text=f"Item: {item_data.get('tarea_manual', item_data.get('material_nombre', 'Sin nombre'))}", 
                 font=('Arial', 10, 'bold')).pack(pady=(0, 20))
        
        # IVA
        iva_frame = ttk.Frame(main_frame)
        iva_frame.pack(fill='x', pady=(0, 15))
        ttk.Checkbutton(iva_frame, text="Aplicar IVA (21%)", variable=aplica_iva_var).pack(anchor='w')
        
        # Descuentos
        descuentos_frame = ttk.LabelFrame(main_frame, text="Descuentos", padding=10)
        descuentos_frame.pack(fill='x', pady=(0, 15))
        
        ttk.Label(descuentos_frame, text="Descuento por porcentaje:").grid(row=0, column=0, sticky='w', pady=(0, 5))
        descuento_pct_entry = ttk.Entry(descuentos_frame, textvariable=descuento_porcentaje_var, width=15)
        descuento_pct_entry.grid(row=0, column=1, sticky='w', padx=(10, 0), pady=(0, 5))
        ttk.Label(descuentos_frame, text="%").grid(row=0, column=2, sticky='w', padx=(5, 0), pady=(0, 5))
        
        ttk.Label(descuentos_frame, text="Descuento fijo:").grid(row=1, column=0, sticky='w')
        descuento_fijo_entry = ttk.Entry(descuentos_frame, textvariable=descuento_fijo_var, width=15)
        descuento_fijo_entry.grid(row=1, column=1, sticky='w', padx=(10, 0))
        ttk.Label(descuentos_frame, text="€").grid(row=1, column=2, sticky='w', padx=(5, 0))
        
        ttk.Label(descuentos_frame, text="Nota: El porcentaje tiene prioridad sobre el descuento fijo", 
                 font=('Arial', 8), foreground='gray').grid(row=2, column=0, columnspan=3, sticky='w', pady=(10, 0))
        
        # Botones
        button_frame = ttk.Frame(main_frame)
        button_frame.pack(fill='x', pady=(20, 0))
        
        def guardar_cambios():
            try:
                # Validar descuentos
                descuento_pct = float(descuento_porcentaje_var.get() or 0)
                descuento_fijo = float(descuento_fijo_var.get() or 0)
                
                if descuento_pct < 0 or descuento_pct > 100:
                    messagebox.showerror("Error", "El descuento por porcentaje debe estar entre 0 y 100")
                    return
                
                if descuento_fijo < 0:
                    messagebox.showerror("Error", "El descuento fijo no puede ser negativo")
                    return
                
                # Actualizar item
                self.presupuesto_items[item_index]['aplica_iva'] = aplica_iva_var.get()
                self.presupuesto_items[item_index]['descuento_porcentaje'] = descuento_pct
                self.presupuesto_items[item_index]['descuento_fijo'] = descuento_fijo
                
                # Actualizar visualización
                self.actualizar_tree_items()
                self.calcular_totales()
                
                dialog.destroy()
                
            except ValueError:
                messagebox.showerror("Error", "Por favor ingrese valores numéricos válidos")
        
        ttk.Button(button_frame, text="Guardar", command=guardar_cambios, style='Accent.TButton').pack(side='right', padx=(5, 0))
        ttk.Button(button_frame, text="Cancelar", command=dialog.destroy).pack(side='right')
    
    def guardar_presupuesto(self):
        if not self.cliente_var.get():
            messagebox.showwarning("Advertencia", "Seleccione un cliente")
            return
        
        if not self.presupuesto_items:
            messagebox.showwarning("Advertencia", "Agregue al menos un material al presupuesto")
            return
        
        try:
            # Obtener ID del cliente
            cliente_text = self.cliente_var.get()
            cliente_id = int(cliente_text.split(' - ')[0])
            
            # Obtener estado del IVA
            iva_habilitado = self.iva_habilitado_var.get()
            
            # Obtener descuentos globales
            try:
                descuento_porcentaje = float(self.descuento_porcentaje_var.get() or 0)
            except ValueError:
                descuento_porcentaje = 0
            
            try:
                descuento_fijo = float(self.descuento_fijo_var.get() or 0)
            except ValueError:
                descuento_fijo = 0
            
            # Crear presupuesto
            presupuesto_id = presupuesto_manager.crear_presupuesto(
                cliente_id, self.presupuesto_items, iva_habilitado,
                descuento_porcentaje, descuento_fijo, self.descuento_antes_iva_var.get()
            )
            
            messagebox.showinfo("Éxito", f"Presupuesto guardado correctamente. ID: {presupuesto_id}")
            
            # Preguntar si desea generar PDF
            if messagebox.askyesno("Generar PDF", "¿Desea generar un archivo PDF del presupuesto?"):
                self.generar_pdf_con_nombre_personalizado(presupuesto_id)
            
            self.limpiar_presupuesto()
            self.refresh_presupuestos()
            
        except Exception as e:
            messagebox.showerror("Error", f"Error al guardar presupuesto: {str(e)}")
    
    def generar_pdf_con_nombre_personalizado(self, presupuesto_id):
        """Genera un PDF con nombre personalizado para el presupuesto"""
        try:
            # Obtener datos del presupuesto
            presupuesto = presupuesto_manager.obtener_presupuesto_por_id(presupuesto_id)
            
            if not presupuesto:
                messagebox.showerror("Error", "No se pudo obtener el presupuesto")
                return
            
            # Crear ventana de diálogo para el nombre del archivo
            nombre_dialog = tk.Toplevel(self.root)
            nombre_dialog.title("Nombre del archivo PDF")
            nombre_dialog.geometry("600x250")
            nombre_dialog.resizable(False, False)
            nombre_dialog.transient(self.root)
            nombre_dialog.grab_set()
            
            # Centrar la ventana
            nombre_dialog.geometry("+%d+%d" % (
                self.root.winfo_rootx() + 50,
                self.root.winfo_rooty() + 50
            ))
            
            # Frame principal
            main_frame = ttk.Frame(nombre_dialog, padding=20)
            main_frame.pack(fill='both', expand=True)
            
            # Información del presupuesto
            info_label = ttk.Label(main_frame, 
                                 text=f"Presupuesto #{presupuesto_id} - {presupuesto['cliente_nombre']}", 
                                 font=('Arial', 10, 'bold'))
            info_label.pack(pady=(0, 15))
            
            # Label y entry para el nombre del archivo
            ttk.Label(main_frame, text="Nombre del archivo PDF:", font=('Arial', 9)).pack(anchor='w', pady=(0, 5))
            
            # Generar nombre sugerido
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            nombre_sugerido = f"presupuesto_{presupuesto_id}_{presupuesto['cliente_nombre'].replace(' ', '_')}_{timestamp}"
            
            nombre_var = tk.StringVar(value=nombre_sugerido)
            nombre_entry = ttk.Entry(main_frame, textvariable=nombre_var, width=50, font=('Arial', 9))
            nombre_entry.pack(fill='x', pady=(0, 10))
            nombre_entry.select_range(0, tk.END)
            nombre_entry.focus()
            
            # Información adicional
            info_text = ttk.Label(main_frame, 
                                text="• El archivo se guardará en la carpeta configurada\n• No incluya la extensión .pdf (se agregará automáticamente)", 
                                font=('Arial', 8), 
                                foreground='gray')
            info_text.pack(anchor='w', pady=(0, 15))
            
            # Variables para el resultado
            resultado = {'nombre': None, 'cancelado': False}
            
            def confirmar():
                nombre = nombre_var.get().strip()
                if not nombre:
                    messagebox.showwarning("Advertencia", "Ingrese un nombre para el archivo")
                    return
                
                # Limpiar el nombre de caracteres no válidos
                import re
                nombre_limpio = re.sub(r'[<>:"/\\|?*]', '_', nombre)
                if nombre_limpio != nombre:
                    if messagebox.askyesno("Confirmar", f"El nombre contiene caracteres no válidos.\n¿Usar '{nombre_limpio}' en su lugar?"):
                        nombre = nombre_limpio
                    else:
                        return
                
                resultado['nombre'] = nombre
                nombre_dialog.destroy()
            
            def cancelar():
                resultado['cancelado'] = True
                nombre_dialog.destroy()
            
            # Botones
            button_frame = ttk.Frame(main_frame)
            button_frame.pack(fill='x')
            
            ttk.Button(button_frame, text="Generar PDF", command=confirmar, style='Accent.TButton').pack(side='right', padx=(10, 0))
            ttk.Button(button_frame, text="Cancelar", command=cancelar).pack(side='right')
            
            # Bind Enter para confirmar
            nombre_entry.bind('<Return>', lambda e: confirmar())
            
            # Esperar a que se cierre el diálogo
            nombre_dialog.wait_window()
            
            # Verificar si se canceló
            if resultado['cancelado']:
                return
            
            # Generar el PDF
            nombre_archivo = f"{resultado['nombre']}.pdf"
            ruta_completa = os.path.join(self.carpeta_pdfs, nombre_archivo)
            
            # Verificar si el archivo ya existe
            if os.path.exists(ruta_completa):
                if not messagebox.askyesno("Archivo existente", f"El archivo '{nombre_archivo}' ya existe.\n¿Desea sobrescribirlo?"):
                    return
            
            # Generar PDF
            archivo_pdf = self.pdf_generator.generate_presupuesto_pdf(presupuesto, ruta_completa)
            
            # Mostrar mensaje de éxito
            messagebox.showinfo("Éxito", f"PDF generado correctamente:\n{archivo_pdf}")
            
            # Preguntar si desea abrir el archivo
            if messagebox.askyesno("Abrir PDF", "¿Desea abrir el archivo PDF generado?"):
                try:
                    if platform.system() == 'Windows':
                        os.startfile(archivo_pdf)
                    elif platform.system() == 'Darwin':  # macOS
                        subprocess.run(['open', archivo_pdf])
                    else:  # Linux
                        subprocess.run(['xdg-open', archivo_pdf])
                except Exception as e:
                    messagebox.showinfo("Información", f"Archivo guardado en: {os.path.abspath(archivo_pdf)}")
            
        except Exception as e:
            messagebox.showerror("Error", f"Error al generar PDF: {str(e)}")
    
    def limpiar_presupuesto(self):
        self.cliente_var.set('')
        self.material_var.set('')
        self.cantidad_entry.delete(0, tk.END)
        self.tarea_descripcion_entry.delete(0, tk.END)
        self.tarea_cantidad_entry.delete(0, tk.END)
        self.tarea_precio_entry.delete(0, tk.END)
        self.iva_habilitado_var.set(True)  # Resetear IVA a habilitado por defecto
        self.presupuesto_items.clear()
        self.actualizar_tree_items()
        self.calcular_totales()
    
    def vista_previa_pdf(self):
        """Muestra una vista previa del PDF antes de guardar"""
        if not self.cliente_var.get():
            messagebox.showwarning("Advertencia", "Seleccione un cliente primero")
            return
        
        if not self.presupuesto_items:
            messagebox.showwarning("Advertencia", "Agregue al menos un item al presupuesto")
            return
        
        try:
            # Obtener ID del cliente
            cliente_text = self.cliente_var.get()
            cliente_id = int(cliente_text.split(' - ')[0])
            cliente = cliente_manager.obtener_cliente_por_id(cliente_id)
            
            if not cliente:
                messagebox.showerror("Error", "No se pudo obtener la información del cliente")
                return
            
            # Obtener descuentos globales
            try:
                descuento_porcentaje = float(self.descuento_porcentaje_var.get() or 0)
            except ValueError:
                descuento_porcentaje = 0
            
            try:
                descuento_fijo = float(self.descuento_fijo_var.get() or 0)
            except ValueError:
                descuento_fijo = 0
            
            # Calcular totales usando el mismo método que el resto de la aplicación
            totales = presupuesto_manager.calcular_totales_completo(
                self.presupuesto_items,
                descuento_porcentaje,
                descuento_fijo,
                self.descuento_antes_iva_var.get(),
                self.iva_habilitado_var.get()
            )
            
            # Crear un presupuesto temporal para la vista previa
            presupuesto_temp = {
                'id': 'VISTA_PREVIA',
                'cliente_nombre': cliente['nombre'],
                'telefono': cliente['telefono'],
                'email': cliente['email'],
                'direccion': cliente['direccion'],
                'fecha_creacion': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
                'subtotal': totales['subtotal'],
                'iva': totales['iva'],
                'total': totales['total'],
                'iva_habilitado': self.iva_habilitado_var.get(),
                'items': self.presupuesto_items
            }
            
            # Generar PDF temporal en la carpeta configurada
            archivo_pdf_path = os.path.join(self.carpeta_pdfs, "vista_previa_temp.pdf")
            archivo_pdf = self.pdf_generator.generate_presupuesto_pdf(presupuesto_temp, archivo_pdf_path)
            
            # Mostrar mensaje y preguntar si desea abrir
            if messagebox.askyesno("Vista Previa", f"PDF generado exitosamente.\n¿Desea abrir el archivo?"):
                import subprocess
                import platform
                
                try:
                    if platform.system() == 'Windows':
                        os.startfile(archivo_pdf)
                    elif platform.system() == 'Darwin':  # macOS
                        subprocess.run(['open', archivo_pdf])
                    else:  # Linux
                        subprocess.run(['xdg-open', archivo_pdf])
                except Exception as e:
                    messagebox.showinfo("Información", f"Archivo guardado en: {os.path.abspath(archivo_pdf)}")
            
        except Exception as e:
            import traceback
            error_details = traceback.format_exc()
            messagebox.showerror("Error", f"Error al generar vista previa:\n{str(e)}\n\nDetalles:\n{error_details}")
    
    def refresh_presupuestos(self):
        if hasattr(self, 'presupuesto_mes_combo') and hasattr(self, 'presupuesto_anio_combo'):
            self.cargar_filtros_fecha_presupuestos(mantener_seleccion=True)
        self.buscar_presupuestos()
    
    def actualizar_tree_presupuestos(self, presupuestos):
        for item in self.presupuestos_tree.get_children():
            self.presupuestos_tree.delete(item)
        
        for presupuesto in presupuestos:
            fecha = presupuesto['fecha_creacion'][:10]  # Solo la fecha
            estado = presupuesto.get('estado', 'Pendiente')
            
            # Color del estado
            item = self.presupuestos_tree.insert('', 'end', values=(
                presupuesto['id'],
                presupuesto['cliente_nombre'],
                fecha,
                f"€{presupuesto['subtotal']:.2f}",
                f"€{presupuesto['iva']:.2f}",
                f"€{presupuesto['total']:.2f}",
                estado
            ))
            
            # Colorear según el estado
            if estado == 'Aprobado':
                self.presupuestos_tree.set(item, 'Estado', '✅ Aprobado')
            elif estado == 'Rechazado':
                self.presupuestos_tree.set(item, 'Estado', '❌ Rechazado')
            else:
                self.presupuestos_tree.set(item, 'Estado', '⏳ Pendiente')
    
    def ver_detalle_presupuesto(self, event=None):
        selection = self.presupuestos_tree.selection()
        if not selection:
            messagebox.showwarning("Advertencia", "Seleccione un presupuesto para ver")
            return
        
        presupuesto_id = self.presupuestos_tree.item(selection[0])['values'][0]
        presupuesto = presupuesto_manager.obtener_presupuesto_por_id(presupuesto_id)
        
        if presupuesto:
            self.mostrar_detalle_presupuesto(presupuesto)
    
    def mostrar_detalle_presupuesto(self, presupuesto):
        # Crear ventana de detalle
        detalle_window = tk.Toplevel(self.root)
        detalle_window.title(f"Detalle Presupuesto #{presupuesto['id']}")
        detalle_window.geometry("900x700")
        
        # Frame principal
        main_frame = ttk.Frame(detalle_window, padding=10)
        main_frame.pack(fill='both', expand=True)
        
        # Información del cliente
        cliente_frame = ttk.LabelFrame(main_frame, text="Información del Cliente", padding=10)
        cliente_frame.pack(fill='x', pady=(0, 10))
        
        ttk.Label(cliente_frame, text=f"Nombre: {presupuesto['cliente_nombre']}").pack(anchor='w')
        ttk.Label(cliente_frame, text=f"Teléfono: {presupuesto['telefono'] or 'N/A'}").pack(anchor='w')
        ttk.Label(cliente_frame, text=f"Email: {presupuesto['email'] or 'N/A'}").pack(anchor='w')
        ttk.Label(cliente_frame, text=f"Dirección: {presupuesto['direccion'] or 'N/A'}").pack(anchor='w')
        
        # Items del presupuesto
        items_frame = ttk.LabelFrame(main_frame, text="Items del Presupuesto", padding=10)
        items_frame.pack(fill='both', expand=True, pady=(0, 10))
        
        # Treeview para items
        columns = ('Tipo', 'Descripción', 'Cantidad', 'Precio Unit.', 'Subtotal')
        items_tree = ttk.Treeview(items_frame, columns=columns, show='headings', height=10)
        
        for col in columns:
            items_tree.heading(col, text=col)
            if col == 'Tipo':
                items_tree.column(col, width=80)
            else:
                items_tree.column(col, width=150)
        
        # Scrollbar
        items_scrollbar = ttk.Scrollbar(items_frame, orient='vertical', command=items_tree.yview)
        items_tree.configure(yscrollcommand=items_scrollbar.set)
        
        items_tree.pack(side='left', fill='both', expand=True)
        items_scrollbar.pack(side='right', fill='y')
        
        # Llenar items
        for item in presupuesto['items']:
            if item.get('es_tarea_manual', 0):
                tipo = "Tarea"
                descripcion = item.get('tarea_manual', 'Tarea manual')
            else:
                tipo = "Material"
                descripcion = f"{item['material_nombre']} ({item['unidad_medida']})"
            
            items_tree.insert('', 'end', values=(
                tipo,
                descripcion,
                f"{item['cantidad']:.2f}",
                f"€{item['precio_unitario']:.2f}",
                f"€{item['subtotal']:.2f}"
            ))
        
        # Totales
        totales_frame = ttk.LabelFrame(main_frame, text="Totales", padding=10)
        totales_frame.pack(fill='x')
        
        ttk.Label(totales_frame, text=f"Subtotal: €{presupuesto['subtotal']:.2f}", font=('Arial', 10, 'bold')).pack(anchor='w')
        
        # Mostrar IVA solo si está habilitado
        iva_habilitado = presupuesto.get('iva_habilitado', True)
        if iva_habilitado:
            ttk.Label(totales_frame, text=f"IVA (21%): €{presupuesto['iva']:.2f}", font=('Arial', 10, 'bold')).pack(anchor='w')
        else:
            ttk.Label(totales_frame, text="IVA: No incluido", font=('Arial', 10, 'bold'), foreground='gray').pack(anchor='w')
        
        ttk.Label(totales_frame, text=f"Total: €{presupuesto['total']:.2f}", font=('Arial', 12, 'bold'), foreground='blue').pack(anchor='w')
        
        # Botones de acción
        botones_frame = ttk.Frame(main_frame)
        botones_frame.pack(fill='x', pady=(10, 0))
        
        def abrir_pdf_presupuesto():
            """Busca y abre el PDF del presupuesto, o lo genera si no existe"""
            try:
                # Buscar PDF existente más reciente
                patron = f"presupuesto_{presupuesto['id']}_*.pdf"
                pdfs_encontrados = []
                
                if os.path.exists(self.carpeta_pdfs):
                    for archivo in os.listdir(self.carpeta_pdfs):
                        if archivo.startswith(f"presupuesto_{presupuesto['id']}_") and archivo.endswith('.pdf'):
                            ruta_completa = os.path.join(self.carpeta_pdfs, archivo)
                            pdfs_encontrados.append((ruta_completa, os.path.getmtime(ruta_completa)))
                
                if pdfs_encontrados:
                    # Ordenar por fecha de modificación (más reciente primero)
                    pdfs_encontrados.sort(key=lambda x: x[1], reverse=True)
                    pdf_path = pdfs_encontrados[0][0]
                else:
                    # Generar PDF si no existe
                    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
                    nombre_archivo = f"presupuesto_{presupuesto['id']}_{timestamp}.pdf"
                    pdf_path = os.path.join(self.carpeta_pdfs, nombre_archivo)
                    os.makedirs(self.carpeta_pdfs, exist_ok=True)
                    self.pdf_generator.generate_presupuesto_pdf(presupuesto, pdf_path)
                
                # Abrir PDF
                if platform.system() == 'Windows':
                    os.startfile(pdf_path)
                elif platform.system() == 'Darwin':  # macOS
                    subprocess.run(['open', pdf_path])
                else:  # Linux
                    subprocess.run(['xdg-open', pdf_path])
                    
            except Exception as e:
                messagebox.showerror("Error", f"Error al abrir PDF: {str(e)}")
        
        ttk.Button(botones_frame, text="📄 Abrir PDF", command=abrir_pdf_presupuesto, style='Accent.TButton').pack(side='left', padx=(0, 10))
        ttk.Button(botones_frame, text="Cerrar", command=detalle_window.destroy).pack(side='right')
    
    def eliminar_presupuesto(self):
        selection = self.presupuestos_tree.selection()
        if not selection:
            messagebox.showwarning("Advertencia", "Seleccione un presupuesto para eliminar")
            return
        
        if messagebox.askyesno("Confirmar", "¿Está seguro de que desea eliminar este presupuesto?"):
            presupuesto_id = self.presupuestos_tree.item(selection[0])['values'][0]
            try:
                if presupuesto_manager.eliminar_presupuesto(presupuesto_id):
                    messagebox.showinfo("Éxito", "Presupuesto eliminado correctamente")
                    self.refresh_presupuestos()
                else:
                    messagebox.showerror("Error", "Error al eliminar presupuesto")
            except Exception as e:
                messagebox.showerror("Error", f"Error al eliminar presupuesto: {str(e)}")
    
    # Métodos para exportación e email
    def exportar_presupuesto_pdf(self):
        """Exporta el presupuesto seleccionado a PDF"""
        selection = self.presupuestos_tree.selection()
        if not selection:
            messagebox.showwarning("Advertencia", "Seleccione un presupuesto para exportar")
            return
        
        presupuesto_id = self.presupuestos_tree.item(selection[0])['values'][0]
        presupuesto = presupuesto_manager.obtener_presupuesto_por_id(presupuesto_id)
        
        if not presupuesto:
            messagebox.showerror("Error", "No se pudo obtener el presupuesto")
            return
        
        try:
            # Generar PDF en la carpeta configurada
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            nombre_archivo = f"presupuesto_{presupuesto['id']}_{timestamp}.pdf"
            ruta_completa = os.path.join(self.carpeta_pdfs, nombre_archivo)
            
            archivo_pdf = self.pdf_generator.generate_presupuesto_pdf(presupuesto, ruta_completa)
            
            # Mostrar mensaje de éxito con la ruta del archivo
            messagebox.showinfo("Éxito", f"Presupuesto exportado correctamente:\n{archivo_pdf}")
            
            # Preguntar si desea abrir el archivo
            if messagebox.askyesno("Abrir PDF", "¿Desea abrir el archivo PDF generado?"):
                try:
                    if platform.system() == 'Windows':
                        os.startfile(archivo_pdf)
                    elif platform.system() == 'Darwin':  # macOS
                        subprocess.run(['open', archivo_pdf])
                    else:  # Linux
                        subprocess.run(['xdg-open', archivo_pdf])
                except Exception as e:
                    messagebox.showinfo("Información", f"Archivo guardado en: {os.path.abspath(archivo_pdf)}")
            
        except Exception as e:
            messagebox.showerror("Error", f"Error al exportar PDF: {str(e)}")
    
    def enviar_presupuesto_email(self):
        """Envía el presupuesto seleccionado por email"""
        selection = self.presupuestos_tree.selection()
        if not selection:
            messagebox.showwarning("Advertencia", "Seleccione un presupuesto para enviar")
            return
        
        presupuesto_id = self.presupuestos_tree.item(selection[0])['values'][0]
        presupuesto = presupuesto_manager.obtener_presupuesto_por_id(presupuesto_id)
        
        if not presupuesto:
            messagebox.showerror("Error", "No se pudo obtener el presupuesto")
            return
        
        # Verificar si el email está configurado
        if not email_sender.configurado:
            if messagebox.askyesno("Configurar Email", "Debe configurar el email primero. ¿Desea configurarlo ahora?"):
                self.configurar_email()
            return
        
        try:
            # Generar PDF temporal en la carpeta configurada
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            nombre_archivo = f"presupuesto_email_{presupuesto['id']}_{timestamp}.pdf"
            ruta_completa = os.path.join(self.carpeta_pdfs, nombre_archivo)
            
            archivo_pdf = self.pdf_generator.generate_presupuesto_pdf(presupuesto, ruta_completa)
            
            # Mostrar diálogo de envío
            email_sender.mostrar_dialogo_envio(self.root, presupuesto, archivo_pdf)
            
            # Limpiar archivo temporal (opcional)
            # os.remove(archivo_pdf)
            
        except Exception as e:
            messagebox.showerror("Error", f"Error al enviar email: {str(e)}")
    
    def configurar_email(self):
        """Configura los datos del servidor de email"""
        email_sender.mostrar_configuracion_email(self.root)
    
    # Métodos para búsqueda y filtrado de presupuestos
    def buscar_presupuestos(self, event=None):
        """Busca presupuestos por término"""
        termino = self.presupuesto_busqueda_entry.get().strip()
        estado_filtro = self.estado_filtro_var.get()
        anio_param = self._obtener_anio_desde_combo(getattr(self, 'presupuesto_anio_filtro_var', None))
        mes_param = self._obtener_mes_desde_combo(getattr(self, 'presupuesto_mes_filtro_var', None))
        
        if termino:
            presupuestos = presupuesto_manager.buscar_presupuestos(
                termino,
                anio=anio_param,
                mes=mes_param
            )
        else:
            presupuestos = presupuesto_manager.obtener_presupuestos(
                anio=anio_param,
                mes=mes_param
            )
        
        # Aplicar filtro de estado
        if estado_filtro != "Todos":
            presupuestos = [p for p in presupuestos if p.get('estado', 'Pendiente') == estado_filtro]
        
        self.actualizar_tree_presupuestos(presupuestos)
    
    def limpiar_busqueda_presupuestos(self):
        """Limpia la búsqueda y muestra todos los presupuestos"""
        self.presupuesto_busqueda_entry.delete(0, tk.END)
        self.estado_filtro_var.set("Todos")
        if hasattr(self, 'presupuesto_anio_filtro_var'):
            self.presupuesto_anio_filtro_var.set("Todos")
        if hasattr(self, 'presupuesto_mes_filtro_var'):
            self.presupuesto_mes_filtro_var.set("Todos")
        self.cargar_filtros_fecha_presupuestos()
        self.refresh_presupuestos()
    
    def filtrar_por_estado(self, event=None):
        """Filtra presupuestos por estado"""
        self.buscar_presupuestos()

    def on_presupuesto_anio_cambiado(self, event=None):
        """Actualiza los meses disponibles al cambiar el año y reaplica filtros"""
        self.cargar_filtros_fecha_presupuestos()
        self.buscar_presupuestos()

    def cargar_filtros_fecha_presupuestos(self, mantener_seleccion: bool = False):
        """Carga los valores de los combos de mes/año para presupuestos"""
        if not hasattr(self, 'presupuesto_mes_combo'):
            return

        mes_actual = self.presupuesto_mes_filtro_var.get() if mantener_seleccion else "Todos"
        anio_actual = self.presupuesto_anio_filtro_var.get() if hasattr(self, 'presupuesto_anio_filtro_var') else "Todos"

        # Poblar años
        anios_disponibles = presupuesto_manager.obtener_anios_presupuestos()
        anios_values = ["Todos"] + anios_disponibles
        self.presupuesto_anio_combo['values'] = anios_values

        if anio_actual not in anios_values:
            anio_actual = "Todos"
        self.presupuesto_anio_filtro_var.set(anio_actual)

        anio_param = self._obtener_anio_desde_combo(self.presupuesto_anio_filtro_var)

        # Poblar meses según el año seleccionado
        meses_disponibles = presupuesto_manager.obtener_meses_presupuestos(anio_param)
        meses_values = ["Todos"] + [self._formatear_mes_opcion(mes) for mes in meses_disponibles]
        self.presupuesto_mes_combo['values'] = meses_values

        if mantener_seleccion and mes_actual in meses_values:
            self.presupuesto_mes_filtro_var.set(mes_actual)
        else:
            self.presupuesto_mes_filtro_var.set("Todos")

    def _formatear_mes_opcion(self, mes_codigo: str) -> str:
        """Devuelve una representación legible del mes"""
        nombre = MESES_NOMBRES.get(mes_codigo, mes_codigo)
        return f"{nombre} ({mes_codigo})"

    def _obtener_anio_desde_combo(self, var):
        """Convierte el valor del combo de año a entero"""
        if not var:
            return None

        valor = var.get() if isinstance(var, tk.StringVar) else var
        if not valor or valor == "Todos":
            return None

        try:
            return int(valor)
        except (TypeError, ValueError):
            return None

    def _obtener_mes_desde_combo(self, var):
        """Convierte el valor del combo de mes a entero"""
        if not var:
            return None

        valor = var.get() if isinstance(var, tk.StringVar) else var
        if not valor or valor == "Todos":
            return None

        match = re.search(r"\((\d{2})\)", valor)
        if match:
            return int(match.group(1))

        if valor.isdigit() and len(valor) == 2:
            return int(valor)

        # Intentar mapa inverso (nombre -> código)
        for codigo, nombre in MESES_NOMBRES.items():
            if nombre.lower() == valor.lower():
                return int(codigo)

        return None
    
    def marcar_aprobado(self):
        """Marca el presupuesto seleccionado como aprobado"""
        selection = self.presupuestos_tree.selection()
        if not selection:
            messagebox.showwarning("Advertencia", "Seleccione un presupuesto para marcar como aprobado")
            return
        
        presupuesto_id = self.presupuestos_tree.item(selection[0])['values'][0]
        try:
            if presupuesto_manager.actualizar_estado_presupuesto(presupuesto_id, 'Aprobado'):
                messagebox.showinfo("Éxito", "Presupuesto marcado como aprobado")
                self.refresh_presupuestos()
            else:
                messagebox.showerror("Error", "Error al actualizar el estado del presupuesto")
        except Exception as e:
            messagebox.showerror("Error", f"Error al actualizar estado: {str(e)}")
    
    def marcar_pendiente(self):
        """Marca el presupuesto seleccionado como pendiente"""
        selection = self.presupuestos_tree.selection()
        if not selection:
            messagebox.showwarning("Advertencia", "Seleccione un presupuesto para marcar como pendiente")
            return
        
        presupuesto_id = self.presupuestos_tree.item(selection[0])['values'][0]
        try:
            if presupuesto_manager.actualizar_estado_presupuesto(presupuesto_id, 'Pendiente'):
                messagebox.showinfo("Éxito", "Presupuesto marcado como pendiente")
                self.refresh_presupuestos()
            else:
                messagebox.showerror("Error", "Error al actualizar el estado del presupuesto")
        except Exception as e:
            messagebox.showerror("Error", f"Error al actualizar estado: {str(e)}")
    
    def marcar_rechazado(self):
        """Marca el presupuesto seleccionado como rechazado"""
        selection = self.presupuestos_tree.selection()
        if not selection:
            messagebox.showwarning("Advertencia", "Seleccione un presupuesto para marcar como rechazado")
            return
        
        presupuesto_id = self.presupuestos_tree.item(selection[0])['values'][0]
        try:
            if presupuesto_manager.actualizar_estado_presupuesto(presupuesto_id, 'Rechazado'):
                messagebox.showinfo("Éxito", "Presupuesto marcado como rechazado")
                self.refresh_presupuestos()
            else:
                messagebox.showerror("Error", "Error al actualizar el estado del presupuesto")
        except Exception as e:
            messagebox.showerror("Error", f"Error al actualizar estado: {str(e)}")
    
    # Métodos para gestión de configuración
    def cargar_configuracion(self):
        """Carga la configuración desde el archivo JSON"""
        try:
            if os.path.exists(self.config_file):
                with open(self.config_file, 'r', encoding='utf-8') as f:
                    config = json.load(f)
                    # Verificar que las carpetas existen
                    carpeta_pdfs = config.get('carpeta_pdfs', os.path.join(os.getcwd(), 'output', 'presupuestos'))
                    if not os.path.exists(carpeta_pdfs):
                        os.makedirs(carpeta_pdfs, exist_ok=True)
                    
                    carpeta_facturas = config.get('carpeta_facturas', os.path.join(os.getcwd(), 'output', 'facturas'))
                    if not os.path.exists(carpeta_facturas):
                        os.makedirs(carpeta_facturas, exist_ok=True)
                    
                    return {
                        'carpeta_pdfs': carpeta_pdfs,
                        'carpeta_facturas': carpeta_facturas
                    }
            else:
                # Crear carpetas por defecto si no existe config
                default_pdfs = os.path.join(os.getcwd(), 'output', 'presupuestos')
                default_facturas = os.path.join(os.getcwd(), 'output', 'facturas')
                os.makedirs(default_pdfs, exist_ok=True)
                os.makedirs(default_facturas, exist_ok=True)
                return {
                    'carpeta_pdfs': default_pdfs,
                    'carpeta_facturas': default_facturas
                }
        except Exception as e:
            print(f"Error cargando configuración: {e}")
            # Crear carpetas por defecto en caso de error
            default_pdfs = os.path.join(os.getcwd(), 'output', 'presupuestos')
            default_facturas = os.path.join(os.getcwd(), 'output', 'facturas')
            os.makedirs(default_pdfs, exist_ok=True)
            os.makedirs(default_facturas, exist_ok=True)
            return {
                'carpeta_pdfs': default_pdfs,
                'carpeta_facturas': default_facturas
            }
    
    def guardar_configuracion(self):
        """Guarda la configuración en el archivo JSON"""
        try:
            config = {
                'carpeta_pdfs': self.carpeta_pdfs,
                'carpeta_facturas': self.carpeta_facturas
            }
            with open(self.config_file, 'w', encoding='utf-8') as f:
                json.dump(config, f, indent=2, ensure_ascii=False)
        except Exception as e:
            print(f"Error guardando configuración: {e}")
    
    # Métodos para gestión de carpeta de PDFs
    def actualizar_label_carpeta_pdfs(self):
        """Actualiza el label que muestra la carpeta actual de PDFs"""
        if hasattr(self, 'carpeta_pdfs_label'):
            carpeta_mostrar = os.path.basename(self.carpeta_pdfs) if self.carpeta_pdfs else "No configurada"
            self.carpeta_pdfs_label.config(text=f"Carpeta PDFs: {carpeta_mostrar}")
    
    def elegir_carpeta_pdfs(self):
        """Permite al usuario elegir la carpeta donde se guardan los PDFs"""
        carpeta_seleccionada = filedialog.askdirectory(
            title="Seleccionar carpeta para guardar PDFs",
            initialdir=self.carpeta_pdfs
        )
        
        if carpeta_seleccionada:
            self.carpeta_pdfs = carpeta_seleccionada
            self.actualizar_label_carpeta_pdfs()
            self.guardar_configuracion()  # Guardar la configuración
            messagebox.showinfo("Éxito", f"Carpeta de PDFs configurada y guardada:\n{self.carpeta_pdfs}")
    
    def abrir_carpeta_pdfs(self):
        """Abre la carpeta donde se guardan los PDFs"""
        if not self.carpeta_pdfs or not os.path.exists(self.carpeta_pdfs):
            messagebox.showwarning("Advertencia", "La carpeta de PDFs no está configurada o no existe")
            return
        
        try:
            if platform.system() == 'Windows':
                os.startfile(self.carpeta_pdfs)
            elif platform.system() == 'Darwin':  # macOS
                subprocess.run(['open', self.carpeta_pdfs])
            else:  # Linux
                subprocess.run(['xdg-open', self.carpeta_pdfs])
        except Exception as e:
            messagebox.showerror("Error", f"No se pudo abrir la carpeta: {str(e)}")
    
    def cargar_configuracion_plantilla(self):
        """Carga la configuración de la plantilla desde el archivo JSON"""
        # Configuración por defecto completa
        config_default = {
            "empresa": {
                "nombre": "Mi Empresa S.L.",
                "direccion": "Calle Principal 123",
                "codigo_postal": "28001",
                "ciudad": "Madrid",
                "provincia": "Madrid",
                "pais": "España",
                "telefono": "+34 123 456 789",
                "email": "info@miempresa.com",
                "web": "www.miempresa.com",
                "cif": "B12345678",
                "registro_mercantil": {
                    "provincia": "Madrid",
                    "tomo": "12345",
                    "folio": "67",
                    "seccion": "8",
                    "hoja": "M-123456",
                    "inscripcion": "1ª"
                }
            },
            "logo": {
                "usar_logo": False,
                "ruta_logo": "",
                "texto_logo": "PRESUPUESTOS",
                "tamaño_logo": 24
            },
            "colores": {
                "color_principal": "#2c3e50",
                "color_secundario": "#3498db",
                "color_texto": "#2c3e50"
            },
            "texto_personalizado": {
                "titulo_principal": "PRESUPUESTO",
                "notas_pie": "• Este presupuesto tiene una validez de 30 días.\n• Los precios incluyen IVA.\n• Para cualquier consulta, contacte con nosotros.",
                "texto_iva_incluido": "Los precios incluyen IVA.",
                "texto_iva_no_incluido": "Los precios NO incluyen IVA.",
                "nota_factura_iva_incluido": "Los importes de esta factura incluyen IVA al tipo correspondiente.",
                "nota_factura_iva_exento": "Operación exenta de IVA conforme a la normativa vigente.",
                "nota_factura_general": "Pago mediante transferencia en un plazo máximo de 30 días."
            },
            "margenes": {
                "superior": 72,
                "inferior": 18,
                "izquierdo": 72,
                "derecho": 72
            },
            "legal": {
                "mostrar_firma": False,
                "firma_texto": "Firma y sello de la empresa",
                "nota_exencion": "Operación exenta según art. 20 de la Ley 37/1992 del IVA."
            },
            "opciones_pdf": {
                "mostrar_registro_mercantil": True
            }
        }
        
        try:
            if os.path.exists(self.plantilla_config_file):
                with open(self.plantilla_config_file, 'r', encoding='utf-8') as f:
                    config_cargada = json.load(f)
                    # Fusionar con configuración por defecto para asegurar que todas las claves existan
                    config_final = self.merge_config(copy.deepcopy(config_default), config_cargada)
                    return self._normalizar_config_plantilla(config_final)
            else:
                return self._normalizar_config_plantilla(copy.deepcopy(config_default))
        except Exception as e:
            print(f"Error cargando configuración de plantilla: {e}")
            return self._normalizar_config_plantilla(copy.deepcopy(config_default))
    
    def merge_config(self, default_config, loaded_config):
        """Fusiona la configuración cargada con la configuración por defecto"""
        merged = copy.deepcopy(default_config)
        for key, value in loaded_config.items():
            if key in merged and isinstance(merged[key], dict) and isinstance(value, dict):
                merged[key] = self.merge_config(merged[key], value)
            else:
                merged[key] = value
        return merged
    
    def _normalizar_config_plantilla(self, config):
        """Normaliza valores especiales dentro de la configuración"""
        empresa = config.get('empresa', {})
        registro = empresa.get('registro_mercantil')
        if isinstance(registro, str):
            try:
                registro_dict = ast.literal_eval(registro)
                if isinstance(registro_dict, dict):
                    empresa['registro_mercantil'] = registro_dict
            except (ValueError, SyntaxError):
                empresa['registro_mercantil'] = {"descripcion": registro}
        elif not isinstance(registro, dict) and registro is not None:
            empresa['registro_mercantil'] = {"descripcion": str(registro)}
        
        opciones_pdf = config.get('opciones_pdf')
        if not isinstance(opciones_pdf, dict):
            config['opciones_pdf'] = {"mostrar_registro_mercantil": True}
        else:
            opciones_pdf.setdefault('mostrar_registro_mercantil', True)
        
        return config
    
    def guardar_configuracion_plantilla(self):
        """Guarda la configuración de la plantilla en el archivo JSON"""
        try:
            with open(self.plantilla_config_file, 'w', encoding='utf-8') as f:
                json.dump(self.plantilla_config, f, indent=2, ensure_ascii=False)
        except Exception as e:
            print(f"Error guardando configuración de plantilla: {e}")
    
    def editar_plantilla_pdf(self):
        """Abre la ventana de edición de plantilla PDF"""
        # Crear ventana de edición
        plantilla_window = tk.Toplevel(self.root)
        plantilla_window.title("Editar Plantilla PDF")
        plantilla_window.geometry("700x750")
        plantilla_window.resizable(True, True)
        plantilla_window.transient(self.root)
        plantilla_window.grab_set()
        
        # Centrar la ventana
        plantilla_window.geometry("+%d+%d" % (
            self.root.winfo_rootx() + 100,
            self.root.winfo_rooty() + 50
        ))
        
        # Frame principal con scroll
        main_frame = ttk.Frame(plantilla_window, padding=10)
        main_frame.pack(fill='both', expand=True)
        
        # Canvas y scrollbar
        canvas = tk.Canvas(main_frame, bg='#ecf0f1')
        scrollbar = ttk.Scrollbar(main_frame, orient="vertical", command=canvas.yview)
        scrollable_frame = ttk.Frame(canvas)
        
        def configure_scroll_region(event=None):
            canvas.configure(scrollregion=canvas.bbox("all"))
        
        def _on_mousewheel(event):
            canvas.yview_scroll(int(-1*(event.delta/120)), "units")
        
        def _bind_to_mousewheel(event):
            canvas.bind_all("<MouseWheel>", _on_mousewheel)
        
        def _unbind_from_mousewheel(event):
            canvas.unbind_all("<MouseWheel>")
        
        scrollable_frame.bind("<Configure>", configure_scroll_region)
        
        canvas.create_window((0, 0), window=scrollable_frame, anchor="nw")
        canvas.configure(yscrollcommand=scrollbar.set)
        
        # Configurar el canvas para que se expanda correctamente
        def configure_canvas(event):
            canvas_width = event.width
            canvas.itemconfig(canvas.find_all()[0], width=canvas_width)
        
        canvas.bind('<Configure>', configure_canvas)
        
        canvas.pack(side="left", fill="both", expand=True)
        scrollbar.pack(side="right", fill="y")
        
        # Bind en el frame principal para que funcione el scroll con rueda del mouse
        main_frame.bind('<Enter>', _bind_to_mousewheel)
        main_frame.bind('<Leave>', _unbind_from_mousewheel)
        canvas.bind('<Enter>', _bind_to_mousewheel)
        canvas.bind('<Leave>', _unbind_from_mousewheel)
        
        # Variables para los campos
        vars_empresa = {}
        vars_logo = {}
        vars_colores = {}
        vars_texto = {}
        vars_margenes = {}
        
        # Sección de información de la empresa
        empresa_frame = ttk.LabelFrame(scrollable_frame, text="Información de la Empresa", padding=10)
        empresa_frame.pack(fill='x', pady=(0, 10))
        
        campos_empresa = [
            ("Nombre de la empresa:", "nombre"),
            ("Dirección:", "direccion"),
            ("Ciudad:", "ciudad"),
            ("Teléfono:", "telefono"),
            ("Email:", "email"),
            ("Sitio web:", "web"),
            ("CIF/NIF:", "cif")
        ]
        
        for i, (label, key) in enumerate(campos_empresa):
            ttk.Label(empresa_frame, text=label).grid(row=i, column=0, sticky='w', padx=(0, 10), pady=5)
            vars_empresa[key] = tk.StringVar(value=self.plantilla_config['empresa'].get(key, ''))
            ttk.Entry(empresa_frame, textvariable=vars_empresa[key], width=30).grid(row=i, column=1, sticky='ew', pady=5)
        
        empresa_frame.columnconfigure(1, weight=1)
        
        # Sección de logo
        logo_frame = ttk.LabelFrame(scrollable_frame, text="Configuración del Logo", padding=10)
        logo_frame.pack(fill='x', pady=(0, 10))
        
        vars_logo['usar_logo'] = tk.BooleanVar(value=self.plantilla_config['logo'].get('usar_logo', False))
        ttk.Checkbutton(logo_frame, text="Usar imagen de logo", variable=vars_logo['usar_logo']).pack(anchor='w', pady=(0, 10))
        
        ttk.Label(logo_frame, text="Ruta del logo:").pack(anchor='w')
        logo_path_frame = ttk.Frame(logo_frame)
        logo_path_frame.pack(fill='x', pady=(5, 10))
        
        vars_logo['ruta_logo'] = tk.StringVar(value=self.plantilla_config['logo'].get('ruta_logo', ''))
        ttk.Entry(logo_path_frame, textvariable=vars_logo['ruta_logo'], width=35).pack(side='left', fill='x', expand=True)
        ttk.Button(logo_path_frame, text="Examinar", command=lambda: self.seleccionar_logo(vars_logo['ruta_logo'])).pack(side='right', padx=(10, 0))
        
        ttk.Label(logo_frame, text="Texto del logo (si no usa imagen):").pack(anchor='w', pady=(10, 0))
        vars_logo['texto_logo'] = tk.StringVar(value=self.plantilla_config['logo'].get('texto_logo', 'PRESUPUESTOS'))
        ttk.Entry(logo_frame, textvariable=vars_logo['texto_logo'], width=30).pack(anchor='w', pady=(5, 0))
        
        ttk.Label(logo_frame, text="Tamaño del logo:").pack(anchor='w', pady=(10, 0))
        vars_logo['tamaño_logo'] = tk.IntVar(value=self.plantilla_config['logo'].get('tamaño_logo', 24))
        ttk.Scale(logo_frame, from_=12, to=48, variable=vars_logo['tamaño_logo'], orient='horizontal').pack(fill='x', pady=(5, 0))
        
        # Sección de colores
        colores_frame = ttk.LabelFrame(scrollable_frame, text="Colores", padding=10)
        colores_frame.pack(fill='x', pady=(0, 10))
        
        campos_colores = [
            ("Color principal:", "color_principal"),
            ("Color secundario:", "color_secundario"),
            ("Color del texto:", "color_texto")
        ]
        
        for i, (label, key) in enumerate(campos_colores):
            ttk.Label(colores_frame, text=label).grid(row=i, column=0, sticky='w', padx=(0, 10), pady=5)
            vars_colores[key] = tk.StringVar(value=self.plantilla_config['colores'].get(key, '#2c3e50'))
            ttk.Entry(colores_frame, textvariable=vars_colores[key], width=12).grid(row=i, column=1, sticky='w', pady=5)
        
        # Sección de texto personalizado
        texto_frame = ttk.LabelFrame(scrollable_frame, text="Texto Personalizado", padding=10)
        texto_frame.pack(fill='x', pady=(0, 10))
        
        ttk.Label(texto_frame, text="Título principal:").pack(anchor='w')
        vars_texto['titulo_principal'] = tk.StringVar(value=self.plantilla_config['texto_personalizado'].get('titulo_principal', 'PRESUPUESTO'))
        ttk.Entry(texto_frame, textvariable=vars_texto['titulo_principal'], width=30).pack(fill='x', pady=(0, 10))
        
        ttk.Label(texto_frame, text="Notas al pie:").pack(anchor='w')
        vars_texto['notas_pie'] = tk.StringVar(value=self.plantilla_config['texto_personalizado'].get('notas_pie', ''))
        text_area = tk.Text(texto_frame, height=4, width=50)
        text_area.pack(fill='x', pady=(5, 10))
        text_area.insert('1.0', vars_texto['notas_pie'].get())
        
        # Sección de márgenes
        margenes_frame = ttk.LabelFrame(scrollable_frame, text="Márgenes (en puntos)", padding=10)
        margenes_frame.pack(fill='x', pady=(0, 10))
        
        campos_margenes = [
            ("Superior:", "superior"),
            ("Inferior:", "inferior"),
            ("Izquierdo:", "izquierdo"),
            ("Derecho:", "derecho")
        ]
        
        for i, (label, key) in enumerate(campos_margenes):
            ttk.Label(margenes_frame, text=label).grid(row=i//2, column=(i%2)*2, sticky='w', padx=(0, 10), pady=5)
            vars_margenes[key] = tk.IntVar(value=self.plantilla_config['margenes'].get(key, 72))
            ttk.Entry(margenes_frame, textvariable=vars_margenes[key], width=8).grid(row=i//2, column=(i%2)*2+1, sticky='w', pady=5)
        
        # Botones
        button_frame = ttk.Frame(scrollable_frame)
        button_frame.pack(fill='x', pady=(20, 0))
        
        def guardar_configuracion():
            try:
                # Actualizar configuración
                self.plantilla_config['empresa'] = {key: var.get() for key, var in vars_empresa.items()}
                self.plantilla_config['logo'] = {key: var.get() for key, var in vars_logo.items()}
                self.plantilla_config['colores'] = {key: var.get() for key, var in vars_colores.items()}
                self.plantilla_config['texto_personalizado'] = {key: var.get() for key, var in vars_texto.items()}
                self.plantilla_config['texto_personalizado']['notas_pie'] = text_area.get('1.0', tk.END).strip()
                self.plantilla_config['margenes'] = {key: var.get() for key, var in vars_margenes.items()}
                if not isinstance(self.plantilla_config.get('opciones_pdf'), dict):
                    self.plantilla_config['opciones_pdf'] = {}
                self.plantilla_config['opciones_pdf']['mostrar_registro_mercantil'] = bool(self.config_mostrar_registro_var.get())
                
                # Guardar archivo
                self.guardar_configuracion_plantilla()
                
                # Actualizar el generador de PDFs con la nueva configuración
                self.pdf_generator = PDFGenerator(self.plantilla_config)
                
                messagebox.showinfo("Éxito", "Configuración de plantilla guardada correctamente")
                plantilla_window.destroy()
                
            except Exception as e:
                messagebox.showerror("Error", f"Error al guardar configuración: {str(e)}")
        
        def cancelar():
            plantilla_window.destroy()
        
        ttk.Button(button_frame, text="💾 Guardar", command=guardar_configuracion, style='Accent.TButton').pack(side='right', padx=(10, 0))
        ttk.Button(button_frame, text="❌ Cancelar", command=cancelar).pack(side='right')
        ttk.Button(button_frame, text="🔄 Restaurar por defecto", command=lambda: self.restaurar_plantilla_por_defecto(vars_empresa, vars_logo, vars_colores, vars_texto, vars_margenes, text_area)).pack(side='left')
    
    def seleccionar_logo(self, var_ruta):
        """Permite seleccionar un archivo de logo"""
        ruta = filedialog.askopenfilename(
            title="Seleccionar logo",
            filetypes=[("Imágenes", "*.png *.jpg *.jpeg *.gif *.bmp"), ("Todos los archivos", "*.*")]
        )
        if ruta:
            var_ruta.set(ruta)
    
    def restaurar_plantilla_por_defecto(self, vars_empresa, vars_logo, vars_colores, vars_texto, vars_margenes, text_area):
        """Restaura la configuración por defecto"""
        if messagebox.askyesno("Confirmar", "¿Está seguro de que desea restaurar la configuración por defecto?"):
            # Restaurar valores por defecto
            vars_empresa['nombre'].set("Mi Empresa")
            vars_empresa['direccion'].set("Calle Principal 123")
            vars_empresa['ciudad'].set("Ciudad")
            vars_empresa['telefono'].set("+34 123 456 789")
            vars_empresa['email'].set("info@miempresa.com")
            vars_empresa['web'].set("www.miempresa.com")
            vars_empresa['cif'].set("B12345678")
            
            vars_logo['usar_logo'].set(False)
            vars_logo['ruta_logo'].set("")
            vars_logo['texto_logo'].set("PRESUPUESTOS")
            vars_logo['tamaño_logo'].set(24)
            
            vars_colores['color_principal'].set("#2c3e50")
            vars_colores['color_secundario'].set("#3498db")
            vars_colores['color_texto'].set("#2c3e50")
            
            vars_texto['titulo_principal'].set("PRESUPUESTO")
            text_area.delete('1.0', tk.END)
            text_area.insert('1.0', "• Este presupuesto tiene una validez de 30 días.\n• Los precios incluyen IVA.\n• Para cualquier consulta, contacte con nosotros.")
            
            vars_margenes['superior'].set(72)
            vars_margenes['inferior'].set(18)
            vars_margenes['izquierdo'].set(72)
            vars_margenes['derecho'].set(72)

    # ============================================
    # MÉTODOS PARA FACTURACIÓN
    # ============================================
    
    def create_facturacion_tab(self):
        """Crea la pestaña de facturación"""
        self.facturacion_frame = ttk.Frame(self.notebook)
        self.notebook.add(self.facturacion_frame, text="Facturación")
        
        # Crear notebook interno para crear y ver facturas
        self.facturacion_notebook = ttk.Notebook(self.facturacion_frame)
        self.facturacion_notebook.pack(fill='both', expand=True, padx=10, pady=10)
        
        # Pestaña de crear factura
        self.crear_factura_frame = ttk.Frame(self.facturacion_notebook)
        self.facturacion_notebook.add(self.crear_factura_frame, text="Crear Factura")
        
        # Pestaña de ver facturas
        self.ver_facturas_frame = ttk.Frame(self.facturacion_notebook)
        self.facturacion_notebook.add(self.ver_facturas_frame, text="Ver Facturas")
        
        # Pestaña de configuración
        self.configuracion_frame = ttk.Frame(self.facturacion_notebook)
        self.facturacion_notebook.add(self.configuracion_frame, text="Configuración")
        
        # Crear contenido de cada pestaña
        self.create_crear_factura_section()
        self.create_ver_facturas_section()
        self.create_configuracion_empresa_section()
    
    def create_crear_factura_section(self):
        """Crea la sección de creación de facturas"""
        # Frame principal con scrollbar
        main_frame = ttk.Frame(self.crear_factura_frame)
        main_frame.pack(fill='both', expand=True, padx=10, pady=10)
        
        # Canvas y scrollbar
        canvas = tk.Canvas(main_frame, bg='#ecf0f1')
        scrollbar = ttk.Scrollbar(main_frame, orient="vertical", command=canvas.yview)
        scrollable_frame = ttk.Frame(canvas)
        
        def configure_scroll_region(event=None):
            canvas.configure(scrollregion=canvas.bbox("all"))
        
        scrollable_frame.bind("<Configure>", configure_scroll_region)
        canvas.create_window((0, 0), window=scrollable_frame, anchor="nw")
        canvas.configure(yscrollcommand=scrollbar.set)
        
        def configure_canvas(event):
            canvas_width = event.width
            canvas.itemconfig(canvas.find_all()[0], width=canvas_width)
        
        canvas.bind('<Configure>', configure_canvas)
        canvas.pack(side="left", fill="both", expand=True)
        scrollbar.pack(side="right", fill="y")
        
        # Configurar scroll con rueda del mouse
        def _on_mousewheel(event):
            canvas.yview_scroll(int(-1*(event.delta/120)), "units")
        
        def _bind_to_mousewheel(event):
            canvas.bind_all("<MouseWheel>", _on_mousewheel)
        
        def _unbind_from_mousewheel(event):
            canvas.unbind_all("<MouseWheel>")
        
        main_frame.bind('<Enter>', _bind_to_mousewheel)
        main_frame.bind('<Leave>', _unbind_from_mousewheel)
        canvas.bind('<Enter>', _bind_to_mousewheel)
        canvas.bind('<Leave>', _unbind_from_mousewheel)
        
        # Frame para selección de cliente y autocompletado
        cliente_frame = ttk.LabelFrame(scrollable_frame, text="Cliente", padding=10)
        cliente_frame.pack(fill='x', pady=(0, 10))
        
        ttk.Label(cliente_frame, text="👤 Cliente:", style='TLabel').grid(row=0, column=0, sticky='w', padx=(0, 10), pady=8)
        self.factura_cliente_var = tk.StringVar()
        self.factura_cliente_combo = ttk.Combobox(cliente_frame, textvariable=self.factura_cliente_var, width=40, state='readonly')
        self.factura_cliente_combo.grid(row=0, column=1, padx=(0, 10), pady=8, sticky='ew')
        self.factura_cliente_combo.bind('<<ComboboxSelected>>', self.on_cliente_select_factura)
        
        # Datos del cliente (editables)
        self.factura_cliente_info_frame = ttk.LabelFrame(cliente_frame, text="Datos del Cliente (Editable)", padding=10)
        self.factura_cliente_info_frame.grid(row=1, column=0, columnspan=2, sticky='ew', pady=(10, 0))
        
        # Teléfono
        ttk.Label(self.factura_cliente_info_frame, text="📞 Teléfono:").grid(row=0, column=0, sticky='w', padx=(0, 10), pady=5)
        self.factura_cliente_telefono_var = tk.StringVar()
        self.factura_cliente_telefono_entry = ttk.Entry(self.factura_cliente_info_frame, textvariable=self.factura_cliente_telefono_var, width=30)
        self.factura_cliente_telefono_entry.grid(row=0, column=1, sticky='ew', padx=(0, 20), pady=5)
        
        # Email
        ttk.Label(self.factura_cliente_info_frame, text="📧 Email:").grid(row=0, column=2, sticky='w', padx=(0, 10), pady=5)
        self.factura_cliente_email_var = tk.StringVar()
        self.factura_cliente_email_entry = ttk.Entry(self.factura_cliente_info_frame, textvariable=self.factura_cliente_email_var, width=30)
        self.factura_cliente_email_entry.grid(row=0, column=3, sticky='ew', padx=(0, 20), pady=5)
        
        # Dirección
        ttk.Label(self.factura_cliente_info_frame, text="📍 Dirección:").grid(row=1, column=0, sticky='w', padx=(0, 10), pady=5)
        self.factura_cliente_direccion_var = tk.StringVar()
        self.factura_cliente_direccion_entry = ttk.Entry(self.factura_cliente_info_frame, textvariable=self.factura_cliente_direccion_var, width=70)
        self.factura_cliente_direccion_entry.grid(row=1, column=1, columnspan=3, sticky='ew', padx=(0, 0), pady=5)
        
        # Configurar columnas para que se expandan
        self.factura_cliente_info_frame.columnconfigure(1, weight=1)
        self.factura_cliente_info_frame.columnconfigure(3, weight=1)
        
        cliente_frame.columnconfigure(1, weight=1)
        
        # Frame para datos de la factura
        datos_frame = ttk.LabelFrame(scrollable_frame, text="Datos de la Factura", padding=10)
        datos_frame.pack(fill='x', pady=(0, 10))
        
        # Número de factura
        ttk.Label(datos_frame, text="📄 Nº Factura:").grid(row=0, column=0, sticky='w', padx=(0, 10), pady=8)
        self.numero_factura_var = tk.StringVar()
        self.numero_factura_entry = ttk.Entry(datos_frame, textvariable=self.numero_factura_var, width=20)
        self.numero_factura_entry.grid(row=0, column=1, padx=(0, 20), pady=8, sticky='w')
        ttk.Button(datos_frame, text="🔄 Auto", command=self.generar_numero_factura_auto, style='Small.TButton').grid(row=0, column=2, pady=8)
        
        # Fecha de vencimiento
        ttk.Label(datos_frame, text="📅 Vencimiento:").grid(row=0, column=3, sticky='w', padx=(20, 10), pady=8)
        self.fecha_vencimiento_var = tk.StringVar()
        self.fecha_vencimiento_entry = ttk.Entry(datos_frame, textvariable=self.fecha_vencimiento_var, width=15)
        self.fecha_vencimiento_entry.grid(row=0, column=4, pady=8, sticky='w')
        ttk.Label(datos_frame, text="(AAAA-MM-DD)", font=('Arial', 8), foreground='gray').grid(row=0, column=5, sticky='w', padx=(5, 0), pady=8)
        
        # Botones rápidos para fecha de vencimiento
        vencimiento_buttons_frame = ttk.Frame(datos_frame)
        vencimiento_buttons_frame.grid(row=1, column=3, columnspan=3, sticky='w', padx=(20, 0), pady=(5, 8))
        
        ttk.Label(vencimiento_buttons_frame, text="Rápido:", font=('Arial', 8)).pack(side='left', padx=(0, 5))
        ttk.Button(vencimiento_buttons_frame, text="+15 días", command=lambda: self.set_fecha_vencimiento(15), 
                  style='Small.TButton').pack(side='left', padx=(0, 3))
        ttk.Button(vencimiento_buttons_frame, text="+30 días", command=lambda: self.set_fecha_vencimiento(30), 
                  style='Small.TButton').pack(side='left', padx=(0, 3))
        ttk.Button(vencimiento_buttons_frame, text="+60 días", command=lambda: self.set_fecha_vencimiento(60), 
                  style='Small.TButton').pack(side='left', padx=(0, 3))
        ttk.Button(vencimiento_buttons_frame, text="+90 días", command=lambda: self.set_fecha_vencimiento(90), 
                  style='Small.TButton').pack(side='left')
        
        # Método de pago
        ttk.Label(datos_frame, text="💳 Método Pago:").grid(row=1, column=0, sticky='w', padx=(0, 10), pady=8)
        self.metodo_pago_var = tk.StringVar(value='Transferencia')
        self.metodo_pago_combo = ttk.Combobox(datos_frame, textvariable=self.metodo_pago_var, 
                                             values=['Transferencia', 'Efectivo', 'Tarjeta', 'Otro'], 
                                             state='readonly', width=18)
        self.metodo_pago_combo.grid(row=1, column=1, padx=(0, 20), pady=8, sticky='w')
        
        # Estado de pago
        ttk.Label(datos_frame, text="💰 Estado Pago:").grid(row=2, column=0, sticky='w', padx=(0, 10), pady=8)
        self.estado_pago_var = tk.StringVar(value='No Pagada')
        self.estado_pago_combo = ttk.Combobox(datos_frame, textvariable=self.estado_pago_var,
                                             values=['No Pagada', 'Pagada'],
                                             state='readonly', width=13)
        self.estado_pago_combo.grid(row=2, column=1, pady=8, sticky='w')
        
        # Retención IRPF
        ttk.Label(datos_frame, text="📊 Retención IRPF (%):").grid(row=2, column=3, sticky='w', padx=(20, 10), pady=8)
        self.retencion_irpf_var = tk.StringVar()
        self.retencion_irpf_entry = ttk.Entry(datos_frame, textvariable=self.retencion_irpf_var, width=10)
        self.retencion_irpf_entry.grid(row=2, column=4, pady=8, sticky='w')
        ttk.Label(datos_frame, text="(opcional, ej: 15)", font=('Arial', 8), foreground='gray').grid(row=2, column=5, sticky='w', padx=(5, 0), pady=8)
        self.retencion_irpf_entry.bind('<KeyRelease>', self.calcular_totales_factura)
        
        # Notas
        ttk.Label(datos_frame, text="📝 Notas:").grid(row=3, column=0, sticky='nw', padx=(0, 10), pady=8)
        self.factura_notas_text = tk.Text(datos_frame, height=3, width=60)
        self.factura_notas_text.grid(row=3, column=1, columnspan=5, pady=8, sticky='ew')
        
        # Frame para agregar materiales (igual que presupuestos)
        material_frame = ttk.LabelFrame(scrollable_frame, text="Agregar Material", padding=10)
        material_frame.pack(fill='x', pady=(0, 10))
        
        ttk.Label(material_frame, text="📦 Material:").grid(row=0, column=0, sticky='w', padx=(0, 10), pady=8)
        self.factura_material_var = tk.StringVar()
        self.factura_material_combo = ttk.Combobox(material_frame, textvariable=self.factura_material_var, width=30, state='readonly')
        self.factura_material_combo.grid(row=0, column=1, padx=(0, 20), pady=8)
        
        ttk.Label(material_frame, text="🔢 Cantidad:").grid(row=0, column=2, sticky='w', padx=(0, 10), pady=8)
        self.factura_cantidad_entry = ttk.Entry(material_frame, width=10)
        self.factura_cantidad_entry.grid(row=0, column=3, padx=(0, 20), pady=8)
        
        ttk.Button(material_frame, text="➕ Agregar Material", command=self.agregar_item_factura, style='Success.TButton').grid(row=0, column=4, padx=(10, 0), pady=8)
        
        # Frame para agregar tareas manuales
        tarea_frame = ttk.LabelFrame(scrollable_frame, text="Agregar Tarea Manual", padding=10)
        tarea_frame.pack(fill='x', pady=(0, 10))
        
        ttk.Label(tarea_frame, text="📝 Descripción:").grid(row=0, column=0, sticky='w', padx=(0, 10), pady=8)
        self.factura_tarea_descripcion_entry = ttk.Entry(tarea_frame, width=40)
        self.factura_tarea_descripcion_entry.grid(row=0, column=1, padx=(0, 20), pady=8)
        
        ttk.Label(tarea_frame, text="🔢 Cantidad:").grid(row=0, column=2, sticky='w', padx=(0, 10), pady=8)
        self.factura_tarea_cantidad_entry = ttk.Entry(tarea_frame, width=10)
        self.factura_tarea_cantidad_entry.grid(row=0, column=3, padx=(0, 20), pady=8)
        
        ttk.Label(tarea_frame, text="💰 Precio Unit.:").grid(row=1, column=0, sticky='w', padx=(0, 10), pady=8)
        self.factura_tarea_precio_entry = ttk.Entry(tarea_frame, width=15)
        self.factura_tarea_precio_entry.grid(row=1, column=1, padx=(0, 20), pady=8)
        
        ttk.Button(tarea_frame, text="➕ Agregar Tarea", command=self.agregar_tarea_factura, style='Success.TButton').grid(row=1, column=2, padx=(10, 0), pady=8)
        
        # Frame para items de la factura
        items_frame = ttk.LabelFrame(scrollable_frame, text="Items de la Factura", padding=10)
        items_frame.pack(fill='both', expand=True, pady=(0, 10))
        
        # Frame para lista y botones
        items_main_frame = ttk.Frame(items_frame)
        items_main_frame.pack(fill='both', expand=True)
        
        items_list_frame = ttk.Frame(items_main_frame)
        items_list_frame.pack(side='left', fill='both', expand=True, padx=(0, 10))
        
        # Treeview para items
        columns = ('Visible', 'IVA', 'Tipo', 'Descripción', 'Cantidad', 'Precio Unit.', 'Desc. %', 'Desc. €', 'Subtotal')
        self.factura_items_tree = ttk.Treeview(items_list_frame, columns=columns, show='headings', height=8)
        
        for col in columns:
            self.factura_items_tree.heading(col, text=col)
            if col == 'Visible' or col == 'IVA':
                self.factura_items_tree.column(col, width=60)
            elif col == 'Tipo':
                self.factura_items_tree.column(col, width=80)
            elif col == 'Desc. %' or col == 'Desc. €':
                self.factura_items_tree.column(col, width=70)
            else:
                self.factura_items_tree.column(col, width=120)
        
        items_scrollbar = ttk.Scrollbar(items_list_frame, orient='vertical', command=self.factura_items_tree.yview)
        self.factura_items_tree.configure(yscrollcommand=items_scrollbar.set)
        
        self.factura_items_tree.pack(side='left', fill='both', expand=True)
        items_scrollbar.pack(side='right', fill='y')
        
        # Bind para eliminar item
        self.factura_items_tree.bind('<Double-1>', self.on_item_double_click_factura)
        self.factura_items_tree.bind('<Button-1>', self.on_item_click_factura)
        
        # Botones de gestión
        buttons_frame = ttk.LabelFrame(items_main_frame, text="Gestión", padding=10)
        buttons_frame.pack(side='right', fill='y', padx=(10, 0))
        
        ttk.Button(buttons_frame, text="✏️ Editar Item", command=self.editar_item_factura, style='Accent.TButton').pack(fill='x', pady=(0, 10))
        ttk.Button(buttons_frame, text="🗑️ Eliminar Item", command=self.eliminar_item_factura, style='Danger.TButton').pack(fill='x', pady=(0, 10))
        ttk.Button(buttons_frame, text="✅ Marcar Todos", command=self.marcar_todos_items_factura).pack(fill='x', pady=(0, 10))
        ttk.Button(buttons_frame, text="❌ Desmarcar Todos", command=self.desmarcar_todos_items_factura).pack(fill='x')
        
        # Frame para totales
        totales_frame = ttk.LabelFrame(scrollable_frame, text="Totales", padding=10)
        totales_frame.pack(fill='x', pady=(0, 10))
        
        # Frame para controles de descuentos globales
        descuentos_frame = ttk.Frame(totales_frame)
        descuentos_frame.pack(fill='x', pady=(0, 10))
        
        # Descuentos globales
        ttk.Label(descuentos_frame, text="Descuento Global:", font=('Arial', 9, 'bold')).pack(side='left', padx=(0, 10))
        
        ttk.Label(descuentos_frame, text="%:").pack(side='left', padx=(0, 5))
        self.factura_descuento_porcentaje_var = tk.StringVar(value="0")
        self.factura_descuento_porcentaje_entry = ttk.Entry(descuentos_frame, textvariable=self.factura_descuento_porcentaje_var, width=8)
        self.factura_descuento_porcentaje_entry.pack(side='left', padx=(0, 10))
        self.factura_descuento_porcentaje_entry.bind('<KeyRelease>', self.calcular_totales_factura)
        
        ttk.Label(descuentos_frame, text="€:").pack(side='left', padx=(0, 5))
        self.factura_descuento_fijo_var = tk.StringVar(value="0")
        self.factura_descuento_fijo_entry = ttk.Entry(descuentos_frame, textvariable=self.factura_descuento_fijo_var, width=10)
        self.factura_descuento_fijo_entry.pack(side='left', padx=(0, 10))
        self.factura_descuento_fijo_entry.bind('<KeyRelease>', self.calcular_totales_factura)
        
        # Radio buttons para aplicar descuento antes o después de IVA
        self.factura_descuento_antes_iva_var = tk.BooleanVar(value=True)
        ttk.Radiobutton(descuentos_frame, text="Antes de IVA", variable=self.factura_descuento_antes_iva_var, 
                       value=True, command=self.calcular_totales_factura).pack(side='left', padx=(10, 5))
        ttk.Radiobutton(descuentos_frame, text="Después de IVA", variable=self.factura_descuento_antes_iva_var, 
                       value=False, command=self.calcular_totales_factura).pack(side='left', padx=(5, 0))
        
        totales_linea_frame = ttk.Frame(totales_frame)
        totales_linea_frame.pack(fill='x')
        
        # Información de items
        self.factura_items_info_label = ttk.Label(totales_linea_frame, text="Items: 0", font=('Arial', 9), foreground='gray')
        self.factura_items_info_label.pack(side='left', padx=(0, 20))
        
        # Checkbox para IVA
        self.factura_iva_habilitado_var = tk.BooleanVar(value=True)
        self.factura_iva_checkbox = ttk.Checkbutton(totales_linea_frame, text="Incluir IVA (21%)",
                                                    variable=self.factura_iva_habilitado_var,
                                                    command=self.calcular_totales_factura)
        self.factura_iva_checkbox.pack(side='left', padx=(0, 20))
        
        # Subtotal
        ttk.Label(totales_linea_frame, text="Subtotal:").pack(side='left', padx=(0, 5))
        self.factura_subtotal_label = ttk.Label(totales_linea_frame, text="€0.00", font=('Arial', 10, 'bold'))
        self.factura_subtotal_label.pack(side='left', padx=(0, 20))
        
        # Descuentos (si hay)
        self.factura_descuento_label = ttk.Label(totales_linea_frame, text="", font=('Arial', 9), foreground='green')
        self.factura_descuento_label.pack(side='left', padx=(0, 20))
        
        # IVA
        ttk.Label(totales_linea_frame, text="IVA (21%):").pack(side='left', padx=(0, 5))
        self.factura_iva_label = ttk.Label(totales_linea_frame, text="€0.00", font=('Arial', 10, 'bold'))
        self.factura_iva_label.pack(side='left', padx=(0, 20))
        
        # TOTAL
        ttk.Label(totales_linea_frame, text="TOTAL:", font=('Arial', 12, 'bold')).pack(side='left', padx=(0, 5))
        self.factura_total_label = ttk.Label(totales_linea_frame, text="€0.00", font=('Arial', 16, 'bold'), foreground='blue')
        self.factura_total_label.pack(side='left', padx=(0, 20))
        
        # Botones principales
        button_frame = ttk.Frame(scrollable_frame)
        button_frame.pack(fill='x', pady=(10, 0))
        
        # Primera fila de botones - acciones principales
        button_row1 = ttk.Frame(button_frame)
        button_row1.pack(fill='x', pady=(0, 5))
        
        ttk.Button(button_row1, text="💾 GUARDAR FACTURA", command=self.guardar_factura, style='Accent.TButton').pack(side='left', padx=(0, 15))
        ttk.Button(button_row1, text="📄 Importar desde Presupuesto", command=self.importar_desde_presupuesto, style='Success.TButton').pack(side='left', padx=(0, 10))
        ttk.Button(button_row1, text="👁️ Vista Previa PDF", command=self.vista_previa_factura_pdf).pack(side='left', padx=(0, 10))
        ttk.Button(button_row1, text="🧹 Limpiar Todo", command=self.limpiar_factura, style='Warning.TButton').pack(side='left')
        
        # Segunda fila de botones - gestión de carpeta
        button_row2 = ttk.Frame(button_frame)
        button_row2.pack(fill='x')
        
        ttk.Button(button_row2, text="📁 Elegir Carpeta PDFs", command=self.elegir_carpeta_facturas, style='Small.TButton').pack(side='left', padx=(0, 8))
        ttk.Button(button_row2, text="📂 Abrir Carpeta PDFs", command=self.abrir_carpeta_facturas, style='Small.TButton').pack(side='left', padx=(0, 8))
        
        # Label para mostrar la carpeta actual de facturas
        self.carpeta_facturas_crear_label = ttk.Label(button_row2, text="Carpeta Facturas: No configurada",
                                                      font=('Arial', 8), foreground='gray')
        self.carpeta_facturas_crear_label.pack(side='left', padx=(15, 0))
        
        # Lista para almacenar items de la factura
        self.factura_items = []
        
        # Actualizar combo de clientes y materiales
        self.actualizar_combo_clientes_factura()
        self.actualizar_combo_materiales_factura()
        
        # Generar número de factura automático al inicio
        self.generar_numero_factura_auto()
        
        # Actualizar label de carpeta de facturas
        self.actualizar_label_carpeta_facturas()
    
    def set_fecha_vencimiento(self, dias):
        """Establece la fecha de vencimiento sumando días a la fecha actual"""
        fecha_vencimiento = factura_manager.calcular_fecha_vencimiento(dias)
        self.fecha_vencimiento_var.set(fecha_vencimiento)
    
    def create_ver_facturas_section(self):
        """Crea la sección para ver facturas existentes"""
        # Canvas y scrollbar para scroll vertical
        canvas = tk.Canvas(self.ver_facturas_frame, bg='#ecf0f1', highlightthickness=0)
        scrollbar = ttk.Scrollbar(self.ver_facturas_frame, orient='vertical', command=canvas.yview)
        scrollable_frame = ttk.Frame(canvas)
        
        scrollable_frame.bind(
            "<Configure>",
            lambda e: canvas.configure(scrollregion=canvas.bbox("all"))
        )
        
        canvas.create_window((0, 0), window=scrollable_frame, anchor='nw')
        canvas.configure(yscrollcommand=scrollbar.set)
        
        # Pack canvas y scrollbar
        canvas.pack(side='left', fill='both', expand=True)
        scrollbar.pack(side='right', fill='y')
        
        # Configurar scroll con rueda del ratón
        def _on_mousewheel(event):
            canvas.yview_scroll(int(-1*(event.delta/120)), "units")
        canvas.bind_all("<MouseWheel>", _on_mousewheel)
        
        # Frame principal dentro del scrollable_frame
        main_frame = ttk.Frame(scrollable_frame)
        main_frame.pack(fill='both', expand=True, padx=10, pady=10)
        
        # Frame para búsqueda y filtros
        search_filter_frame = ttk.LabelFrame(main_frame, text="Búsqueda y Filtros", padding=10)
        search_filter_frame.pack(fill='x', pady=(0, 10))
        
        # Búsqueda
        search_frame = ttk.Frame(search_filter_frame)
        search_frame.pack(fill='x', pady=(0, 10))
        
        ttk.Label(search_frame, text="🔍 Buscar:").pack(side='left', padx=(0, 10))
        self.factura_busqueda_entry = ttk.Entry(search_frame, width=40)
        self.factura_busqueda_entry.pack(side='left', padx=(0, 10))
        self.factura_busqueda_entry.bind('<KeyRelease>', self.buscar_facturas)
        
        ttk.Button(search_frame, text="🔍 Buscar", command=self.buscar_facturas).pack(side='left', padx=(0, 20))
        ttk.Button(search_frame, text="🧹 Limpiar", command=self.limpiar_busqueda_facturas).pack(side='left')
        
        # Filtros de estado
        filter_frame = ttk.Frame(search_filter_frame)
        filter_frame.pack(fill='x')
        
        # Primera línea: Filtro de estado
        filter_line1 = ttk.Frame(filter_frame)
        filter_line1.pack(fill='x', pady=(0, 5))
        
        ttk.Label(filter_line1, text="📊 Filtrar por estado:").pack(side='left', padx=(0, 10))
        
        self.factura_estado_filtro_var = tk.StringVar(value="Todos")
        self.factura_estado_combo = ttk.Combobox(filter_line1, textvariable=self.factura_estado_filtro_var,
                                                values=["Todos", "Pagada", "No Pagada"],
                                                state="readonly", width=15)
        self.factura_estado_combo.pack(side='left', padx=(0, 10))
        self.factura_estado_combo.bind('<<ComboboxSelected>>', self.filtrar_facturas_por_estado)
        
        # Segunda línea: Botones de cambio de estado
        filter_line2 = ttk.Frame(filter_frame)
        filter_line2.pack(fill='x')
        
        ttk.Button(filter_line2, text="✅ Marcar como Pagada", command=self.marcar_factura_pagada, style='Success.TButton').pack(side='left', padx=(0, 5))
        ttk.Button(filter_line2, text="❌ Marcar como No Pagada", command=self.marcar_factura_no_pagada, style='Warning.TButton').pack(side='left')

        # Tercera línea: Filtros por fecha
        date_filter_frame = ttk.Frame(filter_frame)
        date_filter_frame.pack(fill='x', pady=(10, 0))

        ttk.Label(date_filter_frame, text="🗓️ Mes:").pack(side='left', padx=(0, 10))
        self.factura_mes_filtro_var = tk.StringVar(value="Todos")
        self.factura_mes_combo = ttk.Combobox(
            date_filter_frame,
            textvariable=self.factura_mes_filtro_var,
            values=["Todos"],
            state="readonly",
            width=20
        )
        self.factura_mes_combo.pack(side='left', padx=(0, 10))
        self.factura_mes_combo.bind('<<ComboboxSelected>>', self.buscar_facturas)

        ttk.Label(date_filter_frame, text="Año:").pack(side='left', padx=(0, 10))
        self.factura_anio_filtro_var = tk.StringVar(value="Todos")
        self.factura_anio_combo = ttk.Combobox(
            date_filter_frame,
            textvariable=self.factura_anio_filtro_var,
            values=["Todos"],
            state="readonly",
            width=10
        )
        self.factura_anio_combo.pack(side='left', padx=(0, 10))
        self.factura_anio_combo.bind('<<ComboboxSelected>>', self.on_factura_anio_cambiado)

        # Inicializar valores de filtros de fecha
        self.cargar_filtros_fecha_facturas()
        
        # Treeview para facturas
        columns = ('ID', 'Nº Factura', 'Cliente', 'Fecha', 'Vencimiento', 'Total', 'Estado Pago')
        self.facturas_tree = ttk.Treeview(main_frame, columns=columns, show='headings', height=18)
        
        for col in columns:
            self.facturas_tree.heading(col, text=col)
            if col == 'Estado Pago':
                self.facturas_tree.column(col, width=120)
            else:
                self.facturas_tree.column(col, width=120)
        
        # Scrollbar
        facturas_scrollbar = ttk.Scrollbar(main_frame, orient='vertical', command=self.facturas_tree.yview)
        self.facturas_tree.configure(yscrollcommand=facturas_scrollbar.set)
        
        self.facturas_tree.pack(side='left', fill='both', expand=True)
        facturas_scrollbar.pack(side='right', fill='y')
        
        # Bind para ver detalles
        self.facturas_tree.bind('<Double-1>', self.ver_detalle_factura)
        
        # Botones
        button_frame = ttk.Frame(main_frame)
        button_frame.pack(fill='x', pady=(10, 0))
        
        # Primera fila de botones
        button_row1 = ttk.Frame(button_frame)
        button_row1.pack(fill='x', pady=(0, 5))
        
        ttk.Button(button_row1, text="🔄 Actualizar", command=self.refresh_facturas, style='Small.TButton').pack(side='left', padx=(0, 8))
        ttk.Button(button_row1, text="👁️ Ver Detalle", command=self.ver_detalle_factura, style='Small.TButton').pack(side='left', padx=(0, 8))
        ttk.Button(button_row1, text="📄 Exportar PDF", command=self.exportar_factura_pdf, style='SmallAccent.TButton').pack(side='left', padx=(0, 8))
        ttk.Button(button_row1, text="📧 Enviar por Email", command=self.enviar_email_factura, style='SmallSuccess.TButton').pack(side='left', padx=(0, 8))
        ttk.Button(button_row1, text="⚙️ Configurar Email", command=self.configurar_email).pack(side='left', padx=(0, 8))
        
        # Segunda fila de botones para gestión de carpeta y acciones
        button_row2 = ttk.Frame(button_frame)
        button_row2.pack(fill='x')
        
        ttk.Button(button_row2, text="📁 Elegir Carpeta", command=self.elegir_carpeta_facturas, style='Small.TButton').pack(side='left', padx=(0, 8))
        ttk.Button(button_row2, text="📂 Abrir Carpeta", command=self.abrir_carpeta_facturas, style='Small.TButton').pack(side='left', padx=(0, 8))
        ttk.Button(button_row2, text="🗑️ Eliminar", command=self.eliminar_factura, style='SmallDanger.TButton').pack(side='left', padx=(0, 8))
        
        # Label para mostrar la carpeta actual
        self.carpeta_facturas_label = ttk.Label(button_row2, text="Carpeta Facturas: No configurada",
                                               font=('Arial', 8), foreground='gray')
        self.carpeta_facturas_label.pack(side='left', padx=(15, 0))
        
        self.actualizar_label_carpeta_facturas()
    
    # Métodos auxiliares para facturación
    
    def actualizar_combo_clientes_factura(self):
        """Actualiza el combo de clientes para facturas"""
        clientes = cliente_manager.obtener_clientes()
        cliente_names = [f"{c['id']} - {c['nombre']}" for c in clientes]
        self.factura_cliente_combo['values'] = cliente_names
    
    def actualizar_combo_materiales_factura(self):
        """Actualiza el combo de materiales para facturas"""
        materiales = material_manager.obtener_materiales()
        material_names = [f"{m['id']} - {m['nombre']} ({m['unidad_medida']})" for m in materiales]
        self.factura_material_combo['values'] = material_names
    
    def on_cliente_select_factura(self, event=None):
        """Autocompleta los datos del cliente seleccionado"""
        if not self.factura_cliente_var.get():
            return
        
        try:
            cliente_text = self.factura_cliente_var.get()
            cliente_id = int(cliente_text.split(' - ')[0])
            cliente = cliente_manager.obtener_cliente_por_id(cliente_id)
            
            if cliente:
                # Poblar Entry widgets con información del cliente
                self.factura_cliente_telefono_var.set(cliente.get('telefono', ''))
                self.factura_cliente_email_var.set(cliente.get('email', ''))
                self.factura_cliente_direccion_var.set(cliente.get('direccion', ''))
        except Exception as e:
            print(f"Error al cargar datos del cliente: {e}")
    
    def generar_numero_factura_auto(self):
        """Genera un número de factura automático"""
        numero = factura_manager.generar_numero_factura()
        self.numero_factura_var.set(numero)
    
    def agregar_item_factura(self):
        """Agrega un material a la factura"""
        if not self.factura_cliente_var.get():
            messagebox.showwarning("Advertencia", "Seleccione un cliente primero")
            return
        
        if not self.factura_material_var.get():
            messagebox.showwarning("Advertencia", "Seleccione un material")
            return
        
        cantidad_str = self.factura_cantidad_entry.get().strip()
        if not cantidad_str:
            messagebox.showwarning("Advertencia", "Ingrese la cantidad")
            return
        
        try:
            cantidad = float(cantidad_str)
            if cantidad <= 0:
                messagebox.showerror("Error", "La cantidad debe ser mayor a 0")
                return
            
            # Obtener material
            material_text = self.factura_material_var.get()
            material_id = int(material_text.split(' - ')[0])
            material = self.materiales_data[material_id]
            
            # Crear item
            item = {
                'material_id': material_id,
                'tarea_manual': '',
                'material_nombre': material['nombre'],
                'unidad_medida': material['unidad_medida'],
                'cantidad': cantidad,
                'precio_unitario': material['precio_unitario'],
                'subtotal': cantidad * material['precio_unitario'],
                'visible_pdf': 1,
                'es_tarea_manual': 0,
                'aplica_iva': True,
                'descuento_porcentaje': 0,
                'descuento_fijo': 0
            }
            
            self.factura_items.append(item)
            self.actualizar_tree_items_factura()
            self.calcular_totales_factura()
            
            # Limpiar campos
            self.factura_material_var.set('')
            self.factura_cantidad_entry.delete(0, tk.END)
            
        except ValueError:
            messagebox.showerror("Error", "La cantidad debe ser un número válido")
        except Exception as e:
            messagebox.showerror("Error", f"Error al agregar item: {str(e)}")
    
    def agregar_tarea_factura(self):
        """Agrega una tarea manual a la factura"""
        if not self.factura_cliente_var.get():
            messagebox.showwarning("Advertencia", "Seleccione un cliente primero")
            return
        
        descripcion = self.factura_tarea_descripcion_entry.get().strip()
        cantidad_str = self.factura_tarea_cantidad_entry.get().strip()
        precio_str = self.factura_tarea_precio_entry.get().strip()
        
        if not descripcion or not cantidad_str or not precio_str:
            messagebox.showwarning("Advertencia", "Todos los campos son obligatorios")
            return
        
        try:
            cantidad = float(cantidad_str)
            precio = float(precio_str)
            
            if cantidad <= 0 or precio < 0:
                messagebox.showerror("Error", "La cantidad debe ser mayor a 0 y el precio no puede ser negativo")
                return
            
            # Crear tarea manual
            item = {
                'material_id': None,
                'tarea_manual': descripcion,
                'material_nombre': '',
                'unidad_medida': 'unidad',
                'cantidad': cantidad,
                'precio_unitario': precio,
                'subtotal': cantidad * precio,
                'visible_pdf': 1,
                'es_tarea_manual': 1,
                'aplica_iva': True,
                'descuento_porcentaje': 0,
                'descuento_fijo': 0
            }
            
            self.factura_items.append(item)
            self.actualizar_tree_items_factura()
            self.calcular_totales_factura()
            
            # Limpiar campos
            self.factura_tarea_descripcion_entry.delete(0, tk.END)
            self.factura_tarea_cantidad_entry.delete(0, tk.END)
            self.factura_tarea_precio_entry.delete(0, tk.END)
            
        except ValueError:
            messagebox.showerror("Error", "La cantidad y el precio deben ser números válidos")
        except Exception as e:
            messagebox.showerror("Error", f"Error al agregar tarea: {str(e)}")
    
    def actualizar_tree_items_factura(self):
        """Actualiza el tree view de items de la factura"""
        for item in self.factura_items_tree.get_children():
            self.factura_items_tree.delete(item)
        
        for item in self.factura_items:
            # Determinar tipo y descripción
            if item.get('es_tarea_manual', 0):
                tipo = "Tarea"
                descripcion = item.get('tarea_manual', 'Tarea manual')
            else:
                tipo = "Material"
                descripcion = f"{item['material_nombre']} ({item['unidad_medida']})"
            
            # Checkbox de visibilidad e IVA
            visible = "✓" if item.get('visible_pdf', 1) else "✗"
            aplica_iva = "✓" if item.get('aplica_iva', True) else "✗"
            
            # Calcular descuentos
            descuento_pct = item.get('descuento_porcentaje', 0)
            descuento_fijo = item.get('descuento_fijo', 0)
            
            descuento_pct_text = f"{descuento_pct:.1f}%" if descuento_pct > 0 else ""
            descuento_fijo_text = f"€{descuento_fijo:.2f}" if descuento_fijo > 0 else ""
            
            self.factura_items_tree.insert('', 'end', values=(
                visible,
                aplica_iva,
                tipo,
                descripcion,
                f"{item['cantidad']:.2f}",
                f"€{item['precio_unitario']:.2f}",
                descuento_pct_text,
                descuento_fijo_text,
                f"€{item['subtotal']:.2f}"
            ))
        
        # Actualizar contador
        total_items = len(self.factura_items)
        visible_items = sum(1 for item in self.factura_items if item.get('visible_pdf', 1))
        self.factura_items_info_label.config(text=f"Items: {total_items} (Visibles: {visible_items})")
    
    def calcular_totales_factura(self):
        """Calcula los totales de la factura"""
        if not self.factura_items:
            self.factura_subtotal_label.config(text="€0.00")
            self.factura_iva_label.config(text="€0.00")
            self.factura_total_label.config(text="€0.00")
            self.factura_descuento_label.config(text="")
            self.factura_items_info_label.config(text="Items: 0")
            return
        
        # Obtener descuentos globales
        try:
            descuento_porcentaje = float(self.factura_descuento_porcentaje_var.get() or 0)
        except ValueError:
            descuento_porcentaje = 0
        
        try:
            descuento_fijo = float(self.factura_descuento_fijo_var.get() or 0)
        except ValueError:
            descuento_fijo = 0
        
        # Verificar si hay items con IVA habilitado
        items_con_iva = [item for item in self.factura_items if item.get('aplica_iva', True)]
        iva_realmente_habilitado = len(items_con_iva) > 0 and self.factura_iva_habilitado_var.get()
        
        # Obtener retención IRPF
        try:
            retencion_irpf = float(self.retencion_irpf_var.get() or 0) if self.retencion_irpf_var.get().strip() else None
        except ValueError:
            retencion_irpf = None
        
        # Calcular totales usando el nuevo método del backend
        totales = factura_manager.calcular_totales_completo(
            self.factura_items,
            descuento_porcentaje,
            descuento_fijo,
            self.factura_descuento_antes_iva_var.get(),
            self.factura_iva_habilitado_var.get(),
            retencion_irpf
        )
        
        # Actualizar labels
        self.factura_subtotal_label.config(text=f"€{totales['subtotal']:.2f}")
        self.factura_iva_label.config(text=f"€{totales['iva']:.2f}")
        self.factura_total_label.config(text=f"€{totales['total']:.2f}")
        self.factura_items_info_label.config(text=f"Items: {len(self.factura_items)}")
        
        # Mostrar descuentos y retención IRPF si los hay
        descuento_texto = ""
        if totales['descuentos_items'] > 0:
            descuento_texto += f"Desc. Items: -€{totales['descuentos_items']:.2f}"
        if totales['descuento_global'] > 0:
            if descuento_texto:
                descuento_texto += " | "
            descuento_texto += f"Desc. Global: -€{totales['descuento_global']:.2f}"
        if totales.get('retencion_irpf', 0) > 0:
            if descuento_texto:
                descuento_texto += " | "
            descuento_texto += f"Ret. IRPF ({totales.get('retencion_irpf_porcentaje', 0):.1f}%): -€{totales['retencion_irpf']:.2f}"
        
        self.factura_descuento_label.config(text=descuento_texto)
        
        # Cambiar color del IVA según si está habilitado o no
        if iva_realmente_habilitado:
            self.factura_iva_label.config(foreground='black')
            self.factura_iva_checkbox.config(state='normal')
        else:
            self.factura_iva_label.config(foreground='gray')
            # Si ningún item tiene IVA, desactivar el checkbox
            if len(items_con_iva) == 0 and self.factura_items:
                self.factura_iva_checkbox.config(state='disabled')
            else:
                self.factura_iva_checkbox.config(state='normal')
    
    def editar_item_factura(self):
        """Abre diálogo para editar un item de la factura"""
        selection = self.factura_items_tree.selection()
        if not selection:
            messagebox.showwarning("Advertencia", "Seleccione un item para editar")
            return
        
        item = self.factura_items_tree.item(selection[0])
        item_index = self.factura_items_tree.index(selection[0])
        item_data = self.factura_items[item_index]
        
        # Crear ventana de diálogo
        dialog = tk.Toplevel(self.root)
        dialog.title("Editar Item de Factura")
        dialog.geometry("500x400")
        dialog.resizable(False, False)
        dialog.transient(self.root)
        dialog.grab_set()
        
        # Centrar la ventana
        dialog.update_idletasks()
        x = (dialog.winfo_screenwidth() // 2) - (500 // 2)
        y = (dialog.winfo_screenheight() // 2) - (400 // 2)
        dialog.geometry(f"500x400+{x}+{y}")
        
        # Variables
        aplica_iva_var = tk.BooleanVar(value=item_data.get('aplica_iva', True))
        descuento_porcentaje_var = tk.StringVar(value=str(item_data.get('descuento_porcentaje', 0)))
        descuento_fijo_var = tk.StringVar(value=str(item_data.get('descuento_fijo', 0)))
        
        # Frame principal
        main_frame = ttk.Frame(dialog, padding=20)
        main_frame.pack(fill='both', expand=True)
        
        # Información del item
        ttk.Label(main_frame, text=f"Item: {item_data.get('tarea_manual', item_data.get('material_nombre', 'Sin nombre'))}", 
                 font=('Arial', 10, 'bold')).pack(pady=(0, 20))
        
        # IVA
        iva_frame = ttk.Frame(main_frame)
        iva_frame.pack(fill='x', pady=(0, 15))
        ttk.Checkbutton(iva_frame, text="Aplicar IVA (21%)", variable=aplica_iva_var).pack(anchor='w')
        
        # Descuentos
        descuentos_frame = ttk.LabelFrame(main_frame, text="Descuentos", padding=10)
        descuentos_frame.pack(fill='x', pady=(0, 15))
        
        ttk.Label(descuentos_frame, text="Descuento por porcentaje:").grid(row=0, column=0, sticky='w', pady=(0, 5))
        descuento_pct_entry = ttk.Entry(descuentos_frame, textvariable=descuento_porcentaje_var, width=15)
        descuento_pct_entry.grid(row=0, column=1, sticky='w', padx=(10, 0), pady=(0, 5))
        ttk.Label(descuentos_frame, text="%").grid(row=0, column=2, sticky='w', padx=(5, 0), pady=(0, 5))
        
        ttk.Label(descuentos_frame, text="Descuento fijo:").grid(row=1, column=0, sticky='w')
        descuento_fijo_entry = ttk.Entry(descuentos_frame, textvariable=descuento_fijo_var, width=15)
        descuento_fijo_entry.grid(row=1, column=1, sticky='w', padx=(10, 0))
        ttk.Label(descuentos_frame, text="€").grid(row=1, column=2, sticky='w', padx=(5, 0))
        
        ttk.Label(descuentos_frame, text="Nota: El porcentaje tiene prioridad sobre el descuento fijo", 
                 font=('Arial', 8), foreground='gray').grid(row=2, column=0, columnspan=3, sticky='w', pady=(10, 0))
        
        # Botones
        button_frame = ttk.Frame(main_frame)
        button_frame.pack(fill='x', pady=(20, 0))
        
        def guardar_cambios():
            try:
                # Validar descuentos
                descuento_pct = float(descuento_porcentaje_var.get() or 0)
                descuento_fijo = float(descuento_fijo_var.get() or 0)
                
                if descuento_pct < 0 or descuento_pct > 100:
                    messagebox.showerror("Error", "El descuento por porcentaje debe estar entre 0 y 100")
                    return
                
                if descuento_fijo < 0:
                    messagebox.showerror("Error", "El descuento fijo no puede ser negativo")
                    return
                
                # Actualizar item
                self.factura_items[item_index]['aplica_iva'] = aplica_iva_var.get()
                self.factura_items[item_index]['descuento_porcentaje'] = descuento_pct
                self.factura_items[item_index]['descuento_fijo'] = descuento_fijo
                
                # Actualizar visualización
                self.actualizar_tree_items_factura()
                self.calcular_totales_factura()
                
                dialog.destroy()
                
            except ValueError:
                messagebox.showerror("Error", "Por favor ingrese valores numéricos válidos")
        
        ttk.Button(button_frame, text="Guardar", command=guardar_cambios, style='Accent.TButton').pack(side='right', padx=(5, 0))
        ttk.Button(button_frame, text="Cancelar", command=dialog.destroy).pack(side='right')
    
    def on_item_click_factura(self, event):
        """Maneja el clic en los items de factura"""
        region = self.factura_items_tree.identify_region(event.x, event.y)
        if region == "cell":
            column = self.factura_items_tree.identify_column(event.x)
            # Solo cambiar visibilidad si se hace click en la columna "Visible" (primera columna)
            if column == "#1":  # Columna de visibilidad
                self.toggle_visibilidad_item_factura(event)
    
    def on_item_double_click_factura(self, event):
        """Maneja el doble click en los items de factura para abrir diálogo de edición"""
        region = self.factura_items_tree.identify_region(event.x, event.y)
        if region == "cell":
            self.editar_item_factura()
    
    def toggle_visibilidad_item_factura(self, event=None):
        """Cambia la visibilidad de un item de factura"""
        selection = self.factura_items_tree.selection()
        if selection:
            index = self.factura_items_tree.index(selection[0])
            item = self.factura_items[index]
            item['visible_pdf'] = 0 if item.get('visible_pdf', 1) else 1
            self.actualizar_tree_items_factura()
    
    def eliminar_item_factura(self):
        """Elimina un item de la factura"""
        selection = self.factura_items_tree.selection()
        if not selection:
            messagebox.showwarning("Advertencia", "Seleccione un item para eliminar")
            return
        
        if messagebox.askyesno("Confirmar", "¿Está seguro de que desea eliminar este item?"):
            index = self.factura_items_tree.index(selection[0])
            del self.factura_items[index]
            self.actualizar_tree_items_factura()
            self.calcular_totales_factura()
    
    def marcar_todos_items_factura(self):
        """Marca todos los items como visibles"""
        for item in self.factura_items:
            item['visible_pdf'] = 1
        self.actualizar_tree_items_factura()
    
    def desmarcar_todos_items_factura(self):
        """Desmarca todos los items como no visibles"""
        for item in self.factura_items:
            item['visible_pdf'] = 0
        self.actualizar_tree_items_factura()
    
    def guardar_factura(self):
        """Guarda la factura en la base de datos"""
        if not self.factura_cliente_var.get():
            messagebox.showwarning("Advertencia", "Seleccione un cliente")
            return
        
        if not self.factura_items:
            messagebox.showwarning("Advertencia", "Agregue al menos un item a la factura")
            return
        
        if not self.numero_factura_var.get():
            messagebox.showwarning("Advertencia", "Ingrese un número de factura")
            return
        
        try:
            # Obtener ID del cliente
            cliente_text = self.factura_cliente_var.get()
            cliente_id = int(cliente_text.split(' - ')[0])
            
            # Obtener otros datos
            numero_factura = self.numero_factura_var.get().strip()
            fecha_vencimiento = self.fecha_vencimiento_var.get().strip() or None
            metodo_pago = self.metodo_pago_var.get()
            estado_pago = self.estado_pago_var.get()
            notas = self.factura_notas_text.get('1.0', tk.END).strip()
            iva_habilitado = self.factura_iva_habilitado_var.get()
            
            # Obtener descuentos globales
            try:
                descuento_porcentaje = float(self.factura_descuento_porcentaje_var.get() or 0)
            except ValueError:
                descuento_porcentaje = 0
            
            try:
                descuento_fijo = float(self.factura_descuento_fijo_var.get() or 0)
            except ValueError:
                descuento_fijo = 0
            
            # Obtener retención IRPF
            try:
                retencion_irpf = float(self.retencion_irpf_var.get() or 0) if self.retencion_irpf_var.get().strip() else None
            except ValueError:
                retencion_irpf = None
            
            # Validar que el cliente tenga NIF/NIE
            cliente = cliente_manager.obtener_cliente_por_id(cliente_id)
            if not cliente or not cliente.get('dni') or not cliente.get('dni').strip():
                if not messagebox.askyesno("Advertencia", 
                    "El cliente no tiene NIF/NIE/IVA Intracomunitario. ¿Desea continuar de todas formas?"):
                    return
            
            # Crear factura
            factura_id = factura_manager.crear_factura(
                cliente_id=cliente_id,
                items=self.factura_items,
                numero_factura=numero_factura,
                fecha_vencimiento=fecha_vencimiento,
                metodo_pago=metodo_pago,
                estado_pago=estado_pago,
                notas=notas,
                iva_habilitado=iva_habilitado,
                descuento_global_porcentaje=descuento_porcentaje,
                descuento_global_fijo=descuento_fijo,
                descuento_antes_iva=self.factura_descuento_antes_iva_var.get(),
                retencion_irpf=retencion_irpf
            )
            
            messagebox.showinfo("Éxito", f"Factura guardada correctamente. ID: {factura_id}")
            
            # Preguntar si desea generar PDF
            if messagebox.askyesno("Generar PDF", "¿Desea generar un archivo PDF de la factura?"):
                self.generar_pdf_factura_con_nombre_personalizado(factura_id)
            
            self.limpiar_factura()
            self.refresh_facturas()
            
        except Exception as e:
            messagebox.showerror("Error", f"Error al guardar factura: {str(e)}")
    
    def limpiar_factura(self):
        """Limpia el formulario de factura"""
        self.factura_cliente_var.set('')
        self.numero_factura_var.set('')
        self.fecha_vencimiento_var.set('')
        self.metodo_pago_var.set('Transferencia')
        self.estado_pago_var.set('No Pagada')
        self.factura_notas_text.delete('1.0', tk.END)
        self.factura_material_var.set('')
        self.factura_cantidad_entry.delete(0, tk.END)
        self.factura_tarea_descripcion_entry.delete(0, tk.END)
        self.factura_tarea_cantidad_entry.delete(0, tk.END)
        self.factura_tarea_precio_entry.delete(0, tk.END)
        self.factura_iva_habilitado_var.set(True)
        self.retencion_irpf_var.set("")
        self.factura_items.clear()
        self.actualizar_tree_items_factura()
        self.calcular_totales_factura()
        self.generar_numero_factura_auto()
        
        # Limpiar info del cliente (Entry widgets)
        self.factura_cliente_telefono_var.set("")
        self.factura_cliente_email_var.set("")
        self.factura_cliente_direccion_var.set("")
    
    def importar_desde_presupuesto(self):
        """Importa datos desde un presupuesto existente"""
        # Crear ventana de diálogo
        dialog = tk.Toplevel(self.root)
        dialog.title("Importar desde Presupuesto")
        dialog.geometry("900x600")
        dialog.transient(self.root)
        dialog.grab_set()
        
        # Centrar ventana
        dialog.geometry("+%d+%d" % (
            self.root.winfo_rootx() + 100,
            self.root.winfo_rooty() + 50
        ))
        
        # Frame principal
        main_frame = ttk.Frame(dialog, padding=10)
        main_frame.pack(fill='both', expand=True)
        
        ttk.Label(main_frame, text="Seleccione un presupuesto para convertir en factura:", 
                 font=('Arial', 10, 'bold')).pack(pady=(0, 10))
        
        # Treeview para presupuestos
        columns = ('ID', 'Cliente', 'Fecha', 'Total', 'Estado')
        presupuestos_tree = ttk.Treeview(main_frame, columns=columns, show='headings', height=15)
        
        for col in columns:
            presupuestos_tree.heading(col, text=col)
            presupuestos_tree.column(col, width=150)
        
        scrollbar = ttk.Scrollbar(main_frame, orient='vertical', command=presupuestos_tree.yview)
        presupuestos_tree.configure(yscrollcommand=scrollbar.set)
        
        presupuestos_tree.pack(side='left', fill='both', expand=True)
        scrollbar.pack(side='right', fill='y')
        
        # Cargar presupuestos
        presupuestos = presupuesto_manager.obtener_presupuestos()
        for p in presupuestos:
            presupuestos_tree.insert('', 'end', values=(
                p['id'],
                p['cliente_nombre'],
                p['fecha_creacion'][:10],
                f"€{p['total']:.2f}",
                p.get('estado', 'Pendiente')
            ))
        
        # Botones
        button_frame = ttk.Frame(dialog)
        button_frame.pack(fill='x', pady=(10, 0))
        
        def importar():
            selection = presupuestos_tree.selection()
            if not selection:
                messagebox.showwarning("Advertencia", "Seleccione un presupuesto")
                return
            
            presupuesto_id = presupuestos_tree.item(selection[0])['values'][0]
            presupuesto = presupuesto_manager.obtener_presupuesto_por_id(presupuesto_id)
            
            if presupuesto:
                # Cargar datos del presupuesto
                # Seleccionar cliente
                cliente_id = presupuesto['cliente_id']
                self.factura_cliente_var.set(f"{cliente_id} - {presupuesto['cliente_nombre']}")
                self.on_cliente_select_factura()
                
                # Poblar Entry widgets con datos del cliente del presupuesto (sobrescribir si hay datos en el presupuesto)
                if presupuesto.get('telefono'):
                    self.factura_cliente_telefono_var.set(presupuesto['telefono'])
                if presupuesto.get('email'):
                    self.factura_cliente_email_var.set(presupuesto['email'])
                if presupuesto.get('direccion'):
                    self.factura_cliente_direccion_var.set(presupuesto['direccion'])
                
                # Cargar items
                self.factura_items = presupuesto['items'].copy()
                self.actualizar_tree_items_factura()
                
                # Cargar IVA
                self.factura_iva_habilitado_var.set(presupuesto.get('iva_habilitado', True))
                
                # Calcular totales
                self.calcular_totales_factura()
                
                # Generar nuevo número de factura
                self.generar_numero_factura_auto()
                
                # Establecer fecha de vencimiento (30 días desde hoy)
                fecha_venc = (datetime.now() + timedelta(days=30)).strftime('%Y-%m-%d')
                self.fecha_vencimiento_var.set(fecha_venc)
                
                messagebox.showinfo("Éxito", f"Presupuesto #{presupuesto_id} importado correctamente")
                dialog.destroy()
        
        ttk.Button(button_frame, text="✅ Importar", command=importar, style='Accent.TButton').pack(side='right', padx=(10, 0))
        ttk.Button(button_frame, text="❌ Cancelar", command=dialog.destroy).pack(side='right')
    
    def vista_previa_factura_pdf(self):
        """Genera una vista previa del PDF de la factura"""
        if not self.factura_cliente_var.get():
            messagebox.showwarning("Advertencia", "Seleccione un cliente primero")
            return
        
        if not self.factura_items:
            messagebox.showwarning("Advertencia", "Agregue al menos un item a la factura")
            return
        
        try:
            # Obtener datos
            cliente_text = self.factura_cliente_var.get()
            cliente_id = int(cliente_text.split(' - ')[0])
            cliente = cliente_manager.obtener_cliente_por_id(cliente_id)
            
            if not cliente:
                messagebox.showerror("Error", "No se pudo obtener la información del cliente")
                return
            
            # Obtener valores de Entry widgets editables (usar como prioridad sobre BD)
            telefono_editado = self.factura_cliente_telefono_var.get().strip()
            email_editado = self.factura_cliente_email_var.get().strip()
            direccion_editada = self.factura_cliente_direccion_var.get().strip()
            
            # Usar valores editados si están presentes, sino usar valores de BD como fallback
            telefono_final = telefono_editado if telefono_editado else cliente.get('telefono', '')
            email_final = email_editado if email_editado else cliente.get('email', '')
            direccion_final = direccion_editada if direccion_editada else cliente.get('direccion', '')
            
            # Obtener descuentos globales
            try:
                descuento_porcentaje = float(self.factura_descuento_porcentaje_var.get() or 0)
            except ValueError:
                descuento_porcentaje = 0
            
            try:
                descuento_fijo = float(self.factura_descuento_fijo_var.get() or 0)
            except ValueError:
                descuento_fijo = 0
            
            # Calcular totales usando el mismo método que el resto de la aplicación
            totales = factura_manager.calcular_totales_completo(
                self.factura_items,
                descuento_porcentaje,
                descuento_fijo,
                self.factura_descuento_antes_iva_var.get(),
                self.factura_iva_habilitado_var.get()
            )
            
            # Normalizar items para PDF (asegurar banderas de IVA y porcentajes)
            items_para_pdf = []
            iva_porcentaje_general = totales.get('iva_porcentaje', factura_manager.iva_porcentaje)
            for item in self.factura_items:
                item_copy = item.copy()
                aplica_iva = bool(item_copy.get('aplica_iva', True))
                item_copy['aplica_iva'] = aplica_iva
                
                iva_item = item_copy.get('iva_porcentaje')
                if iva_item is None:
                    iva_item = iva_porcentaje_general if aplica_iva else 0.0
                try:
                    iva_item = float(iva_item)
                except (TypeError, ValueError):
                    iva_item = iva_porcentaje_general if aplica_iva else 0.0
                item_copy['iva_porcentaje'] = iva_item
                
                if 'subtotal' not in item_copy and 'subtotal_linea' in item_copy:
                    item_copy['subtotal'] = item_copy['subtotal_linea']
                item_copy['subtotal_linea'] = item_copy.get('subtotal', item_copy.get('cantidad', 0) * item_copy.get('precio_unitario', 0))
                
                items_para_pdf.append(item_copy)
            
            # Crear factura temporal usando valores editados
            factura_temp = {
                'id': 'VISTA_PREVIA',
                'numero_factura': self.numero_factura_var.get() or 'PREVIEW',
                'cliente_nombre': cliente['nombre'],
                'telefono': telefono_final,
                'email': email_final,
                'direccion': direccion_final,
                'dni': cliente.get('dni', ''),
                'fecha_creacion': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
                'fecha_vencimiento': self.fecha_vencimiento_var.get() or '',
                'subtotal': totales['subtotal'],
                'iva': totales['iva'],
                'total': totales['total'],
                'iva_habilitado': self.factura_iva_habilitado_var.get(),
                'metodo_pago': self.metodo_pago_var.get(),
                'estado_pago': self.estado_pago_var.get(),
                'notas': self.factura_notas_text.get('1.0', tk.END).strip(),
                'items': items_para_pdf,
                'iva_porcentaje': iva_porcentaje_general,
                'iva_breakdown': totales.get('iva_breakdown', {}),
                'descuento_global_calculado': totales.get('descuento_global', 0.0),
                'descuentos_items_calculados': totales.get('descuentos_items', 0.0),
                'base_imponible_calculada': totales.get('base_imponible', totales['subtotal']),
                'base_exenta': totales.get('base_exenta', 0.0),
                'retencion_irpf': totales.get('retencion_irpf_porcentaje', 0.0),
                'retencion_irpf_importe': totales.get('retencion_irpf', 0.0)
            }
            
            # Generar PDF temporal
            archivo_pdf_path = os.path.join(self.carpeta_facturas, "vista_previa_factura_temp.pdf")
            
            # Crear carpeta si no existe
            os.makedirs(self.carpeta_facturas, exist_ok=True)
            
            archivo_pdf = self.pdf_generator.generate_factura_pdf(factura_temp, archivo_pdf_path)
            
            # Preguntar si desea abrir
            if messagebox.askyesno("Vista Previa", f"PDF generado exitosamente.\n¿Desea abrir el archivo?"):
                try:
                    if platform.system() == 'Windows':
                        os.startfile(archivo_pdf)
                    elif platform.system() == 'Darwin':
                        subprocess.run(['open', archivo_pdf])
                    else:
                        subprocess.run(['xdg-open', archivo_pdf])
                except Exception as e:
                    messagebox.showinfo("Información", f"Archivo guardado en: {os.path.abspath(archivo_pdf)}")
            
        except Exception as e:
            import traceback
            error_details = traceback.format_exc()
            messagebox.showerror("Error", f"Error al generar vista previa:\n{str(e)}\n\nDetalles:\n{error_details}")
    
    def generar_pdf_factura_con_nombre_personalizado(self, factura_id):
        """Genera PDF de factura con nombre personalizado"""
        try:
            # Obtener factura
            factura = factura_manager.obtener_factura_por_id(factura_id)
            
            if not factura:
                messagebox.showerror("Error", "No se pudo obtener la factura")
                return
            
            # Crear ventana de diálogo para el nombre
            nombre_dialog = tk.Toplevel(self.root)
            nombre_dialog.title("Nombre del archivo PDF")
            nombre_dialog.geometry("600x250")
            nombre_dialog.resizable(False, False)
            nombre_dialog.transient(self.root)
            nombre_dialog.grab_set()
            
            # Centrar la ventana
            nombre_dialog.geometry("+%d+%d" % (
                self.root.winfo_rootx() + 50,
                self.root.winfo_rooty() + 50
            ))
            
            # Frame principal
            main_frame = ttk.Frame(nombre_dialog, padding=20)
            main_frame.pack(fill='both', expand=True)
            
            # Información
            info_label = ttk.Label(main_frame,
                                  text=f"Factura {factura['numero_factura']} - {factura['cliente_nombre']}",
                                  font=('Arial', 10, 'bold'))
            info_label.pack(pady=(0, 15))
            
            # Label y entry para el nombre
            ttk.Label(main_frame, text="Nombre del archivo PDF:", font=('Arial', 9)).pack(anchor='w', pady=(0, 5))
            
            # Generar nombre sugerido
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            numero_limpio = factura['numero_factura'].replace('/', '_').replace('\\', '_').replace(' ', '_')
            nombre_sugerido = f"factura_{numero_limpio}_{factura['cliente_nombre'].replace(' ', '_')}_{timestamp}"
            
            nombre_var = tk.StringVar(value=nombre_sugerido)
            nombre_entry = ttk.Entry(main_frame, textvariable=nombre_var, width=50, font=('Arial', 9))
            nombre_entry.pack(fill='x', pady=(0, 10))
            nombre_entry.select_range(0, tk.END)
            nombre_entry.focus()
            
            # Información adicional
            info_text = ttk.Label(main_frame,
                                text="• El archivo se guardará en la carpeta configurada\n• No incluya la extensión .pdf (se agregará automáticamente)",
                                font=('Arial', 8),
                                foreground='gray')
            info_text.pack(anchor='w', pady=(0, 15))
            
            # Variable para resultado
            resultado = {'nombre': None, 'cancelado': False}
            
            def confirmar():
                nombre = nombre_var.get().strip()
                if not nombre:
                    messagebox.showwarning("Advertencia", "Ingrese un nombre para el archivo")
                    return
                
                # Limpiar caracteres no válidos
                import re
                nombre_limpio = re.sub(r'[<>:"/\\|?*]', '_', nombre)
                if nombre_limpio != nombre:
                    if messagebox.askyesno("Confirmar", f"El nombre contiene caracteres no válidos.\n¿Usar '{nombre_limpio}' en su lugar?"):
                        nombre = nombre_limpio
                    else:
                        return
                
                resultado['nombre'] = nombre
                nombre_dialog.destroy()
            
            def cancelar():
                resultado['cancelado'] = True
                nombre_dialog.destroy()
            
            # Botones
            button_frame = ttk.Frame(main_frame)
            button_frame.pack(fill='x')
            
            ttk.Button(button_frame, text="Generar PDF", command=confirmar, style='Accent.TButton').pack(side='right', padx=(10, 0))
            ttk.Button(button_frame, text="Cancelar", command=cancelar).pack(side='right')
            
            # Bind Enter
            nombre_entry.bind('<Return>', lambda e: confirmar())
            
            # Esperar
            nombre_dialog.wait_window()
            
            # Verificar si se canceló
            if resultado['cancelado']:
                return
            
            # Generar el PDF
            nombre_archivo = f"{resultado['nombre']}.pdf"
            
            # Crear carpeta si no existe
            os.makedirs(self.carpeta_facturas, exist_ok=True)
            
            ruta_completa = os.path.join(self.carpeta_facturas, nombre_archivo)
            
            # Verificar si existe
            if os.path.exists(ruta_completa):
                if not messagebox.askyesno("Archivo existente", f"El archivo '{nombre_archivo}' ya existe.\n¿Desea sobrescribirlo?"):
                    return
            
            # Generar PDF
            archivo_pdf = self.pdf_generator.generate_factura_pdf(factura, ruta_completa)
            
            # Mostrar mensaje
            messagebox.showinfo("Éxito", f"PDF generado correctamente:\n{archivo_pdf}")
            
            # Preguntar si desea abrir
            if messagebox.askyesno("Abrir PDF", "¿Desea abrir el archivo PDF generado?"):
                try:
                    if platform.system() == 'Windows':
                        os.startfile(archivo_pdf)
                    elif platform.system() == 'Darwin':
                        subprocess.run(['open', archivo_pdf])
                    else:
                        subprocess.run(['xdg-open', archivo_pdf])
                except Exception as e:
                    messagebox.showinfo("Información", f"Archivo guardado en: {os.path.abspath(archivo_pdf)}")
            
        except Exception as e:
            messagebox.showerror("Error", f"Error al generar PDF: {str(e)}")
    
    # Métodos para ver facturas
    
    def refresh_facturas(self):
        """Actualiza la lista de facturas"""
        if hasattr(self, 'factura_mes_combo') and hasattr(self, 'factura_anio_combo'):
            self.cargar_filtros_fecha_facturas(mantener_seleccion=True)
        self.buscar_facturas()
    
    def actualizar_tree_facturas(self, facturas):
        """Actualiza el treeview de facturas"""
        for item in self.facturas_tree.get_children():
            self.facturas_tree.delete(item)
        
        for factura in facturas:
            fecha = factura['fecha_creacion'][:10]
            fecha_venc = factura.get('fecha_vencimiento', '')
            if fecha_venc and len(fecha_venc) > 10:
                fecha_venc = fecha_venc[:10]
            
            estado = factura.get('estado_pago', 'No Pagada')
            
            item = self.facturas_tree.insert('', 'end', values=(
                factura['id'],
                factura.get('numero_factura', f"F{factura['id']:04d}"),
                factura['cliente_nombre'],
                fecha,
                fecha_venc or 'N/A',
                f"€{factura['total']:.2f}",
                estado
            ))
            
            # Colorear según estado
            if estado == 'Pagada':
                self.facturas_tree.set(item, 'Estado Pago', '✅ Pagada')
            else:
                self.facturas_tree.set(item, 'Estado Pago', '❌ No Pagada')
    
    def buscar_facturas(self, event=None):
        """Busca facturas por término"""
        termino = self.factura_busqueda_entry.get().strip()
        estado_filtro = self.factura_estado_filtro_var.get()
        anio_param = self._obtener_anio_desde_combo(getattr(self, 'factura_anio_filtro_var', None))
        mes_param = self._obtener_mes_desde_combo(getattr(self, 'factura_mes_filtro_var', None))
        
        if termino:
            facturas = factura_manager.buscar_facturas(
                termino,
                anio=anio_param,
                mes=mes_param
            )
        else:
            facturas = factura_manager.obtener_facturas(
                anio=anio_param,
                mes=mes_param
            )
        
        # Aplicar filtro de estado
        if estado_filtro != "Todos":
            facturas = [f for f in facturas if f.get('estado_pago', 'No Pagada') == estado_filtro]
        
        self.actualizar_tree_facturas(facturas)
    
    def limpiar_busqueda_facturas(self):
        """Limpia la búsqueda de facturas"""
        self.factura_busqueda_entry.delete(0, tk.END)
        self.factura_estado_filtro_var.set("Todos")
        if hasattr(self, 'factura_anio_filtro_var'):
            self.factura_anio_filtro_var.set("Todos")
        if hasattr(self, 'factura_mes_filtro_var'):
            self.factura_mes_filtro_var.set("Todos")
        self.cargar_filtros_fecha_facturas()
        self.refresh_facturas()
    
    def filtrar_facturas_por_estado(self, event=None):
        """Filtra facturas por estado de pago"""
        self.buscar_facturas()

    def on_factura_anio_cambiado(self, event=None):
        """Actualiza los meses disponibles al cambiar el año en facturas"""
        self.cargar_filtros_fecha_facturas()
        self.buscar_facturas()

    def cargar_filtros_fecha_facturas(self, mantener_seleccion: bool = False):
        """Carga los combos de mes/año para facturas"""
        if not hasattr(self, 'factura_mes_combo'):
            return

        mes_actual = self.factura_mes_filtro_var.get() if mantener_seleccion else "Todos"
        anio_actual = self.factura_anio_filtro_var.get() if hasattr(self, 'factura_anio_filtro_var') else "Todos"

        anios_disponibles = factura_manager.obtener_anios_facturas()
        anios_values = ["Todos"] + anios_disponibles
        self.factura_anio_combo['values'] = anios_values

        if anio_actual not in anios_values:
            anio_actual = "Todos"
        self.factura_anio_filtro_var.set(anio_actual)

        anio_param = self._obtener_anio_desde_combo(self.factura_anio_filtro_var)

        meses_disponibles = factura_manager.obtener_meses_facturas(anio_param)
        meses_values = ["Todos"] + [self._formatear_mes_opcion(mes) for mes in meses_disponibles]
        self.factura_mes_combo['values'] = meses_values

        if mantener_seleccion and mes_actual in meses_values:
            self.factura_mes_filtro_var.set(mes_actual)
        else:
            self.factura_mes_filtro_var.set("Todos")
    
    def ver_detalle_factura(self, event=None):
        """Muestra el detalle de una factura"""
        selection = self.facturas_tree.selection()
        if not selection:
            messagebox.showwarning("Advertencia", "Seleccione una factura para ver")
            return
        
        factura_id = self.facturas_tree.item(selection[0])['values'][0]
        factura = factura_manager.obtener_factura_por_id(factura_id)
        
        if factura:
            self.mostrar_detalle_factura(factura)
    
    def mostrar_detalle_factura(self, factura):
        """Muestra una ventana con el detalle de la factura"""
        # Crear ventana
        detalle_window = tk.Toplevel(self.root)
        detalle_window.title(f"Detalle Factura {factura.get('numero_factura', factura['id'])}")
        detalle_window.geometry("950x750")
        
        # Frame principal
        main_frame = ttk.Frame(detalle_window, padding=10)
        main_frame.pack(fill='both', expand=True)
        
        # Información de la factura
        info_frame = ttk.LabelFrame(main_frame, text="Información de la Factura", padding=8)
        info_frame.pack(fill='x', pady=(0, 8))
        
        ttk.Label(info_frame, text=f"Número de Factura: {factura.get('numero_factura', 'N/A')}", font=('Arial', 10, 'bold')).pack(anchor='w')
        ttk.Label(info_frame, text=f"Fecha: {factura['fecha_creacion'][:10]}").pack(anchor='w')
        ttk.Label(info_frame, text=f"Vencimiento: {factura.get('fecha_vencimiento', 'N/A')}").pack(anchor='w')
        ttk.Label(info_frame, text=f"Método de Pago: {factura.get('metodo_pago', 'N/A')}").pack(anchor='w')
        
        estado = factura.get('estado_pago', 'No Pagada')
        color = '#27ae60' if estado == 'Pagada' else '#e74c3c'
        estado_label = ttk.Label(info_frame, text=f"Estado: {estado}", foreground=color, font=('Arial', 10, 'bold'))
        estado_label.pack(anchor='w')
        
        # Información del cliente
        cliente_frame = ttk.LabelFrame(main_frame, text="Información del Cliente", padding=8)
        cliente_frame.pack(fill='x', pady=(0, 8))
        
        ttk.Label(cliente_frame, text=f"Nombre: {factura['cliente_nombre']}").pack(anchor='w')
        ttk.Label(cliente_frame, text=f"Teléfono: {factura['telefono'] or 'N/A'}").pack(anchor='w')
        ttk.Label(cliente_frame, text=f"Email: {factura['email'] or 'N/A'}").pack(anchor='w')
        ttk.Label(cliente_frame, text=f"Dirección: {factura['direccion'] or 'N/A'}").pack(anchor='w')
        
        # Items
        items_frame = ttk.LabelFrame(main_frame, text="Items de la Factura", padding=8)
        items_frame.pack(fill='both', expand=True, pady=(0, 8))
        
        columns = ('Tipo', 'Descripción', 'Cantidad', 'Precio Unit.', 'Subtotal')
        items_tree = ttk.Treeview(items_frame, columns=columns, show='headings', height=6)
        
        for col in columns:
            items_tree.heading(col, text=col)
            if col == 'Tipo':
                items_tree.column(col, width=70)
            elif col == 'Descripción':
                items_tree.column(col, width=200)
            else:
                items_tree.column(col, width=100)
        
        items_scrollbar = ttk.Scrollbar(items_frame, orient='vertical', command=items_tree.yview)
        items_tree.configure(yscrollcommand=items_scrollbar.set)
        
        items_tree.pack(side='left', fill='both', expand=True)
        items_scrollbar.pack(side='right', fill='y')
        
        # Llenar items
        for item in factura['items']:
            if item.get('es_tarea_manual', 0):
                tipo = "Tarea"
                descripcion = item.get('tarea_manual', 'Tarea manual')
            else:
                tipo = "Material"
                descripcion = f"{item['material_nombre']} ({item['unidad_medida']})"
            
            items_tree.insert('', 'end', values=(
                tipo,
                descripcion,
                f"{item['cantidad']:.2f}",
                f"€{item['precio_unitario']:.2f}",
                f"€{item['subtotal']:.2f}"
            ))
        
        # Totales
        totales_frame = ttk.LabelFrame(main_frame, text="Totales", padding=8)
        totales_frame.pack(fill='x', pady=(0, 8))
        
        ttk.Label(totales_frame, text=f"Subtotal: €{factura['subtotal']:.2f}", font=('Arial', 10, 'bold')).pack(anchor='w')
        
        iva_habilitado = factura.get('iva_habilitado', True)
        if iva_habilitado:
            ttk.Label(totales_frame, text=f"IVA (21%): €{factura['iva']:.2f}", font=('Arial', 10, 'bold')).pack(anchor='w')
        else:
            ttk.Label(totales_frame, text="IVA: No incluido", font=('Arial', 10, 'bold'), foreground='gray').pack(anchor='w')
        
        ttk.Label(totales_frame, text=f"Total: €{factura['total']:.2f}", font=('Arial', 12, 'bold'), foreground='blue').pack(anchor='w')
        
        # Notas
        if factura.get('notas'):
            notas_frame = ttk.LabelFrame(main_frame, text="Notas", padding=8)
            notas_frame.pack(fill='x', pady=(0, 8))
            ttk.Label(notas_frame, text=factura['notas'], wraplength=750).pack(anchor='w')
        
        # Botones de acción
        botones_frame = ttk.Frame(main_frame)
        botones_frame.pack(fill='x', pady=(5, 0))
        
        def abrir_pdf_factura():
            """Busca y abre el PDF de la factura, o lo genera si no existe"""
            try:
                # Buscar PDF existente más reciente
                numero_factura = factura.get('numero_factura', f"F{factura['id']:04d}")
                numero_limpio = numero_factura.replace('/', '_').replace('\\', '_').replace(' ', '_')
                pdfs_encontrados = []
                
                if os.path.exists(self.carpeta_facturas):
                    for archivo in os.listdir(self.carpeta_facturas):
                        # Buscar por número de factura o ID
                        if (archivo.startswith(f"factura_{numero_limpio}_") or 
                            archivo.startswith(f"factura_F{factura['id']:04d}_")) and archivo.endswith('.pdf'):
                            # Excluir archivos temporales de vista previa
                            if 'temp' not in archivo.lower():
                                ruta_completa = os.path.join(self.carpeta_facturas, archivo)
                                pdfs_encontrados.append((ruta_completa, os.path.getmtime(ruta_completa)))
                
                if pdfs_encontrados:
                    # Ordenar por fecha de modificación (más reciente primero)
                    pdfs_encontrados.sort(key=lambda x: x[1], reverse=True)
                    pdf_path = pdfs_encontrados[0][0]
                else:
                    # Generar PDF si no existe
                    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
                    nombre_archivo = f"factura_{numero_limpio}_{timestamp}.pdf"
                    pdf_path = os.path.join(self.carpeta_facturas, nombre_archivo)
                    os.makedirs(self.carpeta_facturas, exist_ok=True)
                    self.pdf_generator.generate_factura_pdf(factura, pdf_path)
                
                # Abrir PDF
                if platform.system() == 'Windows':
                    os.startfile(pdf_path)
                elif platform.system() == 'Darwin':  # macOS
                    subprocess.run(['open', pdf_path])
                else:  # Linux
                    subprocess.run(['xdg-open', pdf_path])
                    
            except Exception as e:
                messagebox.showerror("Error", f"Error al abrir PDF: {str(e)}")
        
        ttk.Button(botones_frame, text="📄 Abrir PDF", command=abrir_pdf_factura, style='Accent.TButton').pack(side='left', padx=(0, 10))
        ttk.Button(botones_frame, text="Cerrar", command=detalle_window.destroy).pack(side='right')
    
    def exportar_factura_pdf(self):
        """Exporta una factura a PDF"""
        selection = self.facturas_tree.selection()
        if not selection:
            messagebox.showwarning("Advertencia", "Seleccione una factura para exportar")
            return
        
        factura_id = self.facturas_tree.item(selection[0])['values'][0]
        self.generar_pdf_factura_con_nombre_personalizado(factura_id)
    
    def marcar_factura_pagada(self):
        """Marca una factura como pagada"""
        selection = self.facturas_tree.selection()
        if not selection:
            messagebox.showwarning("Advertencia", "Seleccione una factura")
            return
        
        factura_id = self.facturas_tree.item(selection[0])['values'][0]
        try:
            if factura_manager.actualizar_estado_pago(factura_id, 'Pagada'):
                messagebox.showinfo("Éxito", "Factura marcada como pagada")
                self.refresh_facturas()
            else:
                messagebox.showerror("Error", "Error al actualizar el estado")
        except Exception as e:
            messagebox.showerror("Error", f"Error: {str(e)}")
    
    def marcar_factura_no_pagada(self):
        """Marca una factura como no pagada"""
        selection = self.facturas_tree.selection()
        if not selection:
            messagebox.showwarning("Advertencia", "Seleccione una factura")
            return
        
        factura_id = self.facturas_tree.item(selection[0])['values'][0]
        try:
            if factura_manager.actualizar_estado_pago(factura_id, 'No Pagada'):
                messagebox.showinfo("Éxito", "Factura marcada como no pagada")
                self.refresh_facturas()
            else:
                messagebox.showerror("Error", "Error al actualizar el estado")
        except Exception as e:
            messagebox.showerror("Error", f"Error: {str(e)}")
    
    def eliminar_factura(self):
        """Elimina una factura"""
        selection = self.facturas_tree.selection()
        if not selection:
            messagebox.showwarning("Advertencia", "Seleccione una factura para eliminar")
            return
        
        if messagebox.askyesno("Confirmar", "¿Está seguro de que desea eliminar esta factura?"):
            factura_id = self.facturas_tree.item(selection[0])['values'][0]
            try:
                if factura_manager.eliminar_factura(factura_id):
                    messagebox.showinfo("Éxito", "Factura eliminada correctamente")
                    self.refresh_facturas()
                else:
                    messagebox.showerror("Error", "Error al eliminar factura")
            except Exception as e:
                messagebox.showerror("Error", f"Error al eliminar factura: {str(e)}")
    
    def enviar_email_factura(self):
        """Envía la factura seleccionada por email"""
        selection = self.facturas_tree.selection()
        if not selection:
            messagebox.showwarning("Advertencia", "Seleccione una factura para enviar por email")
            return
        
        # Obtener factura seleccionada
        factura_item = self.facturas_tree.item(selection[0])
        factura_id = factura_item['values'][0]  # ID es la primera columna
        
        # Obtener datos completos de la factura
        factura = factura_manager.obtener_factura_por_id(factura_id)
        
        if not factura:
            messagebox.showerror("Error", "No se pudo obtener los datos de la factura")
            return
        
        # Verificar que el cliente tenga email o pedirlo
        email_destino = factura.get('email', '').strip() if factura.get('email') else ''
        if not email_destino:
            email_destino = simpledialog.askstring("Email requerido", 
                                                  f"El cliente {factura.get('cliente_nombre', '')} no tiene email configurado.\n\nIngrese el email del destinatario:")
            if not email_destino:
                return
            
            # Validar formato básico de email
            if '@' not in email_destino or '.' not in email_destino.split('@')[1]:
                messagebox.showerror("Error", "El email ingresado no es válido")
                return
        
        # Actualizar el email en la factura para el envío
        factura['email'] = email_destino
        
        # Generar PDF temporal
        try:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            numero_limpio = factura.get('numero_factura', f"F{factura_id:04d}").replace('/', '_').replace('\\', '_')
            pdf_path = os.path.join(self.carpeta_facturas, f"factura_{numero_limpio}_{timestamp}.pdf")
            
            # Crear carpeta si no existe
            os.makedirs(self.carpeta_facturas, exist_ok=True)
            
            # Generar PDF
            self.pdf_generator.generate_factura_pdf(factura, pdf_path)
            
            # Enviar email
            try:
                if email_sender.enviar_factura(
                    destinatario=factura['email'],
                    factura=factura,
                    archivo_pdf=pdf_path
                ):
                    messagebox.showinfo("Éxito", f"Factura {factura.get('numero_factura', '')} enviada por email correctamente a {factura['email']}")
            except Exception as e:
                messagebox.showerror("Error", f"Error al enviar email: {str(e)}")
                
        except Exception as e:
            messagebox.showerror("Error", f"Error al generar PDF: {str(e)}")
    
    def actualizar_label_carpeta_facturas(self):
        """Actualiza el label que muestra la carpeta de facturas"""
        carpeta_mostrar = os.path.basename(self.carpeta_facturas) if self.carpeta_facturas else "No configurada"
        
        # Actualizar label en la sección "Ver Facturas"
        if hasattr(self, 'carpeta_facturas_label'):
            self.carpeta_facturas_label.config(text=f"Carpeta Facturas: {carpeta_mostrar}")
        
        # Actualizar label en la sección "Crear Factura"
        if hasattr(self, 'carpeta_facturas_crear_label'):
            self.carpeta_facturas_crear_label.config(text=f"Carpeta Facturas: {carpeta_mostrar}")
    
    def elegir_carpeta_facturas(self):
        """Permite elegir la carpeta para guardar facturas"""
        carpeta_seleccionada = filedialog.askdirectory(
            title="Seleccionar carpeta para guardar facturas",
            initialdir=self.carpeta_facturas
        )
        
        if carpeta_seleccionada:
            self.carpeta_facturas = carpeta_seleccionada
            self.actualizar_label_carpeta_facturas()
            self.guardar_configuracion()
            messagebox.showinfo("Éxito", f"Carpeta de facturas configurada y guardada:\n{self.carpeta_facturas}")
    
    def abrir_carpeta_facturas(self):
        """Abre la carpeta de facturas"""
        if not self.carpeta_facturas or not os.path.exists(self.carpeta_facturas):
            # Crear carpeta si no existe
            try:
                os.makedirs(self.carpeta_facturas, exist_ok=True)
            except:
                messagebox.showwarning("Advertencia", "La carpeta de facturas no está configurada o no se puede crear")
                return
        
        try:
            if platform.system() == 'Windows':
                os.startfile(self.carpeta_facturas)
            elif platform.system() == 'Darwin':
                subprocess.run(['open', self.carpeta_facturas])
            else:
                subprocess.run(['xdg-open', self.carpeta_facturas])
        except Exception as e:
            messagebox.showerror("Error", f"No se pudo abrir la carpeta: {str(e)}")
    
    # ============================================
    # MÉTODOS DE CONFIGURACIÓN DE EMPRESA
    # ============================================
    
    def create_configuracion_empresa_section(self):
        """Crea la sección de configuración de empresa y banco"""
        # Frame principal con scrollbar
        main_frame = ttk.Frame(self.configuracion_frame)
        main_frame.pack(fill='both', expand=True, padx=10, pady=10)
        
        # Canvas y scrollbar
        canvas = tk.Canvas(main_frame, bg='#ecf0f1')
        scrollbar = ttk.Scrollbar(main_frame, orient="vertical", command=canvas.yview)
        scrollable_frame = ttk.Frame(canvas)
        
        def configure_scroll_region(event=None):
            canvas.configure(scrollregion=canvas.bbox("all"))
        
        scrollable_frame.bind("<Configure>", configure_scroll_region)
        canvas.create_window((0, 0), window=scrollable_frame, anchor="nw")
        canvas.configure(yscrollcommand=scrollbar.set)
        
        def configure_canvas(event):
            canvas_width = event.width
            canvas.itemconfig(canvas.find_all()[0], width=canvas_width)
        
        canvas.bind('<Configure>', configure_canvas)
        canvas.pack(side="left", fill="both", expand=True)
        scrollbar.pack(side="right", fill="y")
        
        # Configurar scroll con rueda del mouse
        def _on_mousewheel(event):
            canvas.yview_scroll(int(-1*(event.delta/120)), "units")
        
        def _bind_to_mousewheel(event):
            canvas.bind_all("<MouseWheel>", _on_mousewheel)
        
        def _unbind_from_mousewheel(event):
            canvas.unbind_all("<MouseWheel>")
        
        main_frame.bind('<Enter>', _bind_to_mousewheel)
        main_frame.bind('<Leave>', _unbind_from_mousewheel)
        canvas.bind('<Enter>', _bind_to_mousewheel)
        canvas.bind('<Leave>', _unbind_from_mousewheel)
        
        # === SECCIÓN: DATOS DE LA EMPRESA ===
        empresa_frame = ttk.LabelFrame(scrollable_frame, text="🏢 Datos de la Empresa", padding=15)
        empresa_frame.pack(fill='x', pady=(0, 20))
        
        # Nombre de la empresa
        ttk.Label(empresa_frame, text="🏷️ Nombre de la Empresa:", font=('Arial', 9, 'bold')).grid(row=0, column=0, sticky='w', padx=(0, 10), pady=8)
        self.config_empresa_nombre_var = tk.StringVar()
        self.config_empresa_nombre_entry = ttk.Entry(empresa_frame, textvariable=self.config_empresa_nombre_var, width=40)
        self.config_empresa_nombre_entry.grid(row=0, column=1, columnspan=2, sticky='ew', pady=8)
        
        # CIF
        ttk.Label(empresa_frame, text="🆔 CIF:", font=('Arial', 9, 'bold')).grid(row=1, column=0, sticky='w', padx=(0, 10), pady=8)
        self.config_empresa_cif_var = tk.StringVar()
        self.config_empresa_cif_entry = ttk.Entry(empresa_frame, textvariable=self.config_empresa_cif_var, width=25)
        self.config_empresa_cif_entry.grid(row=1, column=1, padx=(0, 10), pady=8, sticky='w')
        
        # Ciudad
        ttk.Label(empresa_frame, text="🏙️ Ciudad:", font=('Arial', 9, 'bold')).grid(row=1, column=2, sticky='w', padx=(20, 10), pady=8)
        self.config_empresa_ciudad_var = tk.StringVar()
        self.config_empresa_ciudad_entry = ttk.Entry(empresa_frame, textvariable=self.config_empresa_ciudad_var, width=20)
        self.config_empresa_ciudad_entry.grid(row=1, column=3, pady=8, sticky='w')
        
        # Dirección
        ttk.Label(empresa_frame, text="📍 Dirección:", font=('Arial', 9, 'bold')).grid(row=2, column=0, sticky='w', padx=(0, 10), pady=8)
        self.config_empresa_direccion_var = tk.StringVar()
        self.config_empresa_direccion_entry = ttk.Entry(empresa_frame, textvariable=self.config_empresa_direccion_var, width=60)
        self.config_empresa_direccion_entry.grid(row=2, column=1, columnspan=3, sticky='ew', pady=8)
        
        # Teléfono
        ttk.Label(empresa_frame, text="📞 Teléfono:", font=('Arial', 9, 'bold')).grid(row=3, column=0, sticky='w', padx=(0, 10), pady=8)
        self.config_empresa_telefono_var = tk.StringVar()
        self.config_empresa_telefono_entry = ttk.Entry(empresa_frame, textvariable=self.config_empresa_telefono_var, width=25)
        self.config_empresa_telefono_entry.grid(row=3, column=1, padx=(0, 10), pady=8, sticky='w')
        
        # Email
        ttk.Label(empresa_frame, text="📧 Email:", font=('Arial', 9, 'bold')).grid(row=3, column=2, sticky='w', padx=(20, 10), pady=8)
        self.config_empresa_email_var = tk.StringVar()
        self.config_empresa_email_entry = ttk.Entry(empresa_frame, textvariable=self.config_empresa_email_var, width=25)
        self.config_empresa_email_entry.grid(row=3, column=3, pady=8, sticky='w')
        
        # Web
        ttk.Label(empresa_frame, text="🌐 Sitio Web:", font=('Arial', 9, 'bold')).grid(row=4, column=0, sticky='w', padx=(0, 10), pady=8)
        self.config_empresa_web_var = tk.StringVar()
        self.config_empresa_web_entry = ttk.Entry(empresa_frame, textvariable=self.config_empresa_web_var, width=40)
        self.config_empresa_web_entry.grid(row=4, column=1, columnspan=2, sticky='ew', pady=8)
        
        # Registro Mercantil
        ttk.Label(empresa_frame, text="📋 Registro Mercantil:", font=('Arial', 9, 'bold')).grid(row=5, column=0, sticky='w', padx=(0, 10), pady=8)
        self.config_empresa_registro_mercantil_var = tk.StringVar()
        self.config_empresa_registro_mercantil_entry = ttk.Entry(empresa_frame, textvariable=self.config_empresa_registro_mercantil_var, width=40)
        self.config_empresa_registro_mercantil_entry.grid(row=5, column=1, columnspan=2, sticky='ew', pady=8)
        ttk.Label(empresa_frame, text="(Opcional, solo para sociedades)", font=('Arial', 8), foreground='gray').grid(row=5, column=3, sticky='w', padx=(5, 0), pady=8)
        
        self.config_mostrar_registro_var = tk.BooleanVar(value=self.plantilla_config.get('opciones_pdf', {}).get('mostrar_registro_mercantil', True))
        ttk.Checkbutton(empresa_frame, text="Mostrar Registro Mercantil en el PDF",
                        variable=self.config_mostrar_registro_var).grid(row=6, column=0, columnspan=4, sticky='w', padx=(0, 10), pady=(0, 10))
        
        empresa_frame.columnconfigure(1, weight=1)
        empresa_frame.columnconfigure(3, weight=1)
        
        # === SECCIÓN: INFORMACIÓN BANCARIA ===
        banco_frame = ttk.LabelFrame(scrollable_frame, text="🏦 Información Bancaria", padding=15)
        banco_frame.pack(fill='x', pady=(0, 20))
        
        # Método de pago
        ttk.Label(banco_frame, text="💳 Método de Pago:", font=('Arial', 9, 'bold')).grid(row=0, column=0, sticky='w', padx=(0, 10), pady=8)
        self.config_banco_metodo_var = tk.StringVar()
        self.config_banco_metodo_entry = ttk.Entry(banco_frame, textvariable=self.config_banco_metodo_var, width=30)
        self.config_banco_metodo_entry.grid(row=0, column=1, columnspan=2, sticky='ew', pady=8)
        
        # Nombre del banco
        ttk.Label(banco_frame, text="🏛️ Nombre del Banco:", font=('Arial', 9, 'bold')).grid(row=1, column=0, sticky='w', padx=(0, 10), pady=8)
        self.config_banco_nombre_var = tk.StringVar()
        self.config_banco_nombre_entry = ttk.Entry(banco_frame, textvariable=self.config_banco_nombre_var, width=40)
        self.config_banco_nombre_entry.grid(row=1, column=1, columnspan=2, sticky='ew', pady=8)
        
        # Titular de la cuenta
        ttk.Label(banco_frame, text="👤 Titular de la Cuenta:", font=('Arial', 9, 'bold')).grid(row=2, column=0, sticky='w', padx=(0, 10), pady=8)
        self.config_banco_titular_var = tk.StringVar()
        self.config_banco_titular_entry = ttk.Entry(banco_frame, textvariable=self.config_banco_titular_var, width=40)
        self.config_banco_titular_entry.grid(row=2, column=1, columnspan=2, sticky='ew', pady=8)
        
        # Número de cuenta
        ttk.Label(banco_frame, text="🔢 Número de Cuenta:", font=('Arial', 9, 'bold')).grid(row=3, column=0, sticky='w', padx=(0, 10), pady=8)
        self.config_banco_cuenta_var = tk.StringVar()
        self.config_banco_cuenta_entry = ttk.Entry(banco_frame, textvariable=self.config_banco_cuenta_var, width=40)
        self.config_banco_cuenta_entry.grid(row=3, column=1, columnspan=2, sticky='ew', pady=8)
        
        # IBAN
        ttk.Label(banco_frame, text="🏦 IBAN:", font=('Arial', 9, 'bold')).grid(row=4, column=0, sticky='w', padx=(0, 10), pady=8)
        self.config_banco_iban_var = tk.StringVar()
        self.config_banco_iban_entry = ttk.Entry(banco_frame, textvariable=self.config_banco_iban_var, width=40)
        self.config_banco_iban_entry.grid(row=4, column=1, columnspan=2, sticky='ew', pady=8)
        
        banco_frame.columnconfigure(1, weight=1)
        
        # === BOTONES DE ACCIÓN ===
        botones_frame = ttk.Frame(scrollable_frame)
        botones_frame.pack(fill='x', pady=(20, 0))
        
        # Primera fila de botones
        botones_row1 = ttk.Frame(botones_frame)
        botones_row1.pack(fill='x', pady=(0, 10))
        
        ttk.Button(botones_row1, text="💾 Guardar Configuración", command=self.guardar_configuracion_empresa, style='Accent.TButton').pack(side='left', padx=(0, 15))
        ttk.Button(botones_row1, text="🔄 Restaurar", command=self.restaurar_configuracion_empresa, style='Warning.TButton').pack(side='left', padx=(0, 15))
        ttk.Button(botones_row1, text="👁️ Vista Previa", command=self.vista_previa_configuracion, style='Success.TButton').pack(side='left')
        
        # Label de estado
        self.config_status_label = ttk.Label(botones_frame, text="Configuración lista para editar", 
                                           font=('Arial', 9), foreground='gray')
        self.config_status_label.pack(side='left', padx=(20, 0))
        
        # Cargar configuración inicial
        self.cargar_configuracion_empresa()
    
    def cargar_configuracion_empresa(self):
        """Carga la configuración de empresa desde plantilla_config.json"""
        try:
            with open('config/plantilla_config.json', 'r', encoding='utf-8') as f:
                config = json.load(f)
            
            # Cargar datos de empresa
            empresa = config.get('empresa', {})
            registro_valor = empresa.get('registro_mercantil', '')
            if isinstance(registro_valor, dict):
                registro_valor = ", ".join(
                    [f"{k.capitalize()}: {v}" for k, v in registro_valor.items() if v]
                )
            self.config_empresa_nombre_var.set(empresa.get('nombre', ''))
            self.config_empresa_cif_var.set(empresa.get('cif', ''))
            self.config_empresa_direccion_var.set(empresa.get('direccion', ''))
            self.config_empresa_ciudad_var.set(empresa.get('ciudad', ''))
            self.config_empresa_telefono_var.set(empresa.get('telefono', ''))
            self.config_empresa_email_var.set(empresa.get('email', ''))
            self.config_empresa_web_var.set(empresa.get('web', ''))
            self.config_empresa_registro_mercantil_var.set(registro_valor)
            
            opciones_pdf = config.get('opciones_pdf', {})
            self.config_mostrar_registro_var.set(opciones_pdf.get('mostrar_registro_mercantil', True))
            
            # Cargar datos bancarios
            pago = config.get('pago', {})
            self.config_banco_metodo_var.set(pago.get('metodo_pago', 'Transferencia bancaria'))
            self.config_banco_nombre_var.set(pago.get('banco', ''))
            self.config_banco_titular_var.set(pago.get('titular_cuenta', ''))
            self.config_banco_cuenta_var.set(pago.get('numero_cuenta', ''))
            self.config_banco_iban_var.set(pago.get('iban', pago.get('numero_cuenta', '')))
            
            self.config_status_label.config(text="Configuración cargada correctamente", foreground='green')
            
        except FileNotFoundError:
            messagebox.showwarning("Advertencia", "No se encontró el archivo config/plantilla_config.json")
            self.config_status_label.config(text="Error: Archivo no encontrado", foreground='red')
        except json.JSONDecodeError:
            messagebox.showerror("Error", "Error al leer el archivo de configuración. Verifique el formato JSON.")
            self.config_status_label.config(text="Error: Formato JSON inválido", foreground='red')
        except Exception as e:
            messagebox.showerror("Error", f"Error inesperado al cargar configuración: {str(e)}")
            self.config_status_label.config(text="Error al cargar configuración", foreground='red')
    
    def guardar_configuracion_empresa(self):
        """Guarda la configuración de empresa en plantilla_config.json"""
        try:
            # Validar campos críticos
            if not self.config_empresa_nombre_var.get().strip():
                messagebox.showwarning("Advertencia", "El nombre de la empresa es obligatorio")
                return
            
            if not self.config_empresa_email_var.get().strip():
                messagebox.showwarning("Advertencia", "El email de la empresa es obligatorio")
                return
            
            # Leer configuración existente
            try:
                with open('config/plantilla_config.json', 'r', encoding='utf-8') as f:
                    config = json.load(f)
            except FileNotFoundError:
                config = {}
            
            # Actualizar datos de empresa
            if 'empresa' not in config:
                config['empresa'] = {}
            
            config['empresa'].update({
                'nombre': self.config_empresa_nombre_var.get().strip(),
                'cif': self.config_empresa_cif_var.get().strip(),
                'direccion': self.config_empresa_direccion_var.get().strip(),
                'ciudad': self.config_empresa_ciudad_var.get().strip(),
                'telefono': self.config_empresa_telefono_var.get().strip(),
                'email': self.config_empresa_email_var.get().strip(),
                'web': self.config_empresa_web_var.get().strip(),
                'registro_mercantil': self.config_empresa_registro_mercantil_var.get().strip()
            })
            
            if 'opciones_pdf' not in config or not isinstance(config['opciones_pdf'], dict):
                config['opciones_pdf'] = {}
            config['opciones_pdf']['mostrar_registro_mercantil'] = bool(self.config_mostrar_registro_var.get())
            
            # Actualizar datos bancarios
            if 'pago' not in config:
                config['pago'] = {}
            
            config['pago'].update({
                'metodo_pago': self.config_banco_metodo_var.get().strip(),
                'banco': self.config_banco_nombre_var.get().strip(),
                'titular_cuenta': self.config_banco_titular_var.get().strip(),
                'numero_cuenta': self.config_banco_cuenta_var.get().strip(),
                'iban': self.config_banco_iban_var.get().strip()
            })
            
            # Guardar archivo
            with open('config/plantilla_config.json', 'w', encoding='utf-8') as f:
                json.dump(config, f, indent=2, ensure_ascii=False)
            
            # NUEVO: Actualizar configuración en memoria
            self.plantilla_config = self._normalizar_config_plantilla(config)
            
            # Actualizar PDFGenerator con nueva configuración
            self.pdf_generator = PDFGenerator(self.plantilla_config)
            
            self.config_status_label.config(text="✅ Configuración guardada exitosamente", foreground='green')
            messagebox.showinfo("Éxito", "Configuración de empresa y banco guardada correctamente.\n\nLos cambios se aplicarán en las próximas facturas generadas.")
            
        except Exception as e:
            messagebox.showerror("Error", f"Error al guardar configuración: {str(e)}")
            self.config_status_label.config(text="❌ Error al guardar", foreground='red')
    
    def restaurar_configuracion_empresa(self):
        """Restaura la configuración desde el archivo"""
        if messagebox.askyesno("Confirmar", "¿Está seguro de que desea restaurar la configuración desde el archivo?\n\nSe perderán los cambios no guardados."):
            self.cargar_configuracion_empresa()
            messagebox.showinfo("Información", "Configuración restaurada desde el archivo")
    
    def vista_previa_configuracion(self):
        """Muestra una vista previa de cómo se verá la información en las facturas"""
        try:
            # Crear datos de prueba
            factura_preview = {
                'id': 1,
                'numero_factura': 'F0001-2025',
                'cliente_nombre': 'Cliente de Prueba',
                'email': 'cliente@ejemplo.com',
                'telefono': '+34 123 456 789',
                'direccion': 'Calle Ejemplo 123, Ciudad',
                'fecha_creacion': '2025-01-15',
                'subtotal': 200.0,
                'iva': 42.0,
                'total': 242.0,
                'iva_habilitado': True,
                'items': [
                    {
                        'material_nombre': 'Servicio de prueba',
                        'cantidad': 1,
                        'precio_unitario': 200.0,
                        'subtotal': 200.0,
                        'visible_pdf': 1,
                        'es_tarea_manual': 1,
                        'tarea_manual': 'Servicio de prueba'
                    }
                ]
            }
            
            # Generar PDF de prueba
            archivo_preview = os.path.join("output", "vista_previa_configuracion_temp.pdf")
            self.pdf_generator.generate_factura_pdf(factura_preview, archivo_preview)
            
            if messagebox.askyesno("Vista Previa", f"PDF de prueba generado exitosamente.\n¿Desea abrir el archivo?"):
                try:
                    if platform.system() == 'Windows':
                        os.startfile(archivo_preview)
                    elif platform.system() == 'Darwin':
                        subprocess.run(['open', archivo_preview])
                    else:
                        subprocess.run(['xdg-open', archivo_preview])
                except Exception as e:
                    messagebox.showerror("Error", f"No se pudo abrir el archivo: {str(e)}")
                    
        except Exception as e:
            messagebox.showerror("Error", f"Error al generar vista previa: {str(e)}")

    # ============================================
    # FIN DE MÉTODOS DE FACTURACIÓN
    # ============================================

    # ============================================
    # MÉTODOS DE MÉTRICAS
    # ============================================

    def create_metricas_tab(self):
        """Crea la pestaña de métricas con estadísticas de presupuestos y facturas"""
        self.metricas_frame = ttk.Frame(self.notebook)
        self.notebook.add(self.metricas_frame, text="Métricas")
        
        if not MATPLOTLIB_AVAILABLE:
            error_frame = ttk.Frame(self.metricas_frame)
            error_frame.pack(fill='both', expand=True, padx=20, pady=20)
            
            import sys
            current_python = sys.executable
            
            error_text = """⚠️ matplotlib no está disponible

El problema es que la aplicación se está ejecutando con un Python diferente al del entorno virtual.

Python actual: {python}

SOLUCIÓN:
1. Cierra esta aplicación
2. Abre PowerShell o CMD en esta carpeta
3. Ejecuta uno de estos comandos:

   .\\iniciar_app.ps1
   
   O manualmente:
   .\\venv\\Scripts\\python.exe main.py

También puedes configurar tu IDE para usar el Python del venv:
   {venv_python}""".format(
                python=current_python,
                venv_python=os.path.join(os.path.dirname(__file__), 'venv', 'Scripts', 'python.exe')
            )
            
            ttk.Label(error_frame, 
                     text=error_text,
                     font=('Segoe UI', 11),
                     justify='left',
                     foreground='#e74c3c').pack(expand=True, padx=20, pady=20)
            return
        
        # Frame principal con scrollbar
        main_frame = ttk.Frame(self.metricas_frame)
        main_frame.pack(fill='both', expand=True, padx=10, pady=10)
        
        # Canvas y scrollbar para scroll vertical
        canvas = tk.Canvas(main_frame, bg='#ecf0f1')
        scrollbar = ttk.Scrollbar(main_frame, orient="vertical", command=canvas.yview)
        scrollable_frame = ttk.Frame(canvas)
        
        def configure_scroll_region(event=None):
            canvas.configure(scrollregion=canvas.bbox("all"))
        
        scrollable_frame.bind("<Configure>", configure_scroll_region)
        canvas.create_window((0, 0), window=scrollable_frame, anchor="nw")
        canvas.configure(yscrollcommand=scrollbar.set)
        
        def configure_canvas(event):
            canvas_width = event.width
            canvas.itemconfig(canvas.find_all()[0], width=canvas_width)
        
        canvas.bind('<Configure>', configure_canvas)
        canvas.pack(side="left", fill="both", expand=True)
        scrollbar.pack(side="right", fill="y")
        
        # Configurar scroll con rueda del mouse para toda el área
        def _on_mousewheel(event):
            canvas.yview_scroll(int(-1*(event.delta/120)), "units")
        
        # Bindear directamente cuando el mouse entra en el frame de métricas
        def _bind_to_mousewheel(event):
            canvas.bind_all("<MouseWheel>", _on_mousewheel)
            # También bind en Linux/Mac (Button-4 y Button-5)
            canvas.bind_all("<Button-4>", lambda e: canvas.yview_scroll(-1, "units"))
            canvas.bind_all("<Button-5>", lambda e: canvas.yview_scroll(1, "units"))
        
        def _unbind_from_mousewheel(event):
            try:
                canvas.unbind_all("<MouseWheel>")
                canvas.unbind_all("<Button-4>")
                canvas.unbind_all("<Button-5>")
            except:
                pass
        
        # Función recursiva para bindear todos los widgets hijos
        def bind_all_children(widget):
            try:
                widget.bind('<Enter>', _bind_to_mousewheel)
                widget.bind('<Leave>', _unbind_from_mousewheel)
                for child in widget.winfo_children():
                    bind_all_children(child)
            except:
                pass
        
        # Bindear en el frame principal de métricas para que funcione en toda el área
        self.metricas_frame.bind('<Enter>', _bind_to_mousewheel)
        self.metricas_frame.bind('<Leave>', _unbind_from_mousewheel)
        main_frame.bind('<Enter>', _bind_to_mousewheel)
        main_frame.bind('<Leave>', _unbind_from_mousewheel)
        canvas.bind('<Enter>', _bind_to_mousewheel)
        canvas.bind('<Leave>', _unbind_from_mousewheel)
        
        # Bindear todos los widgets existentes
        bind_all_children(scrollable_frame)
        
        # Sección de Presupuestos
        presupuestos_frame = ttk.LabelFrame(scrollable_frame, text="📊 Estadísticas de Presupuestos", padding=15)
        presupuestos_frame.pack(fill='x', padx=10, pady=10)
        
        # Filtro por mes - Presupuestos
        filter_presupuestos_frame = ttk.Frame(presupuestos_frame)
        filter_presupuestos_frame.pack(fill='x', pady=(0, 15))
        
        ttk.Label(filter_presupuestos_frame, text="Filtrar por mes:", style='TLabel').pack(side='left', padx=(0, 10))
        
        self.mes_filtro_presupuestos_var = tk.StringVar(value="Todos")
        # Obtener meses disponibles
        meses_disponibles = self.obtener_meses_disponibles_presupuestos()
        meses_values = ["Todos"] + meses_disponibles
        self.mes_combo_presupuestos = ttk.Combobox(filter_presupuestos_frame, 
                                                   textvariable=self.mes_filtro_presupuestos_var,
                                                   values=meses_values,
                                                   state="readonly", width=20)
        self.mes_combo_presupuestos.pack(side='left', padx=(0, 10))
        self.mes_combo_presupuestos.bind('<<ComboboxSelected>>', self.on_mes_changed_presupuestos)
        
        # Frame para estadísticas de presupuestos
        stats_presupuestos_frame = ttk.Frame(presupuestos_frame)
        stats_presupuestos_frame.pack(fill='both', expand=True, pady=10)
        
        # Estadísticas en grid
        stats_left_frame = ttk.Frame(stats_presupuestos_frame)
        stats_left_frame.pack(side='left', fill='both', expand=True, padx=(0, 10))
        
        # Labels de estadísticas
        self.total_emitidos_label = ttk.Label(stats_left_frame, text="Total Emitidos: 0", 
                                              font=('Segoe UI', 12, 'bold'), style='TLabel')
        self.total_emitidos_label.pack(anchor='w', pady=5)
        
        self.pendientes_label = ttk.Label(stats_left_frame, text="Pendientes: 0 (0%)", 
                                          font=('Segoe UI', 10), style='TLabel')
        self.pendientes_label.pack(anchor='w', pady=3)
        
        self.aprobados_label = ttk.Label(stats_left_frame, text="Aprobados: 0 (0%)", 
                                         font=('Segoe UI', 10), style='TLabel')
        self.aprobados_label.pack(anchor='w', pady=3)
        
        self.rechazados_label = ttk.Label(stats_left_frame, text="Rechazados: 0 (0%)", 
                                          font=('Segoe UI', 10), style='TLabel')
        self.rechazados_label.pack(anchor='w', pady=3)
        
        # Gráfico de presupuestos
        chart_presupuestos_frame = ttk.Frame(stats_presupuestos_frame)
        chart_presupuestos_frame.pack(side='right', fill='both', expand=True)
        
        self.fig_presupuestos = Figure(figsize=(6, 5), dpi=100)
        self.ax_presupuestos = self.fig_presupuestos.add_subplot(111)
        self.canvas_presupuestos = FigureCanvasTkAgg(self.fig_presupuestos, chart_presupuestos_frame)
        self.canvas_presupuestos.get_tk_widget().pack(fill='both', expand=True)
        
        # Sección de Facturas
        facturas_frame = ttk.LabelFrame(scrollable_frame, text="💰 Estadísticas de Facturas", padding=15)
        facturas_frame.pack(fill='x', padx=10, pady=10)
        
        # Filtro por mes - Facturas
        filter_facturas_frame = ttk.Frame(facturas_frame)
        filter_facturas_frame.pack(fill='x', pady=(0, 15))
        
        ttk.Label(filter_facturas_frame, text="Filtrar por mes:", style='TLabel').pack(side='left', padx=(0, 10))
        
        self.mes_filtro_facturas_var = tk.StringVar(value="Todos")
        # Obtener meses disponibles
        meses_disponibles_facturas = self.obtener_meses_disponibles_facturas()
        meses_values_facturas = ["Todos"] + meses_disponibles_facturas
        self.mes_combo_facturas = ttk.Combobox(filter_facturas_frame, 
                                               textvariable=self.mes_filtro_facturas_var,
                                               values=meses_values_facturas,
                                               state="readonly", width=20)
        self.mes_combo_facturas.pack(side='left', padx=(0, 10))
        self.mes_combo_facturas.bind('<<ComboboxSelected>>', self.on_mes_changed_facturas)
        
        # Frame para estadísticas de facturas
        stats_facturas_frame = ttk.Frame(facturas_frame)
        stats_facturas_frame.pack(fill='both', expand=True, pady=10)
        
        # Estadísticas en grid
        stats_left_frame_facturas = ttk.Frame(stats_facturas_frame)
        stats_left_frame_facturas.pack(side='left', fill='both', expand=True, padx=(0, 10))
        
        # Labels de estadísticas
        self.total_emitidas_label = ttk.Label(stats_left_frame_facturas, text="Total Emitidas: 0", 
                                              font=('Segoe UI', 12, 'bold'), style='TLabel')
        self.total_emitidas_label.pack(anchor='w', pady=5)
        
        self.no_pagadas_label = ttk.Label(stats_left_frame_facturas, text="No Pagadas: 0 (0%)", 
                                          font=('Segoe UI', 10), style='TLabel')
        self.no_pagadas_label.pack(anchor='w', pady=3)
        
        self.pagadas_label = ttk.Label(stats_left_frame_facturas, text="Pagadas: 0 (0%)", 
                                       font=('Segoe UI', 10), style='TLabel')
        self.pagadas_label.pack(anchor='w', pady=3)
        
        # Gráfico de facturas
        chart_facturas_frame = ttk.Frame(stats_facturas_frame)
        chart_facturas_frame.pack(side='right', fill='both', expand=True)
        
        self.fig_facturas = Figure(figsize=(6, 5), dpi=100)
        self.ax_facturas = self.fig_facturas.add_subplot(111)
        self.canvas_facturas = FigureCanvasTkAgg(self.fig_facturas, chart_facturas_frame)
        self.canvas_facturas.get_tk_widget().pack(fill='both', expand=True)
        
        # Guardar referencias a las funciones para usarlas después
        self._bind_to_mousewheel_metricas = _bind_to_mousewheel
        self._unbind_from_mousewheel_metricas = _unbind_from_mousewheel
        self._bind_all_children_metricas = bind_all_children
        
        # Cargar datos iniciales
        self.actualizar_metricas_presupuestos()
        self.actualizar_metricas_facturas()
        
        # Volver a bindear después de crear todos los widgets y gráficos para asegurar que funcione en toda el área
        bind_all_children(scrollable_frame)
        
        # También bindear los widgets de matplotlib directamente
        try:
            if hasattr(self, 'canvas_presupuestos') and self.canvas_presupuestos:
                matplotlib_widget = self.canvas_presupuestos.get_tk_widget()
                matplotlib_widget.bind('<Enter>', _bind_to_mousewheel)
                matplotlib_widget.bind('<Leave>', _unbind_from_mousewheel)
            if hasattr(self, 'canvas_facturas') and self.canvas_facturas:
                matplotlib_widget = self.canvas_facturas.get_tk_widget()
                matplotlib_widget.bind('<Enter>', _bind_to_mousewheel)
                matplotlib_widget.bind('<Leave>', _unbind_from_mousewheel)
        except:
            pass
    
    def obtener_meses_disponibles_presupuestos(self):
        """Obtiene la lista de meses disponibles en los presupuestos"""
        query = """
            SELECT DISTINCT strftime('%Y-%m', fecha_creacion) as mes
            FROM presupuestos
            ORDER BY mes DESC
        """
        result = db.execute_query(query)
        meses = []
        meses_nombres = {
            '01': 'Enero', '02': 'Febrero', '03': 'Marzo', '04': 'Abril',
            '05': 'Mayo', '06': 'Junio', '07': 'Julio', '08': 'Agosto',
            '09': 'Septiembre', '10': 'Octubre', '11': 'Noviembre', '12': 'Diciembre'
        }
        for row in result:
            mes = row['mes']
            if mes:
                año, mes_num = mes.split('-')
                nombre_mes = meses_nombres.get(mes_num, mes_num)
                meses.append(f"{nombre_mes} {año} ({mes})")
        return meses
    
    def obtener_meses_disponibles_facturas(self):
        """Obtiene la lista de meses disponibles en las facturas"""
        query = """
            SELECT DISTINCT strftime('%Y-%m', fecha_creacion) as mes
            FROM facturas
            ORDER BY mes DESC
        """
        result = db.execute_query(query)
        meses = []
        meses_nombres = {
            '01': 'Enero', '02': 'Febrero', '03': 'Marzo', '04': 'Abril',
            '05': 'Mayo', '06': 'Junio', '07': 'Julio', '08': 'Agosto',
            '09': 'Septiembre', '10': 'Octubre', '11': 'Noviembre', '12': 'Diciembre'
        }
        for row in result:
            mes = row['mes']
            if mes:
                año, mes_num = mes.split('-')
                nombre_mes = meses_nombres.get(mes_num, mes_num)
                meses.append(f"{nombre_mes} {año} ({mes})")
        return meses
    
    def on_mes_changed_presupuestos(self, event=None):
        """Maneja el cambio de mes en el filtro de presupuestos"""
        self.actualizar_metricas_presupuestos()
    
    def on_mes_changed_facturas(self, event=None):
        """Maneja el cambio de mes en el filtro de facturas"""
        self.actualizar_metricas_facturas()
    
    def actualizar_metricas_presupuestos(self):
        """Actualiza las métricas de presupuestos según el filtro seleccionado"""
        mes_seleccionado = self.mes_filtro_presupuestos_var.get()
        
        fecha_inicio = None
        fecha_fin = None
        
        if mes_seleccionado != "Todos":
            # Extraer año-mes del formato "Nombre Mes Año (YYYY-MM)"
            match = re.search(r'\((\d{4}-\d{2})\)', mes_seleccionado)
            if match:
                año_mes = match.group(1)
                fecha_inicio = f"{año_mes}-01"
                # Calcular último día del mes
                año, mes = año_mes.split('-')
                mes_int = int(mes)
                if mes_int == 12:
                    fecha_fin = f"{int(año)}-12-31"
                elif mes_int in [1, 3, 5, 7, 8, 10]:
                    fecha_fin = f"{año}-{mes}-31"
                elif mes_int in [4, 6, 9, 11]:
                    fecha_fin = f"{año}-{mes}-30"
                else:  # Febrero
                    # Año bisiesto simple (divisible por 4)
                    if int(año) % 4 == 0:
                        fecha_fin = f"{año}-02-29"
                    else:
                        fecha_fin = f"{año}-02-28"
        
        # Obtener estadísticas
        stats = presupuesto_manager.obtener_estadisticas_presupuestos(fecha_inicio, fecha_fin)
        
        total = stats['total_emitidos']
        pendientes = stats['pendientes']
        aprobados = stats['aprobados']
        rechazados = stats['rechazados']
        
        # Actualizar labels
        self.total_emitidos_label.config(text=f"Total Emitidos: {total}")
        
        porcentaje_pendientes = (pendientes / total * 100) if total > 0 else 0
        porcentaje_aprobados = (aprobados / total * 100) if total > 0 else 0
        porcentaje_rechazados = (rechazados / total * 100) if total > 0 else 0
        
        self.pendientes_label.config(text=f"Pendientes: {pendientes} ({porcentaje_pendientes:.1f}%)")
        self.aprobados_label.config(text=f"Aprobados: {aprobados} ({porcentaje_aprobados:.1f}%)")
        self.rechazados_label.config(text=f"Rechazados: {rechazados} ({porcentaje_rechazados:.1f}%)")
        
        # Actualizar gráfico
        self.ax_presupuestos.clear()
        
        if total > 0:
            # Preparar datos para el gráfico
            labels = []
            sizes = []
            colors = []
            
            if pendientes > 0:
                labels.append('Pendientes')
                sizes.append(pendientes)
                colors.append('#f39c12')  # Naranja
            
            if aprobados > 0:
                labels.append('Aprobados')
                sizes.append(aprobados)
                colors.append('#27ae60')  # Verde
            
            if rechazados > 0:
                labels.append('Rechazados')
                sizes.append(rechazados)
                colors.append('#e74c3c')  # Rojo
            
            if sizes:
                self.ax_presupuestos.pie(sizes, labels=labels, colors=colors, autopct='%1.1f%%', startangle=90)
                self.ax_presupuestos.set_title('Distribución de Presupuestos por Estado', fontsize=12, fontweight='bold')
        else:
            self.ax_presupuestos.text(0.5, 0.5, 'No hay datos disponibles', 
                                     ha='center', va='center', fontsize=12)
            self.ax_presupuestos.set_title('Distribución de Presupuestos por Estado', fontsize=12, fontweight='bold')
        
        self.canvas_presupuestos.draw()
    
    def actualizar_metricas_facturas(self):
        """Actualiza las métricas de facturas según el filtro seleccionado"""
        mes_seleccionado = self.mes_filtro_facturas_var.get()
        
        fecha_inicio = None
        fecha_fin = None
        
        if mes_seleccionado != "Todos":
            # Extraer año-mes del formato "Nombre Mes Año (YYYY-MM)"
            match = re.search(r'\((\d{4}-\d{2})\)', mes_seleccionado)
            if match:
                año_mes = match.group(1)
                fecha_inicio = f"{año_mes}-01"
                # Calcular último día del mes
                año, mes = año_mes.split('-')
                mes_int = int(mes)
                if mes_int == 12:
                    fecha_fin = f"{int(año)}-12-31"
                elif mes_int in [1, 3, 5, 7, 8, 10]:
                    fecha_fin = f"{año}-{mes}-31"
                elif mes_int in [4, 6, 9, 11]:
                    fecha_fin = f"{año}-{mes}-30"
                else:  # Febrero
                    # Año bisiesto simple (divisible por 4)
                    if int(año) % 4 == 0:
                        fecha_fin = f"{año}-02-29"
                    else:
                        fecha_fin = f"{año}-02-28"
        
        # Obtener estadísticas
        stats = factura_manager.obtener_estadisticas_facturas(fecha_inicio, fecha_fin)
        
        total = stats['total_emitidas']
        no_pagadas = stats['no_pagadas']
        pagadas = stats['pagadas']
        
        # Actualizar labels
        self.total_emitidas_label.config(text=f"Total Emitidas: {total}")
        
        porcentaje_no_pagadas = (no_pagadas / total * 100) if total > 0 else 0
        porcentaje_pagadas = (pagadas / total * 100) if total > 0 else 0
        
        self.no_pagadas_label.config(text=f"No Pagadas: {no_pagadas} ({porcentaje_no_pagadas:.1f}%)")
        self.pagadas_label.config(text=f"Pagadas: {pagadas} ({porcentaje_pagadas:.1f}%)")
        
        # Actualizar gráfico
        self.ax_facturas.clear()
        
        if total > 0:
            # Preparar datos para el gráfico
            labels = []
            sizes = []
            colors = []
            
            if no_pagadas > 0:
                labels.append('No Pagadas')
                sizes.append(no_pagadas)
                colors.append('#e74c3c')  # Rojo
            
            if pagadas > 0:
                labels.append('Pagadas')
                sizes.append(pagadas)
                colors.append('#27ae60')  # Verde
            
            if sizes:
                self.ax_facturas.pie(sizes, labels=labels, colors=colors, autopct='%1.1f%%', startangle=90)
                self.ax_facturas.set_title('Distribución de Facturas por Estado de Pago', fontsize=12, fontweight='bold')
        else:
            self.ax_facturas.text(0.5, 0.5, 'No hay datos disponibles', 
                                 ha='center', va='center', fontsize=12)
            self.ax_facturas.set_title('Distribución de Facturas por Estado de Pago', fontsize=12, fontweight='bold')
        
        self.canvas_facturas.draw()

    # ============================================
    # FIN DE MÉTODOS DE MÉTRICAS
    # ============================================

    def on_closing(self):
        """Maneja el cierre de la aplicación"""
        try:
            # Guardar configuración antes de cerrar
            self.guardar_configuracion()
            self.guardar_configuracion_plantilla()
            self.root.destroy()
        except Exception as e:
            print(f"Error al cerrar la aplicación: {e}")
            self.root.destroy()

