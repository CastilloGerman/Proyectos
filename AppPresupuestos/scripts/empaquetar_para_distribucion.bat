@echo off
title Empaquetar para Distribución
cd /d "%~dp0\.."
echo ========================================
echo  Empaquetar para Distribución
echo ========================================
echo.

REM Crear carpeta de distribución
set DIST_FOLDER=distribucion
if exist "%DIST_FOLDER%" rmdir /s /q "%DIST_FOLDER%"
mkdir "%DIST_FOLDER%"

echo Creando ejecutable...
call scripts\crear_ejecutable.bat
if errorlevel 1 (
    echo ERROR: No se pudo crear el ejecutable.
    pause
    exit /b 1
)

echo.
echo Copiando archivos necesarios...
copy "dist\AppPresupuestos.exe" "%DIST_FOLDER%\" >nul

REM Copiar archivos de configuración si existen
if exist "config\config.json" copy "config\config.json" "%DIST_FOLDER%\" >nul
if exist "config\email_config.json" copy "config\email_config.json" "%DIST_FOLDER%\" >nul
if exist "config\plantilla_config.json" copy "config\plantilla_config.json" "%DIST_FOLDER%\" >nul

REM Crear README de instalación simple
echo Creando README de instalación...
(
echo INSTRUCCIONES DE INSTALACION
echo =============================
echo.
echo 1. Ejecuta AppPresupuestos.exe
echo.
echo 2. La aplicacion se iniciara automaticamente.
echo    No se requiere instalacion adicional.
echo.
echo 3. La base de datos se creara automaticamente
echo    en la primera ejecucion.
echo.
echo NOTAS:
echo - No se requiere Python ni ninguna dependencia.
echo - El ejecutable es standalone y funciona en cualquier Windows.
echo - Puedes copiar esta carpeta completa a otro equipo.
) > "%DIST_FOLDER%\LEEME.txt"

echo.
echo ========================================
echo  Empaquetado completado!
echo ========================================
echo.
echo Todo listo para distribuir en: %DIST_FOLDER%\
echo.
echo Contenido:
dir /b "%DIST_FOLDER%"
echo.
pause

