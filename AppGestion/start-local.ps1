param(
    [switch]$DisableGemini
)

$ErrorActionPreference = 'Stop'
$appRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$apiPort = 8081
$frontendPort = 4200
$databasePort = 5432
$npmCommand = Get-Command npm.cmd -CommandType Application -ErrorAction SilentlyContinue
if (-not $npmCommand -and $env:LOCALAPPDATA) {
    $intellijNpm = Get-ChildItem -Path (Join-Path $env:LOCALAPPDATA 'JetBrains\*\acp-agents\.runtimes\node\*\npm.cmd') `
        -File -ErrorAction SilentlyContinue |
        Sort-Object -Property LastWriteTime -Descending |
        Select-Object -First 1
    if ($intellijNpm) {
        $npmCommand = [pscustomobject]@{ Source = $intellijNpm.FullName }
    }
}

function Test-TcpPort([int]$Port) {
    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $result = $client.BeginConnect('127.0.0.1', $Port, $null, $null)
        if (-not $result.AsyncWaitHandle.WaitOne(700)) { return $false }
        $client.EndConnect($result)
        return $true
    } catch {
        return $false
    } finally {
        $client.Dispose()
    }
}

function Read-SecretText([string]$Prompt) {
    $secureValue = Read-Host -Prompt $Prompt -AsSecureString
    if ($secureValue.Length -eq 0) {
        return 'postgres'
    }
    return [System.Net.NetworkCredential]::new('', $secureValue).Password
}

function Stop-AppListener([int]$Port) {
    $listeners = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    foreach ($listener in $listeners | Sort-Object -Property OwningProcess -Unique) {
        $processId = [int]$listener.OwningProcess
        $ownedByApp = $false
        for ($depth = 0; $depth -lt 10 -and $processId -gt 0; $depth++) {
            $process = Get-CimInstance Win32_Process -Filter "ProcessId = $processId" -ErrorAction SilentlyContinue
            if (-not $process) { break }
            $commandLine = [string]$process.CommandLine
            if ($commandLine.IndexOf($appRoot, [StringComparison]::OrdinalIgnoreCase) -ge 0) {
                $ownedByApp = $true
                break
            }
            $processId = [int]$process.ParentProcessId
        }
        if (-not $ownedByApp) {
            throw "El puerto $Port estÃ¡ ocupado por otro programa (PID $($listener.OwningProcess)); no lo detuve."
        }
        Write-Host "Deteniendo el proceso local anterior en el puerto $Port (PID $($listener.OwningProcess))."
        Stop-Process -Id $listener.OwningProcess -Force
    }

    $deadline = (Get-Date).AddSeconds(10)
    while ((Get-Date) -lt $deadline -and (Test-TcpPort $Port)) {
        Start-Sleep -Milliseconds 250
    }
    if (Test-TcpPort $Port) {
        throw "El puerto $Port sigue ocupado despuÃ©s de detener el proceso local anterior."
    }
}

function Start-LocalCommand(
    [string]$Name,
    [string]$Command,
    [string]$WorkingDirectory,
    [string]$StdoutPath,
    [string]$StderrPath
) {
    $process = Start-Process -FilePath $env:ComSpec `
        -ArgumentList @('/d', '/s', '/c', "`"$Command`"") `
        -WorkingDirectory $WorkingDirectory `
        -WindowStyle Hidden `
        -RedirectStandardOutput $StdoutPath `
        -RedirectStandardError $StderrPath `
        -PassThru
    Write-Host "$Name iniciado (PID $($process.Id))."
    return $process.Id
}

function Wait-ForLocalService(
    [string]$Name,
    [int]$Port,
    [int]$ProcessId,
    [string]$StdoutPath,
    [string]$StderrPath
) {
    $deadline = (Get-Date).AddSeconds(120)
    while ((Get-Date) -lt $deadline) {
        if (Test-TcpPort $Port) {
            Write-Host "$Name disponible en localhost:$Port."
            return
        }
        if (-not (Get-Process -Id $ProcessId -ErrorAction SilentlyContinue)) { break }
        Start-Sleep -Milliseconds 500
    }
    Write-Host "No se confirmÃ³ el arranque de $Name. Ãšltimas lÃ­neas de log:"
    foreach ($path in @($StdoutPath, $StderrPath)) {
        if (Test-Path $path) {
            Get-Content -Path $path -Tail 20 -ErrorAction SilentlyContinue
        }
    }
    throw "$Name no iniciÃ³ en localhost:$Port. Revisa los logs indicados."
}

Stop-AppListener $apiPort
Stop-AppListener $frontendPort
if (-not (Test-TcpPort $databasePort)) {
    $postgresService = Get-Service -Name 'postgresql*' -ErrorAction SilentlyContinue |
        Where-Object { $_.Status -eq 'Stopped' } |
        Select-Object -First 1
    if ($postgresService) {
        try {
            Start-Service -Name $postgresService.Name
        } catch {
            throw "PostgreSQL no estÃ¡ disponible en localhost:$databasePort y no se pudo iniciar el servicio '$($postgresService.Name)'. InÃ­cialo desde Servicios de Windows y vuelve a ejecutar el script."
        }
        $deadline = (Get-Date).AddSeconds(30)
        while ((Get-Date) -lt $deadline -and -not (Test-TcpPort $databasePort)) {
            Start-Sleep -Milliseconds 500
        }
    }
}
if (-not $npmCommand) {
    throw 'No se encontro npm.cmd. Instala Node.js o configura su carpeta en PATH.'
}
if (-not (Test-TcpPort $databasePort)) {
    throw "PostgreSQL no estÃ¡ escuchando en localhost:$databasePort. Inicia PostgreSQL y vuelve a ejecutar el script."
}

$dbPassword = [Environment]::GetEnvironmentVariable('DB_PASSWORD', 'Process')
if ([string]::IsNullOrWhiteSpace($dbPassword)) {
    $dbPassword = Read-SecretText 'ContraseÃ±a de PostgreSQL (Enter para postgres)'
}
$geminiKey = ''
if (-not $DisableGemini) {
    $geminiKey = [Environment]::GetEnvironmentVariable('GEMINI_API_KEY', 'Process')
    if ([string]::IsNullOrWhiteSpace($geminiKey)) {
        $geminiKey = Read-SecretText 'Clave nueva de Gemini (entrada oculta)'
    }
}

$envNames = @(
    'SPRING_PROFILES_ACTIVE', 'SPRING_DATASOURCE_URL', 'DB_USERNAME', 'DB_PASSWORD',
    'PGUSER', 'PGPASSWORD', 'PORT', 'APP_AI_GEMINI_ENABLED', 'GEMINI_API_KEY'
)
$previousEnvironment = @{}
foreach ($name in $envNames) {
    $previousEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

$apiLogs = Join-Path $appRoot 'api/target'
$frontendLogs = Join-Path $appRoot 'frontend/target'
New-Item -ItemType Directory -Force -Path $apiLogs, $frontendLogs | Out-Null

try {
    $env:SPRING_PROFILES_ACTIVE = 'local'
    $env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:$databasePort/appgestion"
    $env:DB_USERNAME = 'postgres'
    $env:DB_PASSWORD = $dbPassword
    $env:PGUSER = 'postgres'
    $env:PGPASSWORD = $dbPassword
    $env:PORT = "$apiPort"
    $env:APP_AI_GEMINI_ENABLED = [string](-not $DisableGemini)
    if ($DisableGemini) {
        Remove-Item Env:GEMINI_API_KEY -ErrorAction SilentlyContinue
    } else {
        $env:GEMINI_API_KEY = $geminiKey
    }

    $apiOut = Join-Path $apiLogs 'local-api.out.log'
    $apiErr = Join-Path $apiLogs 'local-api.err.log'
    $apiProcessId = Start-LocalCommand 'API' 'call mvnw.cmd -pl api spring-boot:run' $appRoot $apiOut $apiErr
    Wait-ForLocalService 'API' $apiPort $apiProcessId $apiOut $apiErr

    # The frontend does not need database credentials or the Gemini API key.
    Remove-Item Env:DB_PASSWORD, Env:PGPASSWORD, Env:GEMINI_API_KEY -ErrorAction SilentlyContinue
    $npmDirectory = Split-Path -Parent $npmCommand.Source
    $env:PATH = "$npmDirectory;$env:PATH"
    $frontendOut = Join-Path $frontendLogs 'local-frontend.out.log'
    $frontendErr = Join-Path $frontendLogs 'local-frontend.err.log'
    $frontendCommand = 'call "{0}" start' -f $npmCommand.Source
    $frontendProcessId = Start-LocalCommand 'Frontend' $frontendCommand (Join-Path $appRoot 'frontend') $frontendOut $frontendErr
    Wait-ForLocalService 'Frontend' $frontendPort $frontendProcessId $frontendOut $frontendErr
} finally {
    foreach ($name in $envNames) {
        [Environment]::SetEnvironmentVariable($name, $previousEnvironment[$name], 'Process')
    }
    $dbPassword = $null
    $geminiKey = $null
}

Write-Host ''
Write-Host 'Entorno local listo.'
Write-Host 'Frontend: http://localhost:4200'
Write-Host 'API:      http://localhost:8081'
Write-Host 'Logs:     api/target/local-api.*.log y frontend/target/local-frontend.*.log'


