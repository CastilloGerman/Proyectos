# Script de diagnóstico 403 - Ejecutar con: .\diagnostico-403.ps1
# Requiere: API corriendo en http://localhost:8081

Write-Host "=== DIAGNOSTICO 403 ===" -ForegroundColor Cyan
Write-Host ""

# 1. GET sin token
Write-Host "1. GET /api/presupuestos SIN token:" -ForegroundColor Yellow
try {
    $r = Invoke-WebRequest -Uri "http://localhost:8081/api/presupuestos" -Method GET -UseBasicParsing
    Write-Host "   Status: $($r.StatusCode) - OK" -ForegroundColor Green
} catch {
    Write-Host "   Status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    $body = $reader.ReadToEnd()
    Write-Host "   Body: $body"
}

# 2. Registrar usuario de prueba
Write-Host ""
Write-Host "2. Registrando usuario de prueba..." -ForegroundColor Yellow
$registerBody = '{"nombre":"Test Diag","email":"diagnostico' + (Get-Random -Maximum 9999) + '@test.com","password":"test123456"}'
try {
    $reg = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/register" -Method POST -Body $registerBody -ContentType "application/json"
    Write-Host "   Registro OK. Token obtenido." -ForegroundColor Green
    $token = $reg.token
} catch {
    Write-Host "   Registro fallo: $($_.Exception.Message)" -ForegroundColor Red
    $token = $null
}

# 3. GET con token (si tenemos)
if ($token) {
    Write-Host ""
    Write-Host "3. GET /api/presupuestos CON token:" -ForegroundColor Yellow
    try {
        $r2 = Invoke-WebRequest -Uri "http://localhost:8081/api/presupuestos" -Method GET -Headers @{ Authorization = "Bearer $token" } -UseBasicParsing
        Write-Host "   Status: $($r2.StatusCode) - OK" -ForegroundColor Green
    } catch {
        Write-Host "   Status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host "   Body: $($reader.ReadToEnd())"
    }
}

# 4. Login con usuario existente (si el registro fallo, intentar login)
if (-not $token) {
    Write-Host ""
    Write-Host "3. Intentando login (usa tus credenciales si las conoces)..." -ForegroundColor Yellow
    $loginBody = '{"email":"tu@email.com","password":"tu_password"}'
    Write-Host "   Ejecuta manualmente: Invoke-RestMethod -Uri 'http://localhost:8081/api/auth/login' -Method POST -Body '{""email"":""TU_EMAIL"",""password"":""TU_PASS""}' -ContentType 'application/json'"
}

Write-Host ""
Write-Host "=== FIN DIAGNOSTICO ===" -ForegroundColor Cyan
Write-Host "Revisa la consola de la API para ver los logs cuando ocurre el 403."
