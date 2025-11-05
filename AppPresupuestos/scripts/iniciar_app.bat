@echo off
title Sistema de Gestión de Presupuestos
cd /d "%~dp0\.."
echo ========================================
echo  Sistema de Gestión de Presupuestos
echo ========================================
echo.
echo Activando entorno virtual...
call venv\Scripts\activate.bat
if errorlevel 1 (
    echo ERROR: No se pudo activar el entorno virtual
    echo Verifique que la carpeta 'venv' existe en este directorio
    pause
    exit /b 1
)
echo.
echo Entorno virtual activado correctamente
echo.
echo Iniciando aplicación...
echo.
venv\Scripts\python.exe main.py
if errorlevel 1 (
    echo.
    echo ERROR: No se pudo iniciar la aplicación
    echo Verifique que Python esté instalado y configurado correctamente
)
echo.
echo Presione cualquier tecla para cerrar...
pause >nul
