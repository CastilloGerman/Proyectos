@echo off
title Crear Ejecutable - Sistema de Presupuestos
cd /d "%~dp0\.."
echo ========================================
echo  Crear Ejecutable Standalone
echo ========================================
echo.

REM Verificar si PyInstaller está instalado
python -m pip show pyinstaller >nul 2>&1
if errorlevel 1 (
    echo PyInstaller no está instalado. Instalando...
    python -m pip install pyinstaller
    if errorlevel 1 (
        echo ERROR: No se pudo instalar PyInstaller.
        pause
        exit /b 1
    )
    echo PyInstaller instalado correctamente.
    echo.
)

REM Limpiar builds anteriores
echo Limpiando builds anteriores...
if exist "build" rmdir /s /q "build"
if exist "dist" rmdir /s /q "dist"
if exist "AppPresupuestos.spec" del /q "AppPresupuestos.spec"

echo.
echo Creando ejecutable...
echo Esto puede tardar varios minutos...
echo.

REM Crear ejecutable
pyinstaller --onefile ^
    --windowed ^
    --name "AppPresupuestos" ^
    --add-data "presupuestos;presupuestos" ^
    --hidden-import=matplotlib ^
    --hidden-import=matplotlib.backends.backend_tkagg ^
    --hidden-import=PIL ^
    --hidden-import=reportlab ^
    --collect-all matplotlib ^
    --collect-all tkinter ^
    main.py

if errorlevel 1 (
    echo.
    echo ERROR: No se pudo crear el ejecutable.
    echo Revisa los mensajes de error arriba.
    pause
    exit /b 1
)

echo.
echo ========================================
echo  Ejecutable creado exitosamente!
echo ========================================
echo.
echo El ejecutable está en: dist\AppPresupuestos.exe
echo.
echo IMPORTANTE: Copia estos archivos junto con el ejecutable:
echo   - config\config.json (si existe)
echo   - config\email_config.json (si existe)
echo   - config\plantilla_config.json (si existe)
echo.
echo Para distribuir, copia todo el contenido de la carpeta 'dist'
echo.
pause

