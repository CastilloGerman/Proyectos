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

.PARAMETER CloudflareApiToken
  Opcional: API Token (mismo valor que CLOUDFLARE_API_TOKEN). Util si la variable de entorno esta corrupta o vacia en la sesion.

.EXAMPLE
  $env:CLOUDFLARE_API_TOKEN = '...'
  .\configure-cloudflare-noemiweb.ps1 -ApiCnameTarget 'api-xxxxx.up.railway.app' -AppCnameTarget 'app-xxxxx.up.railway.app'
#>
param(
    [Parameter(Mandatory = $true)]
    [string] $ApiCnameTarget,

    [Parameter(Mandatory = $true)]
    [string] $AppCnameTarget,

    [string] $ZoneName = 'noemiweb.com',

    [string] $CloudflareApiToken = ''
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

function Normalize-CfApiToken {
    param([string] $Raw)
    if ($null -eq $Raw) { return '' }
    $t = $Raw.Trim().Trim([char]0xFEFF)
    $t = $t -replace '[\r\n]', ''
    if ($t -match '^(?i)Bearer\s+') {
        $t = $t -replace '^(?i)Bearer\s+', ''
    }
    if (($t.Length -ge 2) -and (
            ($t.StartsWith("'") -and $t.EndsWith("'")) -or
            ($t.StartsWith('"') -and $t.EndsWith('"')))) {
        $t = $t.Substring(1, $t.Length - 2).Trim()
    }
    return $t.Trim()
}

$tokenSource = if (-not [string]::IsNullOrWhiteSpace($CloudflareApiToken)) { $CloudflareApiToken } else { $env:CLOUDFLARE_API_TOKEN }
$token = Normalize-CfApiToken $tokenSource
if ([string]::IsNullOrWhiteSpace($token)) {
    throw 'Define CLOUDFLARE_API_TOKEN o el parametro -CloudflareApiToken con un API Token (My Profile > API Tokens). No uses la Global API Key.'
}
if ($token.Length -lt 20) {
    throw 'El API Token parece incompleto (muy corto). Copialo entero desde Cloudflare.'
}
if ($token -match '\s') {
    throw 'El API Token no debe contener espacios. Vuelve a copiarlo o usa -CloudflareApiToken sin comillas raras.'
}

$base = 'https://api.cloudflare.com/client/v4'

# Cabecera Authorization: .NET puede rechazar o reformatear el token con AuthenticationHeaderValue;
# Cloudflare 6111 tambien aparece si el valor no es un API Token valido. Usamos TryAddWithoutValidation
# por peticion y desactivamos credenciales por defecto del handler.
function Invoke-CfApi {
    param(
        [string] $Method,
        [string] $Uri,
        [object] $Body = $null,
        [switch] $AllowUnauthorizedForVerify
    )

    $null = [System.Reflection.Assembly]::LoadWithPartialName('System.Net.Http')
    $handler = New-Object System.Net.Http.HttpClientHandler
    $handler.UseDefaultCredentials = $false
    $client = New-Object System.Net.Http.HttpClient($handler)
    try {
        $httpMethod = switch ($Method) {
            'Get' { [System.Net.Http.HttpMethod]::Get }
            'Post' { [System.Net.Http.HttpMethod]::Post }
            'Patch' { [System.Net.Http.HttpMethod]::Patch }
            default { throw "Método HTTP no soportado: $Method" }
        }
        $req = New-Object System.Net.Http.HttpRequestMessage($httpMethod, $Uri)
        $authValue = 'Bearer ' + $token
        $null = $req.Headers.TryAddWithoutValidation('Authorization', $authValue)
        $null = $req.Headers.TryAddWithoutValidation('Accept', 'application/json')
        if ($null -ne $Body) {
            $json = ($Body | ConvertTo-Json -Depth 8 -Compress)
            $req.Content = New-Object System.Net.Http.StringContent(
                $json,
                [System.Text.UTF8Encoding]::new($false),
                'application/json')
        }
        $response = $client.SendAsync($req).GetAwaiter().GetResult()

        $raw = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        if (-not $response.IsSuccessStatusCode) {
            if ($AllowUnauthorizedForVerify -and (
                    $response.StatusCode -eq [System.Net.HttpStatusCode]::Unauthorized -or
                    $response.StatusCode -eq [System.Net.HttpStatusCode]::Forbidden)) {
                return $null
            }
            Write-Host "Respuesta Cloudflare: $raw"
            if ($raw -match '"code"\s*:\s*6111|"Invalid format for Authorization header"') {
                Write-Host @"

Accion: Debe ser un API Token (My Profile > API Tokens), no la Global API Key ni un secreto de Workers.
Permisos tipicos: Zone DNS Edit, Zone Read, Zone Settings (SSL).
Prueba fuera de PowerShell (sustituye TU_TOKEN):
  curl.exe -sS -H "Authorization: Bearer TU_TOKEN" "https://api.cloudflare.com/client/v4/user/tokens/verify"
O pasa el token al script: -CloudflareApiToken '...'
"@
            }
            throw "Cloudflare HTTP $($response.StatusCode)"
        }

        $r = $raw | ConvertFrom-Json
        if (-not $r.success) {
            Write-Host "Respuesta Cloudflare: $raw"
            throw "Cloudflare API error: $($r.errors | ConvertTo-Json -Compress)"
        }
        return $r
    }
    finally {
        $client.Dispose()
        $handler.Dispose()
    }
}

# Opcional: tokens solo de zona suelen recibir 401/403 aqui; no implica fallo en el resto.
$verify = Invoke-CfApi -Method Get -Uri "$base/user/tokens/verify" -AllowUnauthorizedForVerify
if (($null -ne $verify) -and $verify.success) {
    Write-Host "Token verify OK (status=$($verify.result.status))."
    if ($verify.result.policies) {
        Write-Host ($verify.result.policies | ConvertTo-Json -Depth 6 -Compress)
    }
}
else {
    Write-Host '(Info) GET /user/tokens/verify no disponible con este token (401 habitual en tokens solo de zona). Continuando...'
}

$zoneId = $env:CLOUDFLARE_ZONE_ID
if ([string]::IsNullOrWhiteSpace($zoneId)) {
    $zoneQ = [uri]::EscapeDataString($ZoneName)
    $zr = Invoke-CfApi -Method Get -Uri "$base/zones?name=$zoneQ"
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
