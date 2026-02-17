# Script para iniciar la aplicación con entorno virtual
# Cambiar al directorio del proyecto
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptPath
Set-Location $projectRoot

Write-Host "Activando entorno virtual..." -ForegroundColor Green
& ".\venv\Scripts\Activate.ps1"

Write-Host ""
Write-Host "Iniciando Sistema de Gestión de Presupuestos..." -ForegroundColor Green
Write-Host ""

# Usar Python del venv directamente
& ".\venv\Scripts\python.exe" main.py

Write-Host ""
Write-Host "Presiona cualquier tecla para cerrar..." -ForegroundColor Yellow
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

