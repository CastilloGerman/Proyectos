# Ejecuta la API con la contraseña de PostgreSQL
# Uso: .\iniciar-api.ps1
# La contraseña se pide al ejecutar (no se guarda en ningun archivo)

$password = Read-Host "Contrasena de PostgreSQL (usuario postgres)" -AsSecureString
$env:DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($password))

Set-Location $PSScriptRoot\..
mvn spring-boot:run -pl api
