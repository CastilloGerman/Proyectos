@echo off
cd /d "%~dp0.."
REM Ejecuta la API. Opciones:
REM 1. Con variable: set DB_PASSWORD=tu_password antes de ejecutar
REM 2. Con application-local.yml: crea el archivo desde application-local.yml.example

if "%DB_PASSWORD%"=="" (
  echo Usando perfil 'local' - asegurate de tener api\src\main\resources\application-local.yml
  mvn spring-boot:run -pl api "-Dspring-boot.run.arguments=--spring.profiles.active=local"
) else (
  mvn spring-boot:run -pl api
)
