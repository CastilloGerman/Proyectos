# Guía para Crear Ejecutable Standalone

## Requisitos Previos

1. **Python instalado** en tu equipo de desarrollo
2. **Todas las dependencias instaladas** en el entorno virtual
3. **PyInstaller** (se instalará automáticamente si no está)

## Método 1: Usar el Script Automático (Recomendado)

### En Windows:

1. **Ejecuta el script:**
   - **PowerShell**: `.\crear_ejecutable.ps1`
   - **CMD**: `crear_ejecutable.bat`

2. Espera a que termine (puede tardar 5-10 minutos)

3. El ejecutable estará en: `dist\AppPresupuestos.exe`

## Método 2: Comando Manual

```bash
# Instalar PyInstaller
pip install pyinstaller

# Crear ejecutable
pyinstaller --onefile --windowed --name "AppPresupuestos" --add-data "presupuestos;presupuestos" main.py
```

## Método 3: Usar el Archivo .spec (Configuración Avanzada)

Si quieres más control sobre la creación del ejecutable:

```bash
pyinstaller AppPresupuestos.spec
```

El archivo `.spec` permite:
- Configurar qué archivos incluir
- Agregar iconos personalizados
- Ajustar opciones de compresión
- Y más...

## Estructura del Ejecutable

El ejecutable incluirá:
- ✅ Python y todas las dependencias
- ✅ Módulos de la aplicación (`presupuestos/`)
- ✅ Bibliotecas necesarias (matplotlib, reportlab, etc.)
- ✅ Base de datos SQLite (se crea automáticamente)

## Archivos Necesarios para Distribución

Cuando distribuyas el ejecutable, también necesitas copiar:

1. **Archivos de configuración** (si existen):
   - `config.json`
   - `email_config.json`
   - `plantilla_config.json`

2. **Carpetas** (si las usas):
   - `facturas/` (si quieres incluir facturas existentes)
   - `presupuestos/` (ya incluido en el .exe)

## Distribución

### Opción A: Solo el Ejecutable
- Copia `dist\AppPresupuestos.exe` a otro equipo
- Funciona inmediatamente, sin necesidad de Python
- La base de datos se crea automáticamente en la primera ejecución

### Opción B: Ejecutable + Configuración
- Copia `AppPresupuestos.exe`
- Copia también los archivos `.json` de configuración si existen
- Útil si quieres mantener configuraciones personalizadas

## Tamaño del Ejecutable

- **Tamaño esperado**: 50-100 MB
- **Razón**: Incluye Python completo y todas las dependencias
- **Ventaja**: No requiere instalación de Python en el equipo destino

## Solución de Problemas

### Error: "No se encuentra el módulo X"
Agrega el módulo faltante a `--hidden-import`:
```bash
--hidden-import=nombre_modulo
```

### Error: "No se encuentra el archivo Y"
Agrega el archivo a `--add-data`:
```bash
--add-data "archivo;destino"
```

### El ejecutable es muy grande
- Usa `--exclude-module` para excluir módulos no necesarios
- Considera usar `--onedir` en lugar de `--onefile` (más rápido pero más archivos)

### El ejecutable no inicia
- Verifica que no falten dependencias
- Revisa los logs en la carpeta `build`
- Prueba ejecutar con `--console` para ver errores

## Agregar un Icono Personalizado

1. Crea o descarga un archivo `.ico`
2. Colócalo en la carpeta del proyecto
3. Modifica el comando:
```bash
--icon=icono.ico
```

O en el archivo `.spec`, cambia:
```python
icon='icono.ico',
```

## Optimizaciones

### Reducir Tamaño
```bash
pyinstaller --onefile --windowed --strip --upx-dir=upx main.py
```

### Incluir solo lo necesario
Edita el archivo `.spec` y excluye módulos no usados.

## Notas Importantes

1. **Desarrollo vs Distribución**: 
   - Durante desarrollo: usa el entorno virtual normalmente
   - Para distribución: crea el ejecutable solo cuando quieras distribuir

2. **Actualizaciones**:
   - Si cambias el código, necesitas recrear el ejecutable
   - Los usuarios no necesitan actualizar Python ni dependencias

3. **Compatibilidad**:
   - El ejecutable funciona en Windows sin Python instalado
   - Para Linux/Mac, necesitas crear el ejecutable en ese sistema

4. **Base de Datos**:
   - Se crea automáticamente en la primera ejecución
   - Se guarda en la misma carpeta que el ejecutable

## Pruebas

Antes de distribuir, prueba el ejecutable:
1. Cópialo a una carpeta temporal
2. Ejecútalo desde ahí
3. Verifica que todas las funcionalidades trabajen
4. Prueba crear presupuestos, facturas, etc.

