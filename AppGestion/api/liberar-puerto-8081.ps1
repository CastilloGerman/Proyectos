# Libera el puerto 8081 matando los procesos que lo usan
# Uso: .\liberar-puerto-8081.ps1
# Ejecuta manualmente cuando el puerto quede bloqueado tras detener la API

$pids = @()
$netstat = netstat -ano 2>$null | Select-String ":8081" | Select-String "LISTENING"
foreach ($line in $netstat) {
    if ($line -match '\s+(\d+)\s*$') {
        $procId = [int]$matches[1]
        if ($procId -gt 0) { $pids += $procId }
    }
}
$pids = $pids | Select-Object -Unique

if ($pids.Count -eq 0) {
    Write-Host "Puerto 8081 libre."
    exit 0
}

foreach ($procId in $pids) {
    try {
        Stop-Process -Id $procId -Force -ErrorAction Stop
        Write-Host "Proceso $procId liberado (puerto 8081)"
    } catch {
        Write-Host "No se pudo detener proceso $procId (puede requerir ejecutar como administrador)"
    }
}
Start-Sleep -Seconds 2
Write-Host "Puerto 8081 liberado."
