@echo off
cd /d "%~dp0.."

REM Liberar puerto 8081 si esta ocupado
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8081 ^| findstr LISTENING') do (
  if not "%%a"=="0" (
    taskkill /PID %%a /F >nul 2>&1
  )
)
timeout /t 2 /nobreak >nul

REM Omitir comprobacion Stripe en desarrollo
set APP_SUBSCRIPTION_SKIP=true

REM Ejecuta la API. Opciones:
REM 1. Con variable: set DB_PASSWORD=tu_password antes de ejecutar
REM 2. Con application-local.yml: crea el archivo desde application-local.yml.example

if "%DB_PASSWORD%"=="" (
  echo Usando perfil 'local' - asegurate de tener api\src\main\resources\application-local.yml
  mvn spring-boot:run -pl api "-Dspring-boot.run.arguments=--spring.profiles.active=local"
) else (
  mvn spring-boot:run -pl api
)
