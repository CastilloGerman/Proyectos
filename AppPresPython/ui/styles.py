"""
Configuración de estilos para la aplicación
"""

from tkinter import ttk

def setup_styles():
    """Configura los estilos de la aplicación"""
    style = ttk.Style()
    style.theme_use('clam')
    
    # Paleta de colores minimalista
    colors = {
        'primary': '#2c3e50',      # Azul oscuro
        'secondary': '#34495e',    # Gris azulado
        'accent': '#3498db',       # Azul claro
        'success': '#27ae60',      # Verde
        'warning': '#f39c12',      # Naranja
        'danger': '#e74c3c',       # Rojo
        'light': '#ecf0f1',        # Gris muy claro
        'dark': '#2c3e50',         # Azul muy oscuro
        'text': '#2c3e50',         # Texto principal
        'text_light': '#7f8c8d'    # Texto secundario
    }
    
    # Configurar notebook
    style.configure('TNotebook', background=colors['light'])
    style.configure('TNotebook.Tab', 
                   padding=[20, 12],
                   background=colors['light'],
                   foreground=colors['text'])
    style.map('TNotebook.Tab',
             background=[('selected', colors['accent']),
                       ('active', colors['secondary'])],
             foreground=[('selected', 'white'),
                       ('active', 'white')])
    
    # Configurar botones
    style.configure('TButton', 
                   padding=[12, 8],
                   font=('Segoe UI', 9),
                   background=colors['light'],
                   foreground=colors['text'],
                   borderwidth=1)
    style.map('TButton',
             background=[('active', colors['accent']),
                       ('pressed', colors['secondary'])],
             foreground=[('active', 'white'),
                       ('pressed', 'white')])
    
    # Botón principal
    style.configure('Accent.TButton', 
                   font=('Segoe UI', 10, 'bold'),
                   padding=[16, 10],
                   background=colors['accent'],
                   foreground='white')
    style.map('Accent.TButton',
             background=[('active', colors['secondary']),
                       ('pressed', colors['dark'])])
    
    # Botón de éxito
    style.configure('Success.TButton',
                   background=colors['success'],
                   foreground='white')
    style.map('Success.TButton',
             background=[('active', '#229954'),
                       ('pressed', '#1e8449')])
    
    # Botón de advertencia
    style.configure('Warning.TButton',
                   background=colors['warning'],
                   foreground='white')
    style.map('Warning.TButton',
             background=[('active', '#e67e22'),
                       ('pressed', '#d68910')])
    
    # Botón de peligro
    style.configure('Danger.TButton',
                   background=colors['danger'],
                   foreground='white')
    style.map('Danger.TButton',
             background=[('active', '#c0392b'),
                       ('pressed', '#a93226')])
    
    # Labels
    style.configure('TLabel', 
                   font=('Segoe UI', 9),
                   foreground=colors['text'])
    style.configure('Heading.TLabel',
                   font=('Segoe UI', 11, 'bold'),
                   foreground=colors['dark'])
    style.configure('Subheading.TLabel',
                   font=('Segoe UI', 10, 'bold'),
                   foreground=colors['secondary'])
    
    # Entry fields
    style.configure('TEntry',
                   font=('Segoe UI', 9),
                   padding=[8, 6],
                   fieldbackground='white',
                   borderwidth=1)
    style.map('TEntry',
             focuscolor=colors['accent'])
    
    # Combobox
    style.configure('TCombobox',
                   font=('Segoe UI', 9),
                   padding=[8, 6],
                   fieldbackground='white')
    
    # Treeview
    style.configure('Treeview',
                   font=('Segoe UI', 9),
                   background='white',
                   foreground=colors['text'],
                   rowheight=25)
    style.configure('Treeview.Heading',
                   font=('Segoe UI', 9, 'bold'),
                   background=colors['secondary'],
                   foreground='white',
                   padding=[8, 6])
    style.map('Treeview',
             background=[('selected', colors['accent'])],
             foreground=[('selected', 'white')])
    
    # LabelFrame
    style.configure('TLabelframe',
                   background=colors['light'],
                   borderwidth=1)
    style.configure('TLabelframe.Label',
                   font=('Segoe UI', 10, 'bold'),
                   foreground=colors['dark'],
                   background=colors['light'])
    
    # Checkbutton
    style.configure('TCheckbutton',
                   font=('Segoe UI', 9),
                   foreground=colors['text'],
                   background=colors['light'])
    
    # Frame
    style.configure('TFrame',
                   background=colors['light'])
    
    # Botones pequeños para la ventana de presupuestos
    style.configure('Small.TButton',
                   font=('Segoe UI', 8),
                   padding=[8, 4],
                   background=colors['light'],
                   foreground=colors['text'])
    style.map('Small.TButton',
             background=[('active', colors['accent']),
                       ('pressed', colors['secondary'])],
             foreground=[('active', 'white'),
                       ('pressed', 'white')])
    
    # Botón pequeño de éxito
    style.configure('SmallSuccess.TButton',
                   font=('Segoe UI', 8),
                   padding=[8, 4],
                   background=colors['success'],
                   foreground='white')
    style.map('SmallSuccess.TButton',
             background=[('active', '#229954'),
                       ('pressed', '#1e8449')])
    
    # Botón pequeño de acento
    style.configure('SmallAccent.TButton',
                   font=('Segoe UI', 8),
                   padding=[8, 4],
                   background=colors['accent'],
                   foreground='white')
    style.map('SmallAccent.TButton',
             background=[('active', colors['secondary']),
                       ('pressed', colors['dark'])])
    
    # Botón pequeño de peligro
    style.configure('SmallDanger.TButton',
                   font=('Segoe UI', 8),
                   padding=[8, 4],
                   background=colors['danger'],
                   foreground='white')
    style.map('SmallDanger.TButton',
             background=[('active', '#c0392b'),
                       ('pressed', '#a93226')])

