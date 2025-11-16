# Estructura del Proyecto AppPresupuestos

## 📁 Organización de Carpetas

```
AppPresupuestos/
│
├── 📄 main.py                    # Punto de entrada principal
├── 📄 presupuestos.db            # Base de datos SQLite
├── 📄 requirements.txt           # Dependencias del proyecto
├── 📄 pyrightconfig.json         # Configuración del linter
│
├── 📁 presupuestos/              # Módulo principal de lógica de negocio
│   ├── __init__.py
│   ├── utils.py                  # Gestor de base de datos
│   ├── clientes.py               # Gestión de clientes
│   ├── materiales.py             # Gestión de materiales/servicios
│   ├── presupuestos.py           # Gestión de presupuestos
│   ├── facturas.py               # Gestión de facturas
│   ├── pdf_generator.py          # Generación de PDFs
│   └── email_sender.py           # Envío de emails
│
├── 📁 ui/                        # Interfaz de usuario
│   ├── __init__.py
│   ├── app.py                    # Aplicación principal tkinter
│   └── styles.py                 # Estilos y temas
│
├── 📁 config/                    # Archivos de configuración
│   ├── config.json               # Configuración general
│   ├── email_config.json         # Configuración de email
│   └── plantilla_config.json     # Configuración de plantillas PDF
│
├── 📁 scripts/                   # Scripts de utilidad
│   ├── iniciar_app.ps1           # Iniciar aplicación (PowerShell)
│   ├── iniciar_app.bat           # Iniciar aplicación (CMD)
│   ├── instalar.ps1              # Instalar dependencias (PowerShell)
│   ├── instalar.bat              # Instalar dependencias (CMD)
│   ├── crear_ejecutable.ps1      # Crear ejecutable (PowerShell)
│   ├── crear_ejecutable.bat      # Crear ejecutable (CMD)
│   ├── empaquetar_para_distribucion.ps1
│   └── empaquetar_para_distribucion.bat
│
├── 📁 output/                    # Archivos generados
│   ├── facturas/                 # PDFs de facturas generadas
│   ├── presupuestos/             # PDFs de presupuestos generados
│   └── vista_previa_*.pdf        # Archivos temporales de vista previa
│
├── 📁 docs/                      # Documentación
│   ├── README.md                 # Documentación principal
│   ├── INSTALACION.md            # Guía de instalación
│   ├── CREAR_EJECUTABLE.md       # Guía para crear ejecutable
│   └── examples/                 # Ejemplos de PDFs
│       ├── factura_completa_test.pdf
│       ├── test_factura.pdf
│       └── test_factura_template.pdf
│
├── 📁 build/                     # Archivos de build (PyInstaller)
│   └── AppPresupuestos.spec     # Especificación de PyInstaller
│
├── 📁 tests/                     # Tests (preparado para futuros tests)
│
└── 📁 venv/                      # Entorno virtual (no versionar)
    └── ...
```

## 📋 Descripción de Archivos Principales

### Raíz del Proyecto
- **main.py**: Punto de entrada de la aplicación. Inicializa tkinter y lanza la aplicación.
- **presupuestos.db**: Base de datos SQLite que almacena todos los datos (clientes, materiales, presupuestos, facturas).
- **requirements.txt**: Lista de dependencias Python necesarias para el proyecto.
- **pyrightconfig.json**: Configuración del linter basedpyright para el proyecto.

### Módulo `presupuestos/`
Contiene toda la lógica de negocio:
- **utils.py**: `DatabaseManager` - Gestión de conexiones y esquema de base de datos
- **clientes.py**: `ClienteManager` - CRUD de clientes
- **materiales.py**: `MaterialManager` - CRUD de materiales/servicios
- **presupuestos.py**: `PresupuestoManager` - CRUD y estadísticas de presupuestos
- **facturas.py**: `FacturaManager` - CRUD y estadísticas de facturas
- **pdf_generator.py**: Generación de PDFs usando reportlab
- **email_sender.py**: Envío de emails con facturas/presupuestos

### Módulo `ui/`
Interfaz gráfica de usuario:
- **app.py**: Clase principal `AppPresupuestos` con todas las ventanas y funcionalidades
- **styles.py**: Configuración de estilos y temas de tkinter

### Carpeta `config/`
Archivos JSON de configuración:
- **config.json**: Configuración general de la aplicación
- **email_config.json**: Configuración del servidor SMTP para envío de emails
- **plantilla_config.json**: Configuración de plantillas PDF (colores, textos, etc.)

### Carpeta `scripts/`
Scripts de automatización:
- Scripts PowerShell (.ps1) y Batch (.bat) para diferentes tareas
- Iniciar aplicación, instalar dependencias, crear ejecutables, etc.

### Carpeta `output/`
Archivos generados por la aplicación:
- **facturas/**: PDFs de facturas generadas
- **presupuestos/**: PDFs de presupuestos generados
- Archivos temporales de vista previa

### Carpeta `docs/`
Documentación del proyecto:
- README, guías de instalación, creación de ejecutables
- Ejemplos de PDFs generados

## ✅ Estado Actual

La estructura está correctamente organizada:
- ✅ Todos los módulos Python están en sus carpetas correspondientes
- ✅ Archivos de configuración están en `config/`
- ✅ Scripts de utilidad están en `scripts/`
- ✅ Documentación está en `docs/`
- ✅ Archivos generados están en `output/`
- ✅ Base de datos está en la raíz (como se referencia en el código)
- ✅ Carpetas vacías innecesarias han sido eliminadas

## 📝 Notas

- La carpeta `venv/` contiene el entorno virtual y no debe versionarse
- La carpeta `build/` contiene archivos temporales de PyInstaller
- La carpeta `tests/` está preparada para futuros tests unitarios
- Los archivos `__pycache__/` son generados automáticamente por Python

