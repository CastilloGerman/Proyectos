# Sistema de Gestión de Presupuestos

Una aplicación de escritorio desarrollada en Python con Tkinter para la gestión completa de presupuestos, clientes y materiales.

## Características

### 1. Gestión de Clientes
- **Alta de clientes**: Agregar nuevos clientes con nombre, teléfono, email y dirección
- **Lista de clientes**: Visualizar todos los clientes existentes en una tabla
- **Edición y eliminación**: Modificar o eliminar clientes existentes
- **Búsqueda**: Buscar clientes por nombre, teléfono o email

### 2. Gestión de Materiales
- **Alta de materiales**: Agregar materiales con nombre, unidad de medida y precio unitario
- **Lista de materiales**: Visualizar todos los materiales con búsqueda
- **Edición y eliminación**: Modificar o eliminar materiales existentes
- **Búsqueda**: Buscar materiales por nombre

### 3. Creación de Presupuestos
- **Selección de cliente**: Elegir cliente de una lista desplegable
- **Agregar materiales**: Seleccionar materiales y especificar cantidades
- **Cálculo automático**: Subtotal, IVA (21%) y total se calculan automáticamente
- **Guardado**: Los presupuestos se guardan en base de datos SQLite

### 4. Visualización de Presupuestos
- **Lista de presupuestos**: Ver todos los presupuestos creados
- **Detalle completo**: Visualizar presupuesto con información del cliente e items
- **Exportación a PDF**: Generar PDF profesional con logo, datos del cliente y listado de materiales
- **Envío por email**: Enviar presupuestos por correo electrónico con PDF adjunto
- **Eliminación**: Eliminar presupuestos existentes

## Instalación

1. **Requisitos del sistema**:
   - Python 3.7 o superior
   - Tkinter (incluido con Python)

2. **Instalación de dependencias**:
   ```bash
   pip install -r requirements.txt
   ```
   
   **Nota**: Las dependencias incluyen:
   - `reportlab`: Para generación de PDFs
   - `Pillow`: Para procesamiento de imágenes (opcional)

3. **Ejecutar la aplicación**:
   ```bash
   python main.py
   ```

## Estructura del Proyecto

```
AppPresupuestos/
├── main.py                 # Aplicación principal con interfaz Tkinter
├── requirements.txt        # Dependencias del proyecto
├── presupuestos.db         # Base de datos SQLite (se crea automáticamente)
├── presupuestos/           # Módulos de la aplicación
│   ├── __init__.py        # Inicialización del paquete
│   ├── utils.py           # Gestión de base de datos
│   ├── clientes.py        # Lógica de clientes
│   ├── materiales.py      # Lógica de materiales
│   ├── presupuestos.py    # Lógica de presupuestos
│   ├── pdf_generator.py   # Generación de PDFs
│   └── email_sender.py    # Envío de emails
└── README.md              # Este archivo
```

## Uso de la Aplicación

### Primera vez
1. Ejecuta `python main.py`
2. La base de datos se creará automáticamente
3. Comienza agregando clientes y materiales

### Flujo de trabajo típico
1. **Gestión de Clientes**: Agrega los clientes con los que trabajarás
2. **Gestión de Materiales**: Registra los materiales y sus precios
3. **Crear Presupuesto**: 
   - Selecciona un cliente
   - Agrega materiales con sus cantidades
   - Revisa los totales calculados automáticamente
   - Guarda el presupuesto
4. **Ver Presupuestos**: Consulta y gestiona presupuestos existentes
5. **Exportar PDF**: Genera PDFs profesionales de los presupuestos
6. **Enviar por Email**: Configura y envía presupuestos por correo electrónico

## Base de Datos

La aplicación utiliza SQLite con las siguientes tablas:

- **clientes**: Información de clientes
- **materiales**: Catálogo de materiales
- **presupuestos**: Presupuestos creados
- **presupuesto_items**: Items de cada presupuesto

## Características Técnicas

- **Interfaz**: Tkinter con tema moderno
- **Base de datos**: SQLite con transacciones
- **Arquitectura**: Separación de lógica de negocio e interfaz
- **Validaciones**: Validación de datos de entrada
- **Búsquedas**: Búsqueda en tiempo real
- **Cálculos**: Cálculo automático de totales con IVA
- **Exportación**: Generación de PDFs profesionales con ReportLab
- **Email**: Envío de presupuestos por correo electrónico

## Funcionalidades de Exportación

### Generación de PDF
- **Formato profesional**: PDF con logo, datos de empresa y cliente
- **Tabla de materiales**: Listado detallado con cantidades y precios
- **Cálculos automáticos**: Subtotal, IVA y total incluidos
- **Diseño personalizable**: Estilos y colores configurables
- **Apertura automática**: Opción de abrir el PDF generado

### Envío por Email
- **Configuración SMTP**: Soporte para Gmail, Outlook y otros servidores
- **PDF adjunto**: Presupuesto enviado como archivo PDF
- **Plantilla de email**: Mensaje personalizado con datos del presupuesto
- **Interfaz amigable**: Diálogos para configuración y envío
- **Validación**: Verificación de configuración antes del envío

## Personalización

### Cambiar porcentaje de IVA
En `presupuestos/presupuestos.py`, modifica la línea:
```python
self.iva_porcentaje = 21.0  # Cambiar por el porcentaje deseado
```

### Modificar estilos
En `main.py`, función `setup_styles()`, puedes personalizar colores y fuentes.

## Solución de Problemas

### Error de base de datos
- Asegúrate de que la aplicación tenga permisos de escritura en el directorio
- Si hay problemas, elimina `presupuestos.db` para recrear la base de datos

### Error de importación
- Verifica que estés ejecutando desde el directorio correcto
- Asegúrate de que Python puede encontrar el módulo `presupuestos`

## Licencia

Este proyecto es de código abierto y está disponible bajo la licencia MIT.
