# Ejecuta la API con la contraseña de PostgreSQL
# Uso: .\iniciar-api.ps1
# La contraseña se pide al ejecutar (no se guarda en ningun archivo)

# Liberar puerto 8081 si está ocupado (p. ej. instancia anterior de la API)
& "$PSScriptRoot\liberar-puerto-8081.ps1"

# Cargar variables de .env si existe (Stripe, etc.)
$envPath = if (Test-Path "$PSScriptRoot\.env") { "$PSScriptRoot\.env" } elseif (Test-Path "$PSScriptRoot\..\.env") { "$PSScriptRoot\..\.env" } else { $null }
if ($envPath) {
    Get-Content $envPath | ForEach-Object {
        if ($_ -match '^\s*([^#][^=]+)\s*=\s*(.*)$') {
            $key = $matches[1].Trim()
            $value = $matches[2].Trim()
            [Environment]::SetEnvironmentVariable($key, $value, 'Process')
        }
    }
}

$password = Read-Host "Contrasena de PostgreSQL (usuario postgres)" -AsSecureString
$env:DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($password))
if (-not $env:APP_SUBSCRIPTION_SKIP) { $env:APP_SUBSCRIPTION_SKIP = "true" }  # Omitir comprobación Stripe en desarrollo (o definir en .env)

Set-Location $PSScriptRoot\..
mvn spring-boot:run -pl api
