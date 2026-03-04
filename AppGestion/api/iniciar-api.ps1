# Ejecuta la API con el perfil 'local' (carga application-local.yml con DB y Stripe)
# Uso: .\iniciar-api.ps1

# Liberar puerto 8081 si está ocupado (p. ej. instancia anterior de la API)
& "$PSScriptRoot\liberar-puerto-8081.ps1"

Set-Location $PSScriptRoot\..
mvn spring-boot:run -pl api "-Dspring-boot.run.profiles=local"
