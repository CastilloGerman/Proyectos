# Script de instalación para PowerShell
# Cambiar al directorio del proyecto
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptPath
Set-Location $projectRoot

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Instalador del Sistema de Presupuestos" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar si Python está instalado
try {
    $pythonVersion = python --version 2>&1
    Write-Host "Python detectado: $pythonVersion" -ForegroundColor Green
} catch {
    Write-Host "ERROR: Python no está instalado en este equipo." -ForegroundColor Red
    Write-Host ""
    Write-Host "Por favor instala Python 3.7 o superior desde:" -ForegroundColor Yellow
    Write-Host "https://www.python.org/downloads/" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Asegúrate de marcar 'Add Python to PATH' durante la instalación." -ForegroundColor Yellow
    Read-Host "Presiona Enter para salir"
    exit 1
}

Write-Host ""

# Crear entorno virtual si no existe
if (-Not (Test-Path "venv")) {
    Write-Host "Creando entorno virtual..." -ForegroundColor Yellow
    python -m venv venv
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: No se pudo crear el entorno virtual." -ForegroundColor Red
        Read-Host "Presiona Enter para salir"
        exit 1
    }
    Write-Host "Entorno virtual creado." -ForegroundColor Green
    Write-Host ""
}

# Activar entorno virtual
Write-Host "Activando entorno virtual..." -ForegroundColor Yellow
& ".\venv\Scripts\Activate.ps1"

# Actualizar pip
Write-Host "Actualizando pip..." -ForegroundColor Yellow
python -m pip install --upgrade pip --quiet

# Instalar dependencias
Write-Host ""
Write-Host "Instalando dependencias..." -ForegroundColor Yellow
Write-Host "Esto puede tardar unos minutos..." -ForegroundColor Gray
python -m pip install -r requirements.txt

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "ERROR: No se pudieron instalar las dependencias." -ForegroundColor Red
    Write-Host "Verifica tu conexión a internet e intenta de nuevo." -ForegroundColor Yellow
    Read-Host "Presiona Enter para salir"
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host " Instalación completada exitosamente!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Para ejecutar la aplicación, usa:" -ForegroundColor Cyan
Write-Host "  .\scripts\iniciar_app.ps1" -ForegroundColor White
Write-Host ""
Write-Host "O ejecuta manualmente:" -ForegroundColor Cyan
Write-Host "  .\venv\Scripts\python.exe main.py" -ForegroundColor White
Write-Host ""
Read-Host "Presiona Enter para cerrar"

