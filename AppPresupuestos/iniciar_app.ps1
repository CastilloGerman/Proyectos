# Script para iniciar la aplicación con entorno virtual
Write-Host "Activando entorno virtual..." -ForegroundColor Green
& ".\venv\Scripts\Activate.ps1"

Write-Host ""
Write-Host "Iniciando Sistema de Gestión de Presupuestos..." -ForegroundColor Green
Write-Host ""

python main.py

Write-Host ""
Write-Host "Presiona cualquier tecla para cerrar..." -ForegroundColor Yellow
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

