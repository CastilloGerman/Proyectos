@echo off
title Instalador - Sistema de Gestión de Presupuestos
cd /d "%~dp0\.."
echo ========================================
echo  Instalador del Sistema de Presupuestos
echo ========================================
echo.

REM Verificar si Python está instalado
python --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Python no está instalado en este equipo.
    echo.
    echo Por favor instala Python 3.7 o superior desde:
    echo https://www.python.org/downloads/
    echo.
    echo Asegúrate de marcar "Add Python to PATH" durante la instalación.
    pause
    exit /b 1
)

echo Python detectado correctamente.
echo.

REM Crear entorno virtual si no existe
if not exist "venv" (
    echo Creando entorno virtual...
    python -m venv venv
    if errorlevel 1 (
        echo ERROR: No se pudo crear el entorno virtual.
        pause
        exit /b 1
    )
    echo Entorno virtual creado.
    echo.
)

REM Activar entorno virtual
echo Activando entorno virtual...
call venv\Scripts\activate.bat
if errorlevel 1 (
    echo ERROR: No se pudo activar el entorno virtual.
    pause
    exit /b 1
)

REM Actualizar pip
echo Actualizando pip...
python -m pip install --upgrade pip --quiet

REM Instalar dependencias
echo.
echo Instalando dependencias...
echo Esto puede tardar unos minutos...
python -m pip install -r requirements.txt
if errorlevel 1 (
    echo.
    echo ERROR: No se pudieron instalar las dependencias.
    echo Verifica tu conexión a internet e intenta de nuevo.
    pause
    exit /b 1
)

echo.
echo ========================================
echo  Instalación completada exitosamente!
echo ========================================
echo.
echo Para ejecutar la aplicación, usa:
echo   scripts\iniciar_app.bat
echo.
echo O ejecuta manualmente:
echo   venv\Scripts\python.exe main.py
echo.
pause

