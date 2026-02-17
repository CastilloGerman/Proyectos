# Sistema de Gestión de Presupuestos y Facturas

Una aplicación de escritorio desarrollada en Python con Tkinter para la gestión completa de presupuestos, facturas, clientes y materiales.

## Características

### 1. Gestión de Clientes
- **Alta de clientes**: Agregar nuevos clientes con nombre, teléfono, email, dirección y DNI
- **Lista de clientes**: Visualizar todos los clientes existentes en una tabla interactiva
- **Edición y eliminación**: Modificar o eliminar clientes existentes
- **Búsqueda en tiempo real**: Buscar clientes por nombre, teléfono o email mientras escribes

### 2. Gestión de Materiales
- **Alta de materiales**: Agregar materiales con nombre, unidad de medida y precio unitario
- **Lista de materiales**: Visualizar todos los materiales con búsqueda integrada
- **Edición y eliminación**: Modificar o eliminar materiales existentes
- **Búsqueda**: Buscar materiales por nombre en tiempo real

### 3. Creación de Presupuestos
- **Selección de cliente**: Elegir cliente de una lista desplegable actualizada
- **Agregar materiales**: Seleccionar materiales del catálogo y especificar cantidades
- **Tareas manuales**: Agregar items personalizados sin necesidad de material predefinido
- **Descuentos**: Aplicar descuentos por porcentaje o fijos a items individuales o globales
- **IVA configurable**: Habilitar o deshabilitar IVA, configurar porcentaje (21% por defecto)
- **Cálculo automático**: Subtotal, IVA y total se calculan automáticamente
- **Estados**: Marcar presupuestos como Pendiente, Aprobado o Rechazado
- **Visibilidad de items**: Controlar qué items aparecen en el PDF
- **Guardado**: Los presupuestos se guardan en base de datos SQLite

### 4. Gestión de Facturas (Nueva Funcionalidad)
- **Creación de facturas**: Generar facturas desde presupuestos o crear nuevas desde cero
- **Numeración automática**: Sistema de numeración secuencial (F0001-2025, F0002-2025, etc.)
- **Vinculación con presupuestos**: Asociar facturas a presupuestos existentes
- **Fechas de vencimiento**: Establecer fechas límite de pago
- **Estados de pago**: Marcar facturas como Pagada, No Pagada, Parcialmente Pagada
- **Métodos de pago**: Registrar método de pago (Transferencia, Efectivo, Cheque, etc.)
- **Retención IRPF**: Opción para aplicar retención IRPF
- **Descuentos avanzados**: Descuentos globales y por item, antes o después de IVA
- **Notas personalizadas**: Agregar notas y observaciones a las facturas

### 5. Visualización y Gestión de Presupuestos
- **Lista de presupuestos**: Ver todos los presupuestos creados con información resumida
- **Filtros avanzados**: Filtrar por estado, año y mes
- **Búsqueda**: Buscar presupuestos por cliente o número
- **Detalle completo**: Visualizar presupuesto con información del cliente e items detallados
- **Exportación a PDF**: Generar PDF profesional con logo personalizado, datos de empresa y cliente
- **Vista previa**: Previsualizar PDF antes de guardar
- **Envío por email**: Enviar presupuestos por correo electrónico con PDF adjunto
- **Cambio de estado**: Marcar presupuestos como aprobados, rechazados o pendientes
- **Eliminación**: Eliminar presupuestos existentes

### 6. Visualización y Gestión de Facturas
- **Lista de facturas**: Ver todas las facturas con información de estado y pago
- **Filtros**: Filtrar facturas por estado de pago, año y mes
- **Búsqueda**: Buscar facturas por número o cliente
- **Detalle completo**: Visualizar factura con todos los detalles
- **Exportación a PDF**: Generar PDF profesional de factura con diseño personalizable
- **Envío por email**: Enviar facturas por correo electrónico
- **Gestión de estados**: Actualizar estado de pago de las facturas

### 7. Configuración y Personalización
- **Configuración de empresa**: Datos de empresa, dirección, CIF, registro mercantil
- **Personalización de PDFs**: 
  - Logo personalizado o texto alternativo
  - Colores personalizables (principal, secundario, texto)
  - Textos personalizados (títulos, notas al pie)
  - Configuración de márgenes
  - Información bancaria para pagos
- **Configuración de email**: Servidor SMTP, credenciales, plantillas de mensaje
- **Rutas de archivos**: Configurar carpetas de salida para PDFs y facturas

### 8. Estadísticas y Reportes
- **Gráficos**: Visualización de estadísticas con matplotlib
- **Análisis de presupuestos**: Estadísticas por período, cliente, estado
- **Análisis de facturas**: Estadísticas de facturación y pagos

## Instalación

### Requisitos del Sistema
- **Python 3.7 o superior**
- **Tkinter** (incluido con Python en la mayoría de distribuciones)
- **Conexión a Internet** (para instalar dependencias)

### Opción 1: Instalación Automática (Recomendada)

#### En Windows:
1. **Ejecuta el script de instalación:**
   - **PowerShell**: Haz clic derecho en `scripts/instalar.ps1` → "Ejecutar con PowerShell"
   - **CMD**: Ejecuta `scripts/instalar.bat`

2. El script automáticamente:
   - Verificará que Python esté instalado
   - Creará un entorno virtual (`venv`)
   - Instalará todas las dependencias necesarias

3. **Para ejecutar la aplicación:**
   - Usa `scripts/iniciar_app.bat` o `scripts/iniciar_app.ps1`
   - O ejecuta: `venv\Scripts\python.exe main.py`

### Opción 2: Instalación Manual

1. **Instala Python** (si no está instalado):
   - Descarga desde: https://www.python.org/downloads/
   - **IMPORTANTE**: Marca "Add Python to PATH" durante la instalación

2. **Abre PowerShell o CMD** en la carpeta del proyecto

3. **Crea el entorno virtual:**
   ```bash
   python -m venv venv
   ```

4. **Activa el entorno virtual:**
   ```powershell
   # PowerShell
   .\venv\Scripts\Activate.ps1
   
   # CMD
   venv\Scripts\activate.bat
   ```

5. **Instala las dependencias:**
   ```bash
   pip install -r requirements.txt
   ```

6. **Ejecuta la aplicación:**
   ```bash
   python main.py
   ```

### Dependencias

Las dependencias incluyen:
- `reportlab`: Para generación de PDFs profesionales
- `Pillow`: Para procesamiento de imágenes (logos)
- `matplotlib`: Para gráficos y estadísticas
- `pyinstaller`: Para crear ejecutables standalone (opcional)

## Estructura del Proyecto

```
AppPresupuestos/
├── main.py                    # Punto de entrada principal
├── requirements.txt           # Dependencias del proyecto
├── presupuestos.db            # Base de datos SQLite (se crea automáticamente)
├── pyrightconfig.json         # Configuración del linter
│
├── presupuestos/              # Módulos de lógica de negocio
│   ├── __init__.py
│   ├── utils.py               # Gestión de base de datos
│   ├── clientes.py            # Lógica de clientes
│   ├── materiales.py          # Lógica de materiales
│   ├── presupuestos.py        # Lógica de presupuestos
│   ├── facturas.py            # Lógica de facturas
│   ├── pdf_generator.py       # Generación de PDFs
│   └── email_sender.py        # Envío de emails
│
├── ui/                        # Interfaz de usuario
│   ├── __init__.py
│   ├── app.py                 # Aplicación principal Tkinter
│   └── styles.py              # Estilos y temas
│
├── config/                    # Archivos de configuración
│   ├── config.json            # Configuración general (rutas, etc.)
│   ├── email_config.json      # Configuración de email SMTP
│   └── plantilla_config.json  # Configuración de plantillas PDF
│
├── scripts/                   # Scripts de utilidad
│   ├── iniciar_app.ps1        # Iniciar aplicación (PowerShell)
│   ├── iniciar_app.bat        # Iniciar aplicación (CMD)
│   ├── instalar.ps1           # Instalar dependencias (PowerShell)
│   ├── instalar.bat           # Instalar dependencias (CMD)
│   ├── crear_ejecutable.ps1   # Crear ejecutable (PowerShell)
│   ├── crear_ejecutable.bat   # Crear ejecutable (CMD)
│   ├── empaquetar_para_distribucion.ps1
│   └── empaquetar_para_distribucion.bat
│
├── output/                    # Archivos generados
│   ├── facturas/              # PDFs de facturas generadas
│   └── presupuestos/          # PDFs de presupuestos generados
│
├── docs/                      # Documentación
│   ├── README.md              # Este archivo
│   ├── INSTALACION.md         # Guía detallada de instalación
│   ├── CREAR_EJECUTABLE.md    # Guía para crear ejecutable
│   └── examples/              # Ejemplos de PDFs generados
│
├── build/                     # Archivos de build (PyInstaller)
│   └── AppPresupuestos.spec   # Especificación de PyInstaller
│
└── tests/                     # Tests (preparado para futuros tests)
```

## Uso de la Aplicación

### Primera Vez
1. Ejecuta `python main.py` o usa los scripts de inicio
2. La base de datos se creará automáticamente
3. Los archivos de configuración se crearán con valores por defecto
4. Configura tus datos de empresa en la pestaña "Configuración"
5. Comienza agregando clientes y materiales

### Flujo de Trabajo Típico

1. **Configuración Inicial**:
   - Configura datos de empresa (nombre, dirección, CIF, etc.)
   - Personaliza plantillas PDF (logo, colores, textos)
   - Configura email SMTP si deseas enviar por correo

2. **Gestión de Clientes**:
   - Agrega los clientes con los que trabajarás
   - Completa información de contacto y DNI

3. **Gestión de Materiales**:
   - Registra los materiales y servicios con sus precios
   - Define unidades de medida (m², unidades, horas, etc.)

4. **Crear Presupuesto**:
   - Selecciona un cliente
   - Agrega materiales del catálogo o crea tareas manuales
   - Especifica cantidades y aplica descuentos si es necesario
   - Revisa los totales calculados automáticamente
   - Guarda el presupuesto

5. **Gestionar Presupuestos**:
   - Visualiza todos los presupuestos creados
   - Usa filtros para encontrar presupuestos específicos
   - Marca estados (Aprobado, Rechazado, Pendiente)
   - Genera PDFs profesionales
   - Envía por email si está configurado

6. **Crear Factura**:
   - Genera factura desde un presupuesto aprobado
   - O crea factura nueva desde cero
   - Establece fecha de vencimiento y método de pago
   - La numeración es automática (F0001-2025, etc.)

7. **Gestionar Facturas**:
   - Visualiza todas las facturas
   - Actualiza estados de pago
   - Genera PDFs de facturas
   - Envía por email

## Base de Datos

La aplicación utiliza SQLite con las siguientes tablas:

- **clientes**: Información de clientes (nombre, teléfono, email, dirección, DNI)
- **materiales**: Catálogo de materiales (nombre, unidad, precio)
- **presupuestos**: Presupuestos creados (cliente, fechas, totales, estado, descuentos)
- **presupuesto_items**: Items de cada presupuesto (materiales o tareas manuales)
- **facturas**: Facturas creadas (número, cliente, fechas, totales, estado de pago)
- **factura_items**: Items de cada factura (materiales o tareas manuales)

## Características Técnicas

- **Interfaz**: Tkinter con tema moderno y personalizable
- **Base de datos**: SQLite con transacciones y migraciones automáticas
- **Arquitectura**: Separación clara de lógica de negocio (`presupuestos/`) e interfaz (`ui/`)
- **Validaciones**: Validación completa de datos de entrada
- **Búsquedas**: Búsqueda en tiempo real en todas las listas
- **Cálculos**: Cálculo automático de totales con IVA configurable
- **Exportación**: Generación de PDFs profesionales con ReportLab
- **Email**: Envío de presupuestos y facturas por correo electrónico
- **Configuración**: Sistema de configuración mediante archivos JSON
- **Estados**: Gestión de estados para presupuestos y facturas
- **Descuentos**: Sistema flexible de descuentos (porcentaje, fijo, antes/después IVA)

## Funcionalidades de Exportación

### Generación de PDF

#### Presupuestos:
- **Formato profesional**: PDF con logo personalizado, datos de empresa y cliente
- **Tabla de materiales**: Listado detallado con cantidades, precios y descuentos
- **Cálculos automáticos**: Subtotal, IVA y total incluidos
- **Diseño personalizable**: Estilos, colores y textos configurables
- **Vista previa**: Previsualizar antes de guardar
- **Apertura automática**: Opción de abrir el PDF generado

#### Facturas:
- **Numeración automática**: Sistema secuencial por año (F0001-2025)
- **Información completa**: Datos de empresa, cliente, fechas, vencimiento
- **Detalles de pago**: Método de pago, estado, información bancaria
- **Diseño personalizado**: Colores y estilos específicos para facturas
- **Retención IRPF**: Soporte para retención IRPF si aplica
- **Notas legales**: Textos personalizables según normativa

### Envío por Email
- **Configuración SMTP**: Soporte para Gmail, Outlook y otros servidores
- **PDF adjunto**: Presupuesto o factura enviado como archivo PDF
- **Plantilla de email**: Mensaje personalizado con datos del documento
- **Interfaz amigable**: Diálogos para configuración y envío
- **Validación**: Verificación de configuración antes del envío

## Personalización

### Configuración de Empresa
Edita `config/plantilla_config.json` o usa la interfaz de configuración:
- Datos de empresa (nombre, dirección, CIF, teléfono, email)
- Registro mercantil
- Información bancaria para pagos

### Personalización de PDFs
En la pestaña "Configuración" o editando `config/plantilla_config.json`:
- **Logo**: Ruta a archivo de imagen o texto alternativo
- **Colores**: Color principal, secundario y texto
- **Textos**: Títulos, notas al pie, mensajes de IVA
- **Márgenes**: Configuración de márgenes del PDF
- **Información legal**: Firma, notas de exención

### Cambiar porcentaje de IVA
En `presupuestos/presupuestos.py` y `presupuestos/facturas.py`, modifica:
```python
self.iva_porcentaje = 21.0  # Cambiar por el porcentaje deseado
```

### Modificar estilos de interfaz
En `ui/styles.py`, puedes personalizar colores y fuentes de la interfaz.

## Crear Ejecutable Standalone

Para crear un ejecutable que funcione sin instalar Python:

### Opción 1: Script Automático
```bash
# PowerShell
.\scripts\crear_ejecutable.ps1

# CMD
scripts\crear_ejecutable.bat
```

### Opción 2: Manual
```bash
pip install pyinstaller
pyinstaller build/AppPresupuestos.spec
```

El ejecutable estará en `dist/AppPresupuestos.exe`

**Nota**: Consulta `docs/CREAR_EJECUTABLE.md` para más detalles.

## Documentación Adicional

- **[INSTALACION.md](INSTALACION.md)**: Guía detallada de instalación paso a paso
- **[CREAR_EJECUTABLE.md](CREAR_EJECUTABLE.md)**: Guía completa para crear ejecutables
- **[ESTRUCTURA_PROYECTO.md](../ESTRUCTURA_PROYECTO.md)**: Documentación detallada de la estructura del proyecto

## Solución de Problemas

### Error de base de datos
- Asegúrate de que la aplicación tenga permisos de escritura en el directorio
- Si hay problemas, elimina `presupuestos.db` para recrear la base de datos
- La aplicación migrará automáticamente tablas existentes si es necesario

### Error de importación
- Verifica que estés ejecutando desde el directorio correcto
- Asegúrate de que el entorno virtual esté activado
- Verifica que todas las dependencias estén instaladas: `pip install -r requirements.txt`

### Error al generar PDF
- Verifica que las rutas de configuración sean correctas
- Asegúrate de que el logo (si se usa) exista en la ruta especificada
- Revisa los permisos de escritura en la carpeta `output/`

### Error al enviar email
- Verifica la configuración SMTP en `config/email_config.json`
- Para Gmail, usa contraseñas de aplicación en lugar de la contraseña normal
- Verifica que el puerto SMTP sea correcto (587 para TLS, 465 para SSL)

### La aplicación no inicia
- Verifica que Python 3.7+ esté instalado
- Asegúrate de que el entorno virtual esté activado
- Ejecuta `pip install -r requirements.txt` para instalar dependencias
- Revisa que estés ejecutando desde el directorio correcto

## Licencia

Este proyecto es de código abierto y está disponible bajo la licencia MIT.

## Versión

Versión actual: 1.0.0
