# Guía de Instalación - Sistema de Gestión de Presupuestos

## Instalación en un Nuevo Equipo

### Requisitos Previos
- **Python 3.7 o superior** instalado en el equipo
- **Conexión a Internet** para descargar dependencias

### Opción 1: Instalación Automática (Recomendada)

#### En Windows:

1. **Ejecuta el script de instalación:**
   - **PowerShell**: Haz clic derecho en `instalar.ps1` → "Ejecutar con PowerShell"
   - **CMD**: Ejecuta `instalar.bat`

2. El script automáticamente:
   - Verificará que Python esté instalado
   - Creará un entorno virtual
   - Instalará todas las dependencias necesarias

3. **Para ejecutar la aplicación:**
   - Usa `iniciar_app.bat` o `iniciar_app.ps1`
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

## Opción 3: Crear Ejecutable Standalone (Sin necesidad de Python)

Si quieres crear un ejecutable que funcione sin instalar Python:

### Usando PyInstaller:

1. **Instala PyInstaller:**
   ```bash
   pip install pyinstaller
   ```

2. **Crea el ejecutable:**
   ```bash
   pyinstaller --onefile --windowed --name "AppPresupuestos" main.py
   ```

3. **El ejecutable estará en:** `dist/AppPresupuestos.exe`

### Notas sobre el Ejecutable:
- El ejecutable será grande (~50-100 MB) porque incluye Python y todas las dependencias
- Funciona en cualquier Windows sin necesidad de instalar Python
- Puedes distribuir solo el `.exe` y el archivo `config.json` (si existe)

## Solución de Problemas

### Error: "Python no está instalado"
- Instala Python desde https://www.python.org/downloads/
- Asegúrate de marcar "Add Python to PATH"

### Error: "No se pueden instalar dependencias"
- Verifica tu conexión a Internet
- Intenta actualizar pip: `python -m pip install --upgrade pip`
- Algunos antivirus bloquean la instalación de paquetes

### Error: "matplotlib no está disponible"
- Asegúrate de estar usando el Python del entorno virtual
- Ejecuta: `venv\Scripts\python.exe -m pip install matplotlib`

### La aplicación no inicia
- Verifica que estés ejecutando desde la carpeta correcta
- Asegúrate de que el entorno virtual esté activado
- Revisa que todas las dependencias estén instaladas

## Archivos Necesarios para Distribución

Para distribuir la aplicación a otro equipo, necesitas:

1. **Toda la carpeta del proyecto** (incluyendo `presupuestos/`)
2. **requirements.txt** (para instalar dependencias)
3. **Scripts de instalación**: `instalar.bat` o `instalar.ps1`
4. **Scripts de ejecución**: `iniciar_app.bat` o `iniciar_app.ps1`

**Opcional:**
- Si creas un ejecutable con PyInstaller, solo necesitas el `.exe`

## Primera Ejecución

1. La base de datos `presupuestos.db` se creará automáticamente
2. Los archivos de configuración se crearán automáticamente si no existen
3. Puedes comenzar a agregar clientes y materiales inmediatamente

