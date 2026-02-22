# Ejecuta la API con la contraseña de PostgreSQL
# Uso: .\iniciar-api.ps1
# La contraseña se pide al ejecutar (no se guarda en ningun archivo)

# Liberar puerto 8081 si está ocupado (p. ej. instancia anterior de la API)
$connections = Get-NetTCPConnection -LocalPort 8081 -ErrorAction SilentlyContinue | Where-Object { $_.OwningProcess -gt 0 }
$pids = $connections | Select-Object -ExpandProperty OwningProcess -Unique
foreach ($procId in $pids) {
    try {
        Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
        Write-Host "Proceso $procId liberado (puerto 8081)"
    } catch {}
}

$password = Read-Host "Contrasena de PostgreSQL (usuario postgres)" -AsSecureString
$env:DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($password))
$env:APP_SUBSCRIPTION_SKIP = "true"  # Omitir comprobación Stripe en desarrollo

Set-Location $PSScriptRoot\..
mvn spring-boot:run -pl api
