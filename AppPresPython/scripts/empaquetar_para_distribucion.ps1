# Script para empaquetar todo para distribución
# Cambiar al directorio del proyecto
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptPath
Set-Location $projectRoot

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Empaquetar para Distribución" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$distFolder = "distribucion"

# Crear carpeta de distribución
if (Test-Path $distFolder) {
    Remove-Item -Recurse -Force $distFolder
}
New-Item -ItemType Directory -Path $distFolder | Out-Null

Write-Host "Creando ejecutable..." -ForegroundColor Yellow
& ".\scripts\crear_ejecutable.ps1"
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: No se pudo crear el ejecutable." -ForegroundColor Red
    Read-Host "Presiona Enter para salir"
    exit 1
}

Write-Host ""
Write-Host "Copiando archivos necesarios..." -ForegroundColor Yellow

# Copiar ejecutable
Copy-Item "dist\AppPresupuestos.exe" -Destination $distFolder -Force

# Copiar archivos de configuración si existen
$configFiles = @("config\config.json", "config\email_config.json", "config\plantilla_config.json")
foreach ($file in $configFiles) {
    if (Test-Path $file) {
        $fileName = Split-Path -Leaf $file
        Copy-Item $file -Destination "$distFolder\$fileName" -Force
        Write-Host "  Copiado: $fileName" -ForegroundColor Gray
    }
}

# Crear README de instalación
Write-Host "Creando README de instalación..." -ForegroundColor Yellow
$readmeContent = @"
INSTRUCCIONES DE INSTALACION
=============================

1. Ejecuta AppPresupuestos.exe

2. La aplicación se iniciará automáticamente.
   No se requiere instalación adicional.

3. La base de datos se creará automáticamente
   en la primera ejecución.

NOTAS:
- No se requiere Python ni ninguna dependencia.
- El ejecutable es standalone y funciona en cualquier Windows.
- Puedes copiar esta carpeta completa a otro equipo.

ARCHIVOS INCLUIDOS:
- AppPresupuestos.exe: Ejecutable principal
- config.json: Configuración (si existe)
- email_config.json: Configuración de email (si existe)
- plantilla_config.json: Configuración de plantilla (si existe)
"@

$readmeContent | Out-File -FilePath "$distFolder\LEEME.txt" -Encoding UTF8

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host " Empaquetado completado!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Todo listo para distribuir en: $distFolder\" -ForegroundColor Cyan
Write-Host ""
Write-Host "Contenido:" -ForegroundColor Yellow
Get-ChildItem $distFolder | ForEach-Object {
    Write-Host "  - $($_.Name)" -ForegroundColor White
}
Write-Host ""
Read-Host "Presiona Enter para cerrar"

