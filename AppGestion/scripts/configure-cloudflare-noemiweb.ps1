#Requires -Version 5.1
<#
.SYNOPSIS
  Aplica el plan Cloudflare para noemiweb.com: CNAMEs proxyados, SSL Full (strict), Always Use HTTPS.

.DESCRIPTION
  Requiere un API Token de Cloudflare con permisos: Zone.DNS Edit, Zone.Settings Edit, Zone.Zone Read.
  Crea o actualiza los registros CNAME api y app (proxied). Ajusta SSL a strict y always_use_https.

  Variables de entorno:
    CLOUDFLARE_API_TOKEN  (obligatorio)
    CLOUDFLARE_ZONE_ID    (opcional; si falta, se resuelve por ZoneName)

  Paso 0 del plan (dominios en Railway): debes copiar de Railway Settings → Networking los targets CNAME.

.PARAMETER ApiCnameTarget
  Target que muestra Railway para api.noemiweb.com (ej. xxxxx.up.railway.app).

.PARAMETER AppCnameTarget
  Target que muestra Railway para app.noemiweb.com.

.PARAMETER ZoneName
  Dominio raíz en Cloudflare (por defecto noemiweb.com).

.EXAMPLE
  $env:CLOUDFLARE_API_TOKEN = '...'
  .\configure-cloudflare-noemiweb.ps1 -ApiCnameTarget 'api-xxxxx.up.railway.app' -AppCnameTarget 'app-xxxxx.up.railway.app'
#>
param(
    [Parameter(Mandatory = $true)]
    [string] $ApiCnameTarget,

    [Parameter(Mandatory = $true)]
    [string] $AppCnameTarget,

    [string] $ZoneName = 'noemiweb.com'
)

function Assert-RealCnameTarget {
    param([string] $Value, [string] $ParamName)
    $t = $Value.Trim()
    if ($t -notmatch '\.') {
        throw "$ParamName debe ser un hostname real (ej. abc123.up.railway.app). Falta un punto en el nombre."
    }
    if ($t -match 'HOST_QUE|PEGA_AQUI|ejemplo\.com|\.\.\.') {
        throw "$ParamName parece un marcador de ejemplo, no el CNAME de Railway. En Railway → Settings → Networking → Custom domain copia el valor exacto del target (sin https://)."
    }
}

Assert-RealCnameTarget -Value $ApiCnameTarget -ParamName 'ApiCnameTarget'
Assert-RealCnameTarget -Value $AppCnameTarget -ParamName 'AppCnameTarget'

$ErrorActionPreference = 'Stop'
$token = $env:CLOUDFLARE_API_TOKEN
if ($null -ne $token) {
    $token = $token.Trim().Trim([char]0xFEFF)
}
if ([string]::IsNullOrWhiteSpace($token)) {
    throw 'Define CLOUDFLARE_API_TOKEN con un API Token de Cloudflare (Zone DNS + Zone Settings).'
}

$base = 'https://api.cloudflare.com/client/v4'
$headers = @{
    Authorization = "Bearer $token"
    'Content-Type' = 'application/json'
}

# Opcional: muchos tokens solo de zona devuelven 401 aquí (falta permiso a nivel usuario); no implica que falle el resto.
try {
    $verify = Invoke-RestMethod -Method Get -Uri "$base/user/tokens/verify" -Headers $headers
    if ($verify.success) {
        Write-Host "Token verify OK (status=$($verify.result.status))."
        if ($verify.result.policies) {
            Write-Host ($verify.result.policies | ConvertTo-Json -Depth 6 -Compress)
        }
    }
} catch {
    Write-Host "(Info) GET /user/tokens/verify no disponible con este token (401 habitual en tokens solo de zona). Continuando..."
}

function Invoke-CfApi {
    param([string]$Method, [string]$Uri, [object]$Body = $null)
    $params = @{ Method = $Method; Uri = $Uri; Headers = $headers }
    if ($null -ne $Body) {
        $params.Body = ($Body | ConvertTo-Json -Depth 8 -Compress)
    }
    $r = Invoke-RestMethod @params
    if (-not $r.success) {
        throw "Cloudflare API error: $($r.errors | ConvertTo-Json -Compress)"
    }
    return $r
}

$zoneId = $env:CLOUDFLARE_ZONE_ID
if ([string]::IsNullOrWhiteSpace($zoneId)) {
    $zr = Invoke-CfApi -Method Get -Uri "$base/zones?name=$ZoneName"
    if ($zr.result.Count -eq 0) {
        throw "No se encontró la zona '$ZoneName' en Cloudflare."
    }
    $zoneId = $zr.result[0].id
    Write-Host "Zona: $ZoneName  id=$zoneId"
} else {
    Write-Host "Zona id (env): $zoneId"
}

function Ensure-Cname {
    param(
        [string] $RecordName,
        [string] $Target
    )
    $fqdn = if ($RecordName -eq '@') { $ZoneName } else { "$RecordName.$ZoneName" }
    $nameQ = [uri]::EscapeDataString($fqdn)
    $list = Invoke-CfApi -Method Get -Uri "$base/zones/$zoneId/dns_records?type=CNAME&name=$nameQ"
    $payload = @{
        type    = 'CNAME'
        name    = $RecordName
        content = $Target.TrimEnd('.')
        proxied = $true
        ttl     = 1
    }
    if ($list.result.Count -gt 0) {
        $id = $list.result[0].id
        Write-Host "Actualizando CNAME $fqdn -> $($payload.content)"
        Invoke-CfApi -Method Patch -Uri "$base/zones/$zoneId/dns_records/$id" -Body $payload | Out-Null
    } else {
        Write-Host "Creando CNAME $fqdn -> $($payload.content)"
        Invoke-CfApi -Method Post -Uri "$base/zones/$zoneId/dns_records" -Body $payload | Out-Null
    }
}

Ensure-Cname -RecordName 'api' -Target $ApiCnameTarget
Ensure-Cname -RecordName 'app' -Target $AppCnameTarget

Write-Host 'SSL/TLS -> Full (strict)'
Invoke-CfApi -Method Patch -Uri "$base/zones/$zoneId/settings/ssl" -Body @{ value = 'strict' } | Out-Null

Write-Host 'Edge Certificates -> Always Use HTTPS: on'
Invoke-CfApi -Method Patch -Uri "$base/zones/$zoneId/settings/always_use_https" -Body @{ value = 'on' } | Out-Null

Write-Host 'Listo. Espera propagación DNS/TLS y verifica:'
Write-Host '  curl.exe -I https://api.noemiweb.com/actuator/health'
Write-Host '  curl.exe -I https://app.noemiweb.com'
