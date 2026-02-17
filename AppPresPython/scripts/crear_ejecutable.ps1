# Script para crear ejecutable con PyInstaller
# Cambiar al directorio del proyecto
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptPath
Set-Location $projectRoot

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Crear Ejecutable Standalone" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar si PyInstaller está instalado
$pyinstallerInstalled = python -m pip show pyinstaller 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "PyInstaller no está instalado. Instalando..." -ForegroundColor Yellow
    python -m pip install pyinstaller
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: No se pudo instalar PyInstaller." -ForegroundColor Red
        Read-Host "Presiona Enter para salir"
        exit 1
    }
    Write-Host "PyInstaller instalado correctamente." -ForegroundColor Green
    Write-Host ""
}

# Limpiar builds anteriores
Write-Host "Limpiando builds anteriores..." -ForegroundColor Yellow
if (Test-Path "build") { Remove-Item -Recurse -Force "build" }
if (Test-Path "dist") { Remove-Item -Recurse -Force "dist" }
if (Test-Path "AppPresupuestos.spec") { Remove-Item -Force "AppPresupuestos.spec" }

Write-Host ""
Write-Host "Creando ejecutable..." -ForegroundColor Yellow
Write-Host "Esto puede tardar varios minutos..." -ForegroundColor Gray
Write-Host ""

# Crear ejecutable
pyinstaller --onefile `
    --windowed `
    --name "AppPresupuestos" `
    --add-data "presupuestos;presupuestos" `
    --hidden-import=matplotlib `
    --hidden-import=matplotlib.backends.backend_tkagg `
    --hidden-import=PIL `
    --hidden-import=reportlab `
    --collect-all matplotlib `
    --collect-all tkinter `
    main.py

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "ERROR: No se pudo crear el ejecutable." -ForegroundColor Red
    Write-Host "Revisa los mensajes de error arriba." -ForegroundColor Yellow
    Read-Host "Presiona Enter para salir"
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host " Ejecutable creado exitosamente!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "El ejecutable está en: dist\AppPresupuestos.exe" -ForegroundColor Cyan
Write-Host ""
Write-Host "IMPORTANTE: Copia estos archivos junto con el ejecutable:" -ForegroundColor Yellow
Write-Host "  - config\config.json (si existe)" -ForegroundColor White
Write-Host "  - config\email_config.json (si existe)" -ForegroundColor White
Write-Host "  - config\plantilla_config.json (si existe)" -ForegroundColor White
Write-Host ""
Write-Host "Para distribuir, copia todo el contenido de la carpeta 'dist'" -ForegroundColor Cyan
Write-Host ""
Read-Host "Presiona Enter para cerrar"

